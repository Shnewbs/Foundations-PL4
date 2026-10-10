# Foundations PL4 — Multi-version CurseForge publication

Verified on October 9, 2026 (US Pacific); GitHub Actions timestamps may be October 10 UTC.

## Publication outcome

The [successful cross-version synchronization run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/38023210688) checked **17 latest tagged Minecraft targets** and ended with **zero failed submissions**. Fourteen runtime JARs were newly accepted by CurseForge's upload API; three previously submitted NeoForge releases were correctly skipped using their matching GitHub receipts.

| Minecraft | Loader | Current submitted release | CurseForge file ID | API result |
|---|---|---|---:|---|
| 1.6.4 | Forge | 0.2a-legacy-preview.3 | 9114881 | Accepted; experimental |
| 1.12.2 | Forge | 0.2a-legacy-preview.3 | 9114854 | Accepted; legacy preview |
| 1.14.4 | Forge | 0.2a-port.1 | 9114856 | Accepted; alpha |
| 1.15.2 | Forge | 0.2a-port.1 | 9114858 | Accepted; alpha |
| 1.16.1 | Forge | 0.2a-port.1 | 9114860 | Accepted; alpha |
| 1.16.2 | Forge | 0.2a-port.1 | 9114862 | Accepted; alpha |
| 1.16.3 | Forge | 0.2a-port.1 | 9114865 | Accepted; alpha |
| 1.16.4 | Forge | 0.2a-port.1 | 9114867 | Accepted; alpha |
| 1.16.5 | Forge | 0.2a-port.2 | 9114869 | Accepted; alpha |
| 1.17.1 | Forge | 0.2a-port.1 | 9114871 | Accepted; alpha |
| 1.18.1 | Forge | 0.2a-port.1 | 9114873 | Accepted; alpha |
| 1.18.2 | Forge | 0.2a-port.2 | 9114874 | Accepted; alpha |
| 1.19.2 | Forge | 0.2a-port.1 | 9114877 | Accepted; alpha |
| 1.20.1 | Forge | 0.2a-port.1 | 9114879 | Accepted; alpha |
| 1.21.1 | NeoForge | 0.2a | 9104103 | Already accepted; no duplicate |
| 26.1.2 | NeoForge | 0.2a | 9105031 | Already accepted; no duplicate |
| 26.3 | NeoForge | 0.2a | 9105041 | Already accepted; no duplicate |

Every new submission records \`curseforge-upload.json\` on its corresponding immutable GitHub release tag, with its Minecraft version, Forge/NeoForge loader, exact runtime SHA256, CurseForge file ID, and alpha/beta/release classification.

**Important:** CurseForge's upload API accepting a file ID does not mean moderators have approved the file, that the public site has listed it, or that installed third-party APIs have passed acceptance testing. Project: https://www.curseforge.com/minecraft/mc-mods/practical-logistics-4-foundations .

## Going forward

The default-branch [publishing workflow](../.github/workflows/curseforge.yml) supports manually selected release tags, release publication events, and **hourly reconciliation**. The scheduled check is necessary because GitHub Actions using the repository's \`GITHUB_TOKEN\` cannot reliably trigger another workflow from a GitHub Release it creates. Backfill considers the **newest published, non-draft release per Minecraft version**, not every historical revision.

Before sending any file, the runner downloads the exact-version runtime and \`SHA256SUMS.txt\` from that tag and rejects absent/ambiguous/mismatched checksums. The upload uses the matching Minecraft version and Forge/NeoForge metadata. Receipts prevent duplicate submissions and reject changed JARs under existing release tags. Individual failed targets remain visible in the Actions evidence rather than being silently relabeled or marked complete.

1.13.2 remains a blocked source port and is **not** submitted because native compile/test acceptance is incomplete. The excluded Minecraft 1.7.10 is never uploaded. Future patches appear only after their own successful GitHub release, not when a placeholder branch is created.

**Further API testing is still required** for installed integration mods, real clients, multiplayer and modpack performance.
