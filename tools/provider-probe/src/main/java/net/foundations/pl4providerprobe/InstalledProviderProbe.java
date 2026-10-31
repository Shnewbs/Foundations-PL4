package net.foundations.pl4providerprobe;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Separate installed mod. PL4 is NEVER a source-set dependency or shaded into this JAR.
 * All PL4 API calls are reflected from the production JAR installed in run/mods.
 */
@Mod("pl4_provider_probe")
public final class InstalledProviderProbe {
    private static final String API = "net.foundations.pl4.api.InfoProviders";
    private static final String PREFIX = "pl4_probe:";
    private static final String MARKER = "PL4_INSTALLED_PROVIDER_PASS:";
    private static final BlockPos FIXTURE = new BlockPos(8, 80, 8);
    private static final BlockPos UNLOADED = new BlockPos(1000000, 80, 1000000);

    public InstalledProviderProbe(IEventBus bus, ModContainer container) {
        NeoForge.EVENT_BUS.addListener(this::onStarted);
    }

    private void onStarted(ServerStartedEvent event) {
        try {
            ServerLevel level = event.getServer().overworld();
            if (!level.getServer().isSameThread()) throw new IllegalStateException("Not server thread");
            String expected = requiredEnv("PL4_EXPECTED_JAR_SHA256");
            if (!expected.matches("[0-9a-f]{64}")) throw new IllegalStateException("Invalid pinned SHA-256");
            String nonce = requiredEnv("PL4_PROVIDER_RUN_NONCE");
            require(nonce.matches("[0-9a-f]{32}"), "Invalid per-preparation nonce");
            String actual = digest(Path.of(requiredEnv("PL4_INSTALLED_JAR")));
            require(actual.equals(expected), "Installed PL4 bytes differ from pinned production JAR");
            Class<?> api = Class.forName(API);
            int version = api.getField("API_VERSION").getInt(null);
            require(version == 1, "Unrecognized installed provider API version");

            // This fixture chunk is deliberately loaded by the test. The independent
            // UNLOADED target below must stay unresident before and after sampling.
            level.getChunkAt(FIXTURE);
            int[] replacement = replacement(level);
            int[] closed = registrationRevocation(level);
            int[] selfClosed = selfRevocation(level);
            int[] exception = exceptionQuarantine(level);
            int[] unloaded = noForceLoad(level);
            @SuppressWarnings("unchecked")
            List<String> registered = (List<String>) api.getMethod("registeredIds").invoke(null);
            require(registered.stream().noneMatch(id -> id.startsWith(PREFIX)), "Leaked provider registration");

            String proof = "{\"schema\":2,\"target\":\"1.21.1\",\"api_version\":1,"
                + "\"installed_pl4_sha256\":\"" + actual + "\","
                + "\"run_nonce\":\"" + nonce + "\","
                + "\"replacement\":{\"first\":" + replacement[0] + ",\"later\":" + replacement[1] + ",\"stale_rows\":0},"
                + "\"registration_close\":{\"first\":" + closed[0] + ",\"later\":" + closed[1] + ",\"first_rows\":1},"
                + "\"self_close\":{\"calls\":" + selfClosed[0] + ",\"rows\":0,\"healthy_calls\":" + selfClosed[1] + "},"
                + "\"exception_quarantine\":{\"failing\":" + exception[0] + ",\"healthy\":" + exception[1] + ",\"partial_rows\":0},"
                + "\"unloaded\":{\"callbacks\":" + unloaded[0] + ",\"rows\":0,\"before_loaded\":false,\"after_loaded\":false},"
                + "\"registration_leaks\":0,\"rc_publication_cleared\":false}\n";
            Path output = Path.of(requiredEnv("PL4_PROVIDER_PROOF"));
            Files.createDirectories(output.toAbsolutePath().getParent());
            Files.writeString(output, proof);
            System.out.println(MARKER + actual + ":" + nonce);
        } catch (Exception failure) {
            throw new IllegalStateException("Installed independent provider probe FAILED", failure);
        }
    }

    private static int[] replacement(ServerLevel level) throws Exception {
        BlockEntity original = chest(level);
        AtomicInteger first = new AtomicInteger(), later = new AtomicInteger();
        AutoCloseable a = register("pl4_probe:a_replace", (context, sink) -> {
            first.incrementAndGet();
            level.removeBlockEntity(FIXTURE);
            level.setBlock(FIXTURE, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(FIXTURE, Blocks.CHEST.defaultBlockState(), 3);
            require(level.getBlockEntity(FIXTURE) != original, "Backing identity was not replaced");
            emit(sink, "stale");
        });
        try {
            AutoCloseable z = register("pl4_probe:z_after_replace", (context, sink) -> {
                later.incrementAndGet();
                emit(sink, "late");
            });
            try {
                List<?> rows = sample(level, FIXTURE);
                require(first.get() == 1 && later.get() == 0 && providerRows(rows, PREFIX) == 0,
                    "Replacement admitted a later callback or stale row");
            } finally { z.close(); }
        } finally { a.close(); }
        return new int[]{first.get(), later.get()};
    }

    private static int[] registrationRevocation(ServerLevel level) throws Exception {
        chest(level);
        AtomicInteger first = new AtomicInteger(), later = new AtomicInteger();
        AutoCloseable[] closeLater = new AutoCloseable[1];
        AutoCloseable a = register("pl4_probe:a_close", (context, sink) -> {
            first.incrementAndGet();
            try { closeLater[0].close(); }
            catch (Exception e) { throw new IllegalStateException(e); }
            emit(sink, "value");
        });
        try {
            AutoCloseable z = register("pl4_probe:z_closed", (context, sink) -> {
                later.incrementAndGet();
                emit(sink, "late");
            });
            closeLater[0] = z;
            try {
                List<?> rows = sample(level, FIXTURE);
                require(first.get() == 1 && later.get() == 0
                    && providerRows(rows, "pl4_probe:a_close/") == 1
                    && providerRows(rows, "pl4_probe:z_closed/") == 0,
                    "Closed provider was invoked or first provider row missing");
            } finally { z.close(); }
        } finally { a.close(); }
        return new int[]{first.get(), later.get()};
    }

    private static int[] exceptionQuarantine(ServerLevel level) throws Exception {
        chest(level);
        AtomicInteger failing = new AtomicInteger(), healthy = new AtomicInteger();
        AutoCloseable a = register("pl4_probe:a_throws", (context, sink) -> {
            failing.incrementAndGet();
            emit(sink, "partial");
            throw new IllegalStateException("Intentional independent provider failure");
        });
        try {
            AutoCloseable z = register("pl4_probe:z_healthy", (context, sink) -> {
                healthy.incrementAndGet();
                emit(sink, "good");
            });
            try {
                List<?> rows = sample(level, FIXTURE);
                require(failing.get() == 1 && healthy.get() == 1
                    && providerRows(rows, "pl4_probe:a_throws/") == 0
                    && providerRows(rows, "pl4_probe:z_healthy/") == 1,
                    "Partial failure rows escaped or healthy provider was suppressed");
            } finally { z.close(); }
        } finally { a.close(); }
        return new int[]{failing.get(), healthy.get()};
    }

    private static int[] selfRevocation(ServerLevel level) throws Exception {
        chest(level);
        AtomicInteger revoked = new AtomicInteger(), healthy = new AtomicInteger();
        AutoCloseable[] selfHandle = new AutoCloseable[1];
        AutoCloseable a = register("pl4_probe:a_self_close", (context, sink) -> {
            revoked.incrementAndGet();
            emit(sink, "revoked");
            try { selfHandle[0].close(); }
            catch (Exception failure) { throw new IllegalStateException(failure); }
        });
        selfHandle[0] = a;
        try {
            AutoCloseable z = register("pl4_probe:z_after_self_close", (context, sink) -> {
                healthy.incrementAndGet();
                emit(sink, "healthy");
            });
            try {
                List<?> rows = sample(level, FIXTURE);
                require(revoked.get() == 1 && healthy.get() == 1
                    && providerRows(rows, "pl4_probe:a_self_close/") == 0
                    && providerRows(rows, "pl4_probe:z_after_self_close/") == 1,
                    "Self-revoked provider published buffered rows or suppressed a healthy callback");
            } finally { z.close(); }
        } finally { a.close(); }
        return new int[]{revoked.get(), healthy.get()};
    }

    private static int[] noForceLoad(ServerLevel level) throws Exception {
        require(!level.hasChunkAt(UNLOADED), "Unloaded fixture was already resident");
        AtomicInteger callbacks = new AtomicInteger();
        AutoCloseable reg = register("pl4_probe:unloaded", (context, sink) -> {
            callbacks.incrementAndGet();
            emit(sink, "bad");
        });
        try {
            List<?> rows = sample(level, UNLOADED);
            require(rows.isEmpty() && callbacks.get() == 0 && !level.hasChunkAt(UNLOADED),
                "Unloaded target invoked a provider, returned rows or force-loaded its chunk");
        } finally { reg.close(); }
        return new int[]{callbacks.get()};
    }

    private static BlockEntity chest(ServerLevel level) {
        level.setBlock(FIXTURE, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(FIXTURE, Blocks.CHEST.defaultBlockState(), 3);
        BlockEntity entity = level.getBlockEntity(FIXTURE);
        require(entity != null, "Missing live fixture chest backing");
        return entity;
    }

    private static AutoCloseable register(String id, BiConsumer<Object,Object> callback) throws Exception {
        Class<?> api = Class.forName(API);
        Class<?> provider = Class.forName(API + "$Provider");
        Object proxy = Proxy.newProxyInstance(provider.getClassLoader(), new Class<?>[]{provider},
            (instance, method, args) -> {
                if ("sample".equals(method.getName())) { callback.accept(args[0], args[1]); return null; }
                if ("toString".equals(method.getName())) return id;
                if ("hashCode".equals(method.getName())) return System.identityHashCode(instance);
                if ("equals".equals(method.getName())) return instance == args[0];
                throw new UnsupportedOperationException(method.getName());
            });
        return (AutoCloseable) api.getMethod("register", String.class, provider).invoke(null, id, proxy);
    }

    private static void emit(Object sink, String key) {
        try {
            Class<?> sinkType = Class.forName(API + "$Sink");
            sinkType.getMethod("add", String.class, String.class, double.class, double.class, String.class)
                .invoke(sink, key, key, 1.0d, 2.0d, "ticks");
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Installed sink API changed", e); }
    }

    private static List<?> sample(ServerLevel level, BlockPos pos) throws Exception {
        Class<?> link = Class.forName("net.foundations.pl4.Part$Link");
        Object target = link.getConstructor(String.class, BlockPos.class, Direction.class, UUID.class, UUID.class)
            .newInstance(level.dimension().location().toString(), pos, Direction.UP, null, null);
        return (List<?>) Class.forName(API).getMethod("sample", ServerLevel.class, link)
            .invoke(null, level, target);
    }

    private static int providerRows(List<?> rows, String prefix) throws Exception {
        int count = 0;
        for (Object row : rows) {
            String key = (String) row.getClass().getMethod("key").invoke(row);
            if (key.startsWith(prefix)) count++;
        }
        return count;
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing attestation: " + name);
        return value;
    }

    private static String digest(Path path) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        try (var in = Files.newInputStream(path)) {
            byte[] bytes = new byte[8192]; int n;
            while ((n = in.read(bytes)) != -1) sha.update(bytes, 0, n);
        }
        return java.util.HexFormat.of().formatHex(sha.digest());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
