## October 28, 2026 — Native legacy publication gates and captured inventory metadata safety

**Development-only checkpoint.** The 1.6.4 and 1.12.2 push-triggered `legacy*-native.yml` jobs retained a separate tag-and-release route after the primary release workflows were hardened. Those native publishers now independently fail closed unless the exact Forge target, checked-out branch/SHA, Gradle/status version, beta RC maturity and explicit `rc_publication_cleared: true` all match. Before creating a tag they verify one exact runtime SHA256 entry (including 1.6.4's `-srg.jar`), refuse existing remote tags/releases, and use atomic immutable tag creation. Branch commits: [1.6.4 guard](https://github.com/Shnewbs/Foundations-PL4/commit/6e385da520589905da9e538ed950d4018e5d3cd3) and [1.12.2 guard](https://github.com/Shnewbs/Foundations-PL4/commit/0d370e69fdbd5aa1034ce131fbd13cd37f4bd8b9). Post-push static policy readbacks passed 22/22 per branch; no eligible end-to-end Windows publisher execution.

The [1.6.4 runtime repair](https://github.com/Shnewbs/Foundations-PL4/commit/01eeced04350c24cb11211838744937f0c067506) now refuses cached native slot metadata after a tile/provider callback detaches its live backing. Its adversarial native test replaces the tile during `getSizeInventory`, requires zero stale slots, preserved 32 iron and no new drops. The exact-source [native run 40630703237](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40630703237) passed 365/365 scenarios, 40 decode restart JVMs and four provider JVMs, 80 native placements and 80 gravity receipts, plus separate historical fault import (three JVMs/60 checks). The packaged remapped runtime SHA-256 is `58c2033386e926bf36b743d0b9b7ac982406f5a1c543e022568157b9ca8274d2`; [artifact 12791681550](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40630703237/artifacts/12791681550), 193 CRC-valid ZIP members, artifact SHA-256 `2e28e3d8acaf61b80a8be6827fbbb67b0f7caf72269a5c6aef2982e17cf35d22`. No new 1.6.4 client, installed optional-provider, multiplayer or sustained-performance acceptance. Previous client evidence is **not** byte-identical to this new JAR.

A retained [failed 1.6.4 diagnostic](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40619649369) directly recorded gravel ID 13 at x80/y200/z32, an overhead gravel block at y201 and one nearby falling-gravel actor (the later failure was `decode-rewrite/write`; the earlier independent x80 failure was `decode-scope/crash-write`). This supports a falling-physics interference hypothesis, but the original failure's actor cause is **not proven**. Successful guarded runs observed zero entities removed; deterministic falling-actor injection and full world/owner acceptance remain open.

Evidence: [new exact-source checkpoint](docs/acceptance/rc-dev-legacy-native-publisher-stale-slots-2026-10-28.json). RC clearance **0/24**; no new GitHub RC release, CurseForge upload or public visibility claim.

# Foundations PL4 — Master Status, Parity Audit, Roadmap, and Expansion Plan

**Roadmap reconciliation:** October 26, 2026 (America/Los_Angeles); historical sections retain their original snapshot dates.
**Current primary-track released baseline:** performance-only `0.2b.0` beta for Minecraft `1.21.1`, `26.1.2` and `26.3`, each as a separate exact-target JAR. Independently versioned Forge ports and legacy previews are listed below.
**Project acceptance:** performance betas and alpha/experimental legacy previews. Full Practical Logistics 2 parity remains incomplete. Further installed-mod API, client visual, multiplayer, sustained-load and fallback testing is required.
**Source of truth:** this section is the current execution roadmap. Older R16/R17, `0.0.2a` phase and `0.1b` sections farther down are retained as historical feature/parity evidence, **not current release or port status**.

## Legacy release asset and no-clobber corrections — October 28, 2026

Exact `mc/1.6.4` publisher source originally validated `FoundationsPL4-1.6.4-<version>-srg.jar` but attempted to publish the unsuffixed runtime JAR. The corrected existing-branch workflow at `9e363db9` now publishes the remapped artifact and requires a unique matching SHA256SUMS entry before tag mutation. The `mc/1.12.2` workflow at `92e7435f` now also rehashes its runtime. Both branch publishers reject wrong loader/target, unclassified or uncleared RC channels and existing-release asset overwrites (`--clobber`). Static post-commit readback passes twelve policy checks per branch; no eligible source, new JAR, Windows publisher run, GitHub RC or CurseForge upload. All 24 RC clearances stay false. Preserve previous 1.6.4 gravel failure evidence and continue exact native/provider/multiplayer/real-client/performance acceptance. Evidence: `docs/acceptance/rc-dev-legacy-release-asset-integrity-2026-10-28.json`.

## Native fixture placement evidence — October 26, 2026

The [placement diagnostic checkpoint](docs/acceptance/rc-dev-legacy-placement-diagnostics-2026-10-26.json) repairs1.6.4 restart-runner evidence loss: exact raw phase JSON is retained before parsing or acceptance, with PID/exit/log/report hashes even for missing or malformed reports. The isolated fixture records native setBlock/block/metadata/tile observations and fails explicitly without creating a synthetic tile or retrying placement. Four retention and150 targeted local controller tests pass;201 discovery plus4 retention executions pass in trusted CI.

Pinned164 ordinary365/365,decode1536/40 JVMs,provider188 andhistorical60 pass. All80 successful placement observations,44 raw phase/process records,20 raw decode worlds and12 newly packaged hashes across164/main were independently verified;main192 passes. The164 production JAR remains byte-identical246afad2, retaining prior exact-payload client evidence without a new client run or screenshot review. Independent branch40448018062 also passes and its publisher is skipped.

The original40442840965 placement NPE remains unresolved. Its compound source line and lost phase JSON do not establish the exact x, failed dereference or seed assertion coverage. Successful instrumented runs do not resolve an intermittent cause. Continue chunk-provider residency/load/population and individual lookup-stage diagnosis on recurrence, then actual handheld endpoint/provider-discovery ordering and loaded/unloaded alias work. Installed providers, dedicated multiplayer/online identity/travel, arbitrary saves, reviewed reconciliation, sustained performance and broader parity remain open. All24 RC flags remainfalse;no new release/upload.

## Combined live mutations across native processes — October 26, 2026

The [live mutation process checkpoint](docs/acceptance/rc-dev-legacy-live-mutation-process-2026-10-26.json) adds164 separate-suite assertions per legacy port across4 fresh JVMs. Combined owner/context edits after cleared scoped admission are observed by transfer readmission; a disconnected same-target alias stays fenced, physical source chests retain32 items, original evidence/tokens survive, and actual removal retains sealed8-unit receipts. Clean and completed-save/flush parent-SIGKILL paths pass; total decode is1208/32 JVMs, separate from provider188 andhistorical60. All16 decode worlds per port and17 packaged hashes qualify. Ordinary native383/361 andmain192 pass;136 targeted local and187 final CI executions pass.

Copied-save diagnostics now select native1.6.4 `OwnerName` rather than omitting the label or accepting the neighboring `Owner` key. Two actual native-copy omissions reproduce under the old collector and pass after the fix;16 bounded queries leave archives unchanged and grant no authorization/reconciliation clearance. The initial new world-controller key mismatch is preserved and corrected. Fresh1.12.2 exact-JAR client passes, with2 captures reviewed and21 hashed; its uncompressed JAR entries are identical to the prior payload despite a changed archive hash.1.6.4 production bytes remain identical, retaining prior exact-payload client evidence without a new run/review.

Next extend distinct owner-only/context-only process variants and unobserved/disconnected alias and loaded/unloaded boundaries without global scanning or force-loading. Installed providers, dedicated multiplayer/online identities/actual travel, arbitrary user/modpack saves, reviewed reconciliation, sustained full-server/provider performance, launcher profiles and broader parity remain unfinished. The other21 targets retain their recorded evidence. Both branch publication steps were skipped; the existing development draft remains unchanged and ineligible. All24 RC flags remainfalse;no new release/upload.

## Context-only native process acceptance — October 26, 2026

The [context-only process checkpoint](docs/acceptance/rc-dev-legacy-context-only-process-2026-10-26.json) adds164 assertions across4 fresh JVMs per legacy port while keeping the current host owner unchanged. Rewritten saved context is rejected at transfer readmission; observed disconnected aliases remain fenced, original evidence/tokens and actual sealed removal receipts survive, and physical source inventories retain32 items. Totaldecode1372/36 JVMs per port passes, separate from provider188 andhistorical60. Ordinary native383/361/main192,138 local and189 CI controller executions pass. All17 packaged hashes,18 raw decode worlds per port and40 distinct decode/provider process identities qualify.

Fresh112 exact packaged-JAR client passes full5-page export and normal regressions;2 captures reviewed and21 hashed.164 production bytes remain identical, retaining prior exact-payload client evidence without a new run or review. Owner-only process coverage, unobserved/disconnected aliases and unloaded boundaries, installed providers, dedicated multiplayer/online identity/travel, arbitrary saves, reviewed reconciliation, sustained performance and broader parity remain open. No global scan/force-load, replay, redemption, minting or fence clearance is added. Both branch publishers were skipped;all24 RC flags remainfalse;no new release/upload.

## Owner-only native process acceptance — October 26, 2026

The [owner-only process checkpoint](docs/acceptance/rc-dev-legacy-owner-only-process-2026-10-26.json) adds164 assertions through4 native JVMs per legacy port, changing only the current host owner while preserving its original journal context. Transfer readmission and the observed disconnected alias remain fenced; original evidence/tokens, sealed actual-removal receipts and32-item physical source stocks survive clean and completed-save/flush parent-SIGKILL paths. Totaldecode1536/40 JVMs per port passes, separate from provider188 andhistorical60. Ordinary native383/361/main192,140 local and191 CI controller executions pass. All17 packaged hashes,20 raw decode worlds per port and44 distinct decode/provider process identities qualify.

Eight bounded native-copy queries preserve the changed host versus original journal owner labels, sealed claims and unchanged archives without granting authorization or reconciliation clearance. Fresh112 exact-JAR stock client passes complete5-page export and normal regressions;2 captures reviewed/21 hashed.164 production hash is unchanged, retaining prior exact-payload client evidence with no new run/review. CurseForge reconciliation records24 existing submissions,0 new uploads and0 failures; moderation/public visibility was not reverified. Both branch publishers were skipped;all24 RC flagsfalse;no release/upload.

Next probe actual handheld endpoint discovery and native disconnected aliases before and after host observation, plus loaded/unloaded boundaries without global scanning or force-load. Source inspection found readiness checks retained-node quarantine without saved-context admission; no new bypass is demonstrated. Installed providers, dedicated multiplayer/online identity/travel, arbitrary saves, reviewed recovery, sustained performance and broader parity remain open.

## Explicit handheld readiness admission — October 26, 2026

The [readiness checkpoint](docs/acceptance/rc-dev-legacy-handheld-readiness-2026-10-26.json) invokes the existing saved-context validator before handheld readiness checks. Both legacy ports reproduced3 failing cases plus1 passing scoped/idempotent control; fixed sources pass all4 at native387/365, withmain192 and146 local/197 CI controller executions passing. The alias case observes the source host through readiness before querying a disconnected same-target alias. No global scan, force-load, provider discovery, replay, redemption or fence clear is added.

Pinned hourly gates independently qualify existing decode1536/40 JVMs,provider188 andhistorical60,all17 packaged hashes and20 raw decode worlds per port. Both fresh exact-JAR clients pass complete5/4-page exports and normal storage regressions;4 captures reviewed/42 hashed. The parallel164 branch run failed a null native tile in `DecodeRestartScenarios.place` during decode-missing/write preparation; the same phase passed hourly. Its artifact/log hashes are preserved, its placement/lifecycle cause remains unresolved and no acceptance is attributed to that failed phase. Local extraction ran out of temporary space, was repaired by deleting only hash-verified prior duplicate ZIPs already preserved in GitHub, and requalified after extraction completed.

Next diagnose the164 native placement failure and probe real handheld endpoint/provider-discovery order, skipped hosts and disconnected aliases before readiness, including loaded/unloaded boundaries. Installed providers, multiplayer/online identity/travel, arbitrary saves, reviewed recovery, sustained performance and broader parity remain open. Both publishers were skipped;all24 RC flagsfalse;no new release/upload.

## Hourly continuation and exact-source builds

The [hourly implementation/build/publication policy](docs/RC_HOURLY_WORKFLOW.md) is active. Its [first native revalidation](docs/acceptance/rc-dev-hourly-native-2026-10-25.json) passed all 24 maintained exact targets, with pinned commits, packaged hashes and retained artifacts. Main also passed 192 native tests; 30 scheduler/publication/upload regressions passed. The guarded CurseForge synchronization confirmed 24 existing release receipts with zero failures. This produced no new RC release and does not transfer prior client/provider acceptance onto new JAR hashes. The main and common CurseForge publishers now require explicit tagged-source RC clearance. The subsequent [branch-local publisher hardening](docs/acceptance/rc-dev-publisher-guards-2026-10-25.json) upgrades 44 workflows across 23 existing version branches, including the unguarded 1.20.1 publisher, to reject snapshots and require explicit boolean RC clearance. Native validation steps and runtime source are preserved; 34 policy/controller/upload regression cases passed. Historical frozen controllers remain excluded from execution. This workflow-only checkpoint adds no gameplay acceptance or new RC release.

## Receipt authorization after restart — October 26, 2026

The [receipt authorization checkpoint](docs/acceptance/rc-dev-legacy-receipt-authorization-2026-10-26.json) binds diagnostic queries to the player's current server world before reading receipt context or journal. Actual command handlers already supplied that world; this is API boundary hardening, not a claimed remote exploit fix.

Both legacy ports pass 64 additional assertions covering recorded owner, unrelated profile, operator messages and foreign WorldServer rejection after clean restart and completed-save/flush parent SIGKILL. They are a subset of the four field suites (528 assertions/16 JVMs); total decode is 876/28 JVMs, separate from provider188/four JVMs and historical60/three JVMs. Ordinary native370/348, main192 and149 local test executions pass. Raw NBT,32 distinct provider/decode process identities per port and17 packaged hashes were verified. Both changed production JARs pass fresh stock clients, full export reconstruction and existing normal regressions; four current captures reviewed and42 hashed.

Live public field mutation remains open: inspect clearing itemFault while ItemLegacyEvidence remains, then test owner/context edits and removal before claiming a transfer bypass or fix. Dedicated multiplayer, online identity, actual dimension travel, installed providers, user/modpack worlds, reviewed reconciliation, sustained performance and broader parity remain unfinished. No recovery, replay, redemption, mint or fence clear is supplied. Branch publication steps were skipped; all24 RC flags remainfalse, with no new release/upload.

## Malformed-field native process acceptance — October 26, 2026

The [field-process checkpoint](docs/acceptance/rc-dev-legacy-field-process-2026-10-26.json) adds separate suites for a valid original token with an integer Owner, string Dimension, nested compound Scope, or typed foreign owner/dimension/host rewrite. Each legacy port passes 464 new assertions across 16 JVMs. Clean and completed-save/flush parent-SIGKILL paths preserve exact rejected contexts, nested evidence, original tokens, actual sealed removal receipts and the opaque dimension barrier. All 28 retained decode worlds were independently inspected.

Total decode coverage is 812 assertions across 28 JVMs per port; provider 188/four JVMs and historical 60/three JVMs remain separate. Ordinary native suites pass 370/348, main passes 192, and 149 local test executions pass. No production source code changed. The changed 1.12.2 packaged hash receives a fresh stock-client pass, complete five-page export reconstruction, two reviewed captures and 21 capture hashes. The byte-identical 1.6.4 production JAR retains prior exact client evidence; no fresh run or screenshot review is claimed.

Post-restart receipt authorization across profiles/dimensions, live external field rewrites, linked-host/receipt exports, installed-provider identity/API/access-control/durability, user/modpack worlds, reviewed reconciliation, sustained performance and broader parity remain open. No original route, spendable replacement, replay, redemption, mint or fence clear is supplied. The other 21 tracks retain their exact evidence. Both branch publishing steps were skipped; all 24 RC flags remain false and no release/upload was created.

## Bounded opaque-journal export — October 26, 2026

The [opaque export checkpoint](docs/acceptance/rc-dev-legacy-opaque-journal-export-2026-10-26.json) adds operator-only read-only original NBT export in the current legacy dimension. Unknown ownership does not authorize access to unrelated opaque records. Snapshots stop at 16 KiB, page 128-byte hex blocks, carry exact native SHA-256, and retain every fence. A bounded transcript verifier rejects changed, mixed, missing, duplicate or tampered pages; native byte hashes are separate from parsed-value and cross-version identities.

Each legacy port passes 13 new native cases, at 370/348; main passes 192. All 133 local test executions, 17 packaged hashes, raw exported NBT and existing provider 188/decode 348/historical 60 evidence qualify separately. Both exact hourly-JAR stock clients pass full export chat reconstruction (five/four pages) plus normal storage/search/grouping/replay/codec/display regressions. Four captures were reviewed and 42 hashed. The failed initial test syntax/API attempts and 1.6.4 JSON chat probe timeout are retained; corrected fixtures and probe pass without changing the latter's production JAR.

The other 21 tracks retain their exact evidence. Larger/offline user saves, other malformed-field process paths, linked-host/receipt export, dedicated multiplayer/online profiles, installed-provider identity/API/auth/durability, reviewed reconciliation, sustained performance and broader parity remain open. No replay, redemption, mint or fence clear is supplied. All 24 RC flags stay false; CurseForge reconciliation skips 24 existing submissions and creates no new upload.

## Legacy evidence settlement and compound native processes — October 26, 2026

The [compound-process checkpoint](docs/acceptance/rc-dev-legacy-compound-process-2026-10-26.json) prevents generic escrow settlement from clearing faults backed by preserved legacy evidence. Each legacy port passes 116 new assertions across four JVMs, with independent NBT checks of two retained worlds. Total decode coverage is 348 assertions across twelve JVMs per port; the 188 provider restart and 60 historical checks remain separate. Ordinary native suites pass 357/335 and main passes 192.

Both fresh exact hourly-JAR clients pass; four current captures were reviewed and forty hashed. All 120 local test executions pass. The observed 1.6.4 artifact HTTP 503 failure is preserved; a bounded retry and checksum download repair succeeds. The other 21 tracks retain their exact evidence. In-game export, other malformed fields, arbitrary rewrites, installed providers, reviewed reconciliation, multiplayer, performance and broader parity remain open. All 24 RC flags remain false; no new release or upload.

## Copied-save record and digest validation — October 26, 2026

The [collector hardening checkpoint](docs/acceptance/rc-dev-copied-save-record-validation-2026-10-26.json) withholds original-target attribution for opaque, malformed and duplicate-token records, rejects malformed containers and uses versioned typed parsed-value hashes. Byte arrays and look-alike compounds no longer share an encoding.115 local test executions,24 native copied-save queries and10 generated malformed-copy cases pass; originals remain unchanged. Main a0b450e2 passes192 native with unchanged production bytes. These are diagnostic/offline checks, not additional legacy native/crash/provider acceptance. Existing exact legacy/client evidence remains retained. Rejected-compound process fixtures, in-game export, installed-provider identity/auth/durability, reviewed reconciliation and broader parity remain open;all24 RC flags false;no release/upload.

## Decode-path native processes and copied-save observations — October 26, 2026

The [process and collector checkpoint](docs/acceptance/rc-dev-legacy-decode-process-restart-2026-10-26.json) adds232 dedicated raw/missing decode assertions across8 JVMs per legacy port, separate from ordinary native357/335,188 provider restart assertions and60 historical checks. Clean and completed-save/flush parent-SIGKILL paths preserve live faulted hosts, actual sealed removal receipts, original nested evidence, raw integer/string types or absence, prior tokens and the opaque dimension barrier. Independent NBT reads verify8 decode,4 provider-restart and2 historical world archives. Main421981c8 passes192;21 other tracks retain evidence.

A bounded read-only copied-save collector records token/owner-label/provider/inventory observations with hashes, explicit partial scans and no inferred balances, history or clearance.24 native-fixture queries and106 local test executions pass. Cross-region priority was repaired and tested. Fresh exact hourly-JAR1.12.2 client passes;2 current captures reviewed and20 hashed.1.6.4 retains prior client proof for identical production bytes. Dedicated rejected-compound/other malformed-field process cases, in-game export, actual installed-provider identity/access control/durability, arbitrary copied worlds, reviewed reconciliation, multiplayer, performance and broader parity remain open. All24 RC flags stay false; no new release/upload.

## Missing and raw context preservation — October 26, 2026

The [decode-preservation checkpoint](docs/acceptance/rc-dev-legacy-decode-context-preservation-2026-10-26.json) preserves saved legacy evidence and a current malformed context's native type through tile saves and sealed removal. Missing recorded contexts remain absent and unresolved, even after fault text disappears; generic settlement respects raw/missing flags. Ten new native cases per port pass at 357/335, plus production startup, preview.3 migration, 188 restart assertions and 60 historical checks; main passes 192 and 21 other tracks retain evidence. Historical fixture placement now loads its explicit disposable-world chunks. Qualification checks migration fixture hashes and restart native version/PASS/shutdown/readiness markers; 77 local test executions pass. Dedicated new-path process/crash evidence, other malformed fields, bounded read-only reconciliation, installed providers, copied worlds, multiplayer, performance and broader parity remain unfinished. All 24 RC flags remain false; no new release/upload.

## Rejected compound evidence preservation — October 26, 2026

The [rejected-context checkpoint](docs/acceptance/rc-dev-legacy-rejected-context-evidence-2026-10-26.json) fixes replacement of a rejected compound after legacy evidence already exists. It preserves that context, original evidence and prior journal in an opaque envelope, freezing all item targets in that dimension without recursive wrapping or new tokens. Six new native cases per port pass at 347/325, alongside production startup, preview.3 migration, 188 restart assertions and 60 historical checks; main passes 192 and 21 other tracks retain evidence. The corrected 1.6.4 source uses its native void NBT API and named-tag payload comparisons. Qualification cross-checks raw restart phase JSON and packaged fixture hashes; 71 local test executions pass. Missing/noncompound decoding, new-path process durability, reviewed recovery, installed providers, multiplayer, performance and broader parity remain open. All 24 RC flags stay false; no new release/upload.

## Historical legacy provider outcomes — October 26, 2026

The [historical provider checkpoint](docs/acceptance/rc-dev-legacy-historical-provider-outcomes-2026-10-26.json) adds actual post-mutation provider faults under the pinned old native JAR, preserving the registered provider's own private state through candidate unknown-route import, host removal and clean restart. Each legacy port passes 60 three-process checks, 188 current-runtime restart assertions and native totals 341/319, production startup and preview.3 migration. Final saved-world NBT independently matches provider identities, ownership labels, counters and inventory counts. Main passes 192; 21 other tracks retain evidence. Original receipt targets/phases stay unknown, barriers remain active, extraction loss is not fabricated and insertion claims remain sealed. Installed-provider access control/durability, reviewed recovery, arbitrary worlds, crash atomicity, multiplayer, performance and broader parity remain open. No RC clearance or new release/upload.

## Registered legacy provider private-state persistence — October 26, 2026

The [provider-persistence checkpoint](docs/acceptance/rc-dev-legacy-provider-persistence-2026-10-26.json) replaces vanilla normalization in the isolated restart fixture with an actually registered adversarial provider. Each exact legacy port passes 188 lifecycle assertions across four JVMs, preserving the provider's own inventory, private UUID, ownership label, armed failure mode and executed mutation counters. Direct region-NBT inspection checks both retained worlds per port. Source post-removal loss stays 8 unaccounted; insertion leaves 32 + 8 spendable units plus a nonspendable sealed 8-unit claim. A replacement host stays fenced without another provider mutation. Native 341/319, production startup, preview.3 migration and 24 historical checks pass; main 192 and 21 other target baselines remain recorded. 48 controller regressions pass. This is fixture-provider evidence, with default-dimension ownership labels; installed optional providers, actual provider access control, historical unknown-route private outcomes, reviewed recovery, arbitrary crash/power-loss behavior, multiplayer, performance and broader parity remain open. All 24 RC flags remain false; no release/upload.

## Legacy saved-context scalar type hardening — October 26, 2026

The [scalar-type checkpoint](docs/acceptance/rc-dev-legacy-context-scalar-types-2026-10-26.json) guards nested token/owner/scope/phase tags before native string decoding and requires typed receipt owner/dimension fields for owner authentication. Malformed identity requires operator inspection without clearance; bad phases remain unknown. Six new native cases each pass on exact 1.12.2/1.6.4 sources (341/341 and319/319), plus production startup, preview.3 migration,124 current-runtime restart assertions and 24 historical migration checks per port. Both exact-JAR stock clients pass normal actions and imported-unknown native chat; four current captures reviewed/preserved and40 hashed. Main passes192;21 other tracks retain evidence. The hourly gate now checks actual scenario JSON counters/per-case outcomes and native completion markers against summaries;38 targeted regressions pass. No recovery/replay/minting/fence-clear operation or RC clearance is added. Installed providers, provider outcomes/recovery, repeated-corruption history, copied worlds, multiplayer, crash atomicity, sustained performance and broader parity remain open.

## Legacy historical item-fault import — October 26, 2026

The [historical-fault checkpoint](docs/acceptance/rc-dev-legacy-preupgrade-fault-import-2026-10-26.json) imports observed loaded faulted tiles with missing, mismatched or malformed operation history as explicit `legacy-unknown` records. It preserves the observed host and raw evidence without inventing an original target or mutation phase. The observed owner is fenced throughout that dimension; unowned faults fence all owners. Known valid target-linked contexts retain their narrower scope. Removing the host leaves a sealed linked receipt and durable barrier. Already-loose unlinked receipts stay unauthenticated. Journal schema 2 reads schema 1; a full-capacity fallback preserves the previous journal plus unknown fault as opaque data across serialized reload. Pre-journal downgrade is unsafe and remains unsupported; see the [inspection/migration guide](docs/LEGACY_ITEM_JOURNAL_INSPECTION.md).

Exact sources `5689ab3d` (1.12.2) and `bb163a5e` (1.6.4) passed 335/335 and 313/313 native cases, with 23 new cases each, production-only startup and generated preview.3 migration. Per port, 124 current-runtime clean/post-save-kill restart assertions and 24 historical migration checks are recorded separately. An actual pinned pre-journal JVM writes faulted tiles, a candidate JVM imports/removes the hosts, and a third JVM reloads the linked receipts/barriers. Both exact-JAR stock clients render the imported token, unknown original target/phase, observed-host-only and owner-wide/no-replay messages through real chat; inspection leaves the barrier active. Six current captures reviewed and preserved, all forty hashed. Main passes 192; 21 other exact targets retain evidence. Fixture cleanup contamination, 1.6.4 named-NBT comparisons, the native malformed-tag cast exception and the reviewed capacity fallback defect/repairs are recorded. This does not certify arbitrary modpack/user worlds, installed-provider private outcomes, pre-save/power-loss atomicity, full-process capacity overflow, dedicated multiplayer, recovery actions or sustained performance. All 24 RC flags remain false; no new release/upload.

## Legacy item receipts and journal across native processes — October 26, 2026

The [process-restart checkpoint](docs/acceptance/rc-dev-legacy-item-process-restart-2026-10-26.json) adds a separate fixture mod and parent runner to both legacy sources, with production packaging isolated from test classes. Each port passes 124 lifecycle assertions across four fresh JVMs: clean write/read and explicit native world/journal save plus save-handler flush, parent SIGKILL, then read. Observed vanilla inventory contents, sealed receipt context and token/owner/dimension/endpoint fences survive original host removal; a replacement host cannot replay the handoff. Source post-removal exceptions still leave eight units unaccounted for and fenced; destination exceptions leave an uncertain sealed claim, not additional spendable items. Fault injectors are replaced by observed-content vanilla chests before save, so no third-party provider-private durability is inferred. Ordinary native totals remain 312/290; production startup and preview.3 migration pass. Main passes 192 and 21 other exact targets retain recorded native evidence. The exact new 1.12.2 production archive also passes stock-client regression with three current diagnostic/group-withdrawal captures reviewed and eighteen hashed; 1.6.4 retains prior client evidence for its identical production hash with original source attribution. Initial 1.12.2 Runtime.halt interception by Forge is preserved and repaired through parent-owned SIGKILL. This closes only clean and controlled post-save-boundary process persistence. Pre-save crash/power-loss atomicity, pre-upgrade uncertainty import, reviewed recovery, installed providers, copied worlds, multiplayer, performance and broader parity remain open. All 24 RC clearances remain false; no new release/upload.

## Legacy public command dispatch and native chat — October 26, 2026

The [command/chat checkpoint](docs/acceptance/rc-dev-legacy-diagnostic-chat-2026-10-26.json) fixes legacy commands being denied before handler authorization when cheats are disabled. Public dispatcher entry now retains the existing owner/operator/proximity checks inside handlers. Receipt text displays a whitelisted recorded phase; unrecognized values are bounded to unknown. Final exact 1.12.2/1.6.4 sources passed 312/312 and 290/290 native tests, including eleven new command/phase cases each and registered dispatcher regression, plus production startup and generated preview.3 migration. Both stock clients received and rendered journal tokens and linked receipt phase through native chat packets; the artificial unresolved target stayed fenced after host removal and inspection. Six current captures reviewed; all thirty-six hashed. The checkpoint records the earlier fake-player position, no-cheats dispatch and token-only probe-filter failures and their repairs. Main passed 192; 22 other targets retain exact evidence. No recovery/replay/redemption/minting command or RC clearance is provided. Full-process/crash semantics, pre-upgrade uncertainty import, reviewed provider outcome/conservation, installed providers, dedicated multiplayer and sustained performance remain open.

## Legacy receipt-linked item diagnostics — October 26, 2026

The [diagnostic checkpoint](docs/acceptance/rc-dev-legacy-receipt-diagnostics-2026-10-26.json) adds saved active operation token/owner/dimension/target/phase context to both legacy ports and their sealed removal receipts. Nearby authorized host inspection uses sorted four-token pages; held receipt inspection requires its recorded owner or an operator. Historical unlinked receipts are operator-only and do not invent an original target. Missing, mismatched, opaque or wrong-dimension context never grants recovery clearance. No replay, redemption, minting or fence-release command is exposed. Exact 1.12.2/1.6.4 sources passed 301/301 and 279/279 native cases, including fourteen new diagnostics cases each, production startup and generated preview.3 migration. Both exact-JAR ordinary stock-client regressions passed; eight current captures reviewed and all thirty-two hashed. Those probes do not exercise diagnostic command chat rendering. Main passed 192; 22 other targets retain exact evidence. Reviewed recovery actions, command integration, full-process/crash semantics, pre-upgrade fault import, installed-provider/multiplayer/performance and broader parity remain open. RC clearance remains false. See the [inspection guide](docs/LEGACY_ITEM_JOURNAL_INSPECTION.md).

## 1.6.4 persistent item mutation journal — October 25, 2026

The [1.6.4 journal checkpoint](docs/acceptance/rc-dev-164-item-journal-2026-10-25.json) ports normalized legacy-owner/dimension/target fences to wireless and automatic item handoffs using native WorldSavedData, perWorldStorage and WorldSpecificSaveHandler. Separate source/destination phase tokens retain the uncertain endpoint after node removal; validated healthy settlement releases only its own token. Bounded malformed/full journals fail closed and retain their payload. Exact source 87efd5cf passed 265/265 native cases, including seventeen wireless and eighteen automatic journal cases, production startup and generated preview.3 migration. A fresh MapStorage reload reads the compressed file within one process; this does not certify full-process/crash semantics. The exact production JAR passed ordinary stock-client regression with four current captures reviewed and sixteen hashed. Main passed 192; 22 other tracks retain exact evidence. Receipt-linked diagnostics/recovery, pre-upgrade fault import, hidden different-coordinate backing aliases, installed providers, multiplayer and sustained performance remain open. All RC clearance remains false.

## 1.12.2 automatic item mutation uncertainty — October 25, 2026

The [automatic journal checkpoint](docs/acceptance/rc-dev-112-automatic-journal-2026-10-25.json) extends persistent owner/dimension/target fences to automatic item handoffs with separate executed extraction and insertion phases. A failed extraction retains its captured source fence; a failed insertion retains its captured destination fence. Validated known escrow can retry after a well-formed refusal, while other unresolved operation tokens remain intact. Source/destination snapshots and ownership are checked after capability discovery. Token UUIDs are allocated only when executed mutation phases are reached; no measured performance gain is claimed. Final exact source passed 287/287 native tests, including eighteen new phase/fault/cross-network/removal/file-load/refusal/rebind cases and sixteen retained wireless journal cases, production-only startup and generated preview.3 migration. The exact production JAR passed ordinary stock-client regression with four current captures reviewed and sixteen hashed; autonomous transport was disabled in that manual-client probe. Main passed 192 and 22 other tracks retain exact evidence. Pre-upgrade tile-fault import, 1.6.4 journal implementation, receipt-linked recovery diagnostics, hidden different-target backing aliases, full-process/crash and broader provider/multiplayer/performance acceptance remain open. No RC is cleared.

## 1.12.2 persistent wireless target uncertainty — October 25, 2026

The [wireless journal checkpoint](docs/acceptance/rc-dev-112-wireless-journal-2026-10-25.json) records per-dimension owner/target operation tokens before executed wireless handoff mutations. Uncertain targets remain fenced after removal, network changes or a later node accessing that coordinate; wireless and autonomous item admission consult the index without scanning loaded world tiles. Validated empty settlement removes only its own token. The journal caps unresolved records at 4096; full/malformed data fail closed and preserve uncertainty. All 269 native cases passed, including sixteen new cross-network/removal/settlement/provider-fault/capacity/malformed-data and real compressed-file save/load cases, plus production-only startup and generated preview.3 migration. The file reload uses a fresh MapStorage within one process, not crash/restart acceptance. The exact production JAR passed ordinary stock-client regression; four current captures were reviewed and sixteen hashed. Main passed 192; 22 other tracks retain recorded artifacts. Automatic-origin coverage is recorded in the subsequent checkpoint above. Pre-upgrade fault import, the 1.6.4 journal port, different-coordinate hidden backing aliases, receipt-linked recovery diagnostics, full process/crash recovery and broader provider/multiplayer/performance acceptance remain open. No RC is cleared.

## Legacy item route pinning — October 25, 2026

The [item-route checkpoint](docs/acceptance/rc-dev-legacy-item-route-pinning-2026-10-25.json) prevents owner face edits from moving the target-based fence while an item node retains a fault, opaque saved escrow or pending held items. Both legacy native paths retain healthy face selection and validate face bounds. 1.12.2 passed 253/253 and 1.6.4 passed 230/230 native cases, including six new cases each, production-only startup and generated preview.3 migration; main passed 192 and 21 other modern tracks retain recorded artifacts. The initial 1.6.4 disconnected fake-player chat failure and repaired stale-alias fixture remain recorded. The subsequent [exact-JAR stock-client regression](docs/acceptance/rc-dev-legacy-item-route-stock-clients-2026-10-25.json) passed both unchanged route-edit production hashes: ordinary transfers, grouping/variants/conservation, search/sort/replay, codecs and display probes. Eight current captures were reviewed and all 32 hashed. These generated integrated worlds do not add cross-network/provider/dedicated multiplayer acceptance. This closes only owner face-edit movement while the endpoint still holds its state. Cross-network and removed/rebound durable uncertainty, opaque providers, broader parity and gameplay/performance acceptance remain open. No RC is cleared or published.

## 1.12.2 PL4-owned sided mapping — October 25, 2026

The [owned sided-adapter checkpoint](docs/acceptance/rc-dev-112-owned-sided-2026-10-25.json) gives PL4's native ISidedInventory fallback explicit bounded physical-slot identity. It captures valid unique native indices, retains raw positions for fixed-work per-slot validation, rechecks native extraction/insertion permissions and delegates physical transfers to Forge InvWrapper. Natural owned faces on one target retain their disjoint physical union; overlapping slots no longer inflate totals; changed or in-place remapping cannot redirect captured selections, and known backing quarantine crosses the owned adapters. Targets advertising item capabilities keep their original handlers. All 247 native cases passed, including fifteen new permission/mapping/bounds/alias cases, packaged-only startup and generated preview.3 migration. Main passed 192 pinned native cases; 22 other exact tracks retain recorded artifacts. The exact-qualified-JAR stock-client regression passed ordinary transfer/grouping/search/sort/display probes; four current captures were reviewed and sixteen hashed. Sided-face behavior remains native-fixture evidence. This covers owned fallback adapters, not opaque provider/composite mappings, cross-network uncertainty, installed-provider performance or the remaining legacy editor/multipart and broader acceptance. No RC is cleared.

## 1.12.2 known native wrapper identity — October 25, 2026

The [known-wrapper checkpoint](docs/acceptance/rc-dev-112-known-wrappers-2026-10-25.json) maps exact native double-chest and plain InvWrapper slots to backing object identity plus physical slot. Per-slot deduplication retains disjoint halves regardless of endpoint order; quarantine follows known backings, and identical contents on a replacement backing cannot authorize stale extraction. Transfers still use the original native handlers. Opaque recreated wrappers retain their previous logical routes, without a hidden-backing alias claim. All 232 native cases passed, including eleven new actual double-chest/overlap/quarantine/replacement/lifecycle cases, packaged-only startup and generated preview.3 migration. Main passed 192 pinned native cases and 22 other targets retain their recorded artifacts. Stock-client regression is recorded separately in the checkpoint. General combined/sided wrappers, cross-network/rebound uncertainty, installed providers and broader parity/gameplay/performance acceptance remain open; no RC is cleared.

## 1.6.4 same-target alias safety — October 25, 2026

The [1.6.4 alias checkpoint](docs/acceptance/rc-dev-164-target-aliases-2026-10-25.json) collects quarantined/pending target coordinates before choosing ready routes. Another face of the same vanilla inventory cannot bypass the escrow fence for withdrawal or deposit; independent targets remain available. All 224 native cases passed, including seven new target-alias cases, packaged-only startup and generated preview.3 migration. Main passed 192 pinned cases and 22 other exact targets retain qualified artifacts. The automatic branch validator also passed and its publication step was observed skipped. Stock-client regression is recorded separately in the checkpoint. This closes only loaded same-owner same-target alias coverage: hidden backing inventories behind distinct wrappers, cross-network and rebound aliases, installed providers, editor/multipart and broader multiplayer/migration/performance remain open. No RC is cleared.

## 1.12.2 handler-alias safety — October 25, 2026

The [handler identity checkpoint](docs/acceptance/rc-dev-112-handler-aliases-2026-10-25.json) prevents distinct blocks exposing the same stable native Forge item handler from inflating grouped totals or bypassing uncertain escrow. It collects quarantined targets/handler identities before selecting ready routes. Independent inventories with equal contents remain distinct. All 221 native cases passed, including six new shared-handler total/withdrawal/deposit/quarantine/independence cases; packaged-only startup and generated preview.3 migration passed. Main passed 192 scoped native cases and 22 other exact targets retain prior evidence. The initial test-fixture generic-cast compile failure is preserved. This applies to 1.12.2 only: hidden backing inventories behind different wrapper objects, 1.6.4 alias fixtures and installed-provider/multiplayer/performance acceptance remain open. Exact stock-client regression outcome is recorded in the linked checkpoint. No RC clearance is granted.

## Legacy grouped-quantity development — October 25, 2026

The [grouping checkpoint](docs/acceptance/rc-dev-legacy-grouping-2026-10-25.json) adds Slots/Groups, full native NBT identity, long aggregate totals, search/sort composition and conservative multi-source withdrawals on 1.12.2 and 1.6.4. Group membership/count/variant preflight rejects stale views; a shared finite transfer budget and persistent escrow retain uncertainty after foreign mutations. Final exact JARs passed 215/215 and 217/217 native cases (31 new grouping cases per target), packaged-only boot and generated preview.3 migration. Initial invalid-control and synthetic-stack fixture failures remain recorded with their repairs. Main passed 192 scoped cases in that initial matrix; 21 unchanged native tracks retain earlier evidence rather than rebuilding.

Both exact native-qualified stock clients passed actual mouse grouping, aggregate sorting, same-name NBT separation, a 64-item withdrawal spanning two source slots, untouched alternate variant, stale replay and prior search/sort/transfer checks. [1.12.2](docs/acceptance/rc-dev-1.12.2-grouping-stock-client.json) and [1.6.4](docs/acceptance/rc-dev-1.6.4-grouping-stock-client.json) preserve exact production/probe/archive hashes and 32 reviewed captures across physical/grouped GUI scales. This closes the scoped legacy search/sort/grouped-quantity implementation item; combined inventory/handler alias fixtures, installed providers, advanced legacy editor/multipart and broader multiplayer/migration/performance acceptance remain open. No RC clearance or new release is recorded.

## Legacy sorting development

The [sorting native checkpoint](docs/acceptance/rc-dev-legacy-sorting-native-2026-10-25.json) adds network, item-name and descending per-slot stack-count sorting to both legacy handheld screens. Sort changes reset paging and invalidate old actions while retaining endpoint/slot/NBT identity. Each exact legacy JAR passed 168 native scenarios, production startup and a generated preview.3 migration. The first 1.6.4 fixture compile failure is preserved; correcting its item-name API and rebuilding only that changed target restored 24-target native qualification. Both exact stock clients subsequently passed real mouse sorting/order/replay, transfers, paging, four GUI scales and network-display privacy; [1.12.2](docs/acceptance/rc-dev-1.12.2-sorting-stock-client.json) and [1.6.4](docs/acceptance/rc-dev-1.6.4-sorting-stock-client.json) record exact runtime/probe hashes and 16 reviewed captures. The 1.6.4 client exposed a native ItemStack-save side effect: serialization names its root NBT compound, invalidating strict binding equality. Its repaired JAR passed 170 native cases and an explicit stock-client save with the window open. The fix ignores only that transport name and preserves full binding-payload checks. Earlier compile and client failures remain recorded. This historical sorting checkpoint preceded the separately qualified search/grouping work above; other parity/acceptance gates remain open and no RC publication is cleared.

## Legacy handheld storage development

Both legacy ports now provide a craftable handheld, saved host binding, a native 45-slot paged storage view, withdrawals into the player inventory and deposits from inventory slots. Each server action rechecks the loaded owner-partitioned graph, tool binding, range/dimension, sided inventory, redstone state, configured limits and escrow availability. Inventory adapters are reused per endpoint instead of resolved for every displayed slot.

Transfers persist uncertainty before foreign mutation; post-mutation failures quarantine the endpoint without replay. Native window packets carry a server view revision. Refreshes and paging invalidate stale actions, signed byte values are decoded explicitly, and revisions never wrap within one open window. Unsupported vanilla cursor/drag/throw/swap modes are blocked.

The [exact native evidence](docs/acceptance/rc-dev-legacy-wireless-native.json) covers 156 passing scenarios per target, including 36 handheld/container cases, plus clean packaged-only server boots and generated published-preview.3 world upgrades. This closes the initial handheld browsing/transfer gap; later search/sort/grouping work is recorded above, while editor/multipart parity, installed providers and broader client/multiplayer acceptance remain open. [Legacy usage and limits](docs/LEGACY_WIRELESS_STORAGE.md) are documented separately.

Both legacy ports have separate stock-client probes using their exact native-qualified production JARs and isolated test mods: [1.12.2](docs/acceptance/rc-dev-1.12.2-stock-wireless-client.json) and [1.6.4](docs/acceptance/rc-dev-1.6.4-stock-wireless-client.json). They check actual mouse withdrawal/deposit/paging, stale view rejection, signed-byte packet codecs, four GUI scales at 1280×960, tooltips and private/published/hidden network-display projections. Physical-display captures exposed a world-lightmap defect; both renderers now isolate screen lighting and restore the previous OpenGL texture-unit state. Exact workflow results and inspected image names are recorded per target. This remains scoped offline integrated-world acceptance; dedicated multiplayer, installed providers, other display/editor modes and wider resolutions remain separate.

## Native resource and Wireless Storage hardening; first real-client evidence

All **24 maintained development targets have passing native gates**. The 22 modern candidates include the resource/Wireless Storage fences below. Both legacy handheld-storage candidates passed [156 scenarios, production-only startup and generated-world migration](docs/acceptance/rc-dev-legacy-wireless-native.json). Fourteen modern targets are covered by [the common gate](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39884343348), three earlier Forge targets by [native-logger validation](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39885198395), and four Forge 1.16 profiles plus main by [the targeted repair](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39885592948). The common workflow's overall failure is retained: its logger/persistence failures were repaired separately rather than relabeled as a green run. Per-target committed source and fixture counts are in the ledger. This is native development qualification, not feature parity or RC publication clearance.

The modern development update now fences fluid, FE and native/conversion energy mutations as well as item transfers. A provider that mutates and then throws, or returns an invalid amount/variant, leaves an uncertainty marker on the driving node. Saved parts preserve that marker even when escrow is empty; unknown fluid records are retained opaquely. Healthy refusals remain retryable. Quarantined endpoints are excluded once per cycle, rather than checked for every route pair, and exact-native fixtures cover both a faulted driver and a faulted passive endpoint.

Wireless Storage had an additional offhand-duplication risk: a provider could accept a deposit and throw before the player stack was reduced. Deposits now stage items in persistent node escrow and remove the spendable hand quantity before calling the provider. Unexpected player-delivery failures also retain quarantine. A busy/quarantined node cannot start another handoff, valid refusals return their staged quantity, and the node exposes a paused diagnostic. Existing native scenario counts are unchanged; assertions now cover post-mutation failures, malformed results, full-hand refusals and one-time settlement. Earlier Forge ports retain their native logging API.

For **Minecraft 1.21.1 only**, the candidate has additional acceptance evidence:

- [Actual client UI captures](docs/acceptance/rc-dev-1.21.1-client-visual.json): guide, populated Wireless Storage and read-only reader views at four GUI scales in a 1280×960 software-OpenGL development client. The inspected views fit; world displays, editor gestures, other resolutions, installed profiles and other ports remain separate.
- [Two actual client processes](docs/acceptance/rc-dev-1.21.1-real-client-storage.json): distinct offline development UUIDs, a private loopback server, a foreign non-operator rejection and explicitly granted operator access. Each client sends the same withdrawal packet twice. They receive 17 items each, leaving 94 from the initial 128. This is real client/server packet traffic, not a fake-player fixture; online authentication, broader gameplay and other ports are not certified.
- [Generated beta-world upgrade](docs/acceptance/rc-dev-1.21.1-generated-beta-world-upgrade.json): a world created with immutable `v0.2b.0` source is saved, copied, reopened by candidate source, and both real clients reconnect. Node identity/owner, the 94 stored items and both 17-item player inventories survive. The isolated development harness is injected into each checkout; this is not a published-JAR hash comparison or an arbitrary user/modpack world.
- [Native operation measurements](docs/acceptance/rc-dev-1.21.1-native-performance.json): three interleaved process trials per revision on loaded wired and wireless networks, each with 4096 physical PL4 hosts and 128 storage endpoints. Initial and later warm-up stages are retained. Latest median idle-transfer cycles were 0.1655→0.1739 ms wired and 0.1665→0.1667 ms wireless; topology rebuilds were 11.26→12.23 ms wired and 10.88→9.62 ms wireless. Results are mixed and CI hardware is noisy. These are operation timings, not whole-server MSPT/TPS, client FPS, active-provider throughput or all-port performance acceptance.

The exact-target native rollout and remaining acceptance are recorded in [the RC development ledger](docs/RC_DEVELOPMENT_STATUS.json). No stable RC publication is cleared. Advanced legacy editor/multipart and combined-inventory/handler alias acceptance, installed optional-provider combinations, full client/world-display/editor coverage, wider multiplayer/migration/load acceptance and 1.16.1–1.16.4 launcher profiles remain open. The new narrow 1.21.1 results do not certify another target.

## October 22 RC hardening and legacy monitoring

The 24 currently published Minecraft target tracks now have `0.2.0-rc.1-dev` source: 22 modern ports and two legacy subsets. The legacy runtimes now include item, fluid, energy and network readers, plus an owner-controlled four-line physical display. Their readers use loaded targets and configured bounds, perform no extraction/insertion, and retain native units (FE on 1.12.2; PL4 internal units on 1.6.4). Displays default to private; publishing the four text lines requires an owner/operator command. This closes the initial monitoring gap but does **not** provide modern graphical editing, multipart or wireless-storage parity.

Both legacy candidates passed **120/120 native scenarios**, separate production-only boots and full process upgrades of generated worlds saved using the checksum-verified immutable `preview.3` JARs. The [legacy gate](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39778867001) checks saved ownership/facing, vanilla inventory, item/fluid/energy escrows, stored quantities and new monitoring on the existing inventory. These results cover the generated baseline worlds; copied user worlds and installed optional mods remain pending.

The transport review found an item duplication risk: a provider could insert items, then throw before PL4 updated escrow, allowing a later automatic retry. Legacy item transport now persists uncertainty before foreign mutations, blocks replay, preserves missing/malformed item records and full uncertain counts, and drops one sealed diagnostic receipt when an affected host is broken. The modern implementation retains quarantine and unresolved data in the saved part payload, fences route edits and validates actual variants/remainders. Healthy refusals remain retryable. The existing destination-failure fixture now mutates first, throws, and requires **no duplicate replay**; fixture counts are unchanged while its assertions are stronger.

All **22 modern exact targets passed** the updated native gate: 20 in [the common run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39781194528), plus 26.1.2/26.3 in [the targeted repair](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39783257087). The repair preserves 26.x transaction rollback semantics; 26.3 passed 192 fixtures on each validated NeoForge profile. The [24-target metadata audit](docs/RC_DEVELOPMENT_STATUS.json) records each target’s required fixture count and actual passing run. The [1.21.1 source/native gate](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39781194462) independently passed all 192 fixtures and production/test isolation. Per-port `BUILD_STATUS.json` and retained logs/hashes identify the checked source; one target's result never certifies another port.

An obsolete legacy workflow briefly published the 1.12.2 development snapshot as a GitHub prerelease. It was moved to a **private draft**, with its tag/history retained. Both legacy publishers now reject development versions/channels, and CurseForge selection/staging/upload paths reject development, snapshot and nightly maturity. Eight publication-policy tests and four upload tests passed. The [subsequent CurseForge reconciliation](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39776495422) recorded the existing 24 receipts with zero failures; the development snapshot was not submitted. **No stable RC has been published.**

Remaining work before an all-version stable RC: advanced legacy feature parity; actual client/UI-scale and installed-provider acceptance; real multiplayer; copied user-world migration; repeatable large-network performance measurements; and the documented 1.16.1–1.16.4 launcher-profile acceptance. Unpublished/version-gap targets retain their separate blockers. Build success alone does not close these requirements.

## October 21 RC development preparation

The shared update is applied to **22 modern exact-Minecraft ports** as `0.2.0-rc.1-dev`. It combines topology membership/storage indexes, per-cycle endpoint identity reuse, transfer status that uses the same channel/direction predicate as routing, compact Wireless Storage layout and correctly versioned guide resources. Each port retains its native loader/API adapters and its own fixture count. [The common native gate](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39713811633) and [the installed Forge repair gate](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39716131269) provide exact-source/artifact evidence. This is development source and build alignment; **zero stable RC releases have been published**.

The divergent `mc/1.21.1` work was reconciled into `main` with both histories preserved. Its 192 native fixtures are compiled in a separate development source set and excluded from the production JAR. The added two-session conservation fixture remains present. The 1.21.1 source mirror is maintained from the validated main history. Generated Python bytecode is removed from source control and generated validation/build outputs are ignored.

The installed Forge 1.20.1/1.20.2/1.20.4 checks exposed a test-bootstrap defect: memory-backed config isolation suppressed fixture registration or ticker dispatch on an installed server. Those controls now use Forge's production-environment flag independently of config isolation. The gate requires **191 executed tests**, compares production entries against the combined scenario artifact, and boots the standalone production JAR separately. No fixture assertions were relaxed to accept an empty registry.

### Remaining gates before an all-version stable RC

- **Legacy feature alignment:** 1.12.2 and 1.6.4 retain their separately validated preview.3 transport subsets (89 scenarios each). They do not yet provide the modern display/editor, multipart and wireless-storage baseline. Their reference `src/main` trees are excluded from the actual legacy runtime and cannot be treated as completed ports.
- **Installed-client and gameplay acceptance:** client startup/UI scaling, real multiplayer with distinct players, save/world migration and installed optional-provider combinations still need exact-candidate evidence. Fake-player/server fixtures do not close these gates.
- **Performance acceptance:** repeatable baseline/updated large-network profiles are still required. No measured TPS/MSPT, allocation or memory improvement is claimed from the source changes alone.
- **Version-specific requirements:** 1.16.1–1.16.4 retain their documented pinned ModLauncher profiles; ordinary launcher/client acceptance remains open. The unpublished 1.13.2 port and audit-only/future targets retain their separate blockers.
- **Publication:** after acceptance, publish a new immutable RC tag, runtime/source/checksums and matching CurseForge submission for each qualifying exact target. Current development builds remain unpublished. Existing 24 latest release receipts remain valid historical publication evidence; they do not represent 24 RCs or moderation approval.

## October 18 code and publication reconciliation

The [scheduled CurseForge synchronization](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39134007983) verified **24 current target receipts and zero failures**, including all six performance betas. `main` build metadata now identifies `0.2b.0` beta and its 192 native fixtures, and the build gate verifies metadata, release notes and native test count. The 1.6.4 branch metadata now reflects its already published preview.3 item/fluid/internal-energy subset, 89 native scenarios and schema 3. These metadata fixes do not turn the remaining 18 published targets into performance betas or close their installed-client and gameplay acceptance. Exact-version source cleanup and native validation remain necessary before further port releases.

## Forge 1.20.x performance beta follow-up

The identical transfer endpoint-key caching change was independently compiled and tested on Minecraft **1.20.1, 1.20.2 and 1.20.4** with their Java 17 Forge loaders. Each passed **191 native GameTests** and produced an exact-target GitHub prerelease: [1.20.1](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.20.1-v0.2b-port.1), [1.20.2](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.20.2-v0.2b-port.1), [1.20.4](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.20.4-v0.2b-port.1). The subsequent checksum-verified upload accepted CurseForge files **9234054**, **9234049** and **9234048** as beta, respectively. The other 18 latest tagged targets in the earlier 24-target inventory remain on their prior revisions; they have not inherited this optimization or its test result. Approval/public listing, installed clients, optional providers, live multiplayer and measured performance are still open.

## October 17 performance-only beta publication

`0.2b.0` passed exact-target native gates and was published as a GitHub prerelease for [1.21.1](https://github.com/Shnewbs/Foundations-PL4/releases/tag/v0.2b.0), [26.1.2](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc26.1.2-v0.2b.0) and [26.3](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc26.3-v0.2b.0). The CurseForge upload API accepted files **9233710**, **9233734** and **9233735**, respectively, with beta maturity. The 1.21.1 release passed 192 native GameTests; 26.1.2 ran 192 fixtures and 26.3 ran 192 fixtures on each of its two validated NeoForge beta profiles. Upload receipts do not prove moderation or public listing. Real-client, installed optional-provider, live multiplayer and measured large-network performance acceptance remain open. The previous `0.2a.1` alpha tags remain immutable. Independent Forge/legacy versions have not been promoted by this beta.

## Performance beta scope and remaining acceptance

The `0.2b.0` revision is a **beta focused entirely on performance**. Preserve gameplay mechanics, transfer rates, routing order, filtering, ownership, save format and packet behavior. The beta change reuses endpoint identity keys during each transfer cycle to avoid repeated construction in route and status checks. This is an implementation change, **not a measured TPS/MSPT claim**.

For subsequent performance revisions and broader acceptance: pass its native build and conservation tests; capture repeatable baseline/updated dedicated-server profiles for large cable and wireless networks (tick time, allocations and network traffic); check installed-client scaling, live two-player conservation, save/reload and optional-provider combinations; port and independently validate the performance changes on each maintained target. Publish exact-target GitHub/CurseForge beta artifacts only for targets meeting their gates, with limitations stated in each release note. Existing `0.2a.1` tags remain immutable.

## October 2026 — current release, parity and port reconciliation

### 1. Release evidence is not functional completion

The [cross-version CurseForge synchronization report](docs/CURSEFORGE_MULTIVERSION_STATUS.md) records **17 latest tagged Minecraft targets** on October 9 (US Pacific; October 10 UTC): 14 new upload-API acceptances and three earlier NeoForge upload receipts without duplicates. The [October 17 reconciliation run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/39005816914) recorded **24 latest target receipts with zero failures**, including three newly accepted performance revisions; this newer inventory supersedes the 17-target count. This confirms submission/receipt, **not CurseForge moderator approval, public listing, installed client acceptance, end-user Java/launcher compatibility, or feature parity**. A GitHub tag, branch or successful compilation cannot substitute for exact-target gameplay evidence. Do not mutate existing release tags.

A previous [version-coverage audit](docs/VERSION_COVERAGE_CHECKPOINT.md) counted **85** eligible final versions, **nine** tagged PL4 targets and **69** unbuilt gaps as of its earlier October 9 snapshot. The subsequent 24-target reconciliation supersedes its *publication count*; **do not reuse the 69-gap count as a live figure** until the version/release inventory is rerun. It also does not prove that all newly tagged binaries passed all user-facing acceptance gates.

The standalone `0.0.1a.R16/R17` source packages and R17 freeze description are historical compared with the current `0.2a` line. Develop from the appropriate maintained exact-Minecraft branch/tag, never replace the newer source with an older local ZIP or port by changing a filename.

### 2. Published/tagged target ledger — latest known receipts

| Minecraft | Branch | Tagged revision | Release/publication status | Outstanding acceptance |
|---|---|---|---|---|
| 1.21.1 | `main` / `mc/1.21.1` | `v0.2b.0` | Published performance beta; 192 native GameTests; CurseForge API file 9233710 | Installed APIs, actual client UI, live multiplayer, measured performance, PL2 parity |
| 26.1.2 | `mc/26.1.2` | `mc26.1.2-v0.2b.0` | Published performance beta; 192 native fixtures; CurseForge API file 9233734 | Installed APIs, actual client UI, live multiplayer, measured performance and PL2 parity |
| 26.3 | `mc/26.3` | `mc26.3-v0.2b.0` | Published performance beta; 192 native fixtures on both validated NeoForge betas; CurseForge API file 9233735 | Installed APIs, actual client UI, live multiplayer, measured performance and PL2 parity; runtime uses pinned 26.3.0.23-beta |
| 1.20.1 | `mc/1.20.1` | `mc1.20.1-v0.2b-port.1` | Published Forge performance beta; 191 native GameTests; CurseForge API file 9234054 | Client/installed API/fallback/performance |
| 1.20.2 | `mc/1.20.2` | `mc1.20.2-v0.2b-port.1` | Published Forge performance beta; 191 native GameTests; CurseForge API file 9234049 | Client/installed API/fallback/performance |
| 1.20.4 | `mc/1.20.4` | `mc1.20.4-v0.2b-port.1` | Published Forge performance beta; 191 native GameTests; indexed lookup; CurseForge API file 9234048 | Client/installed API/fallback/large-network benchmarks |
| 1.20 | `mc/1.20` | `mc1.20-v0.2a-port.1` | Tagged Forge; CurseForge receipt recorded in October 17 sync | Independent installed/client/API and performance acceptance |
| 1.19 / 1.19.1 / 1.19.3 / 1.19.4 | exact-version branches | `0.2a-port.1` per target | Tagged Forge; four distinct CurseForge receipts recorded in October 17 sync | Reconcile native fixture and installed acceptance for each exact patch |
| 1.19.2 | `mc/1.19.2` | `mc1.19.2-v0.2a-port.1` | Published Forge; 191 native GameTests recorded | Client/installed API/fallback/performance |
| 1.18.2 | `mc/1.18.2` | `mc1.18.2-v0.2a-port.2` | Published Forge; 193 native GameTests recorded | Client/installed API/fallback/performance |
| 1.18.1 | `mc/1.18.1` | `mc1.18.1-v0.2a-port.1` | Tagged; CurseForge upload API accepted | Preserve exact-target native evidence; installed/client QA pending |
| 1.17.1 | `mc/1.17.1` | `mc1.17.1-v0.2a-port.1` | Tagged; CurseForge upload API accepted | Preserve exact-target native evidence; installed/client QA pending |
| 1.16.5 | `mc/1.16.5` | `mc1.16.5-v0.2a-port.2` | Tagged; Java 8 compatibility output and CurseForge receipt | Verify installed client, provider combinations; do not rely on old Java-17-only status |
| 1.16.4 | `mc/1.16.4` | `mc1.16.4-v0.2a-port.1` | Tagged; Java 8 compatibility output and CurseForge receipt | **Special Forge 35 ModLauncher 8.1.3 precedence needed**; stock launcher/client parity is not accepted |
| 1.16.3 | `mc/1.16.3` | `mc1.16.3-v0.2a-port.1` | Tagged; CurseForge upload API accepted | Per-target runtime/client/API/fallback acceptance |
| 1.16.2 | `mc/1.16.2` | `mc1.16.2-v0.2a-port.1` | Tagged; CurseForge upload API accepted | Per-target runtime/client/API/fallback acceptance |
| 1.16.1 | `mc/1.16.1` | `mc1.16.1-v0.2a-port.1` | Tagged; CurseForge upload API accepted | Per-target runtime/client/API/fallback acceptance |
| 1.15.2 | `mc/1.15.2` | `mc1.15.2-v0.2a-port.1` | Tagged; CurseForge upload API accepted | Per-target runtime/client/API/fallback acceptance |
| 1.14.4 | `mc/1.14.4` | `mc1.14.4-v0.2a-port.1` | Tagged; CurseForge upload API accepted | Per-target runtime/client/API/fallback acceptance |
| 1.12.2 | `mc/1.12.2` | `mc1.12.2-v0.2a-legacy-preview.3` | Tagged legacy preview; item, fluid and **native FE** energy subset; CurseForge receipt | Missing modern PL2 displays/multipart/wireless, full fallback parity; 89-scenario release gate |
| 1.6.4 | `mc/1.6.4` | `mc1.6.4-v0.2a-legacy-preview.3` | Tagged **experimental** legacy preview; item/fluid and **PL4-internal** energy subset; CurseForge receipt | No claim of external RF/EU/FE; missing modern parity; Java 7 runtime and legacy security restrictions |

Read [current-track rollout evidence](docs/releases/0.2a-port-rollout.md), [newer Forge/legacy implementation evidence](docs/PORT_IMPLEMENTATION_STATUS.md) **with its historical preview-2 date in mind**, [legacy preview-3 release notes on the legacy tags](https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.12.2-v0.2a-legacy-preview.3), and the [latest publisher receipts](docs/CURSEFORGE_MULTIVERSION_STATUS.md). A reported test count is a *native/isolated automated test*, not graphical or installed-provider acceptance. Installed acceptance is still open even for versions with a published binary.

**Important reconciliations:** Earlier `docs/VERSION_COVERAGE_CHECKPOINT.md` and `mc/1.16.4/BUILD_STATUS.json` recorded a prior **0/193 native-scenario scanner failure** and no published 1.16.4 release. The later tagged `0.2a-port.1` source documents a Java-8 conversion and a special launcher profile, and the newer submission ledger records its published tag. **Treat the prior failure as historical, but do not infer stock-client compatibility from the later release.** Likewise `docs/PORT_IMPLEMENTATION_STATUS.md` still refers to legacy `preview.2` and Java-17-only 1.16.5; the later version-specific tags and `preview.3` notes supersede those *release numbers*, not missing user QA.

### 3. Unfinished/version-gap implementation queue

| Target(s) | Status as of this reconciliation | Next release gate |
|---|---|---|
| **1.13.2** / `mc/1.13.2` | **BLOCKED / SOURCE PORT**; Forge 25 native MCP/API backport and 194-scenario gate incomplete; no approved release | Restore exact API compilation, native Forge scenarios, packaged startup, then client verification |
| **1.8.9, 1.11.2** | **NOT IMPLEMENTED** in published target set | Toolchain/license feasibility, version-exact loader, own source branch and functional scenarios |
| **1.19 patch acceptance** | 1.19, 1.19.1, 1.19.3 and 1.19.4 have tagged CurseForge receipts, but user-facing acceptance is not inherited | Preserve independent native and installed compatibility evidence per target |
| **1.20.3, remaining 1.20.x** | 1.20, 1.20.1, 1.20.2 and 1.20.4 have independent Forge releases; other patches remain unverified | Exact point-version loader/adapters, compile/runtime and independent acceptance |
| **1.21.x** (other than 1.21.1) | Coverage research pending | Validate real supported loader, transport/UI differences, separate release identity |
| **26.1 / 26.2** | Existing refs **audit-only**; no cross-version binary equivalence | Exact toolchain, native and client verification before tagging |
| **26.4** | **FUTURE**; usable exact-version toolchain must be verified | Create branch only once feasible; port and test independently |
| **All other original-PL/PL2 gaps** | **AUDIT / RESEARCH**; include final patches rather than quietly skipping | Rerun Mojang/loader manifest inventory and record BUILDABLE/BLOCKED/ALTERNATIVE_LOADER_RESEARCH |

**Scope decisions:** `1.16.4` and `1.16.5` are *distinct* targets. `1.18.2` is explicitly required. `1.6.4` remains optional/experimental but implemented to an honest legacy-preview subset. `1.7.10` is **excluded**; do not add a port or bundle GTNH/1.7.10-only binaries. Retain the user-approved `1.12.2` track despite original PL2 coverage. When a loader does not exist for an exact patch, record the blocker or investigate an appropriate alternative: **never rename a neighboring version's JAR as a port**. See [exact-version coverage policy](docs/VERSION_COVERAGE.md).

### 4. Next engineering milestones (priority order, not claims of completion)

- [ ] **P0 — Evidence and status synchronization.** Regenerate the official-version/loader/GitHub-release inventory after the 17-target submission; update the dated version-count audit, per-target `BUILD_STATUS.json` and central port/readme notes without rewriting immutable tags. For every target expose SOURCE_PORT, NATIVE_BUILD, SCENARIOS, INSTALLED_RUNTIME, INSTALLED_APIS, CLIENT_VISUALS, MULTIPLAYER, PERFORMANCE, RELEASE and CURSEFORGE_ACCEPTED vs MODERATION_APPROVED separately.
- [ ] **P0 — Finish 1.16.4 user-installable compatibility.** Maintain the exact Java-8-transformed binary and Forge 35/ModLauncher requirements; run the 193 server scenarios with published bytes, create/test an actual client launcher profile, test stock-vs-profile behavior, and disclose limitations. No false stock-Forge compatibility claim.
- [ ] **P0 — Complete 1.13.2.** Backport Forge 25/MCP registration, data, renderer, capability and launch APIs; compile and reobfuscate exact-target code; pass the independent 194-scenario suite and installed runtime/client checks before a GitHub/CurseForge release.
- [ ] **P1 — Close requested version gaps.** Investigate/build `1.8.9` and `1.11.2` first, then remaining missing final point releases using the coverage audit rather than a shortlist. Ports must be independently runnable on their named Minecraft patch, not just source branches.
- [ ] **P1 — Bring legacy 1.12.2 and 1.6.4 toward accepted feature parity.** Build beyond item/fluid/energy transport to readers, configurable displays, PL2-style graphical editing, multipart/cable rendering, wireless storage, ownership and restart-safe migrations as feasible. Record target limitations rather than shipping stubs as complete features. Preserve 1.6.4's experimental/security designation.
- [ ] **P1 — Finish PL2 gameplay/GUI parity on current and ported branches.** Finish remaining Wireless Storage/Array/Entity Node behaviors, aggregated storage edge cases, transfer directional filters/rules, Signaller/Clock, display/GSI editing and layouts, machine data, models and missing interaction affordances. Audit against released PL2 behavior; do not treat registrations/textures alone as implemented gameplay.
- [ ] **P1 — Supply verified optional-API alternatives.** JEI/EMI/era-appropriate NEI recipe viewing, Jade/HWYLA/Waila inspection, KubeJS/CraftTweaker or documented native declarative-rule equivalents, target-native item/fluid/energy, and optional Create/AE2/Mekanism/GTCEu/Electrodynamics telemetry. Provide PL4-owned recipe lookup, inspection, storage and configuration paths when optional mods are absent; tests must show the *actual task* works. No fabricated power units or universal RF/EU/J/kinetic conversions. Stability/performance mods are optional test profiles, not mandatory bundled dependencies.
- [ ] **P2 — Native/API/multiplayer/performance release hardening.** Test absent/present/conflicting providers, all faces and voltage policies, no lost/duplicated resources, uncertain escrow/recovery, chunk unload, dimension/ownership changes, network restarts, simultaneous users, client GUI scale, animations and cable placement. Profile large networks, packet payloads, tick/capability budgets and allocations on every supported class of loader. Backups and migration tests must precede broader beta claims.
- [ ] **P2 — Distribution/acceptance closeout.** For every revision publish exact-target runtime, sources, complete source ZIP, checksums and honest changelog to GitHub; then upload with matching CurseForge Minecraft/loader metadata, separately checking API acceptance, moderation and public availability. Maintain strict release-tag immutability.
- [ ] **Future — 26.4 and beyond.** Promote only after official release/toolchain and exact-version native/client evidence; use the same matrix, fallbacks and distribution policy as all earlier ports.

These tasks may run in parallel across independently maintained version branches, but **no target inherits PASS from another target**. A tagged alpha remains alpha until explicit gameplay, installed dependencies, real client, multiplayer and performance gates close. The legacy `0.0.2a.R1`–`R7` phase plan below remains a feature backlog, not a declaration that those tags are the next release identifiers.

### 5. Definition of done and durable status workflow

1. **Implementation:** code/assets/config exist for the exact Minecraft patch; core tasks are runnable without optional mods; missing features are explicitly marked PARTIAL or BLOCKED.
2. **Automated:** production-source checks, real loader compilation, correct runtime remap/reobfuscation, automated native/server scenarios, resource conservation and save/restart safety pass **for that exact artifact**.
3. **User-facing:** actual installed client, dedicated server, GUI geometry and scaling, two-player multiplayer, wireless/network transfers, performance and world migration are recorded. Standalone/native test passes do not satisfy this gate.
4. **Optional APIs:** each compatible candidate is tested *installed* at an exact dependency revision, individually and in a compatible mixed pack, with fallback absent/present paths and units/sidedness verified.
5. **Publishing:** immutable GitHub release contains matching runtime/source/checksums; CurseForge API receipt, moderator approval and publicly downloadable page are **three separate statuses**.

On completion of any milestone, update **this section**, the exact-target status/test evidence and release notes together. Maintain [API_TESTING.md](docs/API_TESTING.md), [VERSION_COVERAGE.md](docs/VERSION_COVERAGE.md), [MULTIVERSION_PORT_PLAN.md](docs/MULTIVERSION_PORT_PLAN.md) and [CURSEFORGE_MULTIVERSION_STATUS.md](docs/CURSEFORGE_MULTIVERSION_STATUS.md) as supporting details. Historical counts are snapshots, not persistent claims of today's completeness. **Further API testing is still required.**

---

## 0.1b reconciliation

New source implements a paged Wireless Storage inventory screen with guarded server actions, independent receive/send item/fluid filters, bounded ALL/ANY Signaller statements, adjustable signal strength, escrow-aware edits and provider API guards. Existing R3 display and routing features are included. Publication is gated by native tests per track; installed-mod, graphical, live multiplayer and large-world acceptance remain separate and pending.

The version advances to 0.1b as a beta milestone. It does not close the entire proposed feature-complete 0.1 roadmap. Aggregated Wireless Storage views/search, remaining component parity, richer integrations and gameplay/performance acceptance remain open. Historical checkboxes and counts below are retained for traceability, not presented as current release evidence.

This beta promotes the existing 0.1a implementation without gameplay or API changes. Further API testing is still required; the beta designation does not change pending acceptance results.

---

## R3 reconciliation

R2 delivered named/searchable/pinned reader targets, provider registration and network diagnostics. R3 source adds editor organization/styles/page links, page names, transfer channels and cycle-based fairness. Historical checkboxes below are not release evidence; native and client acceptance remain distinct. The component, directional-filter and installed-mod parity backlog remains open.

## 1. Purpose of this document

This is the working master roadmap for Foundations PL4. It is intended to prevent the project from drifting between “ported,” “registered,” “looks similar,” and “actually behaves like Practical Logistics 2.”

It records:

- what has already been implemented;
- what has been verified in-game by the current tester;
- what is implemented in source but still needs native acceptance;
- what remains missing compared with released Practical Logistics 2;
- which source files should be revisited when parity work resumes;
- how the remaining work should be divided into versions/phases;
- what should deliberately wait until after the first stable PL2-parity line;
- what Foundations PL4 can eventually become beyond PL2.

This file should be updated whenever a revision closes a roadmap item. **A feature is not considered complete merely because an item/block is registered, a texture exists, or an offline rule test passes.**

---

# 2. Source-of-truth references

## 2.1 Current Foundations PL4 source

Historical audited R16 package:

```text
FoundationsPL4-0.0.1a.R16-TRANSFER-HARDENING-SOURCE.zip
```

Current R17 source retains these paths:

```text
src/main/java/net/foundations/pl4/
src/main/java/net/foundations/pl4/client/
src/main/java/net/foundations/pl4/core/
src/main/resources/assets/foundations_pl4/
src/main/resources/data/foundations_pl4/
docs/
```

Historical R16 source size:

| Item | Count |
|---|---:|
| Production Java files | 78 |
| Resource files | 605 |
| PNG textures | 117 |
| Model JSON files | 370 |
| Blockstate JSON files | 58 |
| Recipes | 35 |
| Field Guide chapters | 28 |
| Registered native GameTests | 114 |
| Multipart/content `Kind` IDs | 23 |

## 2.2 Pinned upstream references

These commits are already recorded in the project’s `docs/UPSTREAM.md` and `UPSTREAM_MANIFEST.json` and should remain the reproducible reference points unless deliberately superseded.

### Practical Logistics 2 — primary reference

```text
Repository: https://github.com/SonarSonic/Practical-Logistics-2
Commit:     4772196103d35c78c33f03c288a7b47aac267197
Version:    3.0.8 / Minecraft 1.12.2
```

This is the **main visual and gameplay reference** for PL4.

The pinned manifest contains 562 PL2 Java files. Important areas include:

```text
core/tiles/connections/
core/tiles/readers/
core/tiles/nodes/
core/tiles/displays/
core/tiles/displays/gsi/
core/tiles/wireless/
core/tiles/misc/
base/channels/
base/filters/
integration/
core/items/guide/
```

### Practical Logistics 1 — historical behavior reference only

```text
Repository: https://github.com/SonarSonic/Practical-Logistics
Commit:     4b82002021cba1788b59b3279a7ade97ebca2357
```

PL1 should only be used when confirming a feature that was genuinely released and later disappeared or changed. Commented-out registrations are not proof of released gameplay.

### Practical Logistics 3 — future design research, not the parity target

```text
Repository: https://github.com/SonarSonic/Practical-Logistics-3
Commit:     bfd78dee190fef0964470549c3e023ddff3a2a61
Status:     unfinished / in development
```

PL3 is useful for ideas such as data addresses, method graphs, advanced GSI components, styled text, layouts, interactions, and node graphs. It must **not** replace PL2 as the visual or feature-completion target.

### Sonar Core — audited and internalized

```text
Repository: https://github.com/SonarSonic/Sonar-Core
Commit:     f5b64e033e2c07af55c2d5f474052fb83fba5ca1
```

PL4 is intentionally standalone. Old Sonar Core and MCMultiPart are not runtime dependencies. Their responsibilities are being reimplemented internally using Minecraft 1.21.1 / NeoForge APIs.

---

# 3. Evidence levels used in this roadmap

Every status should be interpreted using these evidence levels.

| Level | Meaning |
|---|---|
| **USER VERIFIED** | The current tester reported the feature working in an actual client/world. |
| **NATIVE TESTED** | Native Java/NeoForge build and automated GameTests passed for that exact revision. |
| **SOURCE IMPLEMENTED** | Code exists and offline/source tests pass, but native acceptance is incomplete. |
| **PARTIAL** | Some of the original behavior exists, but important parity is missing. |
| **MISSING** | The feature is not meaningfully implemented. |
| **EXPANSION** | New PL4 functionality beyond the released PL2 target. |

Do not silently promote **SOURCE IMPLEMENTED** to **complete**.

---

# 4. Current R16 architecture

## 4.1 Standalone runtime

Foundations PL4 is a single mod:

```text
mod id: foundations_pl4
package: net.foundations.pl4
```

No separate Sonar Core, MCMultiPart, Calculator, or Framework JAR is required for the current runtime design.

## 4.2 Multipart host model

The current multipart system is owned by:

```text
HostBlock.java
HostEntity.java
Part.java
PartItem.java
MultipartShapes.java
PartShapes.java
MultipartTopology.java
ConnectionRules.java
```

Current save contract:

```text
Host schema: 2
Multipart slots: 13
ordinary faces: 0..5
cable center: 6
display faces: 7..12
```

This is an internal replacement for old MCMultiPart behavior, not binary/API compatibility with MCMultiPart.

## 4.3 Network model

Main source:

```text
NetworkEngine.java
MultipartTopology.java
DisjointSets.java
```

Important current behavior:

- network traversal is server-authoritative;
- loaded host parts are cached into groups;
- topology rebuilds only when marked dirty or configuration changes;
- chunks are not force-loaded;
- data and redstone networks remain separate;
- reader `NETWORK` input and `VISUAL` output are distinct;
- a visual export exposes telemetry without electrically merging the two machine networks;
- same-owner wireless emitter/receiver pairs can union network groups;
- cross-dimension wireless can be allowed by config;
- oversized loaded networks fail safely at the configured host limit.

## 4.4 Display model

Main source:

```text
DisplayNetworks.java
DisplayCanvas.java
DisplayPainter.java
DisplayEditorScreen.java
DisplayPropertiesScreen.java
DisplayPickerScreen.java
DisplayColorPickerScreen.java
DisplayElements.java
DisplayLayout.java
DynamicCanvasLayout.java
LayoutTransactions.java
EditorChrome.java
DisplayPicking.java
```

Current typed display elements:

```text
TEXT
ITEM
BLOCK
INVENTORY
FLUID
FLUID_GRID
BAR
```

Current limits:

```text
32 elements
8 pages
128 rendered icons per canvas
up to 8 explicitly bound visible readers
logical canvas max 4096 x 4096
large displays: rectangular joined canvases up to 16 x 16 physical panels
```

## 4.5 Transfer model

Main source:

```text
TransferEngine.java
TransferRules.java
R16GameTests.java
```

R16 defines normal Nodes as passive resource endpoints while Transfer Nodes are active movers.

Supported transported resources:

```text
Items
NeoForge fluids
FE
```

Read-only telemetry only:

```text
GregTech EU
Mekanism Joules
```

Default per-cycle budgets:

```text
Items:  64
Fluid:  1000 mB
Energy: 10000 FE
```

## 4.6 Reader/data model

Main source:

```text
DataSampler.java
EnergyReader.java
VisualSamples.java
ReflectiveEnergyAccess.java
EnergyValues.java
```

Current readers:

```text
Inventory Reader
Fluid Reader
Energy Reader
Info Reader
Network Reader
```

## 4.7 Guide

The current Field Guide has 28 chapters and is maintained as Foundations PL4 documentation for Minecraft 1.21.1 rather than exposing stale original PL2 text to the player.

Primary files:

```text
GuideScreen.java
GuideResources.java
GuideNavigation.java
GuideLayout.java
GuideBook.java
assets/foundations_pl4/guide/en_us.json
```

---

# 5. Current content inventory

`Kind.java` currently preserves these PL2 IDs:

```text
datacable
redstonecable
node
inforeader
inventoryreader
fluidreader
energyreader
networkreader
array
entitynode
transfernode
redstonenode
clock
redstonesignaller
displayscreen
minidisplay
largedisplayscreen
holographicdisplay
advancedholographicdisplay
dataemitter
datareceiver
redstoneemitter
redstonereceiver
```

Additional registered content includes:

```text
sapphireore
hammer
hammer_air (internal structure block)
sapphire
sapphiredust
stoneplate
etchedplate
signallingplate
wirelessplate
operator
transceiver
entitytransceiver
wirelessstorage
plguide
```

---

# 6. What has been done — revision history

## R1 — initial 1.21.1 reconstruction

Initial port baseline: content registration, basic host/network/data concepts, recipes/assets, and early gameplay scaffolding.

## R2 — Core consolidation

- pulled/audited Sonar Core source;
- confirmed 123 explicit PL2 Sonar Core imports plus one wildcard package;
- moved required responsibilities into the PL4 runtime;
- removed the need for a separate legacy Core binary;
- established `foundations_pl4` identity.

## R3 — texture/resource correction

- corrected 1.21.1 texture atlas locations;
- retained original texture pixels;
- added resource/model path validation.

## R4 — GUI render ordering

- fixed custom PL4 screens being blurred/dimmed by inherited background rendering;
- established background -> PL4 content -> widgets -> tooltip ordering.

## R5 — connections, hammer, and first connected displays

- original cable connector mesh family restored;
- stronger multipart connection rules;
- Forging Hammer restored as a three-block animated machine;
- original-style two-slot hammer GUI;
- automation support;
- first bounded connected Large Display implementation.

## R6 — display facing and native energy telemetry

- explicit display-front orientation;
- corrected flat-screen readable side;
- optional read-only Mekanism J telemetry;
- optional read-only GTCEu EU telemetry;
- FE/J/EU totals kept separate;
- KubeJS/data recipe example for the Forging Hammer.

## R7 — compact reader/display connectivity

- separate ordinary and display face slots;
- compact Reader + Display same-face mounting restored;
- visual output no longer requires a cable detour;
- reader `NETWORK` input vs `VISUAL` output separation;
- visual telemetry no longer merges machine networks.

## R8 — display continuity, hologram orientation, Field Guide

- smart Large Display side expansion;
- shared layout continuity across joined displays;
- initial hologram orientation work;
- Foundations-style PL4 Field Guide created and updated for the actual port.

## R9 — PL2-style typed display elements/editor

- real item icons;
- block previews;
- inventory grids;
- fluid graphics/grids;
- progress/energy bars;
- text/value elements;
- world-space editor controls;
- selection/move/resize/snap;
- duplicate/delete/order;
- data picker;
- revision-fenced server-side transactions.

## R10 — item transforms, display presentation, onboarding

- corrected oversized held/dropped multipart item presentations;
- improved automatic monitor table layout;
- added Welcome and guided tutorials;
- improved guide navigation.

## R11 — dynamic joined-display canvas

- Large Display logical workspace now grows with physical screen dimensions;
- old typed layouts migrate proportionally;
- toolbar/editor geometry moved onto the actual display workspace.

## R12 — display plane stabilization

- separated bar background/fill/frame/text/editor planes;
- eliminated major progress/energy-bar z-fighting.

## R13 — editor stabilization and stackable part drops

- button background/border/glyph/hover planes separated;
- four-corner resizing;
- larger cyan resize affordances;
- canonical part item serialization;
- ordinary identical drops stack again;
- configured Operator-removed parts keep meaningful settings/escrow;
- technical-binder Field Guide visual pass.

## R14 — editor UX and block-preview stabilization

- editing help moved to local player HUD;
- world toolbar remains visible to the editing player;
- right-click Back behavior through edit screens;
- 3-column default for grid elements;
- actual column preview;
- visual color palette plus hex support;
- block preview given real shallow 3D depth so its own faces do not z-fight.

## R15 — final-alpha polish attempt

- hologram projection moved farther from projector hardware;
- guide tutorial prose rewritten into a more natural teaching voice.

## R16 — Transfer Node endpoint hardening

- normal Nodes restored as passive transfer endpoints;
- ADD Transfer Nodes can import from passive normal Nodes;
- REMOVE Transfer Nodes can export to passive normal Nodes;
- explicit Transfer Node peers take priority;
- same-cycle receive/re-extract fence added;
- deterministic equal-priority rotation;
- persistent escrow retained;
- ADD/REMOVE is deliberately explicit-peer-only until fuller PL2 channel/filter semantics return.

## R17 — final-freeze emitter anchor

- normal hologram projection begins at the model's physical emitter bar;
- Advanced projection begins at the selected physical projection panel;
- projection clearance is measured along the configured view normal from that emitter anchor;
- R16 transfer behavior, save schema, multipart slots, protocol and recipes remain unchanged;
- native build, all 114 GameTests and real in-game transfer/visual acceptance remain required.

---

# 7. Current user-reported in-game acceptance evidence

The following has been reported working in actual gameplay during the current alpha line. This is useful evidence and should be preserved as regression targets.

## Verified by user

- Forging Hammer model/machine works.
- Forging Hammer GUI works.
- Forging Hammer recipes were tested with KubeJS.
- Hammer automation was tested with hoppers/piping.
- Multiple energy sources are readable, including FE and EU telemetry.
- Large Displays connect and form working canvases.
- Multipart hosts/cables on displays work.
- Display/editor z-fighting was reported fixed after the later render-depth hotfixes.
- Display element resizing became usable/easy after four-corner resize changes.
- Field Guide exists and is usable.
- Typed display content is visible in-game.

## Still not accepted / explicitly open

- R16 passive normal-Node Transfer endpoint behavior has not yet been reported accepted in-game.
- Fluid passive-source/passive-destination transfer still needs real handler acceptance.
- FE passive-source/passive-destination transfer still needs real machine acceptance after R16.
- R17 source now anchors projection geometry at the normal emitter bar / Advanced projection panel; in-game wall/floor/ceiling visual acceptance remains outstanding.
- Full 114 GameTest run for the R17 candidate remains required.

---

# 8. Immediate freeze blockers for `0.0.1a`

The first alpha should not be frozen until every item in this section is closed.

## P0-1 — Fix the hologram emitter anchor

### Current source

```text
core/HologramProjection.java
client/HostRenderer.java
```

R15/R16 projection center logic was based on:

```text
MOUNT_SURFACE_OFFSET
NORMAL_PROJECTION_CLEARANCE
ADVANCED_PROJECTION_CLEARANCE
```

The R17 `geometry()` contract now keeps the mount anchor, emitter anchor, projection normal and distance separate. Normal holograms use the center of the physical emitter bar; Advanced holograms select the physical projection panel facing opposite the configured view.

Offline geometry checks cover all six mount faces, four requested view directions, both projector variants and front/back camera sides. Native rendering still requires the acceptance matrix below.

### PL2 source to revisit

```text
core/tiles/displays/tiles/holographic/BlockHolographicDisplay.java
core/tiles/displays/tiles/holographic/BlockAdvancedHolographicDisplay.java
core/tiles/displays/tiles/holographic/EntityHolographicDisplay.java
core/tiles/displays/tiles/holographic/TileAbstractHolographicDisplay.java
core/tiles/displays/tiles/holographic/TileHolographicDisplay.java
core/tiles/displays/tiles/holographic/TileAdvancedHolographicDisplay.java
core/tiles/displays/tiles/render/RenderHolographicDisplay.java
core/tiles/displays/tiles/holographic/GuiHolographicRescaling.java
network/packets/PacketHolographicDisplayScaling.java
```

### Recommended correction

Introduce an explicit projector geometry contract, for example:

```text
mount anchor
emitter-bar anchor
projection normal
projection distance
projection size/scale
```

`HologramProjection.centre()` should start from the **emitter-bar anchor**, not a generic host-face surface.

Normal and Advanced projectors may use different emitter offsets.

Acceptance matrix:

- wall mount, all horizontal directions;
- floor mount, four View directions;
- ceiling mount, four View directions;
- normal and Advanced variants;
- front and back readable camera sides;
- no intersection with base, bar, cables, or neighboring multipart geometry.

## P0-2 — Native R16 build and all GameTests

Required:

```bat
gradlew.bat --no-daemon --console=plain clean build runGameTestServer
```

Target:

```text
114 / 114 GameTests PASS
```

Do not freeze based only on the offline rule suites.

## P0-3 — R16 real transfer acceptance

### Items

Verify all three useful routing forms:

```text
REMOVE Transfer Node -> ADD Transfer Node
normal Node -> ADD Transfer Node
REMOVE Transfer Node -> normal Node
```

Also verify:

- full destination does not extract;
- item/tag filters;
- priorities;
- equal-priority rotation;
- escrow save/reload;
- Operator removal with pending escrow.

### Fluids

Use at least one real NeoForge tank and test:

```text
normal Node source -> ADD
REMOVE -> normal Node destination
full tank
filter
save/reload escrow
```

### FE

Use at least one real FE battery/machine and test both passive directions and configured rate limits.

EU/J are **not** part of the 0.0.1a transport gate.

## P0-4 — final graphical regression sweep

At minimum:

- GUI scales: small/normal/large where practical;
- windowed and fullscreen;
- normal Display;
- Mini Display;
- joined Large Display;
- normal Hologram;
- Advanced Hologram;
- block icon;
- item icon;
- fluid display;
- grid;
- bar;
- quantities;
- editor toolbar;
- color palette;
- four-corner resize;
- front and shallow side camera views.

## P0-5 — archive the frozen baseline

When the above passes, archive:

```text
exact source ZIP
exact built JAR
SHA256SUMS
R16/R17 final validation log
114 GameTest output
final screenshots
final parity ledger
updater from previous release
```

R17 is the narrow final-freeze revision for the hologram anchor; do not add new feature work before the freeze gate passes.

---

# 9. Detailed subsystem parity audit

| Subsystem | Current R16 state | Evidence | PL2 source to revisit | Remaining work | Planned phase |
|---|---|---|---|---|---|
| Core/runtime | Standalone NeoForge mod; legacy Core responsibilities internalized | Source + earlier local builds | Sonar Core audit; PL2 entrypoints | Public extension API, final packaging acceptance | 0.0.2a+ |
| Multipart host | 13-slot host; cable center + ordinary/display faces | User partial + source | PL2 multipart slots / connection helpers | Remaining placement exceptions, covers, external multipart/API parity | 0.0.2a |
| Data cable | Original connector family, dynamic arms/ports; cached per-group sample inputs | User verified + R1 source optimization | `connections/data/*` | Large network perf/load-unload acceptance and native TPS/memory profile | 0.0.2a hardening |
| Redstone cable | Separate graph and signal propagation | Source | `connections/redstone/*` | Full per-face semantics, feedback loops, wireless parity | 0.0.2a |
| Node | Samples target; R16 passive transfer endpoint | R16 source | `nodes/node/*` | Native transfer acceptance, original GUI details | freeze/0.0.2a |
| Inventory Reader | Counts, filters, sorting, component-aware visuals | User display evidence + source | `readers/items/*` | Historical channel modes, slot/position details, deduplication | 0.0.2a |
| Fluid Reader | NeoForge tank sampling + graphical fluids | Source | `readers/fluids/*` | Per-tank/channel fidelity, modded handlers acceptance | 0.0.2a |
| Energy Reader | FE + optional J/EU read-only telemetry | User FE/EU verified | `readers/energy/*`, old integrations | rates, richer machine semantics, more providers | 0.0.2a/0.0.3a |
| Info Reader | Several vanilla/world/entity metrics | Source | `readers/info/*`, `MasterInfoRegistry` | Restore provider registry breadth and selectable bindings | 0.0.2a |
| Network Reader | Basic host/part counts | Source | `readers/network/*` | Channels, network diagnostics, route details | 0.0.2a |
| Transfer Node | Items/fluids/FE; passive normal endpoints in R16; escrow | Source; native acceptance pending | `nodes/transfer/*`, `base/channels/*`, `base/filters/*` | Directional channels, full filter GUI, deep fairness/recovery tests | 0.0.2a |
| Array | Eight saved links / remote observation | Source | `nodes/array/*` | Restore 8 transceiver inventory slots, GUI, card lifecycle | 0.0.2a |
| Entity Node | UUID binding, entity info/basic scan | Source | `nodes/entity/*` | selection UI, inventory interactions, reconnect semantics | 0.0.2a |
| Data emitter/receiver | Explicit same-owner links and network joining | Source | `wireless/emitters/*`, `wireless/receivers/*` | discovery GUI, channel selection, sharing permissions | 0.0.2a |
| Redstone emitter/receiver | Basic wireless redstone graph joining | Source | same + redstone integration | GUI, statement/channel semantics, tests | 0.0.2a |
| Wireless Storage | Bound target opens read-only-ish current target UI | Partial source | `items/wirelessstoragereader/*` | emitter list, remote inventory/fluid management, permissions | 0.0.2a |
| Transceiver | block/entity link item | Partial source | `items/transceiver/*`, API transceiver interfaces | original inventories/range/selection behavior | 0.0.2a |
| Redstone Signaller | one numeric comparison -> output | Source | `misc/signaller/*` | statement list, AND/OR logic, richer conditions | 0.0.2a |
| Clock | sampled pulse + world-time row | Source | `misc/clock/*` | original editor/visual hand animation parity | 0.0.2a |
| Forging Hammer | 3-block animated machine, GUI, recipes, automation | USER VERIFIED | `misc/hammer/*` | JEI/EMI recipe category, final visual parity | 0.0.2a integration |
| Standard/Mini Display | typed renderers + editor | USER VERIFIED/partial | PL2 display/GSI trees | full styles/actions/containers, pixel-perfect polish | 0.0.2a |
| Large Display | rectangular shared dynamic canvas <=16x16 | USER VERIFIED | connected display files | arbitrary shapes if desired, cross-chunk soak, complete GSI | 0.0.2a |
| Holograms | readable frame and clearance logic | OPEN visual defect | holographic display files | emitter-bar anchor, scale controls, advanced/volumetric parity | freeze + 0.0.2a |
| GSI editor | move/resize, 4 corners, palette, pages, typed elements | USER VERIFIED partial | 35 PL2 GSI classes | styled text, groups, hyperlinks/actions, extraction, sharing/export | 0.0.2a |
| Field Guide | 28 current PL4 chapters, search/bookmarks/tutorials | USER VERIFIED partial | original guide only as content reference | final prose/art/readability, recipe illustrations | 0.0.2a polish |
| Operator | configure/remove/toggle cable ports | Source/user use | `items/operator/*` | clone settings, overlays, rotation modes | 0.0.2a |
| KubeJS | recipe JSON compatibility | USER VERIFIED hammer recipes | CraftTweaker old integration as design reference | real PL4 KubeJS plugin/events/provider registration | 0.0.2a/0.0.3a |
| JEI/EMI | no full hammer category | MISSING | PL2 JEI integration | hammer category, recipe display, focus support | 0.0.2a |
| Save migration | current schema 2 / protocol 4 | Source | n/a | explicit future migrations, downgrade guards | every version |

---

# 10. Important source-audit findings to revisit

These are technical observations from the current R16 source that should guide future implementation.

## 10.1 `Part` is currently a monolithic state container

File:

```text
Part.java
```

One `Part` contains fields for readers, displays, transfers, redstone, wireless links, energy settings, canvas state, pending escrow, and more.

This was efficient for alpha development, but long-term it has costs:

- unrelated fields exist on every part;
- migrations become harder;
- addon/API extension is difficult;
- validation is distributed across packet handlers and UI.

**Recommendation:** do not refactor this during the freeze. During a later API phase, consider versioned per-kind state records/components while maintaining a migration layer from schema 2.

## 10.2 `Kind` is a fixed enum

File:

```text
Kind.java
```

This is safe for a closed mod but blocks external addons from registering new PL4 component kinds.

**Long-term option:** introduce a data/provider registry around a stable internal ID while keeping built-in `Kind` entries compatible.

## 10.3 Generic string-based edit packets will become limiting

File:

```text
PLPackets.java
```

Current `Edit` and `LayoutEdit` packets are bounded and permission checked, which is good, but they use string field/action payloads.

For more complex GSI/actions/scripting, use typed codecs for new protocol families rather than continuing to add arbitrary field names.

## 10.4 Display editor is functional but not full PL2 GSI

Files:

```text
DisplayElements.java
LayoutTransactions.java
DisplayEditorScreen.java
DisplayPropertiesScreen.java
DisplayPickerScreen.java
DisplayPainter.java
```

Current transaction actions are only roughly:

```text
add
update
delete
forward
backward
clear
mode
page
```

PL2’s GSI source has dedicated concepts for:

```text
selection modes
grid modes
create-info modes
click interactions
hyperlinks
element containers
saved element data
look interactions
multiple style/alignment types
styled text
```

This should be the first major parity target in `0.0.2a`.

## 10.5 Hologram position still uses a generic mount reference

Files:

```text
HologramProjection.java
HostRenderer.java
```

The current math knows the face, requested view, normal vs Advanced clearance, and camera side. It does not encode the actual emitter-bar geometry.

This is the current best explanation for the remaining visual mismatch and should be corrected before freezing `0.0.1a`.

## 10.6 Transfer routing is intentionally bounded

File:

```text
TransferEngine.java
```

R16 is intentionally safer than blindly matching incomplete historical semantics:

- normal Nodes are passive endpoints;
- explicit Transfer peers win ties;
- `ADD_REMOVE` does not blindly interact with the passive pool;
- same-cycle re-extraction is blocked;
- no chunk force-loading;
- Energy readers do not convert EU/J. R1.5 adds explicit, server-controlled Transfer Node conversion; telemetry remains native.

This is a good alpha safety boundary. The correct next step is to port **channels and filters**, not to loosen the safety rules blindly.

## 10.7 `NetworkEngine` needs measured performance acceptance

File:

```text
NetworkEngine.java
```

Good current properties:

- cached graph;
- dirty invalidation;
- loaded-only traversal;
- bounded network host count;
- no force-loading.

Still required:

- measured 100/500/1000/4096-host rebuild cost;
- repeated chunk unload/reload testing;
- multiple dimensions;
- many independent networks;
- wireless-heavy networks;
- display-heavy networks;
- stress with item/fluid/energy transfers at the same time.

Future optimization should be driven by profiles, not assumptions.

## 10.8 Reader/provider architecture should become extensible

Current files:

```text
DataSampler.java
EnergyReader.java
ReflectiveEnergyAccess.java
```

The current system works for built-ins and a few optional native energy APIs. PL2 had a broader info/provider registry and many integration classes.

A future `PL4DataProvider` API would let:

- Foundations modules;
- KubeJS;
- third-party mods;
- server packs

register monitored values without editing `DataSampler` itself.

## 10.9 Current configuration is intentionally small

R1.4 adds optional aggregate item/mB/FE delivery caps per connected data network per cycle, including escrow retries. Existing per-node settings remain; shared caps default to zero (uncapped). See `docs/TRANSFER_CONFIG.md`.


File:

```text
PLConfig.java
```

Current config includes network tick rate, host limits, transfer limits, wireless/cross-dimension toggles, entity scan radius, and optional energy readers.

Future versions should consider configurable:

- display limits;
- maximum canvas dimensions;
- wireless range/policy;
- permissions/public links;
- transfer fairness mode;
- per-resource transfer rates;
- provider enable/disable;
- display sync limits;
- diagnostics/logging levels;
- history/graph retention if time-series features are added.

## 10.10 Wireless tools are functional scaffolding, not full UX

File:

```text
ToolItem.java
```

Current transceivers store direct links and receivers validate owner/type. This is safe and simple, but PL2 had dedicated emitter/receiver UIs and Wireless Storage screens.

Do not mistake “link stored in item CustomData” for full wireless parity.

---

# 11. High-value upstream PL2 files to keep open during parity work

## Network/multipart

```text
api/core/tiles/connections/*
core/tiles/connections/data/handling/AbstractConnectionHandler.java
core/tiles/connections/data/handling/CableConnectionHandler.java
core/tiles/connections/data/handling/CableConnectionHelper.java
core/tiles/connections/data/network/LogisticsNetwork.java
core/tiles/connections/data/network/LogisticsNetworkHandler.java
core/tiles/connections/data/network/NetworkHelper.java
core/tiles/connections/redstone/*
```

## Readers/channels

```text
core/tiles/readers/base/TileAbstractReader.java
core/tiles/readers/base/TileAbstractListReader.java
core/tiles/readers/base/TileAbstractLogicReader.java
core/tiles/readers/items/*
core/tiles/readers/fluids/*
core/tiles/readers/energy/*
core/tiles/readers/info/*
core/tiles/readers/network/*
base/channels/ChannelList.java
base/channels/GuiChannelSelection.java
base/channels/ContainerChannelSelection.java
base/channels/handling/*
```

PL2’s `TileAbstractReader` explicitly treats the cable-facing side as `NETWORK` and the opposite side as `VISUAL`; R7 correctly moved PL4 back toward this contract.

## Transfer Nodes

```text
core/tiles/nodes/transfer/TileTransferNode.java
core/tiles/nodes/transfer/handling/NetworkItemHandler.java
core/tiles/nodes/transfer/handling/TransferNetworkChannels.java
base/filters/*
base/channels/*
```

PL2 `TileTransferNode` includes:

- priority;
- transfer mode;
- filters;
- `ChannelList`;
- connection toggle;
- item/fluid/energy toggles;
- selected block/entity channel data.

R16 restores endpoint behavior but not this entire selection/channel UX.

## Displays/GSI

```text
core/tiles/displays/gsi/DisplayGSI.java
core/tiles/displays/gsi/gui/*
core/tiles/displays/gsi/interaction/*
core/tiles/displays/gsi/modes/*
core/tiles/displays/gsi/packets/*
core/tiles/displays/gsi/render/GSIOverlays.java
core/tiles/displays/gsi/storage/*
core/tiles/displays/info/elements/*
core/tiles/displays/info/types/*
```

Important missing concepts include:

```text
clickable elements
hyperlinks/actions
element containers
selection/grid modes
styled text
alignment/fill types
saved layouts
look interactions
richer progress/list/grid editors
```

## Large connected displays

```text
base/events/types/ConnectedDisplayEvent.java
base/utils/worlddata/ConnectedDisplayData.java
core/tiles/displays/tiles/connected/ConnectedDisplay.java
core/tiles/displays/tiles/connected/ConnectedDisplayChange.java
core/tiles/displays/tiles/connected/BlockLargeDisplay.java
core/tiles/displays/tiles/connected/TileLargeDisplayScreen.java
network/packets/PacketConnectedDisplayRemove.java
network/packets/PacketConnectedDisplayUpdate.java
```

## Holographic displays

```text
core/tiles/displays/tiles/holographic/*
core/tiles/displays/tiles/render/RenderHolographicDisplay.java
network/packets/PacketHolographicDisplayScaling.java
```

## Wireless

```text
core/tiles/wireless/emitters/*
core/tiles/wireless/receivers/*
core/tiles/wireless/base/*
api/core/tiles/wireless/*
core/items/transceiver/*
core/items/wirelessstoragereader/*
```

## Array / Entity Node

```text
core/tiles/nodes/array/*
core/tiles/nodes/entity/*
```

## Redstone logic

```text
core/tiles/misc/signaller/*
api/core/tiles/misc/signaller/*
core/tiles/misc/clock/*
```

## Integrations

PL2 includes explicit integration source for:

```text
AE2
Calculator
CraftTweaker
Ender IO
Extra Utilities
Extreme Reactors
Flux Networks
IC2
Immersive Engineering
JEI
Mekanism
Vanilla providers
```

Do not blindly port old APIs; map each historical integration to a current 1.21.1 equivalent or explicitly retire it.

---

# 12. Recommended phase plan

# Phase 0 — Freeze `0.0.1a` (complete)

**Goal:** finish the foundation without adding another large subsystem.

### Completed freeze gate

- R17 emitter-relative hologram anchor and wall/floor/ceiling visual matrix accepted.
- Native Java 21 build and all 114 GameTests accepted.
- R16 item/fluid/FE transfer, escrow/reload, blocked-destination conservation, display/editor regression, and dedicated-server smoke checks accepted.
- Source/JAR/logs/screenshots/checksums archived by the release operator.

### Exit condition

`0.0.1a` is the immutable base for `0.0.2a`. The acceptance above is user-reported release evidence; repeat the native checks when producing the public frozen artifact.

---

# Phase 1 — `0.0.2a.R1`: Full PL2 display/GSI parity foundation

This should be the first major next-alpha work because displays are the feature users see constantly and the current editor is already a solid base.

## Add/restore

### Element styling

- text scale; **implemented in the R1 continuation** (25%–400% bounded data model; separate scale and alignment controls);
- text alignment; **implemented in the R1 continuation** (left, centre, right);
- width/height alignment;
- fill/background styles;
- opacity;
- border styles;
- icon scale;
- quantity position;
- bar style/direction;
- per-element formatting.

### Styled text

Use PL2/PL3 research for:

- wrapped text; **reconciled from the older R1 branch, with persisted wrap controls and element-height clipping**;
- titles;
- line breaks;
- style spans;
- formatting shortcuts;
- alignment.

### Editor workflow

- element list panel; **implemented in R1.3: frontmost-first current-page list, Shift-toggle and Ctrl+A selection, bounded list navigation**;
- multi-select; **extended in R1.1 with Shift-toggle, Ctrl+A, box selection, rigid multi-element movement and atomic multi-delete**;
- box/lasso select; **box selection implemented in R1.1 with page-local overlap, reverse drag and Shift-add; freeform lasso remains planned**;
- alignment tools; **implemented in R1: left/right/top/bottom/centre, using canvas bounds for one element and selection bounds for multiple elements**;
- distribute evenly; **implemented in R1: horizontal/vertical equal edge gaps for three or more current-page elements, preserving outer anchors**;
- copy/paste; **extended in R1.1 to an entire page-local selection, preserving styles/spacing, with one revision-fenced paste**;
- duplicate with deterministic offset; **extended in R1.1: Ctrl+D duplicates the entire selection with a shared 4px offset clamped to canvas edges**;
- group/ungroup;
- lock element;
- hide/show element;
- undo/redo; **implemented in the R1 continuation: bounded client-side history of whole-layout snapshots, restored server-side through a new revision-fenced `replace` transaction (Ctrl+Z / Ctrl+Y, Ctrl+Shift+Z)**;
- page names;
- page duplicate/delete; **implemented in R1.4: duplicate current page into an empty slot with fresh element IDs; undoable clear of current page contents; eight stable slots retained**;
- keyboard shortcuts;
- clearer selected-layer order; **implemented in R1.3: stable page-local multi-selection forward/backward/front/back, one revision and one undo snapshot**.

### Containers/actions

Port PL2 concepts for:

- element containers;
- clickable elements;
- display actions;
- hyperlinks/page links;
- safe player interactions.

### Layout storage

- export/import layout JSON;
- named templates;
- copy layout with Operator;
- optional server-provided templates;
- versioned layout schema.

### Acceptance

A user should be able to rebuild representative PL2 screens without editing NBT or scripts.

---

# Phase 2 — `0.0.2a.R2`: Reader channels and data-provider parity

## Channels

Restore a real modern equivalent of PL2 `ChannelList` behavior:

- single/unlimited channel types;
- node selection;
- block/entity targets;
- named channels where useful;
- reader channel-selection UI;
- selected target feedback;
- safe unloaded-target state;
- permissions.

## Inventory Reader

- per-slot view;
- storage/stack/list modes;
- exact component-aware grouping;
- filter UI;
- sorting UI;
- duplicate combined-inventory handling;
- channel selection.

## Fluid Reader

- per-tank view;
- aggregate view;
- channel selection;
- filters;
- capacity/fill data;
- third-party tank acceptance.

## Energy Reader

Keep units native and add, where APIs can provide them honestly:

- stored;
- capacity;
- fill percentage;
- input rate;
- output rate;
- GT voltage/amperage/tier;
- Mekanism native rate fields;
- FE rate sampling.

Do **not** invent cross-unit conversions just to simplify displays.

## Info Reader

Rebuild a provider registry instead of continuing to hard-code every property into `DataSampler`.

Potential provider categories:

- block state;
- furnace/process progress;
- redstone;
- comparator output;
- crops;
- world time/weather;
- entity health/armor/food/xp;
- machine progress;
- mod-specific values.

## Network Reader

Upgrade from basic counts to:

- host count;
- part count;
- channel count;
- resource endpoints;
- wireless edges;
- network health;
- rebuild count/debug data when permissions allow.

---

# Phase 3 — `0.0.2a.R3`: Transfer Node parity

R16 provides a safe base. R3 should add the historical selection/routing model rather than removing safeguards.

## Restore

- ChannelList-style source/destination selection;
- rich filter GUI;
- item + tag filters;
- fluid filters;
- component-sensitive filter options;
- directional behavior;
- connection toggle;
- priority UI;
- transfer rate settings where desired;
- explicit ADD/REMOVE/BOTH semantics based on selected channels.

## Reliability

Test:

- full destinations;
- partial insertion;
- simulation disagreement;
- source changes mid-cycle;
- chunk unload;
- server stop during escrow;
- reload;
- cross-dimension wireless;
- many equal-priority sinks;
- saturation/backpressure;
- duplicate endpoints to the same combined inventory.

## Native energy transport

R1.5 implements user-requested optional native Mekanism J, GTCEu EU and Electrodynamics/Voltaic Joule transport plus explicit node A → B conversion. See `docs/ENERGY_TRANSFER.md`. Native FE remains the default; this does not complete Phase 3 transfer parity.

Recommended default:

- FE transport remains core;
- native EU/J transport is optional integration-specific behavior only when the external API has clear safe transaction semantics;
- never auto-convert merely because the reader can display another unit.

---

# Phase 4 — `0.0.2a.R4`: Wireless, Arrays, Entity Nodes, and remote storage

## Data/Redstone Emitters and Receivers

Add:

- emitter discovery screen;
- receiver pairing screen;
- names;
- channels/frequencies;
- public/private/team policy;
- permission checks;
- range/config policy;
- live link status;
- cross-dimension status.

## Array

Restore the original concept more faithfully:

- eight physical/logical transceiver slots;
- insert/remove link items;
- GUI;
- slot status;
- loaded/unloaded target state;
- item lifecycle when broken.

## Entity Node

Add:

- entity selection/scanning UI;
- display target name/type;
- inventory/equipment data where safe;
- reconnect to UUID after unload/reload;
- dimension changes;
- permission rules for players/entities.

## Wireless Storage

PL2 reference:

```text
ItemWirelessStorageReader.java
GuiWirelessStorageEmitterList.java
GuiWirelessStorageReader.java
ContainerEmitterList.java
ContainerStorageViewer.java
PacketWirelessStorage.java
```

PL4 target:

- emitter list;
- remote inventory view;
- search;
- fluid view;
- safe deposit/withdraw if enabled;
- server-authoritative transactions;
- range/dimension/permission rules;
- no client-trusted inventory manipulation.

---

# Phase 5 — `0.0.2a.R5`: Redstone logic and automation

## Redstone Signaller

Current one-comparison behavior should become a full statement system.

Port concepts from:

```text
RedstoneSignaller.java
RedstoneSignallerStatement.java
GuiStatementList.java
SignallerModes.java
```

Target features:

- multiple statements;
- AND/OR group logic;
- `>`, `<`, `>=`, `<=`, `=`, `!=`;
- named data source selection;
- per-face output;
- invert;
- pulse/level modes;
- diagnostics explaining why output is on/off.

## Clock

- original-style editor;
- interval/duty cycle;
- phase offset;
- redstone output side;
- restore/modernize clock hand animation if still visually desirable.

---

# Phase 6 — `0.0.2a.R6`: Integrations, JEI/EMI, KubeJS, public API

## Recipe viewers

Add:

- JEI Forging Hammer category;
- EMI category;
- input/output/timing display;
- recipe focus support;
- guide links to recipe viewers where available.

## Jade inspection

Required integration requested by the user:

- identify the targeted multipart component rather than treating every host as a generic cable;
- show component/network connection status and configured update interval;
- expose useful reader/transfer diagnostics through existing permission checks;
- reuse bounded existing state instead of scanning the whole network per tooltip frame;
- remain optional at runtime so the standalone mod still starts without Jade.

## Modern integration matrix

Evaluate current equivalents for historical PL2 integrations:

| Historical PL2 integration | Modern PL4 target |
|---|---|
| AE2 | storage/network/crafting telemetry using current AE2 APIs |
| Calculator | native Foundations Calculator telemetry |
| CraftTweaker | modern CraftTweaker recipe/provider hooks if demand exists |
| Ender IO | current machine progress/energy/storage APIs |
| Extra Utilities | no direct port if no current equivalent; map only where relevant |
| Extreme Reactors | current reactor/turbine telemetry |
| Flux Networks | current network/buffer telemetry |
| IC2 | only if a maintained 1.21.1 equivalent exists |
| Immersive Engineering | machine process/energy/fluid telemetry |
| Mekanism | richer native J/machine telemetry |
| JEI | hammer recipe category |

Also consider modern ecosystems absent from old PL2:

```text
Create
Modern Industrialization
GregTech CEu Modern
Powah
Industrial Foregoing
Thermal-series equivalents if available
Refined Storage / RS2 if applicable
CC:Tweaked
```

## KubeJS

Move beyond recipe JSON.

Potential API shape:

```js
PL4Events.dataProviders(event => { ... })
PL4Events.displayElements(event => { ... })
PL4Events.networkActions(event => { ... })
```

Possible scriptable capabilities:

- register custom reader rows;
- register custom machine providers;
- register safe custom display element types;
- create named formatters;
- create display templates;
- observe transfer events;
- add custom conditions.

Keep server authority and validation mandatory.

## Public Java API

Create a small versioned API rather than exposing internal `Part` directly.

Possible interfaces:

```text
PL4DataProvider
PL4DisplayElementProvider
PL4NetworkObserver
PL4TransferEndpoint
PL4PermissionService
```

---

# Phase 7 — `0.0.2a.R7`: Performance, security, multiplayer, release hardening

## Performance benchmarks

Test at least:

```text
100 hosts
500 hosts
1000 hosts
4096 hosts
```

Scenarios:

- simple cable line;
- dense branching;
- many readers;
- many large displays;
- many item icons;
- many wireless links;
- transfers enabled;
- multiple independent networks;
- chunk unload/reload churn.

Record:

```text
server tick cost
network rebuild time
sampling time
packet volume
client frame time
display render batches
memory growth
```

## Multiplayer/security

Test:

- unauthorized edit packets;
- stale layout revisions;
- player disconnect while editing;
- two players editing same canvas;
- link ownership;
- spawn protection;
- remote target permissions;
- malformed element payloads;
- packet spam/rate limits;
- huge NBT/components;
- broken/unloaded links.

The current packet revision fencing and rate limiting are good foundations; expand tests instead of weakening them.

## Save/recovery

Test:

- world save during transfer escrow;
- server kill/restart;
- chunk unload with pending transfer;
- display controller removal;
- large display split/rejoin;
- wireless target missing/reappearing;
- schema migration from frozen `0.0.1a`.

## Release gate for `0.0.2a`

- clean Java 21 build;
- full GameTest suite;
- dedicated server soak;
- graphical acceptance;
- integration matrix;
- migration from frozen 0.0.1a world copy;
- no known item loss/duplication;
- no silent permission bypass.

---

# 13. `0.0.3a` and beyond — expansion beyond PL2

Once PL2 parity is in good shape, PL4 can become more than a port.

These are **expansion ideas**, not promises for 0.0.2a.

## 13.1 PL3-inspired data addresses and method graph

PL3 contains unfinished but interesting architecture:

```text
server/address/*
server/data/*
server/data/methods/*
client/nodes/Graph.java
client/nodes/AdvancedNodeGraph.java
client/nodes/DataSourceNode.java
client/gsi/*
```

Potential PL4 evolution:

- every monitored value has a typed data address;
- data can be transformed through methods;
- method nodes can perform arithmetic/formatting/logic;
- display elements bind to graph outputs rather than directly to reader rows;
- graph editor is optional and advanced, not required for normal PL2-style usage.

This should be additive. The simple PL2 workflow must remain easy.

## 13.2 Time-series graphs and history

New display elements:

- line graph;
- area graph;
- sparkline;
- min/max/average;
- delta/rate;
- historical bar chart.

Potential monitored data:

- energy usage;
- generation;
- storage fill;
- item throughput;
- fluid throughput;
- machine progress;
- server/network health.

Use bounded retention and server-configurable sampling. Do not synchronize unbounded history.

## 13.3 Alerts and notifications

Allow conditions such as:

```text
battery < 20%
chest > 90% full
machine stopped
fluid tank empty
network disconnected
transfer escrow stuck
```

Actions could include:

- display warning;
- redstone output;
- player toast/chat;
- sound;
- Foundations HUD notification;
- optional webhook/plugin integration later.

## 13.4 Network diagnostics mode

Operator or Network Reader could expose:

- topology overlay;
- cable port states;
- network ID;
- endpoint count;
- wireless edges;
- data vs visual edges;
- transfer route;
- rebuild reason;
- overloaded network warnings.

This would make complicated builds much easier to troubleshoot.

## 13.5 Layout/template library

Add:

- save named display template;
- copy/paste between worlds/servers where allowed;
- server template folder;
- KubeJS/datapack-provided templates;
- import/export JSON;
- template version/migration.

Example templates:

```text
Battery dashboard
Reactor dashboard
Storage wall
Fluid plant
Machine status board
Server/network status
```

## 13.6 Team/permission model

Move beyond owner-only links with explicit policies:

```text
Private
Team
Public read-only
Public interactive
Admin
```

Integrate with common team/claim APIs where available, but keep a native fallback.

## 13.7 Handheld diagnostic terminal

Expand Wireless Storage / Operator into a configurable handheld terminal:

- network overview;
- reader data;
- remote display preview;
- alerts;
- search;
- transfer diagnostics;
- admin-only topology details.

Do not let the client directly mutate remote inventories without server validation.

## 13.8 Foundations ecosystem integration

Potential optional integrations with other Foundations modules:

- Economy balances/market/stock telemetry on PL4 displays;
- Calculator machine dashboards;
- Soil sensor dashboards;
- Cluster/server health dashboards for server operators;
- shared Foundations Field Guide navigation;
- Framework theme hooks while keeping PL4 standalone when Framework is absent.

These should be optional bridges, never hard dependencies unless a future architecture decision explicitly changes that.

## 13.9 Computer integration

Potential CC:Tweaked/peripheral style API:

- read reader rows;
- read network status;
- set display page;
- push safe text/layout updates;
- inspect alerts.

Write operations should be permission-gated and rate-limited.

## 13.10 Data-driven custom readers

Allow datapacks/KubeJS/API providers to define:

- labels;
- units;
- numeric properties;
- capacity pairs;
- formatting;
- machine detection;
- icon/colour metadata.

This could make PL4 useful across mods without shipping a hardcoded Java bridge for every machine.

---

# 14. Things PL4 should deliberately avoid

These rules protect the project from repeating old problems.

1. **Do not make PL3 the visual target.** PL2 remains the in-world visual/interaction reference.
2. **Do not reintroduce Sonar Core as a required runtime JAR.** Keep the combined modern implementation.
3. **Do not claim generic FE support equals every historical energy integration.**
4. **Do not convert EU/J/FE silently.** Native units should stay honest.
5. **Do not force-load chunks just to keep dashboards alive.** Make unloaded state explicit.
6. **Do not let client packets grant access to data the server topology does not expose.**
7. **Do not use unique transient NBT on ordinary dropped parts.** Equivalent default parts must remain stackable.
8. **Do not fix z-fighting by pushing UI geometry visibly far off the screen.** Maintain shallow ordered render planes.
9. **Do not hide parity gaps by registering placeholder content.**
10. **Do not change save schema casually.** Every schema change needs migration and downgrade guidance.
11. **Do not make advanced graph/scripting features mandatory for basic monitoring.** PL2-style simple workflows should stay simple.

---

# 15. Testing strategy

## 15.1 Single-account constraint

Testing must continue to assume one Minecraft account is available.

Where multiplayer behavior is required, use:

- GameTest fake/server players where possible;
- admin/test commands;
- simulated second owner UUIDs;
- deterministic permission fixtures;
- dedicated server/client reconnect tests.

Do not make acceptance depend on owning a second account.

## 15.2 Automated test groups

Maintain separate GameTest groups for:

```text
multipart placement
network connectivity
visual-only reader exports
wireless links
reader sampling
transfer routing
item conservation
fluid conservation
FE conservation
escrow persistence
hammer processing
display continuity
layout migration
NBT stackability
permissions/packet validation
save/reload
```

## 15.3 Graphical test matrix

Test:

```text
GUI scales: small / normal / large
windowed
fullscreen
first-person right hand
first-person left hand
third person
dropped items
item frames
front-on displays
shallow-angle displays
shader off
common shader/render mods where practical
```

## 15.4 Mod compatibility acceptance matrix

For each supported integration, record:

```text
exact mod version
exact API path used
machine/block tested
reader values
transfer behavior if applicable
restart behavior
known warnings
```

Do not advertise “supports Mod X” based only on reflection code existing.

## 15.5 Transfer conservation tests

Every transfer implementation must prove:

```text
before source + destination + escrow
=
after source + destination + escrow
```

for items, fluids, and energy within the unit’s exact representable type.

Test partial insertion and deliberately hostile/misbehaving handlers.

---

# 16. Version and packaging policy

Use the existing Foundations revision policy:

```text
0.0.1a
0.0.1a.R2
...
0.0.1a.R16
0.0.1a.R17 — narrow final-freeze emitter anchor
```

Keep the same base alpha while finishing that alpha’s agreed scope.

After the freeze gate:

```text
0.0.1a = frozen foundation baseline
0.0.2a = PL2 parity expansion line
0.0.3a = post-parity / PL3-inspired expansion line
```

For each revision, prefer:

- complete source ZIP;
- portable updater ZIP;
- BAT launcher usable outside the project folder;
- Windows folder picker + manual fallback;
- expected baseline hash verification;
- backup of changed files;
- optional build;
- optional GameTest run;
- validation markdown;
- acceptance markdown;
- SHA-256 list.

If save schema changes, also include:

- schema number;
- automatic migration path;
- downgrade warning;
- copy-world acceptance plan.

---

# 17. Recommended next-version sequence

## If R16 still has the hologram anchor issue

Use:

```text
0.0.1a.R17 — Hologram anchor + final freeze only
```

Do **not** add new feature work to R17.

Then freeze `0.0.1a`.

## Begin `0.0.2a`

Recommended sequence:

```text
0.0.2a.R1  Advanced PL2 GSI/editor parity
0.0.2a.R2  Reader channels + provider registry
0.0.2a.R3  Transfer Node channel/filter parity
0.0.2a.R4  Wireless / Array / Entity Node / Wireless Storage
0.0.2a.R5  Redstone Signaller + Clock parity
0.0.2a.R6  JEI/EMI + integrations + KubeJS/public API
0.0.2a.R7  Performance / multiplayer / migration / release hardening
```

The exact number of revisions may change, but the dependency order should stay close to this sequence.

---

# 18. Priority backlog

## P0 — before freezing `0.0.1a`

- [x] R17 source anchors normal holograms at the emitter bar and Advanced holograms at the selected projection panel.
- [x] R17 normal/Advanced holograms visually accepted on wall, floor and ceiling mounts.
- [x] Exact freeze source builds under Java 21.
- [x] 114/114 GameTests pass.
- [x] R16 item passive-endpoint transfers accepted in-game.
- [x] R16 real fluid passive-endpoint transfers accepted.
- [x] R16 real FE passive-endpoint transfers accepted.
- [x] Full destination cannot delete resources.
- [x] Escrow survives restart and delivers once.
- [x] R14/R15 display/editor render fixes stay fixed.
- [x] Final save/reload and dedicated-server smoke.
- [x] Frozen source/JAR/checksums/logs archived.

## P1 — first half of `0.0.2a`

- [ ] Full GSI element/style/action roadmap implemented.
- [ ] Styled text and alignment.
- [ ] Multi-select/group/copy/paste/undo.
- [ ] Layout import/export/templates.
- [ ] Reader ChannelList equivalent.
- [ ] Full reader mode/filter GUIs.
- [ ] Provider registry.
- [ ] Network Reader diagnostics.
- [ ] Transfer Node channel/filter parity.
- [ ] Transfer fairness/recovery stress tests.

## P2 — second half of `0.0.2a`

- [ ] Wireless emitter/receiver selection GUI.
- [ ] Array transceiver slots and GUI.
- [ ] Entity Node selection/interactions.
- [ ] Wireless Storage remote GUI/actions.
- [ ] Redstone Signaller statement lists.
- [ ] Clock editor/visual parity.
- [ ] Jade multipart inspection and permission-aware status.
- [ ] JEI/EMI hammer integration.
- [ ] Modern mod provider matrix.
- [ ] KubeJS provider/event API.
- [ ] Public Java API.
- [ ] Large-network profiling and optimization.
- [ ] Security/multiplayer stress.

## P3 — `0.0.3a+` expansion

- [ ] PL3-inspired data-address/method graph.
- [ ] Time-series charts/history.
- [ ] Alerts/notifications.
- [ ] Network topology debugger.
- [ ] Layout/template library.
- [ ] Team permission model.
- [ ] Handheld diagnostic terminal.
- [ ] Foundations ecosystem dashboards.
- [ ] Computer/peripheral integration.
- [ ] Data-driven custom readers/providers.

---

# 19. Definition of “PL2 parity” for PL4

PL4 should only claim strong Practical Logistics 2 parity when all of the following are true:

1. The major released PL2 components exist and behave recognizably like PL2.
2. The multipart placement/connectivity rules support normal PL2 build patterns.
3. Reader channel selection and modes are restored or intentionally modernized with equivalent capability.
4. Displays support the important original information types and editor interactions.
5. Connected displays behave predictably through expansion, split, reload, and chunk boundaries.
6. Holographic displays look and behave correctly in every mount orientation.
7. Transfer Nodes have proper filters/channels/routing and cannot lose or duplicate resources.
8. Wireless/Array/Entity/Wireless Storage systems have real UIs and lifecycle behavior, not only raw stored links.
9. Redstone Signaller/Clock functionality reaches useful original parity.
10. Important historical integrations are either mapped to modern APIs or explicitly documented as retired.
11. A complete native automated test suite passes.
12. Client visual acceptance passes across common GUI scales and view angles.
13. Dedicated-server and save/restart tests pass.
14. No known critical item/fluid/energy loss, duplication, permission bypass, or save corruption issue remains.

PL4 does **not** need to reproduce old Forge/Sonar APIs byte-for-byte. The target is behavioral and visual parity on a safe modern architecture.

---

# 20. Final direction

The project has moved well beyond a registration-only port. The current alpha has a real internal multipart system, server network graph, typed displays, large canvases, reader telemetry, a working hammer, a modern Field Guide, and a conservative transfer engine.

The next mistake to avoid is broadening scope before the foundation is frozen.

Recommended order from here:

1. **Visually accept the R17 hologram anchor.**
2. **Native-test R16 transfers and all 114 GameTests.**
3. **Freeze `0.0.1a`.**
4. **Use the frozen alpha as the migration baseline.**
5. **Spend `0.0.2a` completing PL2 parity, especially GSI, channels, transfers, wireless, and integrations.**
6. **Only after that, use PL3 and new Foundations ideas to expand beyond PL2.**

That gives Foundations PL4 a clear identity:

> **PL2’s recognizable logistics/monitoring experience, rebuilt safely for Minecraft 1.21.1, then extended into a modern programmable information and automation platform.**

---

# Appendix A — Current R16 config surface

Current server configuration is intentionally small:

```text
network.updateTicks
network.maximumHosts
network.maximumRows
network.wirelessEnabled
network.crossDimensionWireless
network.entityScanRadius
transfer.enabled
transfer.itemsPerCycle
transfer.millibucketsPerCycle
transfer.fePerCycle
energyReader.mekanismJoules
energyReader.gregtechEU
energyReader.maximumContainersPerHandler
```

This is adequate for `0.0.1a` but should grow carefully as new systems become real.

---

# Appendix B — Current display element contract

```text
TEXT
ITEM
BLOCK
INVENTORY
FLUID
FLUID_GRID
BAR
```

Current editor limits:

```text
MAX_ELEMENTS = 32
MAX_PAGES = 8
MAX_ICONS = 128
MAX_CANVAS = 4096
```

These limits are safety bounds, not necessarily permanent UX limits.

---

# Appendix C — Current native test count by revision source file

```text
PLGameTests.java   16
R5GameTests.java   15
R6GameTests.java    4
R7GameTests.java   23
R8GameTests.java   15
R9GameTests.java   20
R10GameTests.java   5
R11GameTests.java   5
R13GameTests.java   3
R16GameTests.java   8
--------------------
TOTAL             114
```

R12/R14/R15 primarily added source/offline/render guards rather than additional native GameTest classes.

---

# Appendix D — Upstream provenance reminders

Primary parity reference:

```text
Practical Logistics 2
https://github.com/SonarSonic/Practical-Logistics-2
commit 4772196103d35c78c33f03c288a7b47aac267197
```

Historical reference:

```text
Practical Logistics
https://github.com/SonarSonic/Practical-Logistics
commit 4b82002021cba1788b59b3279a7ade97ebca2357
```

Future design research only:

```text
Practical Logistics 3
https://github.com/SonarSonic/Practical-Logistics-3
commit bfd78dee190fef0964470549c3e023ddff3a2a61
```

Legacy Core audit:

```text
Sonar Core
https://github.com/SonarSonic/Sonar-Core
commit f5b64e033e2c07af55c2d5f474052fb83fba5ca1
```

Maintain these pins in the project so future refactors can always answer “which original code were we comparing against?”

## R1.10 combined display workflow update

Implemented on both active tracks: schema-1 JSON whole-layout export/import, client-local named templates, eight-page spatial previews, fit-to-screen bounds, fresh imported identities, reader-binding clearing, revision-fenced and undoable apply, and item/fluid/energy starter boards. See the current field guide. Optional server templates, Operator-held layout copying, element lock/hide/grouping and the remaining integration backlog are still pending.


## 0.0.2a.R2 — first reader/provider batch

Implemented: persisted endpoint pins independent of data mode; bounded target cycling and reset; no fallback for missing pins; fluid tank SLOT selection; held-item filter shortcut; trimmed Info Reader metric keys; read-only provider registry with bounded namespaced results, failure isolation and unregister handles. Vanilla information sampling now goes through the registry.

Still open in Phase 2: user-named channels and richer searchable selection, combined-inventory deduplication, full per-slot/per-tank presentation, richer network diagnostics, third-party machine providers and installed-mod acceptance. This batch does not mark the entire R2 parity phase complete.


## 0.0.2a.R2.1 — reader correctness closeout

Completed: paginated/searchable endpoint selection; saved per-reader aliases; correct pin status beyond 64 endpoints; current-network validation of target edits; exact handler and vanilla combined-inventory deduplication; network availability diagnostics; current 26.3 furnace keys; registration generation safety; immutable provider snapshot reuse; dead-code cleanup.

Release acceptance remains explicitly open for real-client visuals, two-player gameplay and installed optional power mods. Generic overlapping modded storage views require provider-specific identities; no heuristic equality-based deduplication was added. Broader machine providers, richer per-slot/per-tank presentation and full Phase 2 parity remain follow-up work.


## 0.0.2a.R2.2 — final audit performance fix

Reader-only browsing controls no longer invalidate network topology. Native packet coverage verifies unchanged topology build count across successful search/page edits. This completes the current correctness cleanup, while the client/multiplayer/installed-mod acceptance boundaries listed above remain open.


## 2026-10-25 legacy search development

Legacy handheld storage now implements bounded display-name substring search with the native text field and server-authoritative, revision-fenced view changes on both 1.12.2 and 1.6.4. It composes with existing per-slot sorting. Search supports 32 printable ASCII characters; the later grouped-quantity checkpoint above records its own exact-source evidence, while remaining legacy editor/multipart parity stays open. Exact-source build/client outcomes are in the RC ledger; RC clearance remains false.

The 26.x fixture count is reconciled: each retained loader-profile log has 192 PL4 tests plus one `minecraft:default` environment test, for 193 required passes. Keep the PL4 source declaration at 192. Future controller reports retain both totals and environment attribution rather than conflating them.

## 2026-10-26 retained legacy evidence quarantine

Both legacy ports now treat retained item evidence as an independent quarantine signal. Clearing public fault text cannot enable route edits, flush uncertain escrow through a rewritten owner network, lose wide saved escrow counts or turn actual removal into spendable drops in the eight exact native cases. The unchanged production first failed all eight; fixed sources pass378/378 and356/356. Current hourly process/world gates and both exact-JAR stock clients also pass. See `docs/acceptance/rc-dev-legacy-live-evidence-2026-10-26.json`. New separate-process live-mutation and disconnected-alias probes, installed providers, multiplayer, arbitrary copied worlds, reviewed reconciliation, sustained performance and broader parity remain open. All24 RC clearances remainfalse.

## 2026-10-26 live fault guards across native restart

Added168 live fault-clearing/empty-escrow face-edit/native-NBT preservation assertions per legacy port before context rejection and on retained hosts after restart. Total decode1044/28 JVMs per port now passes (raw/missing/compound41/29 per write/read;field45/33), separate from provider188 and historical60. Native378/356/main192 pass;119 targeted local controller tests pass. All17 packaged hashes, all raw decode/provider/historical saved worlds and distinct process identities qualified. Fresh1.12.2 exact-JAR client passes complete5-page export and ordinary regressions;2 captures reviewed,21 hashed.1.6.4 production bytes unchanged5074b085, retaining prior exact-payload client evidence without a new run/review. All24 RC flagsfalse;no new release/upload.

Extend remaining owner/context rewrite, transfer and actual removal live mutations across separate native processes. Probe disconnected aliases after owner/context edits with itemImportPending already false. Current168 new process assertions cover fault clearing, empty-escrow face-edit refusal and NBT evidence/count preservation only; no remaining alias bypass demonstrated.

## 2026-10-26 observed live legacy admission

Fixed the cleared admission latch on retained legacy evidence: explicit admission/server ticks now revalidate existing owner/context and preserve original evidence, freezing rejected changes and fencing the tested disconnected same-target alias after observation. Both ports reproduced4 failures plus1 passing idempotence control, then pass all5 new cases: native383/383 and361/361; main192 passes.125 local controller and176 CI executions pass. Existing decode1044/28 JVMs,provider188 andhistorical60 per port separately requalify, with raw saved NBT andall17 packaged hashes. Both fresh exact-JAR clients pass complete5/4-page export and normal regressions;4 captures reviewed,42 hashed. Initial1.6.4 fixture-constructor/cascading-cleanup failure is recorded and corrected before accepted reproduction. CurseForge reconciliation confirms24 already-submitted receipts,0 new uploads. All24 RC flagsfalse.

Extend live owner/context rewrite, transfer and actual-removal mutations through separate native processes. Probe disconnected alias reads before any host tick/admission observation and loaded/unloaded host boundaries without a global world scan or force-load. Current5 native cases cover explicit admission, observed tick, observed disconnected same-target alias, valid idempotence and raw context edits; they do not grant general alias or mutation-process clearance.


## 2026-10-28 — 1.12.2 capability-discovery replacement guard

Forge 1.12.2 handheld endpoint discovery now checks that the installed target tile, host tile, loaded state, route and owner are unchanged **after** an untrusted native capability callback. A callback replacing the target aborts enumeration before a subsequent player-inventory debit. The exact branch source is `e567fe33a8d6ca30204464e910176a6d3fca4199`, preserving development-only and `rc_publication_cleared: false`. An adversarial native fixture replaces a chest during `hasCapability`, verifies zero transferred/created items, unchanged 32-item player stock and no additional drops; the declared ordinary native count becomes **392**. On `main`, the unique-case controller guard and its two positive/negative Python regression tests are committed at `b7816b902fc50865dee2c5c4b5dc4ca10443474b` and discovery tests passed.

Exact-version native workflow [40636413380](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40636413380) was **in_progress** at this checkpoint, not an accepted 392-case native/JAR/restart receipt. Existing 391-case 1.12.2 and 365-case 1.6.4 source-specific passes remain historical evidence only. New installed-provider, client, multiplayer, migration variants, sustained performance and explicit RC clearance remain open, as does the 1.6.4 gravel-actor cause. No RC or CurseForge publication was authorized.

## 2026-10-28 — second capability lookup and native receipt reconciliation

The 1.12.2 capability discovery candidate `e567fe33a8d6ca30204464e910176a6d3fca4199` **passed** 392/392 Forge-native scenarios and exact separate-process evidence in the retained [hourly action 40636974001](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40636974001). Its [artifact 12793932585](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40636974001/artifacts/12793932585) passes ZIP CRC, five JAR hashes, production-only boot, generated legacy preview.3 migration, 1,536 decode assertions through 40 JVMs, 188 registered-provider assertions through four JVMs and 60 historical assertions through three processes. Exact packaged runtime SHA-256: `2e8e3eb5b8796449d442fd7f234be7c5348f4059be52d57477f44e72e10ff726`. The same-source branch job [40636413380](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40636413380) was cancelled at 30 minutes and is not a passing independent job. These results qualify only `e567fe33a8d6ca30204464e910176a6d3fca4199`, not subsequent source.

The new `mc/1.12.2` [commit 791a94df](https://github.com/Shnewbs/Foundations-PL4/commit/791a94df5e1247d5c5cef9a7722784e972b7f0b4) strengthens fail-closed post-capability **host residency, unchanged host position and target** checks, and adds a distinct native regression where a second capability query replaces the installed inventory after prior endpoint enumeration. The new exact-source count is **393**, with [branch validation 40642756603](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40642756603) **pending** at this checkpoint. No 393-case pass or new runtime hash is claimed. The original 1.6.4 x=80 gravel collision remains unresolved; installed providers, multiplayer/identities, client visuals, wider user migrations, sustained performance and full parity remain unaccepted. All 24 RC clearances remain false. Latest CurseForge sync [40639237226](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40639237226) reconciles 24 already-submitted historical files and no new uploads. No new GitHub RC was published. Complete evidence: `docs/acceptance/rc-dev-112-second-lookup-2026-10-28.json`.
