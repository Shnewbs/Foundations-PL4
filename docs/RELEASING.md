# Building and releasing versions

Each published version gets a tagged Windows/Java 21 build and a GitHub Release containing the mod JAR, sources JAR and SHA-256 checksums. The release workflow verifies that the tag version exactly matches Gradle's `version`, then runs `clean build runGameTestServer`; it publishes nothing if any build, asset, rule or native GameTest gate fails.

Pushing a new Gradle version to `main` automatically builds, tests, tags and publishes it to GitHub and the configured CurseForge project. Main updates whose version already has a tag skip publication. Tag pushes and manual dispatch remain supported.

To release a version:

1. Update `version` in `build.gradle` and matching version references in mod metadata and release documentation.
2. Confirm `.\gradlew.bat --no-daemon --console=plain clean build runGameTestServer` passes.
3. Create and push a version tag whose name is `v` plus the exact Gradle version. For example:

   ```powershell
   git tag -a v0.0.1a.R17 -m "Foundations PL4 0.0.1a.R17"
   git push origin v0.0.1a.R17
   ```

4. The `Build and release` GitHub Actions workflow builds the tagged source and publishes the release with generated notes and build artifacts.

## Release directly from main

Open Actions → Build and release → Run workflow and choose `main`. The workflow reads the exact Gradle version, builds and runs native GameTests, then creates a tag at that exact source commit and publishes the Release. It refuses an existing version tag pointing to different source; increment the version before a new release.

Releases contain the runtime JAR, sources JAR, complete tracked-source ZIP (including the roadmap and build wrapper), and SHA-256 checksums. Version-specific notes come from `docs/releases/<version>.md`; generated notes are the fallback. Alpha/beta/RC versions are marked as prereleases. Rerunning the same version/source refreshes artifacts without creating duplicate releases.

A successful automated build does not replace client visual acceptance. Release notes must list remaining roadmap scope accurately.

## Optional CurseForge publication

Create or select the CurseForge Minecraft mod project, then configure this GitHub repository:

- Actions variable `CURSEFORGE_PROJECT_ID`: numeric CurseForge project ID.
- Actions secret `CURSEFORGE_API_TOKEN`: the author API token, entered through GitHub's secure secret settings (never committed).

Every release workflow uploads the tested runtime JAR with the same version notes after GitHub publication. It resolves Minecraft 1.21.1 and NeoForge against CurseForge's versions API, and chooses alpha/beta/release from the version. CurseForge approval still controls availability. With neither setting configured, this step skips and GitHub publishing works normally. An incomplete configuration fails visibly.

A `curseforge-upload.json` receipt in the matching GitHub Release records the uploaded file ID and SHA-256. Reruns skip an identical recorded upload and reject changed source/project metadata. A connection failure or failure to save the receipt requires checking the CurseForge project before retrying, because an upload may have succeeded.

API reference: https://support.curseforge.com/support/solutions/articles/9000197321

Configured destination: Practical Logistics 4 - Foundations, project `1716767` (https://www.curseforge.com/minecraft/mc-mods/practical-logistics-4-foundations).

To publish or retry an existing GitHub version, use Actions → Publish existing release to CurseForge → Run workflow on main and enter its tag. This downloads the published runtime JAR, verifies its release checksum, uses the published notes, and uploads without rebuilding or changing the tag. Use this after fixing credentials. The API token must be generated in the CurseForge author dashboard; the general developer API key is a different credential.
