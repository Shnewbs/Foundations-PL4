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
    private static long topologyBuilds;
    private static int cachedMaxNetwork;
    private record Group(List<Ref> parts,int hostCount,boolean redstone,List<Part.Link> targets,List<Ref> readers,TransferEngine.Plan transfers) {}
    public record Ref(HostEntity host,Part part) {
        public ServerLevel level(){return (ServerLevel)host.getLevel();}
        public Part.Link adjacent(){return new Part.Link(level().dimension().identifier().toString(),host.getBlockPos().relative(part.face),part.face.getOpposite(),null,null);}
    }
    public static void add(HostEntity h){if(LOADED.add(h))dirty=true;}
    public static void remove(HostEntity h){if(LOADED.remove(h))dirty=true;}
    public static void invalidate(Level l){if(!l.isClientSide())dirty=true;}
    public static long topologyBuildCount(){return topologyBuilds;}
    public static void stopped(ServerStoppedEvent e){
        EnergyReader.clear();EnergyPorts.clear();DataSampler.clearFilters();DisplayNetworks.clear();LOADED.clear();cachedRefs=List.of();cachedHosts=List.of();cachedGroups=List.of();cachedServer=null;dirty=true;deferDirtyRebuild=false;topologyBuilds=0;
    }
    public static ServerLevel level(MinecraftServer server,Part.Link link){
        Identifier id=Identifier.tryParse(link.dimension());return id==null?null:server.getLevel(ResourceKey.create(Registries.DIMENSION,id));
    }
    public static boolean loaded(MinecraftServer server,Part.Link link){ServerLevel l=level(server,link);return l!=null&&l.hasChunkAt(link.pos());}
    private static boolean loadedHost(HostEntity h,MinecraftServer s){
        return !h.isRemoved()&&h.getLevel() instanceof ServerLevel l&&l.getServer()==s&&l.hasChunkAt(h.getBlockPos());
    }
    public static List<Ref> all(MinecraftServer s){
        List<Ref> result=new ArrayList<>();
        for(HostEntity h:LOADED)if(loadedHost(h,s))h.parts.values().forEach(p->result.add(new Ref(h,p)));
        return result;
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
        Map<UUID,Ref> identities=new HashMap<>();
        Map<HostEntity,int[]> arms=new IdentityHashMap<>();
        for(int i=0;i<refs.size();i++) {
            Ref r=refs.get(i);index.put(r.part,i);identities.put(r.part.identity,r);
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
        for(var edge:topology.network())sets.union(edge.a(),edge.b());
        topology.cableArms().forEach((i,values)->arms.put(refs.get(i).host,values));
        Map<HostEntity,Integer> leads=new IdentityHashMap<>();
        for(int i:topology.externalLeads()){
            Ref r=refs.get(i);leads.merge(r.host,1<<r.part.slot(),(a,b)->a|b);
        }
        if(PLConfig.WIRELESS.get())for(Ref r:refs)if(r.part.kind.receiver())for(Part.Link link:r.part.links){
            Ref target=identities.get(link.part());
            if(target==null||!target.part.kind.emitter()||target.part.kind.redstone()!=r.part.kind.redstone()
                ||!Objects.equals(target.part.owner,r.part.owner))continue;
            if(!PLConfig.CROSS_DIMENSION.get()&&target.level()!=r.level())continue;
            sets.union(index.get(r.part),index.get(target.part));
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
            if(usable.contains(from)&&usable.contains(to))busReaders.get(to).add(refs.get(export.reader()));
        }
        Map<Part,List<Ref>> visible=new IdentityHashMap<>();
        topology.displayFeeds().forEach((display,source)->{
            Ref feed=refs.get(source);int bus=sets.find(source);
            if(usable.contains(bus))visible.put(refs.get(display).part,feed.part.kind.reader()?List.of(feed):List.copyOf(busReaders.get(bus)));
        });
        DisplayNetworks.rebuild(refs,visible);
        arms.forEach((host,values)->{host.setConnections(values);host.setExternalLeads(leads.getOrDefault(host,0));});
        cachedRefs=List.copyOf(refs);cachedHosts=List.copyOf(arms.keySet());cachedGroups=List.copyOf(complete);
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
    public static void ensureCurrent(MinecraftServer server){if(dirty||cachedServer!=server||cachedWireless!=PLConfig.WIRELESS.get()||cachedCrossDimension!=PLConfig.CROSS_DIMENSION.get()||cachedMaxNetwork!=PLConfig.MAX_NETWORK.get())rebuild(server);}
    public static void tick(ServerTickEvent.Post e){
        MinecraftServer server=e.getServer();
        if(cachedServer!=server||cachedWireless!=PLConfig.WIRELESS.get()||cachedCrossDimension!=PLConfig.CROSS_DIMENSION.get()||cachedMaxNetwork!=PLConfig.MAX_NETWORK.get())dirty=true;
        boolean sample=server.getTickCount()%PLConfig.TICK_RATE.get()==0;
        if(sample&&!dirty)for(HostEntity h:cachedHosts)if(!loadedHost(h,server)){dirty=true;break;}
        if(dirty&&deferDirtyRebuild){deferDirtyRebuild=false;return;}
        if(dirty)rebuild(server);
        if(!sample)return;
        for(Group group:cachedGroups) {
            if(group.hostCount>PLConfig.MAX_NETWORK.get()) {
                for(Ref r:group.parts){r.part.rows.clear();r.part.status="Network exceeds configured host limit";setSignal(r,0);}continue;
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
        for(Ref r:refs){
            Part p=r.part;
            if(p.kind.cable()||p.kind==Kind.NODE||p.kind==Kind.ARRAY)p.status=connectionStatus;
            if(p.kind==Kind.ENTITY_NODE&&p.links.isEmpty()){
                int count=r.level().getEntities(null,new net.minecraft.world.phys.AABB(r.host.getBlockPos()).inflate(PLConfig.ENTITY_RANGE.get())).size();
                p.rows.clear();p.rows.add(new Part.Row("entities","Nearby entities",count,0,""));
            }
            if(p.kind==Kind.SIGNALLER||p.kind==Kind.DATA_EMITTER){
                Ref reader=readers.stream().filter(x->p.selected.isEmpty()||x.part.identity.toString().equals(p.selected)||x.part.label.equals(p.selected)).findFirst().orElse(null);
                p.rows.clear(); if(reader!=null)p.rows.addAll(reader.part.rows);
                p.status=reader==null?"No selected reader":reader.part.title();
                if(p.kind==Kind.SIGNALLER){
                    Part.Row row=p.rows.stream().filter(a->p.metric.isEmpty()||a.key().equals(p.metric)).findFirst().orElse(null);
                    setSignal(r,row!=null&&compare(row.value(),p.threshold,p.comparison)?15:0);
                }
            }
            if(p.kind==Kind.CLOCK){
                p.ticks+=PLConfig.TICK_RATE.get(); long period=Math.max(PLConfig.TICK_RATE.get()*2,Math.min(24000,(long)p.threshold));
                setSignal(r,p.ticks%period<PLConfig.TICK_RATE.get()?15:0);
                p.rows.clear();p.rows.add(new Part.Row("time","World time",r.level().getDefaultClockTime()%24000,24000,"ticks"));p.status="Clock interval "+period+" ticks";
            }
            if(p.kind==Kind.NETWORK_READER)p.status="Network: "+hosts+" hosts / "+refs.size()+" components";
        }
        if(PLConfig.TRANSFERS.get())TransferEngine.run(server,transfers);
    }
    public static boolean compare(double a,double b,String op){return switch(op){case ">"->a>b;case "<"->a<b;case "<="->a<=b;case "="->Double.compare(a,b)==0;case "!="->Double.compare(a,b)!=0;default->a>=b;};}
    private static void setSignal(Ref r,int signal){if(r.part.signal!=signal){r.part.signal=signal;r.host.setChanged();r.level().updateNeighborsAt(r.host.getBlockPos(),r.host.getBlockState().getBlock());}}
}
