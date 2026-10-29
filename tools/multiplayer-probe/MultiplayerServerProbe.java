package net.foundations.pl4;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Disposable loopback-only server fixture; never included in a published artifact. */
@EventBusSubscriber(modid=FoundationsPL4.ID,value=Dist.DEDICATED_SERVER)
public final class MultiplayerServerProbe {
    private static final Set<String> NAMES=Set.of("PL4ProbeA","PL4ProbeB"),ready=new HashSet<>(),sent=new HashSet<>();
    private static HostEntity host;private static Part node;private static BarrelBlockEntity barrel;private static int verifyAt=-1,stopAt=-1;

    /** Independent installed-binary seven-centre cable route, never an explicit Ref transfer. */
    private static final BlockPos[] CABLE_CELLS = {
        new BlockPos(0,80,5),new BlockPos(0,80,6),new BlockPos(1,80,6),
        new BlockPos(2,80,6),new BlockPos(3,80,6),new BlockPos(4,80,6),
        new BlockPos(4,80,5)
    };
    private static final HostEntity[] cableHosts=new HostEntity[7];
    private static final Part[] cables=new Part[7], cableEndpoints=new Part[2];
    private static final BarrelBlockEntity[] cableBarrels=new BarrelBlockEntity[2];
    private static final int CABLE_EAST_BIT=1<<Direction.EAST.ordinal();
    private static int cableStage, cableStageTick, cableSourceBefore, cableSinkBefore, cableSeveredTicks;
    private static Map<String,Object> cableReceipt;
    private static List<String> cableIdentities;
    private static final boolean RESTORE="restore".equals(System.getenv("PL4_PROBE_PHASE"));
    private static UUID uuid(String name){return UUID.nameUUIDFromBytes(("OfflinePlayer:"+name).getBytes(StandardCharsets.UTF_8));}
    @SubscribeEvent public static void started(ServerStartedEvent event){
        var level=event.getServer().overworld();
        if(RESTORE){
            host=(HostEntity)level.getBlockEntity(new BlockPos(0,80,0));barrel=(BarrelBlockEntity)level.getBlockEntity(new BlockPos(1,80,0));
            if(host==null||barrel==null)throw new IllegalStateException("Saved probe world lost its native host/inventory");
            node=host.parts.get(Direction.EAST.ordinal());
            try{if(node==null||!node.owner.equals(uuid("PL4ProbeA"))||!node.identity.toString().equals(Files.readString(Path.of("probe-node-identity.txt")).trim()))throw new IllegalStateException("Saved node identity/owner did not migrate");}
            catch(java.io.IOException failure){throw new IllegalStateException(failure);}
            NetworkEngine.add(host);NetworkEngine.rebuild(event.getServer());return;
        }
        for(int x=-4;x<=4;x++)for(int z=-5;z<=4;z++)level.setBlock(new BlockPos(x,79,z),Blocks.IRON_BLOCK.defaultBlockState(),2);
        BlockPos at=new BlockPos(0,80,0);level.setBlock(at,FoundationsPL4.HOST.get().defaultBlockState(),2);host=(HostEntity)level.getBlockEntity(at);
        node=new Part(Kind.NODE,Direction.EAST,uuid("PL4ProbeA"));host.parts.put(node.slot(),node);host.parts.put(6,new Part(Kind.DATA_CABLE,Direction.DOWN,node.owner));host.changed();NetworkEngine.add(host);
        level.setBlock(at.east(),Blocks.BARREL.defaultBlockState(),2);barrel=(BarrelBlockEntity)level.getBlockEntity(at.east());
        barrel.setItem(0,new ItemStack(Items.DIAMOND,64));barrel.setItem(1,new ItemStack(Items.DIAMOND,64));
        seedCableRoute(level);level.setDefaultSpawnPos(new BlockPos(0,80,-4),0);NetworkEngine.rebuild(event.getServer());
        try{Files.writeString(Path.of("probe-node-identity.txt"),node.identity.toString()+"\n");}catch(java.io.IOException failure){throw new IllegalStateException(failure);}
    }

    private static int barrelDiamonds(BarrelBlockEntity barrel){
        int n=0;
        for(int i=0;i<barrel.getContainerSize();i++){
            ItemStack item=barrel.getItem(i);
            if(!item.isEmpty()&&!item.is(Items.DIAMOND))throw new IllegalStateException("Foreign item in native transfer barrel");
            n+=item.getCount();
        }
        return n;
    }
    private static void seedCableRoute(net.minecraft.server.level.ServerLevel level){
        for(int i=0;i<CABLE_CELLS.length;i++){
            BlockPos pos=CABLE_CELLS[i];
            level.setBlock(pos.below(),Blocks.IRON_BLOCK.defaultBlockState(),3);
            if(!level.setBlock(pos,FoundationsPL4.HOST.get().defaultBlockState(),3))
                throw new IllegalStateException("Native cable fixture placement failed");
            HostEntity h=(HostEntity)level.getBlockEntity(pos);
            if(h==null)throw new IllegalStateException("Native cable host missing");
            Part c=new Part(Kind.DATA_CABLE,Direction.DOWN,uuid("PL4ProbeA"));
            if(!HostBlock.canAdd(h,c))throw new IllegalStateException("Native centre cable overlap");
            h.parts.put(6,c);
            if(i==0||i==6){
                Part endpoint=new Part(Kind.TRANSFER_NODE,Direction.EAST,uuid("PL4ProbeA"));
                endpoint.transferMode=0;endpoint.items=false;endpoint.fluids=false;endpoint.energy=false;
                if(!HostBlock.canAdd(h,endpoint))throw new IllegalStateException("Native transfer endpoint overlap");
                h.parts.put(5,endpoint);
                if(!level.setBlock(pos.east(),Blocks.BARREL.defaultBlockState(),3))
                    throw new IllegalStateException("Native transfer barrel placement failed");
                BarrelBlockEntity inventory=(BarrelBlockEntity)level.getBlockEntity(pos.east());
                if(inventory==null)throw new IllegalStateException("Native transfer barrel block entity missing");
                if(i==0){
                    for(int slot=0;slot<27;slot++)inventory.setItem(slot,new ItemStack(Items.DIAMOND,64));
                    inventory.setChanged();
                }
            }
            h.changed(); // onLoad registers the real host; no probe-side fake graph registration
        }
    }
    /** Called only when both actual clients are connected; defer until native onLoad registered all hosts. */
    private static boolean primeCableRoute(net.minecraft.server.MinecraftServer server,int tick){
        var level=server.overworld();
        for(int i=0;i<CABLE_CELLS.length;i++){
            if(!(level.getBlockEntity(CABLE_CELLS[i]) instanceof HostEntity h))return false;
            Part cable=h.parts.get(6);
            if(cable==null||!((Enum<?>)cable.kind).name().equals("DATA_CABLE")||cable.blockedFaces!=0
                    ||!cable.owner.equals(uuid("PL4ProbeA")))return false;
            if(h.parts.size()!=((i==0||i==6)?2:1))throw new IllegalStateException("Saved cable multipart layout invalid");
            cableHosts[i]=h;cables[i]=cable;
            if(i==0||i==6){
                Part endpoint=h.parts.get(5);
                if(endpoint==null||!((Enum<?>)endpoint.kind).name().equals("TRANSFER_NODE")
                        ||endpoint.face!=Direction.EAST||endpoint.transferMode!=0
                        ||endpoint.items||!endpoint.owner.equals(uuid("PL4ProbeA")))
                    throw new IllegalStateException("Saved native transfer endpoint changed");
                cableEndpoints[i==0?0:1]=endpoint;
                if(!(level.getBlockEntity(CABLE_CELLS[i].east()) instanceof BarrelBlockEntity b))
                    throw new IllegalStateException("Native transfer barrel not loaded");
                cableBarrels[i==0?0:1]=b;
            }
        }
        var connected=NetworkEngine.storageNodes(server,cableEndpoints[0]);
        if(connected.size()!=2)return false;
        if(connected.stream().map(NetworkEngine.Ref::part).distinct().count()!=2
                ||connected.stream().noneMatch(r->r.part()==cableEndpoints[0])
                ||connected.stream().noneMatch(r->r.part()==cableEndpoints[1]))
            throw new IllegalStateException("Native NetworkEngine discovered foreign endpoint");
        var ids=new ArrayList<String>();
        ids.add(cableEndpoints[0].identity.toString());
        ids.add(cableEndpoints[1].identity.toString());
        for(Part c:cables)ids.add(c.identity.toString());
        if(new HashSet<>(ids).size()!=9)throw new IllegalStateException("Duplicate physical cable identity");
        cableIdentities=List.copyOf(ids);
        try{
            Path identities=Path.of("probe-cable-identities.txt");
            if(RESTORE){
                if(!Files.readAllLines(identities).equals(ids))
                    throw new IllegalStateException("Saved native cable identities changed across installed JARs");
            }else Files.write(identities,ids);
        }catch(java.io.IOException err){throw new IllegalStateException("Cannot attest cable UUIDs",err);}
        cableSourceBefore=barrelDiamonds(cableBarrels[0]);
        cableSinkBefore=barrelDiamonds(cableBarrels[1]);
        if(cableSourceBefore+cableSinkBefore!=1728||cableSourceBefore<=64
                ||(RESTORE&&cableSinkBefore==0)||(!RESTORE&&cableSinkBefore!=0))
            throw new IllegalStateException("Saved native cable inventory was not conserved");
        // Revoke the physical network BEFORE activating transfer; enforce no delivery
        // through disconnected adjacent inventories over multiple real server ticks.
        cables[3].blockedFaces=CABLE_EAST_BIT;cableHosts[3].changed();
        var isolated=NetworkEngine.storageNodes(server,cableEndpoints[0]);
        if(isolated.size()!=1||isolated.getFirst().part()!=cableEndpoints[0])
            throw new IllegalStateException("Blocked cable arm did not revoke opposite endpoint");
        for(int i=0;i<2;i++){
            cableEndpoints[i].transferMode=i==0?2:1;
            cableEndpoints[i].items=true;
            cableHosts[i==0?0:6].changed();
        }
        cableStage=1;cableStageTick=tick;
        return true;
    }
    private static void advanceCableRoute(net.minecraft.server.MinecraftServer server,int tick){
        int source=barrelDiamonds(cableBarrels[0]),sink=barrelDiamonds(cableBarrels[1]);
        if(cableStage==1){
            if(source!=cableSourceBefore||sink!=cableSinkBefore)
                throw new IllegalStateException("Installed transfer bypassed disconnected cable revocation");
            if(tick-cableStageTick>=60){
                cableSeveredTicks=tick-cableStageTick;
                cables[3].blockedFaces=0;cableHosts[3].changed();
                if(NetworkEngine.storageNodes(server,cableEndpoints[0]).size()!=2)
                    throw new IllegalStateException("Rejoined native cable did not restore both endpoints");
                cableStage=2;cableStageTick=tick;
            }
            return;
        }
        if(cableStage!=2)return;
        if(tick-cableStageTick>160)
            throw new IllegalStateException("No native automatic transfer after cable reconnection");
        if(source==cableSourceBefore&&sink==cableSinkBefore)return;
        int moved=cableSourceBefore-source;
        if(moved<=0||moved!=sink-cableSinkBefore||source+sink!=1728||source<=0)
            throw new IllegalStateException("Connected native cable item transfer lost or minted inventory");
        for(int i=0;i<2;i++){
            if(!cableEndpoints[i].pendingItem.isEmpty())
                throw new IllegalStateException("Scheduled installed cable left pending escrow");
            cableEndpoints[i].transferMode=0;cableEndpoints[i].items=false;
            cableHosts[i==0?0:6].changed();cableBarrels[i].setChanged();
        }
        cableReceipt=new LinkedHashMap<>();
        cableReceipt.put("engine","net.foundations.pl4.NetworkEngine.tick");
        cableReceipt.put("cable_count",7);cableReceipt.put("connected_endpoints",2);
        cableReceipt.put("severed_endpoints",1);cableReceipt.put("severed_ticks",cableSeveredTicks);
        cableReceipt.put("source_before",cableSourceBefore);cableReceipt.put("source_after",source);
        cableReceipt.put("sink_before",cableSinkBefore);cableReceipt.put("sink_after",sink);
        cableReceipt.put("delivered",moved);cableReceipt.put("total",1728);
        cableReceipt.put("direct_transfer_invocations",0);cableReceipt.put("pending_escrow",0);
        cableReceipt.put("identities",cableIdentities);
        cableReceipt.put("two_real_clients_connected",server.getPlayerList().getPlayerCount()==2);
        if(server.getPlayerList().getPlayerCount()!=2)
            throw new IllegalStateException("Scheduled native delivery did not occur under two real clients");
        System.out.println("PL4_INSTALLED_CABLE_CLIENT_TICK:"+System.getenv("PL4_INSTALLED_JAR_SHA256")+":"+moved);
        cableStage=3;
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("pl4probe")
            .then(Commands.literal("ready").executes(ctx->{ServerPlayer player=ctx.getSource().getPlayerOrException();if(!NAMES.contains(player.getGameProfile().getName()))throw new IllegalStateException("Unexpected probe account");ready.add(player.getGameProfile().getName());return 1;}))
            .then(Commands.literal("sent").executes(ctx->{sent.add(ctx.getSource().getPlayerOrException().getGameProfile().getName());return 1;})));
    }
    private static int diamonds(ServerPlayer player){int n=0;for(int i=0;i<36;i++)if(player.getInventory().getItem(i).is(Items.DIAMOND))n+=player.getInventory().getItem(i).getCount();return n;}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        var server=event.getServer();int now=server.getTickCount();
        if(stopAt>=0){if(now>=stopAt)server.halt(false);return;}
        if(cableStage==1||cableStage==2)advanceCableRoute(server,now);
        if(ready.size()==2&&verifyAt==-1){
            var a=server.getPlayerList().getPlayerByName("PL4ProbeA");var b=server.getPlayerList().getPlayerByName("PL4ProbeB");
            if(a==null||b==null)return;
            if(!primeCableRoute(server,now))return;
            for(ServerPlayer player:List.of(a,b)){
                if(player instanceof net.neoforged.neoforge.common.util.FakePlayer||!player.getUUID().equals(uuid(player.getGameProfile().getName())))throw new IllegalStateException("Expected two real offline development clients");
                if(RESTORE){if(diamonds(player)!=17)throw new IllegalStateException("Saved player inventory did not migrate");}
                else player.getInventory().clearContent();player.setGameMode(GameType.CREATIVE);player.teleportTo(server.overworld(),.5,80,-3.5,0,15);
                ItemStack tool=new ItemStack(FoundationsPL4.item("wirelessstorage"));Part.Link binding=new Part.Link(server.overworld().dimension().location().toString(),host.getBlockPos(),node.face,null,node.identity);
                CustomData.update(DataComponents.CUSTOM_DATA,tool,t->t.put("pl_link",binding.save()));player.setItemInHand(InteractionHand.MAIN_HAND,tool);player.containerMenu.broadcastChanges();
            }
            server.getPlayerList().deop(b.getGameProfile());
            if(WirelessStorage.open(b,InteractionHand.MAIN_HAND)!=null)throw new IllegalStateException("Foreign non-operator gained access");
            server.getPlayerList().op(b.getGameProfile());
            if(WirelessStorage.open(a,InteractionHand.MAIN_HAND)==null||WirelessStorage.open(b,InteractionHand.MAIN_HAND)==null)throw new IllegalStateException("Authorized clients could not open independent sessions");
            verifyAt=now+450;
        }
        if(verifyAt>=0&&sent.containsAll(NAMES)&&cableStage==3){
            var a=server.getPlayerList().getPlayerByName("PL4ProbeA");var b=server.getPlayerList().getPlayerByName("PL4ProbeB");
            if(a==null||b==null)throw new IllegalStateException("Probe client disconnected early");
            int remaining=0;for(int i=0;i<barrel.getContainerSize();i++)if(barrel.getItem(i).is(Items.DIAMOND))remaining+=barrel.getItem(i).getCount();
            if(diamonds(a)!=17||diamonds(b)!=17||remaining!=94)throw new IllegalStateException("Network request conservation/replay failed: "+diamonds(a)+"/"+diamonds(b)+"/"+remaining);
            try{
                Map<String,Object> report=new LinkedHashMap<>();report.put("source_commit",System.getenv("PL4_PROBE_SOURCE"));report.put("cable_transfer",cableReceipt);report.put("phase",RESTORE?"restore":"seed");report.put("node_identity",node.identity.toString());report.put("real_client_names",List.of(a.getGameProfile().getName(),b.getGameProfile().getName()));report.put("real_client_uuids",List.of(a.getUUID().toString(),b.getUUID().toString()));report.put("fake_players",false);report.put("foreign_non_operator_denied",true);report.put("operator_access_explicitly_granted",true);report.put("initial_items",128);report.put("items_per_player",List.of(diamonds(a),diamonds(b)));report.put("remaining_items",remaining);report.put("duplicate_network_requests_ignored",!RESTORE);report.put("saved_world_restore",RESTORE);report.put("scope","Two actual development clients on a private loopback offline server. Seed checks concurrent single-use packets; restore checks saved node identity/owner, inventory, players and reopened sessions. No online authentication, arbitrary user-world, dimension, optional-provider or other-port acceptance.");
                Path out=Path.of(System.getenv("PL4_PROBE_OUTPUT"));Files.createDirectories(out);Files.writeString(out.resolve("server-multiplayer-proof.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
            }catch(java.io.IOException failure){throw new IllegalStateException(failure);}
            stopAt=now+100;
        }else if(verifyAt>=0&&now>verifyAt)throw new IllegalStateException("Both real clients did not complete storage requests");
    }
}
