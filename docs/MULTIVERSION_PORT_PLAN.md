# PL4 parallel Minecraft version tracks

## Branches
| Branch | Role | Current compatibility |
|---|---|---|
| main | Existing 1.21.1 release baseline and coordination | 1.21.1 / NeoForge 21.1.250 / Java 21 |
| mc/1.21.1 | Maintained 1.21.1 track | Starts from the R1.6 source |
| mc/26.1 | 26.1 backport/compatibility track | Scaffold only; still contains 1.21.1 build settings |
| mc/26.2 | 26.2 backport/compatibility track | Scaffold only; still contains 1.21.1 build settings |
| mc/26.3 | Primary new-version port | Scaffold only; still contains 1.21.1 build settings |
| mc/26.4 | Future track, create when its target toolchain is available | Planned, no compatibility claim |

Creating a branch does not complete a port. Never label or upload a 1.21.1 artifact as a 26.x build.

## Port sequence
1. Keep the existing 1.21.1 release path operational.
2. Verify and pin the actual NeoForge and Java toolchain for each 26.x target. NeoForge's 26.1 migration guide specifies Java 25 and Gradle 9.1 or newer; verify later targets independently.
3. Start the 26.3 port workspace alongside 1.21.1. Migrate registration, metadata, codecs, saved data, networking, recipes, models, screen rendering, world displays and GameTests.
4. Migrate item/fluid/energy transfer APIs and revalidate optional Mekanism, GregTech and Voltaic adapters against the exact versions available for that Minecraft target. Omit unsupported adapters explicitly.
5. Preserve UUID identity, escrow, conversion policy, caps, undo/redo and immediate cable geometry. Test save/load, chunk lifecycle, removal, multiplayer permissions and packet validation on each track.
6. Apply shared feature/fix commits across tracks by reviewed cherry-picks. Keep Minecraft-specific rendering and loader changes on their own track; do not merge whole port branches blindly into main.
7. Establish 26.1 and 26.2 compatibility separately. Do not infer binary compatibility from a successful 26.3 build.
8. Create the 26.4 track when its toolchain is available and run the same acceptance gates.

## Release and CI gates
Current workflows publish from main and use 1.21.1 artifact names. Version branches do not yet have automatic publication.
Before enabling port releases:
- parameterize per-track Minecraft, NeoForge, Java, artifact names and supported metadata;
- use distinct tags, for example mc26.3-v0.0.2a.R1.6, to avoid colliding with existing v0.0.2a.R1.6 tags;
- verify tag-to-branch/version matching and independent build/native GameTest jobs;
- select exact CurseForge Minecraft/loader/environment IDs per target and retain duplicate-upload protection;
- publish only the tested target artifacts; retain separate runtime and sources JARs and checksums;
- require in-game visual and installed-provider acceptance for each port.

## References
- https://neoforged.net/news/26.1release/
- https://neoforged.net/news/transfer-rework/
