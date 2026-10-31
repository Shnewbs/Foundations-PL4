# Direct CurseForge upload (without GitHub Actions or a GitHub Release)

The local upload path submits a tested, exact-target runtime JAR and its version-specific Markdown changelog. It does not create a GitHub release or run GitHub Actions.

1. Build and run the native scenarios for the exact Minecraft branch. Check that the JAR name, embedded mod version, loader metadata and `BUILD_STATUS.json` agree. Record any client/API limitations in the changelog.
2. Validate the upload metadata before publishing:

   ```sh
   python -B tools/direct_curseforge_upload.py build/libs/FoundationsPL4-1.21.1-<version>.jar docs/releases/<version>.md
   ```

3. Set `CURSEFORGE_API_TOKEN` in the local process environment (never in a checked-in file). The project ID defaults to the existing PL4 CurseForge project `1716767`, and can be overridden with `CURSEFORGE_PROJECT_ID`. Submit the same command with `--upload`.

The tool refuses source JARs, empty notes, mismatched mod IDs/versions/Minecraft targets and a changed JAR with an existing receipt. An accepted upload writes `<jar-base>.curseforge.json` beside the JAR with the exact SHA-256 and CurseForge file ID. Keep that receipt with the delivered build. If the network fails during upload and there is no receipt, inspect the project's file list before retrying to avoid a duplicate. API acceptance and CurseForge moderation/public availability are separate states.

For Forge 1.16.4 and 1.16.5, supply the verified `-java8.jar` output. The unconverted intermediate JAR is rejected. Older 1.12.2 and experimental 1.6.4 metadata need separate legacy validation and are outside this direct uploader's current support.

The existing `tools/upload_curseforge.py` remains the GitHub-release workflow path. Neither workflow turns an unverified source branch into a releasable version.

**Publication safety (October 31):** The direct uploader rejects development/nightly versions even if an old matching receipt exists. RC uploads require the checked-out branch's `BUILD_STATUS.json` to match the exact Minecraft/version and explicitly set `rc_publication_cleared: true`; an ordinary dry run applies the same gate. NeoForge runtime metadata must declare a single exact Minecraft version (`[x.y.z]`), not an open or multi-version range. Forge's historically bounded metadata ranges are preserved. These checks are necessary but do **not** substitute for exact-source native, installed-provider, multiplayer, migration and performance acceptance or for verifying the destination after upload.

**Runtime archive integrity (October 31):** Before even printing a dry-run upload plan, the direct uploader now checks every ZIP member's CRC and rejects duplicate archive names, traversal/absolute/backslash paths, and symbolic-link entries. This prevents a syntactically valid mod metadata entry from concealing a corrupted class or a second conflicting metadata record. These checks verify archive structure only; they are **not** a native build, installed-provider result, source-to-JAR attestation, or release clearance. Keep all existing exact-target acceptance and destination-verification requirements.
