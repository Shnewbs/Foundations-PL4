# 1.16.4 runtime compatibility checkpoint

Verified October 9, 2026. **This source port is not yet a published or playable build.**

The modern-feature source compiles and reobfuscates against actual Minecraft 1.16.4 / Forge 35.1.37. Exact Minecraft metadata, a separate payload identity, the Forge-35 account-history configuration implementation, and two extra native regression fixtures are present. All 191 original server scenarios are retained; the required total is 193.

## Verified blocker

The first native launch failed in ModLauncher 8.0.9 accessing JDK 17 manifest-verifier internals. The explicit profile below gets past that launch stage:

- ModLauncher 8.1.3, ASM libraries 9.2.
- `--add-exports=java.base/sun.security.util=ALL-UNNAMED`
- `--add-opens=java.base/java.util.jar=ALL-UNNAMED`

The test module's leftover Forge-36 metadata was also corrected to Forge 35 and exact Minecraft 1.16.4. However, the next native run fails while Forge scans Java record classes: `UnsupportedOperationException: Record requires ASM8`, from `net.minecraftforge.fml.loading.moddiscovery.Scanner`. Replacing the ASM library alone does not change the visitor API level selected inside Forge.

Latest checked source: `74969c5387e3d7ce1377ee09c169078ee2ab152b`. Native diagnostic run: https://github.com/Shnewbs/Foundations-PL4/actions/runs/37999800213 . Compilation, reobfuscation and packaging pass; **zero of the 193 scenarios execute because mod discovery fails first**. A Gradle BUILD SUCCESSFUL line is not native acceptance; the workflow correctly fails for the absent scenario report.

## Remaining implementation

Backport record/sealed-class behavior properly, including equality, hashing, serialization and the separate scenario module. Do not merely strip class attributes or suppress the scanner failure. A reviewed build-time compatibility conversion is a candidate, not an implemented or accepted feature. JvmDowngrader 2.0.1 was identified upstream as a possible tool; its exact CLI/API, license, necessary runtime helpers and output semantics must be verified before adoption. Its README's older example API-JAR version must not be assumed to match the current executable.

After conversion, repeat native scenarios and verify the exact reobfuscated artifact on an installed server, then test the real client. The diagnostic profile above is insufficient by itself and must not be advertised as a working end-user launch recipe. No Forge 36 binary is used to certify 1.16.4. No Java-8 compatibility is currently established.

Publication is gated off pending this work. Optional-provider combinations, client visuals, multiplayer and extended performance checks remain pending. **Further API testing is still required.**

Upstream launcher fix: https://github.com/McModLauncher/modlauncher/blob/main-8.1.x/src/main/java/cpw/mods/modlauncher/SecureJarHandler.java
Build-time conversion candidate: https://github.com/unimined/JvmDowngrader/releases/tag/2.0.1
