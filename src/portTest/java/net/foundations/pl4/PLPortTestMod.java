package net.foundations.pl4;

import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.MultipleTestTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import java.util.Collection;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.RegisterGameTestsEvent;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/** Test-only mod loaded solely from the isolated, combined scenario artifact. */
@Mod("foundations_pl4_porttests")
public final class PLPortTestMod {
    public PLPortTestMod() {
        if (!Boolean.getBoolean("foundations_pl4.portTestServer"))
            throw new IllegalStateException("PL4 GameTests require the isolated test-server launch property");
        // Forge's development-only event is disabled on installed production
        // servers. Register the same annotated methods in this test-only mod.
        Set<Method> fixtures = new HashSet<>();
        PLGameTests.register(new RegisterGameTestsEvent(fixtures));
        for (Method fixture : fixtures) GameTestRegistry.register(fixture, Set.of(FoundationsPL4.ID));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) GameTestTicker.SINGLETON.tick();
        });
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> {
            event.getDispatcher().register(Commands.literal("pl4native")
                .requires(source -> source.hasPermission(4))
                .executes(context -> runWithTrace(context.getSource())));
            org.apache.logging.log4j.LogManager.getLogger("FoundationsPL4")
                .info("Forge44 native GameTest registry contains {} tests",
                    GameTestRegistry.getAllTestFunctions().size());
        });
    }

    private static int runWithTrace(CommandSourceStack source) {
        try {
            var logger = org.apache.logging.log4j.LogManager.getLogger("FoundationsPL4");
            var level = source.getLevel();
            Collection<net.minecraft.gametest.framework.TestFunction> tests = GameTestRegistry.getAllTestFunctions();
            if (tests.size() != 191) throw new IllegalStateException("Expected 191 native fixtures, found " + tests.size());
            GameTestRegistry.forgetFailedTests();
            GameTestRunner.clearMarkers(level);
            Collection<GameTestInfo> running = GameTestRunner.runTests(tests, new BlockPos(0, 80, 3),
                Rotation.NONE, level, GameTestTicker.SINGLETON, 8);
            MultipleTestTracker tracker = new MultipleTestTracker(running);
            tracker.addListener(new GameTestListener() {
                public void testStructureLoaded(GameTestInfo info) { }
                public void testPassed(GameTestInfo info) { complete(); }
                public void testFailed(GameTestInfo info) { logger.error("PL4 NATIVE TEST FAIL {}", info.getTestName(), info.getError()); complete(); }
                private void complete() {
                    if (!tracker.isDone()) return;
                    if (tracker.getTotalCount() == 191 && !tracker.hasFailedRequired() && !tracker.hasFailedOptional())
                        logger.info("PL4 NATIVE SUCCESS: All 191 required tests passed");
                    else logger.error("PL4 NATIVE FAILURE: {} of {} tests failed", tracker.getFailedRequiredCount() + tracker.getFailedOptionalCount(), tracker.getTotalCount());
                }
            });
            logger.info("PL4 NATIVE START: {} installed Forge44 GameTests", tests.size());
            return 1;
        } catch (Throwable error) {
            org.apache.logging.log4j.LogManager.getLogger("FoundationsPL4").error("Native GameTest command failed", error);
            throw new IllegalStateException("Native GameTest command failed", error);
        }
    }
}
