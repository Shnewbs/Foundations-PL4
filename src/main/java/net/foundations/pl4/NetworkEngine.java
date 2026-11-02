package net.foundations.pl4;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.LoggerFactory;

/** All capability access, sampling and transfers run on the server thread. Never force-load chunks. */
public final class NetworkEngine {
    private static final Set<HostEntity> LOADED=Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean dirty=true, cachedWireless, cachedCrossDimension, deferDirtyRebuild;
    private static MinecraftServer cachedServer;
    private static List<Ref> cachedRefs=List.of();
    private static List<HostEntity> cachedHosts=List.of();
    private static List<Group> cachedGroups=List.of();
    private static Map<Part,Group> cachedMembership=Map.of();
    private static Map<Part,TopologySnapshot> cachedTopology=Map.of();
    private static Map<Part,List<Ref>> cachedStorageNodes=Map.of();
    private static long topologyBuilds;
    private static int cachedMaxNetwork;
    private record Group(List<Ref> parts,int hostCount,boolean redstone,List<Part.Link> targets,List<Ref> readers,TransferEngine.Plan transfers) {}
    /** Immutable membership snapshot; live owner, identity, ports and links must still match. */
    private record TopologySnapshot(UUID owner,UUID identity,int blockedFaces,List<Part.Link> links,int hostPartCount) {
        static TopologySnapshot capture(Ref ref){
            Part p=ref.part;
            return new TopologySnapshot(p.owner,p.identity,p.blockedFaces,List.copyOf(p.links),ref.host.parts.size());
        }
        boolean matches(Ref ref){
            Part p=ref.part;
            return Objects.equals(owner,p.owner)&&Objects.equals(identity,p.identity)
                &&blockedFaces==p.blockedFaces&&links.equals(p.links)
                &&hostPartCount==ref.host.parts.size();
        }
    }
    public record Ref(HostEntity host,Part part) {
        public ServerLevel level(){return (ServerLevel)host.getLevel();}
        public Part.Link adjacent(){return new Part.Link(level().dimension().identifier().toString(),host.getBlockPos().relative(part.face),part.face.getOpposite(),null,null);}
    }
    public static void add(HostEntity h){if(LOADED.add(h))dirty=true;}
    public static void remove(HostEntity h){if(LOADED.remove(h))dirty=true;}
    public static void invalidate(Level l){if(!l.isClientSide())dirty=true;}
    public static long topologyBuildCount(){return topologyBuilds;}
    public static void stopped(ServerStoppedEvent e){
        EnergyReader.clear();EnergyPorts.clear();DataSampler.clearFilters();DisplayNetworks.clear();LOADED.clear();cachedRefs=List.of();cachedHosts=List.of();cachedGroups=List.of();cachedMembership=Map.of();cachedStorageNodes=Map.of();cachedTopology=Map.of();cachedServer=null;dirty=true;deferDirtyRebuild=false;topologyBuilds=0;
    }
    public static ServerLevel level(MinecraftServer server,Part.Link link){
        Identifier id=Identifier.tryParse(link.dimension());return id==null?null:server.getLevel(ResourceKey.create(Registries.DIMENSION,id));
    }
    public static boolean loaded(MinecraftServer server,Part.Link link){ServerLevel l=level(server,link);return l!=null&&l.hasChunkAt(link.pos());}
    private static boolean loadedHost(HostEntity h,MinecraftServer s){
        return !h.isRemoved()&&h.getLevel() instanceof ServerLevel l&&l.getServer()==s
            &&l.hasChunkAt(h.getBlockPos())&&l.getBlockEntity(h.getBlockPos())==h;
    }
    public static List<Ref> all(MinecraftServer s){
        List<Ref> result=new ArrayList<>();
        for(HostEntity h:LOADED)if(loadedHost(h,s))h.parts.values().forEach(p->result.add(new Ref(h,p)));
        return result;
    }
    /** Physical contact and visual telemetry must not cross owner boundaries. */
    private static boolean sharedOwner(Ref source,Ref destination){
        return source.part.owner!=null&&source.part.owner.equals(destination.part.owner);
    }
    /** A persisted wireless link is valid only for the exact live emitter it names. */
    private static boolean matchesWirelessTarget(Part.Link link,Ref target){
        return link.dimension().equals(target.level().dimension().identifier().toString())
            &&link.pos().equals(target.host.getBlockPos())&&link.side()==target.part.face;
    }
    /** Server-only loaded-part graph with typed NETWORK inputs and separate VISUAL exports. */
    public static void rebuild(MinecraftServer server){
        List<Ref> refs=new ArrayList<>();
        for(Iterator<HostEntity> iterator=LOADED.iterator();iterator.hasNext();){
            HostEntity host=iterator.next();
            if(!loadedHost(host,server)){iterator.remove();continue;}
            host.parts.values().forEach(part->refs.add(new Ref(host,part)));
        }
        refs.sort(Comparator.comparing((Ref r)->r.level().dimension().identifier().toString())
            .thenComparingLong(r->r.host.getBlockPos().asLong()).thenComparingInt(r->r.part.slot()));
        var sets=new net.foundations.pl4.core.DisjointSets(refs.size());
        Map<Part,Integer> index=new IdentityHashMap<>();
        // Only emitters can be wireless targets. Avoid indexing every reader, cable and
        // node (and allocating UUID buckets) when rebuilding large or wireless-off graphs.
        boolean wirelessEnabled=PLConfig.WIRELESS.get();
        Map<UUID,List<Ref>> identities=new HashMap<>();
        Map<HostEntity,int[]> arms=new IdentityHashMap<>();
        for(int i=0;i<refs.size();i++) {
            Ref r=refs.get(i);index.put(r.part,i);
            if(wirelessEnabled&&r.part.kind.emitter()&&r.part.owner!=null&&r.part.identity!=null)
                identities.computeIfAbsent(r.part.identity,k->new ArrayList<>()).add(r);
            arms.computeIfAbsent(r.host,k->new int[6]);
        }
        List<net.foundations.pl4.core.MultipartTopology.Node> nodes=new ArrayList<>();
        for(int i=0;i<refs.size();i++){
            Ref r=refs.get(i);BlockPos p=r.host.getBlockPos();
            nodes.add(new net.foundations.pl4.core.MultipartTopology.Node(i,
                new net.foundations.pl4.core.MultipartTopology.Cell(r.level().dimension().identifier().toString(),p.getX(),p.getY(),p.getZ()),
                r.part.kind,r.part.face.ordinal(),r.part.blockedFaces));
        }
        var topology=net.foundations.pl4.core.MultipartTopology.plan(nodes);
        // Physical contact never grants a network edge across independent owners.
        // The stale cache refresh must keep a valid local Node when a remote owner
        // changes during a provider callback without notifying the graph.
        for(var edge:topology.network()){
            Ref a=refs.get(edge.a()), b=refs.get(edge.b());
            if(sharedOwner(a,b))
                sets.union(edge.a(),edge.b());
        }
        topology.cableArms().forEach((i,values)->arms.put(refs.get(i).host,values));
        Map<HostEntity,Integer> leads=new IdentityHashMap<>();
        for(int i:topology.externalLeads()){
            Ref r=refs.get(i);leads.merge(r.host,1<<r.part.slot(),(a,b)->a|b);
        }
        if(wirelessEnabled)for(Ref r:refs)if(r.part.kind.receiver()&&r.part.owner!=null)for(Part.Link link:r.part.links){
            // Duplicate persisted emitter UUIDs must not shadow a valid coordinate-matched target.
            for(Ref target:identities.getOrDefault(link.part(),List.of())){
                if(!target.part.kind.emitter()||target.part.kind.redstone()!=r.part.kind.redstone()
                    ||!sharedOwner(r,target)||!matchesWirelessTarget(link,target))continue;
                if(!PLConfig.CROSS_DIMENSION.get()&&target.level()!=r.level())continue;
                sets.union(index.get(r.part),index.get(target.part));
                break;
            }
        }
        Map<Integer,List<Ref>> groups=new LinkedHashMap<>();
        for(int i=0;i<refs.size();i++)groups.computeIfAbsent(sets.find(i),k->new ArrayList<>()).add(refs.get(i));
        List<Group> complete=new ArrayList<>();
        for(List<Ref> group:groups.values()) {
            Set<HostEntity> hosts=Collections.newSetFromMap(new IdentityHashMap<>());group.forEach(r->hosts.add(r.host));
            List<Ref> ordered=new ArrayList<>(group);
            ordered.sort(Comparator.<Ref>comparingInt(r->r.part.priority).reversed()
                .thenComparing(r->r.host.getBlockPos().asLong()).thenComparing(r->r.part.slot()));
            LinkedHashSet<Part.Link> uniqueTargets=new LinkedHashSet<>();
            for(Ref r:ordered){
                if(r.part.kind==Kind.NODE||r.part.kind==Kind.TRANSFER_NODE)uniqueTargets.add(r.adjacent());
                if(r.part.kind==Kind.ARRAY||r.part.kind==Kind.ENTITY_NODE)uniqueTargets.addAll(r.part.links);
            }
            List<Ref> groupReaders=ordered.stream().filter(r->r.part.kind.reader()).toList();
            complete.add(new Group(List.copyOf(ordered),hosts.size(),ordered.getFirst().part.kind.redstone(),
                List.copyOf(uniqueTargets),groupReaders,TransferEngine.prepare(ordered)));
        }
        // A visual reader export adds telemetry visibility to the destination cable bus,
        // NEVER an edge to its machine/transfer network. Wireless unions are already resolved.
        Map<Integer,Set<Ref>> busReaders=new HashMap<>();Set<Integer> usable=new HashSet<>();
        for(Group group:complete)if(group.hostCount<=PLConfig.MAX_NETWORK.get()){
            int id=sets.find(index.get(group.parts.getFirst().part));usable.add(id);
            Set<Ref> readers=busReaders.computeIfAbsent(id,k->new LinkedHashSet<>());
            for(Ref r:group.parts)if(r.part.kind.reader())readers.add(r);
        }
        for(var export:topology.visual()){
            int from=sets.find(export.reader()),to=sets.find(export.cable());
            if(usable.contains(from)&&usable.contains(to)
                &&sharedOwner(refs.get(export.reader()),refs.get(export.cable())))
                busReaders.get(to).add(refs.get(export.reader()));
        }
        Map<Part,List<Ref>> visible=new IdentityHashMap<>();
        topology.displayFeeds().forEach((display,source)->{
            Ref feed=refs.get(source);int bus=sets.find(source);
            if(usable.contains(bus)&&sharedOwner(refs.get(display),feed))
                visible.put(refs.get(display).part,feed.part.kind.reader()?List.of(feed):List.copyOf(busReaders.get(bus)));
        });
        DisplayNetworks.rebuild(refs,visible);
        arms.forEach((host,values)->{host.setConnections(values);host.setExternalLeads(leads.getOrDefault(host,0));});
        Map<Part,Group> membership=new IdentityHashMap<>();
        Map<Part,List<Ref>> storageNodes=new IdentityHashMap<>();
        for(Group group:complete){
            List<Ref> storageRefs=group.parts.stream().filter(r->r.part.kind==Kind.NODE||r.part.kind==Kind.TRANSFER_NODE).toList();
            for(Ref r:group.parts){membership.put(r.part,group);storageNodes.put(r.part,storageRefs);}
        }
        cachedRefs=List.copyOf(refs);cachedHosts=List.copyOf(arms.keySet());cachedGroups=List.copyOf(complete);
        Map<Part,TopologySnapshot> topologySnapshots=new IdentityHashMap<>();
        for(Ref ref:refs)topologySnapshots.put(ref.part,TopologySnapshot.capture(ref));
        cachedMembership=membership;cachedStorageNodes=storageNodes;cachedTopology=topologySnapshots;
        cachedServer=server;cachedWireless=PLConfig.WIRELESS.get();cachedCrossDimension=PLConfig.CROSS_DIMENSION.get();cachedMaxNetwork=PLConfig.MAX_NETWORK.get();dirty=false;topologyBuilds++;
        deferDirtyRebuild=false;
        // Send geometry now, not after the next (potentially very slow) sampling interval.
        cachedHosts.forEach(HostEntity::syncIfChanged);
    }
    /** Publish exact local cable geometry now; defer the expensive global graph rebuild by one tick. */
    public static void refreshCableGeometry(HostEntity anchor){
        if(!(anchor.getLevel() instanceof ServerLevel))return;
        CableGeometry.refresh(anchor);
        deferDirtyRebuild=true;
    }
    /** Cached reader targets are not authorization after host, part, cable or owner revocation.
     * Revalidate loaded physical membership and rebuild once before exporting target positions. */
    public static List<Part.Link> targetsFor(MinecraftServer server,Part reader){
        ensureCurrent(server);
        Group group=cachedMembership.get(reader);
        if(group==null||group.redstone||group.hostCount>PLConfig.MAX_NETWORK.get()||!reader.kind.reader())
            return List.of();
        if(!currentOwnedGroup(server,reader,group)){
            dirty=true;
            rebuild(server);
            group=cachedMembership.get(reader);
            if(group==null||group.redstone||group.hostCount>PLConfig.MAX_NETWORK.get()
                ||!currentOwnedGroup(server,reader,group))return List.of();
        }
        return group.targets;
    }
    /** No stale loaded or newly foreign part can remain a cached inventory alias. */
    private static boolean currentOwnedGroup(MinecraftServer server,Part anchor,Group group){
        if(anchor.owner==null)return false;
        boolean attached=false;
        // One host can carry multiple parts. Verify the live block entity once per
        // host while still validating every part's owner, identity and attachment.
        Set<HostEntity> verifiedHosts=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Ref ref:group.parts){
            TopologySnapshot snapshot=cachedTopology.get(ref.part);
            if(snapshot==null||!snapshot.matches(ref)
                ||(verifiedHosts.add(ref.host)&&!loadedHost(ref.host,server))
                ||ref.host.parts.get(ref.part.slot())!=ref.part
                ||!anchor.owner.equals(ref.part.owner))return false;
            if(ref.part==anchor)attached=true;
        }
        return attached;
    }
    /** Physical data network only: visual reader exports never grant inventory access. */
    public static List<Ref> storageNodes(MinecraftServer server,Part anchor){
        ensureCurrent(server);
        Group group=cachedMembership.get(anchor);
        if(group==null||group.redstone||group.hostCount>PLConfig.MAX_NETWORK.get())return List.of();
        if(!currentOwnedGroup(server,anchor,group)){
            dirty=true;
            rebuild(server);
            group=cachedMembership.get(anchor);
            if(group==null||group.redstone||group.hostCount>PLConfig.MAX_NETWORK.get()
                ||!currentOwnedGroup(server,anchor,group))return List.of();
        }
        return cachedStorageNodes.getOrDefault(anchor,List.of());
    }
    public static void ensureCurrent(MinecraftServer server){if(dirty||cachedServer!=server||cachedWireless!=PLConfig.WIRELESS.get()||cachedCrossDimension!=PLConfig.CROSS_DIMENSION.get()||cachedMaxNetwork!=PLConfig.MAX_NETWORK.get())rebuild(server);}

    /**
     * Interactive display reads must not serialize a stale visual reader alias before
     * the next sampling tick. This full snapshot check is only on UI read/edit paths,
     * never in the high-frequency transfer or cable sampling loop.
     */
    public static void ensureLiveDisplayTopology(MinecraftServer server){
        long before=topologyBuilds;
        ensureCurrent(server);
        if(topologyBuilds!=before)DisplayNetworks.sample();
        // A multipart host needs only one live block-entity lookup per UI preflight.
        Set<HostEntity> verifiedHosts=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Ref ref:cachedRefs){
            TopologySnapshot snapshot=cachedTopology.get(ref.part);
            if(snapshot==null||!snapshot.matches(ref)
                ||(verifiedHosts.add(ref.host)&&!loadedHost(ref.host,server))
                ||ref.host.parts.get(ref.part.slot())!=ref.part){
                dirty=true;
                rebuild(server);
                // Clear stale rows, reader choices and element bindings before packet serialization.
                DisplayNetworks.sample();
                return;
            }
        }
    }
    public static void tick(ServerTickEvent.Post e){
        MinecraftServer server=e.getServer();
        if(cachedServer!=server||cachedWireless!=PLConfig.WIRELESS.get()||cachedCrossDimension!=PLConfig.CROSS_DIMENSION.get()||cachedMaxNetwork!=PLConfig.MAX_NETWORK.get())dirty=true;
        boolean sample=server.getTickCount()%PLConfig.TICK_RATE.get()==0;
        if(sample&&!dirty)for(HostEntity h:cachedHosts)if(!loadedHost(h,server)){dirty=true;break;}
        // Unannounced part additions/removals, owner, port, identity or link mutations cannot keep a
        // stale graph alive for another sampling cycle.
        if(sample&&!dirty)for(Ref ref:cachedRefs){
            TopologySnapshot snapshot=cachedTopology.get(ref.part);
            if(snapshot==null||!snapshot.matches(ref)
                ||ref.host.parts.get(ref.part.slot())!=ref.part){dirty=true;break;}
        }
        if(dirty&&deferDirtyRebuild){deferDirtyRebuild=false;return;}
        if(dirty)rebuild(server);
        if(!sample)return;
        for(Group group:cachedGroups) {
            if(group.hostCount>PLConfig.MAX_NETWORK.get()) {
                for(Ref r:group.parts){r.part.rows.clear();r.part.targetChoices.clear();r.part.targetCount=0;r.part.targetLabel="Network exceeds configured host limit";r.part.status="Network exceeds configured host limit";setSignal(r,0);}continue;
            }
            try { process(server,group.parts,group.hostCount,group.redstone,group.targets,group.readers,group.transfers); }
            catch(RuntimeException ex) {
                LoggerFactory.getLogger("FoundationsPL4").error("Network operation failed at {}",group.parts.getFirst().host.getBlockPos(),ex);
                for(Ref r:group.parts){r.part.rows.clear();r.part.status="Provider error; check server log";setSignal(r,0);}
            }
        }
        DisplayNetworks.sample();
        cachedHosts.forEach(HostEntity::syncIfChanged);
    }
    private static void process(MinecraftServer server,List<Ref> refs,int hosts,boolean redstone,List<Part.Link> targets,List<Ref> readers,TransferEngine.Plan transfers){
        if(redstone){
            int signal=0;
            for(Ref r:refs)if(r.part.kind==Kind.REDSTONE_NODE||r.part.kind==Kind.REDSTONE_EMITTER)signal=Math.max(signal,r.level().getBestNeighborSignal(r.host.getBlockPos().relative(r.part.face)));
            String status="Connected: "+hosts+" hosts";
            for(Ref r:refs){r.part.rows.clear();r.part.rows.add(new Part.Row("signal","Redstone",signal,15,""));r.part.status=status;if(r.part.kind==Kind.REDSTONE_RECEIVER)setSignal(r,signal);else if(r.part.kind==Kind.REDSTONE_CABLE)r.part.signal=signal;}
            return;
        }
        for(Ref r:readers){
            r.part.rows.clear();
            ReaderChannels.refresh(server,r.part,targets);
            List<Part.Link> selectedTargets=ReaderChannels.select(r.part,targets);
            r.part.rows.addAll(DataSampler.sample(server,r,selectedTargets,hosts));
            if(r.part.kind!=Kind.ENERGY_READER)r.part.status=targets.isEmpty()?"No node connections":"Connected: "+targets.size()+" targets / "+hosts+" hosts";
            if(!r.part.targetChannel.isEmpty()){
                if(selectedTargets.isEmpty())r.part.status="Selected target disconnected";
                else if(!ReaderChannels.available(server,selectedTargets.getFirst()))r.part.status="Selected target unloaded";
                else r.part.status="Channel: "+ReaderChannels.label(r.part)+" · "+r.part.status;
            }
        }
        String connectionStatus="Connected: "+hosts+" hosts / "+refs.size()+" parts";
        // Most networks have no selected emitter/signaller. Build a first-match reader
        // lookup only when one needs it, rather than scanning every reader per device.
        Map<String,Ref> readerLookup=null;
        for(Ref r:refs){
            Part p=r.part;
            if(p.kind.cable()||p.kind==Kind.ARRAY)p.status=connectionStatus;
            if(p.kind==Kind.NODE)p.status=WirelessItemTransfers.available(p)?connectionStatus:"Item storage paused; inspect node escrow";
            if(p.kind==Kind.ENTITY_NODE&&p.links.isEmpty()){
                int count=r.level().getEntities(null,new net.minecraft.world.phys.AABB(r.host.getBlockPos()).inflate(PLConfig.ENTITY_RANGE.get())).size();
                p.rows.clear();p.rows.add(new Part.Row("entities","Nearby entities",count,0,""));
            }
            if(p.kind==Kind.SIGNALLER||p.kind==Kind.DATA_EMITTER){
                Ref reader;
                if(p.selected.isEmpty())reader=readers.isEmpty()?null:readers.getFirst();
                else{
                    if(readerLookup==null){
                        readerLookup=new HashMap<>();
                        // In insertion order: first reader wins even when its label
                        // collides with a later reader's UUID or duplicate label.
                        for(Ref candidate:readers){
                            readerLookup.putIfAbsent(candidate.part.identity.toString(),candidate);
                            readerLookup.putIfAbsent(candidate.part.label,candidate);
                        }
                    }
                    reader=readerLookup.get(p.selected);
                }
                p.rows.clear(); if(reader!=null)p.rows.addAll(reader.part.rows);
                p.status=reader==null?"No selected reader":reader.part.title();
                if(p.kind==Kind.SIGNALLER){
                    setSignal(r,SignallerLogic.evaluate(p,readers));
                    if(!p.statements.isEmpty())p.status=(p.statementsAll?"ALL":"ANY")+" of "+p.statements.size()+" statements · output "+p.signal;

                }
            }
            if(p.kind==Kind.CLOCK){
                if(!p.clockPaused)p.ticks+=PLConfig.TICK_RATE.get();long period=net.foundations.pl4.core.ClockRules.period(p.threshold,PLConfig.TICK_RATE.get());
                setSignal(r,net.foundations.pl4.core.ClockRules.high(p.ticks,p.threshold,PLConfig.TICK_RATE.get(),p.clockPulse,p.clockPhase,p.clockPaused)?15:0);
                p.rows.clear();p.rows.add(new Part.Row("time","World time",r.level().getDefaultClockTime()%24000,24000,"ticks"));p.status=(p.clockPaused?"Paused · ":"")+"Clock interval "+period+" ticks · pulse "+(p.clockPulse==0?PLConfig.TICK_RATE.get():Math.min(period,p.clockPulse))+" · phase "+p.clockPhase;
            }
            if(p.kind==Kind.NETWORK_READER)p.status="Network: "+hosts+" hosts / "+refs.size()+" components";
        }
        if(PLConfig.TRANSFERS.get())TransferEngine.run(server,transfers);
    }
    public static boolean compare(double a,double b,String op){return switch(op){case ">"->a>b;case "<"->a<b;case "<="->a<=b;case "="->Double.compare(a,b)==0;case "!="->Double.compare(a,b)!=0;default->a>=b;};}
    private static void setSignal(Ref r,int signal){if(r.part.signal!=signal){r.part.signal=signal;r.host.setChanged();r.level().updateNeighborsAt(r.host.getBlockPos(),r.host.getBlockState().getBlock());}}
}
