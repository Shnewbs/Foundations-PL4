# Foundations PL4 — Multi-version CurseForge publication

Latest recorded upload reconciliation: October 22, 2026. RC source/build preparation was updated October 22; historical sections retain their original snapshot dates.

## Additional RC qualification evidence — no publication

All 24 maintained development targets have independently passing native gates. Modern development source now also fences fluid/FE/native-energy and Wireless Storage handoff uncertainty. The exact-target native gates remain separate from publication clearance. The 1.21.1 development candidate has actual client snapshots, two real loopback-client storage sessions, a generated released-tag-source world upgrade and native operation measurements; scope and remaining gaps are recorded in the [RC ledger](RC_DEVELOPMENT_STATUS.json) and [engineering roadmap](../FOUNDATIONS_PL4_MASTER_STATUS_PARITY_ROADMAP.md). No new stable RC has been submitted to CurseForge, and the prior published receipt inventory is unchanged.

## October 22 development hardening

No stable RC has been submitted. Both legacy development candidates gained basic readers/displays, persistent item transaction quarantine and 120 passing native scenarios, production-only boots and generated preview.3 world upgrades. All 22 modern targets passed their independent native gates with matching item-fault protection (20 in the common run, two 26.x targets in the repair run). See [current engineering evidence](../FOUNDATIONS_PL4_MASTER_STATUS_PARITY_ROADMAP.md).

The old 1.12.2 branch workflow briefly exposed `mc1.12.2-v0.2.0-rc.1-dev` as a GitHub prerelease; it was returned to a private draft and was not sent to CurseForge. Legacy publishing rejects development maturity, and the CurseForge publisher now excludes development/snapshot/nightly versions from both backfill and explicit submissions. The [October 22 reconciliation](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39776495422) retained **24 existing receipt records, zero failures**. The draft development snapshot is not an additional published RC or a new CurseForge file.

## October 21 RC preparation

The `0.2.0-rc.1-dev` work is an unpublished development snapshot across the 22 modern ports. Native verification and source cleanup do not promote these builds to a stable RC. The two legacy previews retain their smaller transport feature sets, and client/API/multiplayer/migration/performance acceptance remains open. **No new stable RC was submitted in this preparation pass.** The 24 receipt inventory below still describes previously published revisions (six performance betas and eighteen earlier revisions). See [the current RC roadmap](../FOUNDATIONS_PL4_MASTER_STATUS_PARITY_ROADMAP.md).

## October 18 scheduled reconciliation

The [latest successful cross-version run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39134007983) rechecked the newest published release for **24 exact Minecraft targets**. Every target had a matching CurseForge upload receipt; **zero failed submissions**. This is upload-API evidence, not public availability, functional parity, or proof that every port has the performance beta.

## October 17 Forge 1.20.x beta follow-up (US Pacific)

Three additional exact-target Forge `0.2b-port.1` JARs passed 191 native GameTests each and were published as GitHub prereleases. The checksum-verified CurseForge workflow accepted these files with **beta** metadata:

| Minecraft | GitHub release | CurseForge file ID |
|---|---|---:|
| 1.20.1 | [mc1.20.1-v0.2b-port.1](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.20.1-v0.2b-port.1) | 9234054 |
| 1.20.2 | [mc1.20.2-v0.2b-port.1](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.20.2-v0.2b-port.1) | 9234049 |
| 1.20.4 | [mc1.20.4-v0.2b-port.1](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.20.4-v0.2b-port.1) | 9234048 |

These supersede the earlier 1.20.x alpha receipts for those three exact patches. The remaining 18 targets from the 24-target inventory still have their prior releases; no beta parity or performance result transfers to them. CurseForge moderator approval and public listing are not established by API acceptance.

## October 17 performance beta (US Pacific)

The exact `0.2b.0` NeoForge runtime JARs passed native gates, were published as GitHub prereleases, and were accepted by the CurseForge upload API with **beta** maturity. These receipts supersede the 0.2a.1 alpha receipts for the three current tracks; moderator approval and public listing remain unverified.

| Minecraft | GitHub release | CurseForge file ID | Native gate |
|---|---|---:|---|
| 1.21.1 | [v0.2b.0](https://github.com/Shnewbs/Foundations-PL4/releases/tag/v0.2b.0) | 9233710 | 192 GameTests |
| 26.1.2 | [mc26.1.2-v0.2b.0](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc26.1.2-v0.2b.0) | 9233734 | 192 fixtures |
| 26.3 | [mc26.3-v0.2b.0](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc26.3-v0.2b.0) | 9233735 | 192 fixtures on each validated NeoForge beta |

The change reuses endpoint target keys in transfer routing and status checks; gameplay rates and routing semantics are unchanged. No large-network TPS/MSPT improvement is claimed without measured benchmarks. Installed optional providers, real client visuals, live multiplayer, migration and public CurseForge availability remain separate acceptance gates. The other independently versioned Forge and legacy ports remain on their prior revisions.

## October 16 update (US Pacific; October 17 UTC)

The [new reconciliation run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39005816914) recorded **24 latest Minecraft target receipts, zero failures**. New performance revisions passed their exact Forge native gates, were published to GitHub and accepted by the CurseForge upload API:

| Minecraft | Exact revision | CurseForge file ID | Gate |
|---|---|---:|---|
| 1.20.1 | `mc1.20.1-v0.2a-port.3` | 9219142 | 191 native GameTests; API accepted |
| 1.20.2 | `mc1.20.2-v0.2a-port.2` | 9219143 | 191 native GameTests; API accepted |
| 1.20.4 | `mc1.20.4-v0.2a-port.2` | 9219144 | 191 native GameTests; API accepted |

The new inventory also includes previously accepted 1.19, 1.19.1, 1.19.3, 1.19.4 and 1.20 point releases. The table below is an **October 9 historical snapshot**; its 1.20.1 port.1 receipt has been superseded. Approval, public listing and installed-client acceptance are separate gates.

### 1.21.1 follow-up

The [0.2a.1 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/v0.2a.1) passed 192 native GameTests and its release workflow submitted the exact 1.21.1 NeoForge JAR to CurseForge: **file 9219192**. The earlier 0.2a file ID in the historical table below remains a historical receipt. The compact Wireless Storage panel has not yet passed real-client visual acceptance; the two fake-player server scenario does not certify live multiplayer.

### 26.x follow-up

The exact `0.2a.1` [26.1.2 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc26.1.2-v0.2a.1) and [26.3 release](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc26.3-v0.2a.1) passed their respective Java 25 native gates and were accepted by CurseForge's upload API as **9219212** and **9219214**. The 26.3 validation ran 192 fixtures on each pinned 26.3.0.23-beta and 26.3.0.39-beta profile; the release workflow publishes its pinned exact-version runtime. Both client visual acceptance and installed optional-provider testing remain open.

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
