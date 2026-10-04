# Info Reader provider API (0.0.2a.R2.1)

Use `net.foundations.pl4.api.InfoProviders.register` during common setup to install a synchronous, read-only telemetry callback. Compile against the matching Minecraft track; these jars are not cross-version binaries.

```java
InfoProviders.register("example:machine", (context, sink) -> {
    if (context.entity() != null) return;
    var machine = context.level().getBlockEntity(context.target().pos());
    if (machine instanceof MyMachine block) {
        sink.add("progress", "Progress", block.progress(), block.duration(), "ticks");
    }
});
```

The display key is `example:machine/progress`. Vanilla keys (including `x`, `health`, `cook_time`) keep their historical names. Info Reader Data key accepts comma-separated keys with surrounding spaces.

## Contract

- Callbacks execute on the server thread for the first available Info Reader target. A selected channel restricts this to that endpoint. Block targets are checked with `hasChunkAt`; missing entities are skipped. PL4 does not force-load chunks for sampling.
- Do not mutate the world, extract resources, start asynchronous work, load neighboring chunks, or retain the context/sink. The API is a read-only convention for trusted add-ons, not a sandbox against malicious Java code. A sink stops accepting rows when its callback returns and rejects writes from other threads.
- Block targets include the exposed face; entity targets include the loaded entity. Inspect support before reading a third-party machine and handle its own access rules. The registry adds no new remote access route: the network supplies the targets and PL4's existing ownership checks govern reader edits.
- IDs must be unique lowercase `namespace:path`, at most 64 characters. Up to 64 add-on providers can register. `foundations_pl4:vanilla` is reserved. Providers run in sorted ID order after vanilla.
- Each add-on may publish 32 rows; the combined API result is capped at 256. The reader then applies the server MAX_ROWS limit. Keys are 1–48 ASCII letters/digits/underscore/dot/hyphen. Names are truncated to 96 characters, units to 24, with control/formatting characters stripped. Values/capacities must be finite; capacities cannot be negative. Duplicate keys keep the first valid row.
- Runtime and linkage errors discard that provider's entire partial sample, log once per registration, and allow remaining providers to run. Do not rely on a row being present when a provider or machine is unavailable.
- Keep the returned `Registration` if you need to unregister; `close()` is idempotent, releases that registration, and cannot remove a newer registration even if it reuses the same callback. Ordinary integrations register once for the mod lifetime.

This release supplies vanilla telemetry and the extension point. It does not claim installed-mod acceptance tests, automatic third-party machine discovery, native power transfer, or new player/entity permissions.
