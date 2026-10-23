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
        level.setDefaultSpawnPos(new BlockPos(0,80,-4),0);NetworkEngine.rebuild(event.getServer());
        try{Files.writeString(Path.of("probe-node-identity.txt"),node.identity.toString()+"\n");}catch(java.io.IOException failure){throw new IllegalStateException(failure);}
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
        if(ready.size()==2&&verifyAt==-1){
            var a=server.getPlayerList().getPlayerByName("PL4ProbeA");var b=server.getPlayerList().getPlayerByName("PL4ProbeB");
            if(a==null||b==null)return;
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
            verifyAt=now+200;
        }
        if(verifyAt>=0&&sent.containsAll(NAMES)){
            var a=server.getPlayerList().getPlayerByName("PL4ProbeA");var b=server.getPlayerList().getPlayerByName("PL4ProbeB");
            if(a==null||b==null)throw new IllegalStateException("Probe client disconnected early");
            int remaining=0;for(int i=0;i<barrel.getContainerSize();i++)if(barrel.getItem(i).is(Items.DIAMOND))remaining+=barrel.getItem(i).getCount();
            if(diamonds(a)!=17||diamonds(b)!=17||remaining!=94)throw new IllegalStateException("Network request conservation/replay failed: "+diamonds(a)+"/"+diamonds(b)+"/"+remaining);
            try{
                Map<String,Object> report=new LinkedHashMap<>();report.put("source_commit",System.getenv("PL4_PROBE_SOURCE"));report.put("phase",RESTORE?"restore":"seed");report.put("node_identity",node.identity.toString());report.put("real_client_names",List.of(a.getGameProfile().getName(),b.getGameProfile().getName()));report.put("real_client_uuids",List.of(a.getUUID().toString(),b.getUUID().toString()));report.put("fake_players",false);report.put("foreign_non_operator_denied",true);report.put("operator_access_explicitly_granted",true);report.put("initial_items",128);report.put("items_per_player",List.of(diamonds(a),diamonds(b)));report.put("remaining_items",remaining);report.put("duplicate_network_requests_ignored",!RESTORE);report.put("saved_world_restore",RESTORE);report.put("scope","Two actual development clients on a private loopback offline server. Seed checks concurrent single-use packets; restore checks saved node identity/owner, inventory, players and reopened sessions. No online authentication, arbitrary user-world, dimension, optional-provider or other-port acceptance.");
                Path out=Path.of(System.getenv("PL4_PROBE_OUTPUT"));Files.createDirectories(out);Files.writeString(out.resolve("server-multiplayer-proof.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
            }catch(java.io.IOException failure){throw new IllegalStateException(failure);}
            stopAt=now+100;
        }else if(verifyAt>=0&&now>verifyAt)throw new IllegalStateException("Both real clients did not complete storage requests");
    }
}
