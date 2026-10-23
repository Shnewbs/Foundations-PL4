package net.foundations.pl4;

import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Injected into disposable checkouts only. Measures native operations, not player FPS or whole-server MSPT. */
@PrefixGameTestTemplate(false)
public final class ReleasePerformanceGameTests {
    private static final UUID OWNER=UUID.fromString("bbbb0000-0000-0000-0000-000000000024");
    private static final com.sun.management.ThreadMXBean ALLOC=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
    private static volatile int consumed;
    private static Map<String,Object> measure(String name,int warmup,int rounds,int operations,Runnable work){
        for(int i=0;i<warmup;i++)work.run();
        long thread=Thread.currentThread().threadId();List<Long> elapsed=new ArrayList<>(),allocated=new ArrayList<>(),cpu=new ArrayList<>();
        for(int i=0;i<rounds;i++){
            long a=ALLOC.getThreadAllocatedBytes(thread),c=ALLOC.getCurrentThreadCpuTime(),n=System.nanoTime();
            for(int j=0;j<operations;j++)work.run();
            elapsed.add(System.nanoTime()-n);cpu.add(ALLOC.getCurrentThreadCpuTime()-c);allocated.add(ALLOC.getThreadAllocatedBytes(thread)-a);
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("operation",name);result.put("operations_per_round",operations);result.put("warmup_operations",warmup);result.put("elapsed_ns",elapsed);result.put("cpu_ns",cpu);result.put("allocated_bytes",allocated);return result;
    }
    @GameTest(template="empty",templateNamespace=FoundationsPL4.ID,timeoutTicks=2400)
    public static void loadedWiredAndWirelessNetworks(GameTestHelper h){
        if(!ALLOC.isThreadAllocatedMemorySupported()||!ALLOC.isCurrentThreadCpuTimeSupported())throw new IllegalStateException("Required JVM counters unavailable");
        ALLOC.setThreadAllocatedMemoryEnabled(true);ALLOC.setThreadCpuTimeEnabled(true);
        MinecraftServer server=h.getLevel().getServer();List<Map<String,Object>> workloads=new ArrayList<>();
        for(boolean wireless:new boolean[]{false,true}){
            List<HostEntity> hosts=new ArrayList<>();List<NetworkEngine.Ref> endpoints=new ArrayList<>();List<BlockPos> inventories=new ArrayList<>();
            try{
                for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++){
                    BlockPos at=h.absolutePos(new BlockPos(x+(wireless&&x>=8?1:0),y+3,z));
                    if(!h.getLevel().hasChunkAt(at))throw new IllegalStateException("Probe must not force-load chunks");
                    h.getLevel().setBlock(at,FoundationsPL4.HOST.get().defaultBlockState(),2);
                    HostEntity host=(HostEntity)h.getLevel().getBlockEntity(at);if(host==null)throw new IllegalStateException("Missing native host");
                    host.parts.put(6,new Part(Kind.DATA_CABLE,Direction.DOWN,OWNER));NetworkEngine.add(host);hosts.add(host);
                    if(y==15&&z<8){
                        Part p=new Part(x==0?Kind.TRANSFER_NODE:Kind.NODE,Direction.UP,OWNER);p.transferMode=x==0?2:0;
                        host.parts.put(p.slot(),p);endpoints.add(new NetworkEngine.Ref(host,p));
                        BlockPos storage=at.above();h.getLevel().setBlock(storage,Blocks.BARREL.defaultBlockState(),2);inventories.add(storage);
                    }
                }
                if(wireless){
                    HostEntity left=hosts.get(7*256+8*16+8),right=hosts.get(8*256+8*16+8);
                    Part emitter=new Part(Kind.DATA_EMITTER,Direction.EAST,OWNER),receiver=new Part(Kind.DATA_RECEIVER,Direction.WEST,OWNER);
                    left.parts.put(emitter.slot(),emitter);right.parts.put(receiver.slot(),receiver);
                    receiver.links.add(new Part.Link(h.getLevel().dimension().location().toString(),left.getBlockPos(),Direction.EAST,null,emitter.identity));
                }
                Part reader=new Part(Kind.NETWORK_READER,Direction.DOWN,OWNER);hosts.get(0).parts.put(reader.slot(),reader);
                NetworkEngine.rebuild(server);Part anchor=endpoints.getFirst().part();
                h.assertTrue(hosts.size()==4096&&endpoints.size()==128,"Probe requires 4096 physical hosts and 128 storage endpoints");
                h.assertTrue(NetworkEngine.storageNodes(server,anchor).size()==128&&NetworkEngine.targetsFor(server,reader).size()==128,"All wired/wireless endpoints must belong to the same physical network");
                TransferEngine.Plan plan=TransferEngine.prepare(endpoints);
                List<Map<String,Object>> measurements=new ArrayList<>();
                measurements.add(measure("storage_membership_lookup",2000,11,10000,()->consumed=NetworkEngine.storageNodes(server,anchor).size()));
                measurements.add(measure("reader_targets_lookup",2000,11,10000,()->consumed=NetworkEngine.targetsFor(server,reader).size()));
                measurements.add(measure("idle_transfer_cycle_128_endpoints_8_drivers",5000,15,1000,()->TransferEngine.run(server,plan)));
                measurements.add(measure("topology_rebuild_4096_hosts",50,21,1,()->NetworkEngine.rebuild(server)));
                Map<String,Object> workload=new LinkedHashMap<>();workload.put("network",wireless?"two_2048_host_cubes_with_wireless_bridge":"4096_host_wired_cube");workload.put("host_count",hosts.size());workload.put("endpoint_count",endpoints.size());workload.put("measurements",measurements);workloads.add(workload);
            }finally{
                for(HostEntity host:hosts){NetworkEngine.remove(host);h.getLevel().setBlock(host.getBlockPos(),Blocks.AIR.defaultBlockState(),2);}
                for(BlockPos at:inventories)h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),2);
                NetworkEngine.rebuild(server);
            }
        }
        try{
            Map<String,Object> report=new LinkedHashMap<>();report.put("source_commit",System.getenv("PL4_BENCHMARK_SOURCE"));report.put("trial",System.getenv("PL4_BENCHMARK_TRIAL"));report.put("java",System.getProperty("java.runtime.version"));report.put("os",System.getProperty("os.name"));report.put("processors",Runtime.getRuntime().availableProcessors());report.put("workloads",workloads);report.put("scope","Native operation timing/allocation; excludes setup and teardown. No client/FPS, active-provider or whole-server MSPT result.");
            Path output=Path.of("verification-logs/performance-probe.json");Files.createDirectories(output.getParent());Files.writeString(output,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(java.io.IOException failure){throw new IllegalStateException(failure);}
        h.succeed();
    }
}
