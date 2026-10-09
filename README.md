# Foundations PL4 - Minecraft 1.17.1 feature port

Target: Forge 37.1.1 / Java 17 / `0.2a-port.1`.

Carries the modern-derived multipart/cable network, readers, displays and editor,
wireless inventory storage, item/fluid/energy routing and escrow, ownership, field guide,
and native sapphire generation. This is not the reduced legacy transport subset.

Build: `bash gradlew --no-daemon clean build`.
Native acceptance: `python3 tools/verify_installed117.py` in a disposable checkout.

The 193 scenarios execute on installed Forge in a combined test-only module to avoid
JPMS split packages. Every production class/resource entry is compared with the standalone
runtime. A second independent startup and shutdown uses only the exact production JAR,
without any test classes or test-mode property. Test JARs are never release runtime assets.
No development-only success is substituted for the installed-runtime gate.

**Further API testing is still required.** Installed-provider combinations, real-client
visuals, multiplayer and sustained performance acceptance remain pending. Full PL2 parity
is not claimed. Back up worlds and match client/server PL4 versions.
