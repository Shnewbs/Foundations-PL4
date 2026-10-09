# 1.16.4 Java 17 runtime compatibility profile

This modern-feature source port currently emits Java 17 bytecode. Unmodified Forge 35.1.37 ships ModLauncher 8.0.9, which fails before PL4 starts on the tested Java 17 runtime. A compiled JAR is not yet a working installed port.

The diagnostic Gradle launch profile pins ModLauncher 8.1.3 and ASM 9.2 and grants the launcher's required JDK package access:

```
--add-exports=java.base/sun.security.util=ALL-UNNAMED
--add-opens=java.base/java.util.jar=ALL-UNNAMED
```

Minecraft stays 1.16.4; Forge stays 35.1.37. No Forge 36 classes, bundled alternative modloader or guessed cross-version binary compatibility are used. These settings describe a compatibility experiment, not blanket support for every Java version or an unmodified older launcher. The old runtime must not be exposed as a public server merely because it starts; separate platform security and modpack acceptance remain necessary.

Upstream basis: https://github.com/McModLauncher/modlauncher/blob/main-8.1.x/src/main/java/cpw/mods/modlauncher/SecureJarHandler.java handles both manifest-verifier constructor forms. The dependencies and JVM flags must also be reproduced by the real installation, not only Gradle.

Release publication is paused while installed-runtime setup and acceptance are unresolved. Client visuals, third-party APIs and multiplayer also remain pending. Further API testing is still required.
