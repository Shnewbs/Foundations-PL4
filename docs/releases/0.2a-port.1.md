# Foundations PL4 1.16.1 0.2a-port.1

Full-feature source port derived from the verified 1.16.4 track, not a renamed JAR or a transport-only preview.

Includes multipart cable/reader/display behavior, hologram and display editing, physical-network Wireless Storage with search/sorting and transfers, sided item/fluid/FE routing with saved buffers, ownership, configuration, recipes and the built-in field guide. Foreign energy telemetry is not universal energy conversion.

## Exact runtime

Minecraft **1.16.1**, Forge **32.0.108**, **Java 8**. Java 17 is the build JDK only. Build-time conversion supplies required relocated Java compatibility code; no runtime Java agent is needed. JvmDowngrader source and license notices accompany the release.

This old Forge track uses an explicit **ModLauncher 8.1.3 launch profile**. This is NOT a stock-launcher compatibility claim. Install the exact Forge server, then run the supplied `forge35_profile.py --server SERVER_DIRECTORY --java PATH_TO_JAVA8 --launch`. The helper verifies the pinned launcher hash and uses classpath precedence without replacing the Forge JAR or its bundled libraries. Client launcher setup and graphical acceptance are still pending; do not assume this server helper configures clients.

## Release gates

Independent target compilation/reobfuscation, original production-rule suites, all 193 required native server scenarios on the exact installed runtime, Java 8 bytecode/archive checks, and a separate production-only startup without the test mod. Check the attached runtime summary for actual results. Source status before CI remains PENDING; this is not proof of a passing run.

Clients and servers must use matching exact-target versions. Back up worlds and do not downgrade existing worlds. No cross-version world migration is promised.

**Further API testing is still required.** Real-client visuals, installed third-party APIs, multiplayer and sustained modpack performance remain unverified. Core guide/inspection/native storage and transfer fallbacks are retained, but this is not full optional-API or scripting parity.
