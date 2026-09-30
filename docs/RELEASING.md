# Building and releasing versions

Each published version gets a tagged Windows/Java 21 build and a GitHub Release containing the mod JAR, sources JAR and SHA-256 checksums. The release workflow verifies that the tag version exactly matches Gradle's `version`, then runs `clean build runGameTestServer`; it publishes nothing if any build, asset, rule or native GameTest gate fails.

To release a version:

1. Update `version` in `build.gradle` and matching version references in mod metadata and release documentation.
2. Confirm `.\gradlew.bat --no-daemon --console=plain clean build runGameTestServer` passes.
3. Create and push a version tag whose name is `v` plus the exact Gradle version. For example:

   ```powershell
   git tag -a v0.0.1a.R17 -m "Foundations PL4 0.0.1a.R17"
   git push origin v0.0.1a.R17
   ```

4. The `Build and release` GitHub Actions workflow builds the tagged source and publishes the release with generated notes and build artifacts.

The current R17 candidate is not releasable yet: `verifyClientAssets` reports six missing Large Display back textures, and the latest native GameTest run passed 94/114 tests. Fix those blockers and rerun the full release gate before tagging.
