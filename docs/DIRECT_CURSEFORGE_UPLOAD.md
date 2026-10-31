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


**Unambiguous mod/dependency metadata (October 31, 11:05 PDT):** Before an upload or dry run, the direct uploader now requires exactly one `foundations_pl4` mod entry, exactly one Minecraft dependency and exactly one dependency for the selected loader. Duplicate Minecraft ranges or loader entries, mixed Forge/NeoForge dependencies and malformed dependency tables are rejected instead of silently accepting the first matching entry. Five regression methods cover these cases. These checks do not establish source-to-JAR provenance or qualify an RC by themselves.


**Exact-target bytecode preflight (October 31, 12:05 PDT):** The direct publisher now reads the header of every PL4 `.class` member and rejects fake/truncated headers, a zero constant-pool count, preview/nonstandard minor versions, and bytecode above the Java runtime supported by the target. Limits: Java 8 / class 52 for 1.16.x and earlier supported Forge ports; Java 16 / class 60 for 1.17.x; Java 17 / class 61 for 1.18–1.20.4; Java 21 / class 65 for 1.20.5–1.21.1; Java 25 / class 69 for 26.x. The checks prevent obvious mispackaging (for example a Java 25 PL4 class in a 1.21.1 JAR) but do **not** fully parse bytecode, prove source provenance, or replace exact-source native/installed validation. No new release is authorized by this preflight.

**Local receipt validation (October 31, 13:08 PDT):** A pre-existing `<jar-base>.curseforge.json` now suppresses a repeat upload only when it is a JSON object containing a positive integer CurseForge file ID (not a Boolean or string), the exact project, Minecraft version, PL4 version, loader and JAR SHA-256, and the explicit `accepted_by_api_not_moderation_verified` status. Missing, corrupt or contradictory receipts fail closed and require manual reconciliation against the CurseForge project before retrying. Even a valid local receipt is **not** a remote existence check or proof of moderation approval. The uploader explicitly prints this distinction; never delete the local JAR solely because a receipt exists.

**Immutable upload snapshot (October 31, 14:05 PDT):** The direct uploader now reads the runtime JAR once and validates its ZIP contents, PL4 class headers and metadata from that same immutable byte snapshot. Its SHA-256 receipt and the multipart bytes sent to CurseForge are derived from that snapshot, even if another process replaces the on-disk JAR after validation. The shared multipart helper remains backward-compatible with callers that do not supply a snapshot. Two new regression methods simulate disk replacement and assert the uploaded bytes and receipt hash stay tied to the inspected original. This closes a local build/upload time-of-check/time-of-use gap, **not** an authenticated remote destination check, native test, or RC release clearance.
