# Foundations PL4 - Minecraft 1.17.1 feature port

Current target: Forge 37.1.1 / Java 17 / `0.2a-port.1`.

This port carries the modern-source cable/multipart network, readers, displays and editor,
wireless item storage, routing, item/fluid/energy escrow, ownership and field guide forward
from the 1.18.2 feature branch. It is not the reduced legacy transport implementation.

Native tags and sapphire generation are adapted to this exact target. The complete 193-scenario
suite, production-rule checks, reobfuscation and exact-target resources must pass before
publication. A branch or successful compiler exit is not a finished port.

**Further API testing is still required.** Installed third-party providers, ordinary client
visuals, multiplayer and sustained performance acceptance remain pending. No unavailable
foreign energy API receives invented conversion ratios. Full PL2 parity is not claimed.

Build and native scenarios: `bash gradlew --no-daemon clean build runServer -PportScenarios` with Java 17.
Install only a published runtime JAR for this exact target; back up worlds and match client/server versions.

The 193 scenarios execute in a separate isolated dedicated-server harness because this target predates Forge's modern GameTest registration hook. The runtime JAR excludes all scenario classes.
