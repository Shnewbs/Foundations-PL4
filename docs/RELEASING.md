# Building and releasing versions

Each published version gets a tagged Windows/Java 21 build and a GitHub Release containing the mod JAR, sources JAR and SHA-256 checksums. The release workflow verifies that the tag version exactly matches Gradle's `version`, then runs `clean build runGameTestServer`; it publishes nothing if any build, asset, rule or native GameTest gate fails.

To release a version:

1. Update `version` in `build.gradle` and matching version references in mod metadata and release documentation.
2. Confirm `.\gradlew.bat --no-daemon --console=plain clean build runGameTestServer` passes.
3. Create and push a version tag whose name is `v` plus the exact Gradle version. For example:

   ```powershell
   git tag -a v0.0.2a.R1 -m "Foundations PL4 0.0.2a.R1"
   git push origin v0.0.2a.R1
   ```

4. The `Build and release` GitHub Actions workflow builds the tagged source and publishes the release with generated notes and build artifacts.

The R17 clean build and all 114 native GameTests passed. The R17/0.0.1a freeze gate still needs native hologram visual acceptance, R14 display/editor and R16 item/fluid/FE transfer acceptance, escrow reload, and dedicated-server smoke testing. R1 is an early 0.0.2a development build and must not be treated as closing those gates.
