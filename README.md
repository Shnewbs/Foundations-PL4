# Foundations PL4 1.14.4 native port — in progress

Minecraft 1.14.4 / recommended Forge 28.2.26 / Java 8, built using Java 17
with conversion to Java 8 bytecode. Derived from full-feature 1.15.2 source,
not the limited legacy transport implementation. Actual native APIs, storage,
networking, visuals and recipes still require independent port validation.

**NOT a released runtime.** The workflow must pass target-native compilation,
all 194 isolated scenarios, archive checks and production-only startup before
a binary may be released. No 1.15.2 JAR is compatible by renaming.
**Further API testing is required.**
