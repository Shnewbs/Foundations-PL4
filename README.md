# Foundations PL4 - Minecraft 1.15.2 feature port

Target: Minecraft 1.15.2 / Forge 31.2.62 / Java 8 runtime / `0.2a-port.1`.
Builds use Java 17; the published artifact is converted to Java 8 before reobfuscation.
Do not install the unconverted intermediate JAR. No runtime agent is required.

## Feature implementation

Carries the modern-derived multipart/cable network, readers, displays and editor,
wireless inventory storage, item/fluid/energy routing and escrow, ownership, field
guide and sapphire generation. This is not the reduced legacy transport subset.
Native Forge 31 dimensions, NBT, interaction hooks, recipes and rendering are adapted
rather than substituting a neighboring Minecraft version's binary.

Strict native JSON recipe serialization replaces unavailable modern codecs. Native
optional-tag arrays preserve Forge material aliases and vanilla fallbacks. Saved parts
remove both older long-pair UUID fields and newer UUID keys for runtime owner/identity,
while preserving meaningful configuration and resource escrow.

## Build and acceptance

With a Java 17 build JDK and JAVA_HOME_8_X64 pointing to Java 8:

`bash gradlew --no-daemon clean build`

`python3 tools/java8/runtime.py native`

Run native acceptance only in a clean disposable checkout, not a production server.
The complete original 191 scenarios are retained, with three additional tests for
native sapphire attachment, malformed optional JSON fields, and actual Minecraft
ItemStack entity-transceiver interaction. Publication requires all **194 scenarios**,
production-rule suites, exact resources, reobfuscation and Java 8 archive checks.
`java8-summary.json` in the release is the actual test result, not a pre-build status claim.

Only required relocated JvmDowngrader 2.0.1 API helpers are included. The release includes
LGPL notices, the matching upstream source archive and build/relinking scripts. Test
fixtures are compiled separately and excluded from the runtime JAR.

**Further API testing is still required.** Installed third-party provider combinations,
real-client visuals, live multiplayer, and sustained modpack/performance acceptance
remain open. No full PL2 parity or arbitrary foreign-energy compatibility is claimed.
Back up worlds; install only the matching published runtime, and update clients and server together.
