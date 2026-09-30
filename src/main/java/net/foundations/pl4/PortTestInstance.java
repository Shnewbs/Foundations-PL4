package net.foundations.pl4;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Keeps the regression fixtures executable through the native 26.3 test-instance registry. */
public final class PortTestInstance extends GameTestInstance {
    private static final Class<?>[] FIXTURES={PLGameTests.class,R5GameTests.class,R6GameTests.class,R7GameTests.class,R8GameTests.class,R9GameTests.class,R10GameTests.class,R11GameTests.class,R13GameTests.class,R16GameTests.class,PerformanceGameTests.class,EnergyIntegrationGameTests.class};
    public static final MapCodec<PortTestInstance> CODEC=RecordCodecBuilder.mapCodec(in->in.group(
        TestData.CODEC.fieldOf("data").forGetter(PortTestInstance::info),
        Codec.STRING.fieldOf("testId").forGetter(i->i.testId)).apply(in,PortTestInstance::new));
    private static final DeferredRegister<MapCodec<? extends GameTestInstance>> TYPES=DeferredRegister.create(Registries.TEST_INSTANCE_TYPE,FoundationsPL4.ID);
    static {TYPES.register("regression",()->CODEC);}
    private final String testId;
    public PortTestInstance(TestData<Holder<TestEnvironmentDefinition<?>>> data,String testId){super(data);this.testId=testId;}
    public static void registerTypes(IEventBus bus){TYPES.register(bus);}
    public static void register(RegisterGameTestsEvent event){
        for(var fixture:FIXTURES){
            var batch=event.registerEnvironment(FoundationsPL4.id(fixture.getSimpleName().toLowerCase(Locale.ROOT)));
            for(var method:fixture.getDeclaredMethods()){
                var spec=method.getAnnotation(PortGameTest.class);if(spec==null)continue;
                String id=fixture.getSimpleName()+"/"+method.getName();
                event.registerTest(FoundationsPL4.id(id.toLowerCase(Locale.ROOT)),new PortTestInstance(new TestData<>(batch,net.minecraft.resources.Identifier.fromNamespaceAndPath(spec.templateNamespace(),spec.template()),spec.timeoutTicks(),0,true),id));
            }
        }
    }
    @Override public void run(GameTestHelper helper){
        String[] name=testId.split("/",2);
        for(var fixture:FIXTURES)if(fixture.getSimpleName().equals(name[0])){
            try {fixture.getDeclaredMethod(name[1],GameTestHelper.class).invoke(null,helper);return;}
            catch(InvocationTargetException failure){
                if(failure.getCause() instanceof RuntimeException runtime)throw runtime;
                if(failure.getCause() instanceof Error error)throw error;
                throw new IllegalStateException("Test failed: "+testId,failure.getCause());
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Cannot invoke test: "+testId,failure);}
        }
        throw new IllegalStateException("Unknown test: "+testId);
    }
    @Override public MapCodec<? extends GameTestInstance> codec(){return CODEC;}
    @Override protected MutableComponent typeDescription(){return Component.literal("PL4 regression");}
}
