package net.foundations.pl4.compat.scenarios;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.foundations.pl4.PLGameTests;
import net.foundations.pl4.PLConfig;
import org.apache.logging.log4j.LogManager;

/** Development-only test module, compiled separately and never shipped in the runtime JAR. */
@Mod("foundations_pl4_porttests")
public final class PortScenarioMod {
    private final List<Method> tests=new ArrayList<>();
    private final List<String> failures=new ArrayList<>();
    private MinecraftServer server;
    private ServerWorld world;
    private GameTestHelper current;
    private Method method;
    private int index,passed;
    private boolean finished;
    private long ticks;
    private final BlockPos origin=new BlockPos(0,80,0);
    private static final org.apache.logging.log4j.Logger LOG=LogManager.getLogger();
    public PortScenarioMod(){
        if(!Boolean.getBoolean("foundations_pl4.portScenarioServer"))throw new IllegalStateException("PL4 scenarios require the isolated test-server launch property");
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::tick);
    }
    private void started(FMLServerStartedEvent event){
        server=event.getServer();
        if(!server.isDedicatedServer())throw new IllegalStateException("Scenarios require a disposable dedicated server");
        world=server.getLevel(net.minecraft.world.dimension.DimensionType.OVERWORLD);
        RegisterGameTestsEvent registry=new RegisterGameTestsEvent();PLGameTests.register(registry);
        for(Class<?> fixture:registry.fixtures())for(Method candidate:fixture.getDeclaredMethods())if(candidate.isAnnotationPresent(GameTest.class)){
            if(!Modifier.isStatic(candidate.getModifiers())||!Arrays.equals(candidate.getParameterTypes(),new Class<?>[]{GameTestHelper.class}))throw new IllegalStateException("Invalid fixture "+candidate);
            tests.add(candidate);
        }
        tests.sort(Comparator.comparing(m->m.getDeclaringClass().getSimpleName()+"."+m.getName()));
        if(tests.size()!=194)throw new IllegalStateException("Expected all 194 native fixtures, found "+tests.size());
        for(int x=-1;x<=2;x++)for(int z=-1;z<=2;z++){world.setChunkForced(x,z,true);world.getChunk(x,z);}
        LOG.info("PL4 LEGACY SCENARIOS: {} fixtures on real Minecraft 1.13.2 / Forge",tests.size());
    }
    private void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||server==null||finished)return;
        ticks++;
        if(current==null){
            if(index==tests.size()){finish();return;}
            cleanFixtureRegion();
            var memory=com.electronwill.nightconfig.core.CommentedConfig.inMemory();PLConfig.SPEC.correct(memory);PLConfig.SPEC.setConfig(memory);
            method=tests.get(index++);current=new GameTestHelper(world,origin);
            LOG.info("PL4 SCENARIO START {}/{} {}",index,tests.size(),name());
            try{method.invoke(null,current);}catch(InvocationTargetException error){fail(error.getCause());return;}catch(ReflectiveOperationException error){fail(error);return;}
        }
        try{
            current.advance();
            if(current.passed){passed++;LOG.info("PL4 SCENARIO PASS {}",name());current=null;}
            else if(current.tick>method.getAnnotation(GameTest.class).timeoutTicks())fail(new AssertionError("Fixture timeout at tick "+current.tick));
        }catch(Throwable error){fail(error);}
    }
    private String name(){return method.getDeclaringClass().getSimpleName()+"."+method.getName();}
    private void fail(Throwable error){failures.add(name()+": "+error);LOG.error("PL4 SCENARIO FAIL "+name(),error);current=null;}
    private void cleanFixtureRegion(){
        for(BlockPos p:BlockPos.betweenClosed(-3,76,-3,38,96,38))if(!world.isEmptyBlock(p))world.setBlock(p,Blocks.AIR.defaultBlockState(),3);
        for(var entity:new ArrayList<>(world.getEntities((net.minecraft.entity.Entity)null,new AxisAlignedBB(-8,72,-8,45,104,45))))if(!(entity instanceof PlayerEntity))entity.remove();
    }
    private void finish(){
        finished=true;cleanFixtureRegion();
        for(int x=-1;x<=2;x++)for(int z=-1;z<=2;z++)world.setChunkForced(x,z,false);
        try{
            var report=new com.google.gson.JsonObject();report.addProperty("minecraft","1.13.2");report.addProperty("harness","PL4 isolated dedicated-server scenarios");report.addProperty("total",tests.size());report.addProperty("passed",passed);report.addProperty("failed",failures.size());report.addProperty("server_ticks",ticks);
            var errors=new com.google.gson.JsonArray();for(String failure:failures)errors.add(failure);report.add("failures",errors);
            Files.writeString(Path.of("port-scenarios.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));
        }catch(java.io.IOException error){failures.add("Unable to write scenario report: "+error);LOG.error("Scenario report failure",error);}
        if(failures.isEmpty())LOG.info("PL4 SCENARIOS SUCCESS: All 194 required native scenarios passed");
        else LOG.error("PL4 SCENARIOS FAILURE: {} of {} failed",failures.size(),tests.size());
        server.halt(false);
    }
}
