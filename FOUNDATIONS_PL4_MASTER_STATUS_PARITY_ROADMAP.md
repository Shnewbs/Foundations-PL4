
### October 29, 2026 UTC — installed 1.21.1 real-client native NBT acceptance, scoped PASS

Controller [d632543d](https://github.com/Shnewbs/Foundations-PL4/commit/d632543d2d2168aa9cb8f2346f32eb08e1172af3) adds fail-closed direct saved-player gzip-NBT and raw Anvil multipart/barrel checks after actual two-client published-beta → exact candidate binary migration. [Run 40837667228](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40837667228) completed successfully: both real installed clients per phase, 17+17 diamonds in persisted playerdata, 94 saved in barrel, 128 total conserved, wireless tool link points to saved node, owned node/cable UUIDs unchanged, and no pending escrow. Independently downloaded [retained artifact 12881367190](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40837667228/artifacts/12881367190) SHA-256 `7e5de9d675affc1dea07c506357a69e90d868d1435ef3e5b460b801f1b2ae336`: all 45 ZIP entries and both 26-file and 30-file nested saved-world archives CRC-valid. Previous [failed 40836989229](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40836989229) recorded a genuine test-harness race: server had saved/stopped while Gradle wrapper was still alive and RCON socket closed; bounded natural shutdown was added before authenticated fallback with four guard regressions. The separate [main native verification 40837667338](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40837667338) passed 194/194 GameTests. [Publisher 40837667268](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40837667268) skipped new GitHub release and CurseForge upload. This is scoped offline 1.21.1 installed binary acceptance; it does **not** qualify combined connected cable+client transfer, external providers, online auth, arbitrary worlds, sustained performance or the remaining 23 exact versions. All 24 RC clearances are false. [Evidence](docs/acceptance/rc-dev-1211-installed-clients-native-saved-nbt-2026-10-29.json). Next combine connected auto-transfer with two genuine installed clients and independent saved NBT, then broad provider/performance/port acceptance.

## October 28, 2026 PT — exact installed 1.21.1 network scheduler auto-transfer PASS; RC clearance 0/24

On main controller [source `25a7a88`](https://github.com/Shnewbs/Foundations-PL4/commit/25a7a88ae647511bca66f4df90fe9fa5bae19a72), the independently installed published `v0.2b.0` and previously qualified development JAR each executed **actual native `NetworkEngine.tick` item delivery via real `ServerTickEvent.Post`**, on a physically discovered seven-centre DATA_CABLE graph with two owned `TRANSFER_NODE` endpoints. Neither scheduled phase directly calls `TransferEngine.run`, injects synthetic `NetworkEngine.Ref` entries, nor invokes `NetworkEngine.tick` from the probe. Each phase moved 64 diamonds after 19 server ticks; actual saved raw Anvil NBT verifies network source/sink **1728/0 → 1664/64 → 1600/128** across a byte-identical saved-world input and upgrade, all 1,728 conserved, zero escrow, with original nine owned cable/endpoint UUIDs retained. The earlier explicit-ref transfer checks are preserved as a separate test.

[Successful installed-binary native workflow `40828273375`](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40828273375) passed **96/96** fail-closed policy regressions and both real server phases. Independently downloaded [artifact `12877272732`](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40828273375/artifacts/12877272732) SHA-256 `b7a70f582af989aa295e9fd904dfb8ba9c3b2a672b735fd76e81634839f881d5`, 25/25 ZIP CRC-valid; the two nested native saved-world archives are CRC-valid with 19 and 22 files. [Main verification `40828273434`](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40828273434) passed **194/194 native GameTests**, without producing a new published candidate JAR. Initial obsolete synthetic negative fixture failed [run `40828207495`](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40828207495); corrected test fixture in `25a7a88`. [Publisher `40828273443`](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40828273443) skipped GitHub/CurseForge publish steps. These are **server-only binary tests**: two genuinely connected installed clients, UI/screenshots, third-party providers, unloaded inventory aliases, arbitrary user worlds, sustained performance, and the other 23 exact-version RC acceptances remain unqualified. All **24 RC publication flags remain false**, no new GitHub RC or CurseForge upload. [Machine-readable native/NBT receipt](docs/acceptance/rc-dev-1211-installed-scheduled-network-tick-2026-10-29.json). Next implement real installed 2-client upgrade and owner/permission routing acceptance.

## October 28, 2026 PT — installed binary item transfers + native saved NBT PASS; RC clearance still 0/24

[Exact source 60afabd4](https://github.com/Shnewbs/Foundations-PL4/commit/60afabd44c404e93c3a98e67c20778a551390b85) passed [native installed-binary workflow 40817349163](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40817349163) with **75/75** offline regression tests. The *real installed* PL4 TransferEngine moved **64 diamonds in the published beta** and **64 more after restoring the exact copied world under the previously 194/194-native-qualified development JAR**. Source/sink balance advanced 1728/0 → 1664/64 → 1600/128 with all 1,728 items conserved, zero escrow and persistent node UUIDs/owner. Independent raw Anvil NBT attested the actual persisted barrel counts and configured Inventory Reader/Data Cable parts. [Retained artifact 12872604397](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40817349163/artifacts/12872604397) SHA-256 `a9dad6ad8c969cc6590af497ad3af2b9120ace67bc623c19351bf2dabf515137`, 25/25 ZIP entries and both nested saved-world ZIPs CRC valid. The earlier [strict NBT check failure](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40816881432) was repaired to accept absent *zero/empty* escrow fields only for the immutable older beta; the candidate still must serialize its modern fields, with six new adversarial NBT regressions.

**Scope limitation:** Actual item delivery invokes the installed engine using *explicit endpoint refs*. This is **not** automatic cable/topology discovery, two installed multiplayer clients, an external installed provider, arbitrary-world acceptance or sustained performance. No production JAR was rebuilt; the older release and previously qualified development JAR remain pinned by their unchanged exact SHA-256. All **24 RC flags are false** and [publisher run 40817349194](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40817349194) skipped both GitHub release and CurseForge upload. Full machine-readable [receipt](docs/acceptance/rc-dev-1211-installed-beta-candidate-live-item-transfer-2026-10-29.json).

## October 28, 2026 PT — actual installed beta -> candidate configured multipart NBT persistence PASS, RC still blocked

The exact installed production JARs now persisted a real PL4 **Inventory Reader (east slot 5) and Data Cable (centre slot 6)** with the same owner and both original UUIDs through a clean native saved-world upgrade, instead of a misleading zero-part host. [Successful native workflow 40811316320](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40811316320), at source [dbba3ede](https://github.com/Shnewbs/Foundations-PL4/commit/dbba3ede46033fde1e3d598cffb356794b05a9d2), independently checked 25/25 artifact entries, both nested world ZIPs, exact seed-input replay and raw Anvil region NBT; **62/62** policy regressions passed. Seed world 19 files, candidate 22 files, 128 barrel diamonds per phase; artifact [12869549832](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40811316320/artifacts/12869549832), ZIP SHA-256 `fef32063287e88192efa89a3912b4caff900b464a6fd4f7d36f1b282302291b3`. Published beta JAR SHA `7fa14351e1bbf039846afba040e4b66ce16b98db64103f1b33fc8ce14df0c229`; previously native-qualified candidate JAR SHA `cee5787af35f95af092a65b4a049276619bf24547b342c60192674cb360f030c`, not freshly rebuilt. Both isolated installed server jobs [40811316287](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40811316287) passed. Initial synthetic test directory reuse and missing Java `Direction` import caused preserved failures and were fixed. This **does not** prove live item transfers, actual installed clients, third-party providers, arbitrary-world recovery, multiplayer auth or sustained performance. All 24 RC flags remain false; no new GitHub RC or CurseForge publication. [Complete exact-source evidence](docs/acceptance/rc-dev-1211-installed-configured-multipart-native-nbt-2026-10-29.json). Next create genuine two-client installed-JAR migration with real PL4 item transfers, then extend exact-port acceptance.

## October 28, 2026 PT — exact-source native candidate binary provenance and paired installed-server probes

[Candidate provenance code](tools/multiplayer-probe/exact_native_candidate.py) now handles the actual [retained previous 1.21.1 native artifact](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40785212711/artifacts/12859271452) without inventing an embedded `SOURCE_COMMIT.txt`. GitHub artifact metadata binds source `aa9b439b69106b6232ef2c58fdfa155889d0ba1f`, repository identity, artifact digest and run to the exact previously successful binary. Independently downloaded ZIP SHA-256 `357be6c367d5f3e7b0b8a0edde28e57c8be06d4f237d4d3a5f931208109026d7`, six CRC-valid entries, native log SHA-256 `2fd1e39d2f4e7d736f027b9397048f05f579a9470d910b1282495521825678c8` with **194/194 passed**, and 882-entry candidate production JAR SHA-256 `cee5787af35f95af092a65b4a049276619bf24547b342c60192674cb360f030c` were reverified. **This is a reinspection of a previous passing run, not a new native build.** The [new isolated workflow](.github/workflows/installed-beta-binary-smoke.yml) now runs separate published-beta and exact-candidate JAR server-only probes, with **28 newly authored policy tests**. Their CI/regression/native results were **not yet observed** at this checkpoint, and neither can clear multiplayer migration or an RC. All 24 exact-target RC clearances remain false; no new GitHub or CurseForge publication. [Full evidence](docs/acceptance/rc-dev-1211-isolated-installed-release-binary-server-2026-10-28.json).

## October 28, 2026 PT — standalone installed-beta JAR runtime gate implemented, native result pending

New [standalone installed binary probe](tools/multiplayer-probe/installed_binary_smoke.py), [13 adversarial policy cases](tools/multiplayer-probe/test_installed_binary_smoke.py) and [new push-triggered native workflow](.github/workflows/installed-beta-binary-smoke.yml) move beyond the prior download-only `v0.2b.0` JAR preflight. The separate probe builds **only its own test mod** rather than source-built Foundations PL4 classes, installs the pinned SHA-256 `7fa14351e1bbf039846afba040e4b66ce16b98db64103f1b33fc8ce14df0c229` released JAR in a dedicated NeoForge 1.21.1 mods folder, and fails closed unless actual native server startup provides a matching installed-mod receipt. It retains source/JAR/log hashes and stays loopback-only. **No new native execution, CI regression pass, run ID, server receipt, client migration or RC acceptance has been independently verified as of this checkpoint.** The successful 194/194 native GameTests and earlier two-client world migration remain *prior source-compiled* evidence, not proof of this new installed binary. The next task is to inspect first CI run and correct real observed failures, then execute the exact candidate production binary and build true installed-JAR two-client migration evidence. `0/24` publication clearances, no new GitHub RC or CurseForge upload. [Evidence](docs/acceptance/rc-dev-1211-isolated-installed-release-binary-server-2026-10-28.json).

## October 28, 2026 — final independent native and released-beta byte-acquisition receipt

[Source aa9b439b](https://github.com/Shnewbs/Foundations-PL4/commit/aa9b439b69106b6232ef2c58fdfa155889d0ba1f) passed [194/194 native GameTests](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40785212711) and [two-client source-compiled migration](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40785212768) with 17/17 migration/binary-adversarial regressions. Independently inspected retained artifacts: native ZIP `357be6c3…09026d7`, development production JAR `cee5787a…60f030c`; migration ZIP `e7bc12cf…3c5bec` has 37 CRC-valid entries, genuine downloaded `v0.2b.0` JAR SHA `7fa14351…df0c229`, four 960×720 client screenshots and exact 26-file seed world copy preserved into 30-file restored world. Two client identities and 17+17+94 item balance survived. **Release JAR was downloaded and SHA-verified, not installed or executed**: server/clients still compile original and candidate sources. Follow up with separate probe-only installed-release-JAR/production-candidate-JAR migration; this does not clear any RC. [Evidence](docs/acceptance/rc-dev-1211-published-beta-jar-byte-preflight-2026-10-28.json).

## October 28, 2026 — immutable released-beta binary acquisition before two-client migration

[Implementation aa9b439b](https://github.com/Shnewbs/Foundations-PL4/commit/aa9b439b69106b6232ef2c58fdfa155889d0ba1f) adds a fail-closed preflight for the **actual downloaded** v0.2b.0 production JAR: GitHub release 416116341, exact tag commit 7c7c07f, 1,244,008 bytes, SHA-256 `7fa14351e1bbf039846afba040e4b66ce16b98db64103f1b33fc8ce14df0c229`, ZIP integrity and mod/1.21.1 metadata. Nine new adversarial tests plus eight existing migration tests and the [binary preflight step in run 40785212768](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40785212768) passed. The source-compiled seed/restore runtime remained in progress at checkpoint; **download verification is not binary installation or execution**. Next construct a probe-only installed-JAR runtime to substantiate a true released-binary → exact candidate-binary saved-world upgrade. All 24 RC flags remain false, and no release/upload was made. [Evidence](docs/acceptance/rc-dev-1211-published-beta-jar-byte-preflight-2026-10-28.json).

## October 28, 2026 — 1.21.1 live wireless provider-authority/escrow fix native 194/194; current-source real-world upgrade dispatched

[Production source `347a497`](https://github.com/Shnewbs/Foundations-PL4/commit/347a497808877721d40d05ec4b5c860c2dfac8f1) now revalidates binding, loaded live host/part, owner, player access and adjacent target after external capability enumeration and before/after mutations, and **journals actual extracted items before sealing them when a provider revokes access during extraction**. Two native regressions extend main from 192 to **194 GameTests**. Initial [red verification 40769689387](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40769689387) was the stale static 30-test contract, fixed by [`18fbc616`](https://github.com/Shnewbs/Foundations-PL4/commit/18fbc6169847dbee284efa3bbb21271963b2864f). Corrected source [verification 40770322178](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40770322178) **passed 194/194**.

[Migration/evidence change `c967590`](https://github.com/Shnewbs/Foundations-PL4/commit/c967590970f8aacec042e1c4e2c12f5100e704c5) pins historical released-beta `v0.2b.0` to immutable commit `7c7c07f`, and checks the **actual triggering source `${{ github.sha }}`**, replacing the obsolete `1cb1ee0` candidate; offline regression passed. [Current-source native hourly 40770696373](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40770696373) **passed 194/194** on exact source `c967590970f8aacec042e1c4e2c12f5100e704c5` (only changed main target selected; 23 unchanged tracks deduplicated). [Artifact 12852707753](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40770696373/artifacts/12852707753) ZIP SHA-256 `7546c6a365ae0ad53f982dfa8978b1727f3656ccd8e1962499d147550d451f66`; production 1.21.1 JAR SHA-256 `cee5787af35f95af092a65b4a049276619bf24547b342c60192674cb360f030c`; [main verification 40770696465](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40770696465) passed. [Actual two-client released-beta world-upgrade 40770696422](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40770696422) is **in_progress**, with outcome and artifact inspection still pending. Scope is exact native and controller attestation; no installed optional provider, unloaded alias, current client captures, sustained performance or full arbitrary-world/multiplayer RC acceptance.

**All 24 exact-target RC publication flags remain false; no new GitHub RC or CurseForge upload.** [Publishing policy run 40770696444](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40770696444) skipped both release/upload paths. This documentation-only checkpoint follows the tested `c967590970f8aacec042e1c4e2c12f5100e704c5` source and does not inherit its native hash as a new exact-source qualification. [Full evidence](docs/acceptance/rc-dev-1211-provider-authority-and-current-world-upgrade-2026-10-28.json).

## October 28, 2026 — new 26.x loader-attested native generation and reproducible legacy builds verified

[Corrected main implementation `781395d`](https://github.com/Shnewbs/Foundations-PL4/commit/781395dc6493a71725da9299671af2cf1372757b) stops the hourly controller from deduplicating historical 26.1.2 and 26.3 artifacts lacking the newly mandatory `native_coverage` native-log/JAR attestation. Retained v2 artifacts keep the exact source SHA and new `-native-attestation-v2` generation suffix; other exact-version artifact names are unchanged. The first regression had a wrong test-only expectation for non-maintained 26.1/26.2 ([red 40765939819](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40765939819)); corrected **22/22** controller tests and [main verification](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40766042140) passed. [Pinned hourly native run 40766042082](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40766042082) **passed all five selected exact source targets** without force or relabeling.

[26.1.2 artifact 12849783549](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40766042082/artifacts/12849783549): source `d6a6948`, **192 PL4 native plus 1 other native test**, independent native log SHA and complete packaged production checksum `847bd064c999fb220f926441a6de547ec6bf2a277d4fa2e1df93845ae0cbacf7`; artifact ZIP SHA `492ccbd00ce1dd3d74615b99c1d4c623e1b6f7b766178a3ee7526451b65f78a8`. [26.3 artifact 12850926212](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40766042082/artifacts/12850926212): source `cc5e0cf`, **192 PL4 plus 1 other test independently on both 26.3.0.23-beta and 26.3.0.39-beta profiles**, two separate verified JVM log SHAs and complete loader-staged JAR manifests; production JAR SHA `12c78fc5dae35299b46243339bc097e30eeb93d2f96895f2991817689217e591`; outer ZIP SHA `91b2ad085dde6ed838030994d312a3908bfa5c7c89ba4484228ba5c3b63336d8`. These JAR bytes match historical same-source payloads, but **the newly required per-profile independent attestation is fresh**.

[1.12.2 artifact 12850851577](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40766042082/artifacts/12850851577) source `d431ca4`: **399/399** native and restart/historical, full production JAR SHA `e4b9e5441ebd9cb05df42f34c4949828978575647467f1ee67ba154e270a02bb`, identical byte-for-byte with independent native [branch build 40762858757](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40762858757); 142/142 JAR entry timestamps normalized to 1980-01-01, 44/44 restart log SHA checks from unique process IDs. [1.6.4 artifact 12849264728](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40766042082/artifacts/12849264728) source `f5c79e8`: **372/372** native, SRG JAR SHA `1656e86eb72db1c3758eda3ad16a92c734be0248cab313634ec66573bfb3dee7` identical to earlier native branch validation, 44/44 unique restart JVM log hashes. [1.21.1 artifact 12850910577](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40766042082/artifacts/12850910577) source `781395d`: **192/192** native. All retained archives passed ZIP CRC checks; two prior 26.x archives lacked native_coverage. Legacy branch generic publishers remain frozen at `jobs.release.if: false`, and active branch-native publishers retain RC clearance, immutable receipt and fail-closed API checks. [Main policy workflow 40766042029](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40766042029) skipped both releases/upload steps.

**No RC/public CurseForge qualification from these scoped tests: all 24 exact-target `rc_publication_cleared` values remain false and no new GitHub or CurseForge release was created.** Installed optional provider, disconnected aliases, current client capture, multiplayer, arbitrary world migration, sustained performance and the spontaneous 1.6.4 x=80 gravel cause remain open. [Full exact-source evidence](docs/acceptance/rc-dev-26x-native-attested-hourly-2026-10-28.json).

## October 28, 2026 — obsolete generic legacy publishers frozen and 1.12.2 post-reobf reproducibility fixed pending native

Both existing legacy branches now disable the obsolete generic tag-triggered release.yml job at the job level, while keeping their validated exact-target native publishers and RC clearance/HTTP404/checksum guards. The [1.12.2 initial test failure](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40760919935) was an overescaped regression assertion, repaired in [05960aa0](https://github.com/Shnewbs/Foundations-PL4/commit/05960aa08f7528f8e5f76b99ee3123b2b810fa15), with its policy step passing in [40761009320](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40761009320) (full native pending). [1.6.4 f5c79e8](https://github.com/Shnewbs/Foundations-PL4/commit/f5c79e8e75f9808f7fff0a5b9d6ca4a9ba4786d9) passed 372/372 native plus restart/historical in [40761058763](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40761058763), with exact production SRG SHA256 1656e86eb72db1c3758eda3ad16a92c734be0248cab313634ec66573bfb3dee7 and [artifact12849095502](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40761058763/artifacts/12849095502) CRC-valid SHA256 b502059287e0168d487b540e141b73e6f7567c2b42dc7d7acdd710e6b84168a8. Development publishers were skipped.

Independent archive inspection of two **same-source** 1.12.2 399-case passes [40752206494](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40752206494) and [40754013917](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40754013917) showed unequal complete runtime hashes 39edfc... and c495f5... even though all 142 ordered entry payloads, CRCs and compression metadata matched; every one of the 142 timestamps differed. The native ForgeGradle reobf stage overwrote Gradle reproducible timestamp settings. A local Java ZIP-normalization proof preserved all 142 contents and produced byte-identical normalized JARs with SHA256 020e2cd751f4d4fb5e0820e95e0a2ab15222b01c8276988a91bd68234685d4f4. Source [6b9acb5](https://github.com/Shnewbs/Foundations-PL4/commit/6b9acb5b8d4f2b0cdb48feedfae78fba02143c2c) adds post-reobfuscation ZIP timestamp normalization and a native timestamp gate; [exact-source validation40761899439](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40761899439) is pending. This local probe digest is **not** transferable to CI-produced JARs. Main latest scheduled [40757651587](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40757651587) passed 192/192. All 24 clearance flags remain false; no GitHub RC or CurseForge upload. Installed providers, unloaded aliases, spontaneous x80 gravel root cause, client visuals, multiplayer, unknown save migration, sustained performance and 26.3 current-loader attestation remain pending. Full [evidence](docs/acceptance/rc-dev-frozen-legacy-generic-publishers-and-reproducibility-2026-10-28.json).

## October 28, 2026 — legacy fail-closed publisher guarded; 1.12.2 passes, 1.6.4 installer download fails

New push-triggered legacy publisher policies on [1.12.2 `af1feb4`](https://github.com/Shnewbs/Foundations-PL4/commit/af1feb4a3f10a05f701db84f84eb82f36d044de3) and [1.6.4 `5fa3ecf`](https://github.com/Shnewbs/Foundations-PL4/commit/5fa3ecf1174c93a0530eaf09e67a7882e7674c0d) require authenticated repo HTTP 200 and exact GitHub release HTTP 404, rejecting failed tag lookups, existing releases and network/rate-limit/API errors. Both **6/6** GitHub policy test steps passed; development publishers skipped. [1.12.2 native run40752206494](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40752206494) completed **399/399** ordinary native plus restart and historical migration. Exact [artifact12843643836](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40752206494/artifacts/12843643836) archive SHA-256 `a51debad6aab5b411503b47d1d0c4f5bdfb78e96bcf4efc1fd24a326c050beca`, runtime JAR SHA-256 `39edfc082d17eaf63ac636b5e1b3202134321b506dab5d67225004cba2a8e8ce` (all 142 entries byte-equivalent to predecessor, but timestamps differ). [1.6.4 run40752183303](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40752183303) built source successfully but failed before native tests on Forge Maven `curl (56) connection reset`; retained [failure artifact12845260858](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40752183303/artifacts/12845260858) has production SRG SHA256 `1656e86eb72db1c3758eda3ad16a92c734be0248cab313634ec66573bfb3dee7`. No new 1.6.4 native qualification. Main [controller source `f93fd79`](https://github.com/Shnewbs/Foundations-PL4/commit/f93fd79f887d4de245aeb02e7888f8903001aeea) adds reviewed exact-source three-hour external reset deferral to **22:15:14 UTC** and **21/21** locally passing controller regressions; [pinned hourly validation40754013917](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40754013917) is in progress at this checkpoint. Previously retained NeoForge 26.3 two-loader source artifact has independently inspected checksum/logs, but is older than current attestation rules. **RC publication clearances remain 0/24; no new GitHub/CurseForge release.** [Evidence](docs/acceptance/rc-dev-legacy-release-http404-integrity-2026-10-28.json).

## October 28, 2026 — legacy publisher fail-closed HTTP 404 and independently verified runtime bytes

Push-triggered `mc/1.6.4` and `mc/1.12.2` native publishers had a concrete ambiguous API failure path: an unsuccessful `gh release view` or remote tag query could be treated as no prior release. [1.6.4 fix `5fa3ecf`](https://github.com/Shnewbs/Foundations-PL4/commit/5fa3ecf1174c93a0530eaf09e67a7882e7674c0d) and [1.12.2 fix `af1feb4`](https://github.com/Shnewbs/Foundations-PL4/commit/af1feb4a3f10a05f701db84f84eb82f36d044de3) now require authenticated repository HTTP 200 and exact release HTTP 404 (all 401/403/429/5xx, timeouts and tag lookup errors block), with 6/6 offline adversarial tests. Native branch runs [1.6.4 `40752183303`](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40752183303) and [1.12.2 `40752206494`](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40752206494) have passed their newly added regression step; full native/restart/migration still **in progress** at this receipt.

Independent ZIP/JAR inspection of previously successful source commits recovered Forge **1.6.4** production SRG JAR SHA-256 `1656e86eb72db1c3758eda3ad16a92c734be0248cab313634ec66573bfb3dee7` ([372/372 artifact](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40743305276/artifacts/12840088380)) and **1.12.2** production JAR SHA-256 `e1344967254fa2e9aba9ba533aaeed5e083a9fe1bfb371d68ec857fdfb022800` ([399/399 artifact](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40743560785/artifacts/12841370997)). Outer ZIP digests/CRC and production JAR CRC, metadata and source blob identities were checked; these exact-source results do **not** qualify newer commits automatically. Publication remains development, **0/24 RC clearances**, no new release or upload. [Full receipt](docs/acceptance/rc-dev-legacy-release-http404-integrity-2026-10-28.json).

## October 28, 2026 — exact-source legacy loaded-alias native validation passes

Both corrected legacy branches completed their exact-source native workflows: [Forge 1.6.4 run 40743305276](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40743305276) **372/372**, and [Forge 1.12.2 run 40743560785](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40743560785) **399/399**. These successful workflows include native server scenario and restart/pre-upgrade validation with publication explicitly skipped. Their retained artifact IDs are `12840088380` and `12841370997`, respectively. The first 1.6.4 371/372 regression failure remains preserved as a separate receipt and was corrected with the narrower owner-scoped check.

Main 1.21.1 production-side publisher immutability and controller checks passed **20/20**, with **192/192** GameTests. Unchanged 1.12.2 source also recovered from its external Forge installer timeout and passed **397/397** on the pinned hourly run, distinct from the updated **399/399** branch source. See [per-source SHA and limitations](docs/acceptance/rc-dev-legacy-loaded-foreign-alias-native-passes-2026-10-28.json). Full client, installed provider, multiplayer, arbitrary-world migration, sustained performance, and the historical natural gravel-failure cause remain open; **0/24** exact RC clearances and **no new GitHub/CurseForge release**.

## October 28, 2026 — legacy loaded foreign-owner alias parity and regression

The original [1.6.4 loaded-alias candidate `37c02c4`](https://github.com/Shnewbs/Foundations-PL4/commit/37c02c4ff742b4f7fe529d398b328dac6c04985a) passed **371/372** native scenarios but [failed one existing independent-source conservation test](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40742486566). The overbroad alias check incorrectly treated a same-owner driver's unrelated destination fault as source contamination. [Correction `97ccb4f`](https://github.com/Shnewbs/Foundations-PL4/commit/97ccb4f24611ef018a91dbd3dace159cf6fedf9b) now scopes the loaded-alias veto to other owners. Its new [372-scenario native run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40743305276) was **in progress** at the checkpoint and is not qualified yet.

On existing `mc/1.12.2`, [`4de703f`](https://github.com/Shnewbs/Foundations-PL4/commit/4de703f105189d9eebd778c611ea8d68f25be8bc) implements the corresponding already-loaded foreign-owner alias guard for native capability-backed inventories, with two adversarial cases (399 declared). [New native run 40743560785](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40743560785) was in progress. Separately, the automatic [hourly run 40742077744](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40742077744) retried the unchanged prior `1.12.2` source after its external installer timeout and **passed 397/397**, smoke and preview.3 migration, preserving source/JAR identity and new artifact `12839714684`. This prior pass does not qualify the changed 399-case candidate.

The owner-scoped network and target journal protections remain; no force-loading, remote/unseen alias claim or new acceptance clearance. Both versions remain development, **0/24 RC clearances**, and no GitHub/CurseForge RC was created. Exact [evidence](docs/acceptance/rc-dev-legacy-foreign-owner-alias-followup-2026-10-28.json).

## October 28, 2026 — immutable main publication and loaded foreign-owner alias guard

[Main commit `7f7876c`](https://github.com/Shnewbs/Foundations-PL4/commit/7f7876c987765a47c38d4950a9b4569401504cb6) closes an unsafe existing-release mutation path: the 1.21.1 publication job now refuses an existing or unverified GitHub release before tag creation and no longer uploads with `--clobber` or edits prior releases. [Policy run 40742077972](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40742077972) passed **20/20** controller/publisher regressions, with both release and CurseForge steps skipped. [Hourly run 40742077744](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40742077744) verified **192/192** main native GameTests and exact-source production JAR SHA-256 `4325adacd93add2a26ef71435f47fe8d1e1e2722cd5832523c79c3929cf8955b`; 1.12.2's separate retried download/native job was still running.

[Forge 1.6.4 source `37c02c4`](https://github.com/Shnewbs/Foundations-PL4/commit/37c02c4ff742b4f7fe529d398b328dac6c04985a) adds a loaded physical-target neighbor check for a different-owner alias that has unresolved escrow, preventing a healthy handheld face from hiding the same physical chest. Two new native cases make **372 declared**; [branch run 40742486566](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40742486566) was **in progress** at this checkpoint, so no new 372-case qualification or changed-source JAR digest is claimed. No world scan or force-loading; unloaded aliases, installed providers, client, multiplayer, arbitrary save migration, sustained performance and natural 1.6.4 gravel failure still require acceptance. All 24 RC flags remain false, with no new release or CurseForge upload. See [exact evidence](docs/acceptance/rc-dev-publisher-immutability-loaded-alias-2026-10-28.json).

## October 28, 2026 — Exact 26.x native loader-profile JAR/log integrity

[Main implementation `e66ce662`](https://github.com/Shnewbs/Foundations-PL4/commit/e66ce6626d892c13027b53aa4f83195c939038d6) now independently checks 26.x claimed native environment counts and completed GameTest markers against retained native JVM logs and their SHA-256. Each NeoForge 26.3.0.23-beta and 26.3.0.39-beta profile's JAR copies gain their own complete SHA-256 manifest; the recorder checks both copied binary sets and the final .39 profile against the final packaged JARs. **18/18** scheduled controller regressions passed. This repairs build *evidence integrity*, not runtime feature acceptance.

The [exact-main pinned hourly action 40732504834](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40732504834) passed **192/192 Minecraft 1.21.1 native GameTests** at this implementation's immutable main source and retained [artifact 12835801828](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40732504834/artifacts/12835801828), GitHub-reported ZIP SHA-256 `1bbe5bc8a6abf18f108ecbeaccf401270d1d0acd14424a21ae52562b5a381cf6`, production JAR SHA-256 `4325adacd93add2a26ef71435f47fe8d1e1e2722cd5832523c79c3929cf8955b`. [Main verifier 40732504838](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40732504838) passed; [publisher 40732504833](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40732504833) passed with release and CurseForge steps **skipped**. Earlier corrected main action 40724186230 also finished successfully (192/192, artifact12832965494). The last retained historical 26.3 native profile pass is run40205878290; these tests do **not** retroactively qualify that artifact under the new JAR attestations. No 26.3 exact-source native rebuild was triggered in this run, and current binary checksums are not newly certified for its old source.

External Forge 1.12.2 installer timeout is deferred until **17:25:03 UTC**, natural 1.6.4 x80 gravel root cause remains open; installed providers, unloaded aliases, stock-client visuals, multiplayer, arbitrary-world migrations, sustained performance and parity remain separate acceptance. **All 24 RC flags false; no new GitHub RC or CurseForge upload.** [Full acceptance receipt](docs/acceptance/rc-dev-26-loader-profile-native-integrity-2026-10-28.json).

## October 28, 2026 — Forge 1.6.4 native placement/independent JVM log binding

[Exact source `84d4d035`](https://github.com/Shnewbs/Foundations-PL4/commit/84d4d035a8bf0ae7436217f7cad94da092d47b65) validates **all three actual native placements** x64, x80 and x200 (journal barrier), original air, chunk identity, metadata zero, native tile validity, and identical ordered native JVM log vs saved phase JSON. The first [candidate `cea23cd8`](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40702626890) failed its new Python test because it assumed only two placements; corrected [native run 40703299815](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40703299815) passed **370/370 real cases**, **18/18 controller tests**, decode **1536 assertions/40 JVMs**, provider **188/4** and historic migration **60/3**. Independent inspection of [retained artifact 12823242470](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40703299815/artifacts/12823242470) checked 60 native placement log/report rows, 80 terrain observations, 80 gravity guards, all 193 ZIP entries/CRC and SHA256 `25bcf5c53dc700a41b38ddf49973fe9d9f8789475533cf411d6f9217cedef2ab`. Production Java7 SRG JAR unchanged, SHA256 `598b26f2614319f40c4bba54381fd17fb732aa179fa449547c3a78aaa8e0f6b2`. This is **native-only development evidence**, not resolution of the historical spontaneous x80 gravel failure or installed-provider, unloaded alias, client/multiplayer, arbitrary-save or sustained-load acceptance. **All 24 RC clearances remain false; no new GitHub/CurseForge release.** [Evidence](docs/acceptance/rc-dev-164-placement-log-binding-2026-10-28.json).

## October 28, 2026 — Corrected 1.6.4 handheld root-callback candidate validates natively

[Source correction `a527fbe3`](https://github.com/Shnewbs/Foundations-PL4/commit/a527fbe342bfcf8010a87d5563cd7c9e79fc25ff) repaired the two native test subclasses that caused the first source candidate `205fdecc` to [fail compilation](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40694385199); the original production authorization fix remains unchanged. The [corrected exact-source native run 40695140590](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40695140590) **passed 370/370** native scenarios, 1,536 decode assertions in 40 distinct JVMs, 188 provider assertions in four JVMs, 60 historical migration assertions in three JVMs, 80 native terrain snapshots and 44 separately verified restart log hashes. I verified all 193 [artifact ZIP](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40695140590/artifacts/12819367391) entries with CRC; SHA-256 `f66ddd7c19aa4da7347fb910a918c8f3f28ad3ef0872a8d27842f48148db1433`. Exact Forge 1.6.4 SRG production JAR SHA-256: `598b26f2614319f40c4bba54381fd17fb732aa179fa449547c3a78aaa8e0f6b2`. This grants **native-only scoped qualification, not an RC publication clearance**. The original x80 gravel cause, installed third-party provider binaries, current client visuals, multiplayer, arbitrary worlds and sustained full-server performance remain open. The publisher skipped; existing CurseForge reconciliation found 24 prior submission receipts and **zero new uploads**. **RC 0/24**. [Evidence](docs/acceptance/rc-dev-164-bound-root-callback-2026-10-28.json).

## October 28, 2026 — Bound-root handheld callback continuity candidate (development)

[Exact Forge 1.6.4 source `205fdecc`](https://github.com/Shnewbs/Foundations-PL4/commit/205fdeccdbbe1d42cd1975fdae038ee5a7ca4adb) hardens the native handheld path against a downstream inventory callback revoking the original controller. Inventory enumeration now discards all observed rows if the tool/root binding changes; player-side extract/insert checks bound-root identity before and after mutations. Two new adversarial native cases exercise revocation during downstream inventory read and metadata discovery. The ordinary native case declaration is **370**, up from previous **368**. [Exact-source run 40694385199](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40694385199) was **completed** (failure) at this checkpoint; **no new native acceptance or runtime JAR checksum is claimed**. The previously qualified 368-case evidence remains scoped to `a861764`.

1.12.2 and the 22 modern ports are unchanged. Branch publication guards were inspected first; `BUILD_STATUS.json` is still development/uncleared, and there is no eligible GitHub RC or CurseForge beta. **All 24 RC clearances remain false.** Next: reconcile this source's native processes, independently verify retained exact-source artifacts, continue spontaneous x80 gravel reproduction and installed-provider/unloaded-alias, client, multiplayer and sustained-performance gates. Evidence: [bound-root callback candidate](docs/acceptance/rc-dev-164-bound-root-callback-2026-10-28.json).

## October 28, 2026 — Native 1.6.4 terrain evidence integrity and corrected source acceptance

Forge 1.6.4 [exact source `2c902d6`](https://github.com/Shnewbs/Foundations-PL4/commit/2c902d64d82925ecccda5207f0cf7db50acc8e65) passed the [native restart and migration run 40677792469](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40677792469), retained [artifact 12811418460](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40677792469/artifacts/12811418460). All **80 actual pre-cleanup observations** from 20 write/crash-write processes across x64,69,80,85 (chunk x4/x5) report original air and zero ambient gravel/sand or falling actors. That passing sample does **not** resolve historical intermittent x80 block13 substitution.

I added a native JVM raw stdout-vs-retained JSON log-binding guard and three adversarial Python regression tests on the existing `mc/1.6.4` branch. Initial [source `3bd24c4`](https://github.com/Shnewbs/Foundations-PL4/commit/3bd24c451c97bcfb8424590fafa5cd37528f711d) passed ordinary native cases but failed its new Python controller due to incorrectly escaped newlines; the regression was fixed in [source `a861764`](https://github.com/Shnewbs/Foundations-PL4/commit/a861764f13250467cc5aafd80ba256dc3ba813cb). Its [exact-source run 40685571384](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40685571384) now **passes** 368/368 native, 16/16 controller tests, 1536 decode assertions/40 JVMs, 188 registered-provider/four JVMs, and 60 historical fault-import checks. I independently checked 193 artifact ZIP entries/CRC, all 40 decode phase hashes, 20/20 native stdout/phase terrain rows (80 unique observations), and the Forge Java 7 production SRG JAR hash `8c01867a248fb1ac4b980a608b62486e50ed68900c8dab17c6e70d06e3b7e472`. [Artifact 12815905181](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40685571384/artifacts/12815905181), ZIP SHA256 `76db088988e16b1d1dbd24b69b304f615b5869710026d80327f19b9ef69f3a63`. New validation is controller/fixture-only; production runtime remains byte-identical to the prior candidate. Publication job correctly skipped development.

**All 24 RC clearance flags remain false; no new GitHub RC or CurseForge upload.** Existing source/artifacts preserved. Next: native naturally occurring x80 gravity/tick/chunk-boundary reproduction, unloaded aliases and installed optional providers, current client, multiplayer, migrations and sustained performance. Full evidence: [current ledger](docs/acceptance/rc-dev-164-terrain-log-binding-2026-10-28.json).

## October 28, 2026 — exact legacy automatic provider invalidation (development only)

Two production safety repairs were pushed to the **existing** exact-version branches, preserving all modern ports. [Forge 1.12.2 source `aa23bef`](https://github.com/Shnewbs/Foundations-PL4/commit/aa23bef3398930aae28179594aea81d1f56df9fa) now rejects a native capability handler if `hasCapability/getCapability` changes the installed loaded host/target, owner, side or world during automatic item transport. Its distinct native regression passed **395/395** in [run 40654775646](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40654775646); production runtime SHA-256 `fa5bfe811862835d0a8d3f3e773f35a8e18912b63d3a980ae18fb397523b8505`, retained [artifact 12801636439](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40654775646/artifacts/12801636439). [Forge 1.6.4 source `56a302d`](https://github.com/Shnewbs/Foundations-PL4/commit/56a302d9182c0863c569d5f12393429ce59b6d48) now rejects an `IInventory` wrapper constructed after callback-driven tile replacement, with a specific automatic-route test passing **366/366** in [run 40655791384](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40655791384); runtime SRG SHA-256 `051751257a7688921e73a0bec21ac1332fd3055964fd6669eac4ee8bedd3e8c8`, retained [artifact 12801713423](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40655791384/artifacts/12801713423). Both exact native workflows pass clean/post-save restarts, prior fault-world import, production JAR/ZIP and Git source-blob checks; both branch publisher steps skip development.

These tests are scoped to built-in native providers, not installed third-party mods, fresh client screenshots, online multiplayer, arbitrary user-world migration or sustained performance. The 1.6.4 x80 gravel-related placement failure remains causally unexplained. **RC clearance remains 0/24; no new GitHub RC or CurseForge upload.** The pre-existing modern target evidence, beta publishing guards and immutable source/release identities remain intact. Evidence: `docs/acceptance/rc-dev-legacy-automatic-provider-invalidation-2026-10-28.json`. Next: independent pinned hourly reconciliation, callback-time mutation safety, alias/unload and actual-provider/multiplayer/client/performance gates before exact-target RC flags change.

## October 28, 2026 — 1.12.2 bound-root provider revocation acceptance (development only)

The exact [393-case 1.12.2 candidate](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40642756603) passed on Forge 14.23.5.2864 / Java 8, including 40 decode restart JVMs, pre-upgrade fault-world import, production-only boot and preview.3 migration. Its immutable runtime SHA-256 is `555965d25f130ce75df6e3a7745a020edaf4df355cf22604f8e2a4d6cf96acb5` (artifact 12796576801).

A new native provider test changes the *bound controller's owner* from a different inventory's `getSlots()` callback, after endpoint discovery. `StorageInventory` now checks the original bound root/player/tool as well as the installed host/target on provider access, preventing a now-unauthorized destination from consuming player inventory. The initial `de1215b` candidate failed real Java compilation in [run 40648730978](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40648730978) due to an overlooked ungrouped withdrawal call. [Corrected branch source 117df59](https://github.com/Shnewbs/Foundations-PL4/commit/117df59bc0f54f05b3385688156e866bf2c575c9) passes the [independent pinned hourly run 40649176900](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40649176900): **394/394 unique native cases**, 1536 decode assertions/40 separate JVMs, pre-upgrade, production boot, migration and archive checks. [Artifact 12798718363](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40649176900/artifacts/12798718363) SHA-256 `4058ab06d2d91d59c8563f38e0c14828691f83c52b4ad8ec82f7d1eeab7198bd`; 155 archive entries pass CRC; actual runtime SHA-256 `2701c7a1ba4438a9b8b0c5f3b6d3d21eb746dadc43c01060d40b1e0361d7043a`. Its separate branch run 40648972109 was still active at this checkpoint; do not duplicate the already-qualified pinned source.

This only qualifies exact-source Linux hosted Forge-native, saved-restart and migration behavior, **not** installed optional providers, current stock-client visuals, online multiplayer/travel, arbitrary-world recovery or sustained provider performance. The 1.6.4 x80 falling-gravel anomaly remains unresolved. **All 24 RC publication clearances remain false; no new GitHub RC release or CurseForge upload.** Existing development drafts and user source are untouched. See [the exact work/evidence register](docs/acceptance/rc-dev-112-root-revocation-2026-10-28.json).

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

## Native terrain-before-cleanup boundary observations — October 28, 2026

The existing `mc/1.6.4` source at [2c902d64](https://github.com/Shnewbs/Foundations-PL4/commit/2c902d64d82925ecccda5207f0cf7db50acc8e65) now records the real, pre-cleanup native block, overhead falling-block count, falling-entity count, chunk presence and x/chunk identity for columns x64,69,80,85 before the rewrite suite injects its separate gravel actor. The restart controller fails closed on missing/duplicate/invalid terrain receipts; three additional offline regression cases cover valid observations, bad chunks and counter types. This does not generate synthetic evidence for naturally occurring actors. [Exact-source native validation run](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40677792469) was **in_progress** as of 2026-10-28T11:12:03.351Z; it is not yet an accepted new JAR. Prior [ef01e15 native 368/368](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40669706429) remains scoped to its original code and production SRG SHA `8c01867a248fb1ac4b980a608b62486e50ed68900c8dab17c6e70d06e3b7e472`. The intermittent original x80 gravel root cause is still unproven. Installed providers, unloaded aliases, dedicated multiplayer, arbitrary-world migration, client visuals and sustained performance remain open. All 24 release clearance flags remain false; **no new RC or CurseForge upload**. Evidence: `docs/acceptance/rc-dev-164-terrain-before-cleanup-2026-10-28.json`.

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

## 2026-10-28 — automatic provider callback continuity for both legacy ports

New development candidates `mc/1.12.2` [f0d75b5](https://github.com/Shnewbs/Foundations-PL4/commit/f0d75b55448a5aa80e6d9742847530f752c97d6d) and `mc/1.6.4` [b0a4f45](https://github.com/Shnewbs/Foundations-PL4/commit/b0a4f4537222770be934c3fbcbf4551e686a0e0b) pin source/sink provider adapters, original root/endpoint owner and route, loaded world and installed backing tile across **every native item slot, simulation and execution callback**. Each adds two adversarial native cases: simulated destination revocation must not debit the source; executed source revocation must preserve unresolved original-route journal evidence with no automatic retry. Declared ordinary native counts are **397** (previous 395) and **368** (previous 366). Branch native runs [40661731129](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40661731129) and [40661943594](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40661943594) were in_progress / in_progress at the 2026-10-28T09:12:04.470Z checkpoint, so **no new exact-source native/JAR/migration qualification or RC clearance is claimed**. The 1.6.4 x80 gravel actor remains unresolved. Publisher guards checked before branch pushes; versions are development-only and all 24 RC flags remain false. Full receipt: `docs/acceptance/rc-dev-legacy-automatic-callback-continuity-2026-10-28.json`.


## 2026-10-28 — callback-continuity native receipts and 1.6.4 gravel-actor acceptance probe

Forge 1.12.2 exact source [f0d75b5](https://github.com/Shnewbs/Foundations-PL4/commit/f0d75b55448a5aa80e6d9742847530f752c97d6d) passed **397/397** native cases in [40661731129](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40661731129). Its packaged production SHA-256 is `2d075e20842e8bb8680ddd3b855b88ae990d7f1c8b73c743eada6c9b1a5cd245`, retained [artifact 12803933722](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40661731129/artifacts/12803933722).

Forge 1.6.4 exact source [b0a4f45](https://github.com/Shnewbs/Foundations-PL4/commit/b0a4f4537222770be934c3fbcbf4551e686a0e0b) passed **368/368** native cases in [40661943594](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40661943594). Its production **SRG** SHA-256 is `8c01867a248fb1ac4b980a608b62486e50ed68900c8dab17c6e70d06e3b7e472`, retained [artifact 12804950866](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40661943594/artifacts/12804950866). Both exact-source artifacts passed independently inspected ZIP CRC, runtime hash, 1536 decode assertions/40 JVMs, 188 provider assertions/four JVMs, 60 historical assertions/three processes, and 20 saved decode worlds. Production-only boot and preview.3 migration steps passed; no new current-client, installed-provider, multiplayer, arbitrary-world, durability or sustained performance clearance follows.

To test the previously unexercised falling-entity cleanup under an **actual native actor**, [1.6.4 commit ef01e15](https://github.com/Shnewbs/Foundations-PL4/commit/ef01e150801e08b8d0e7aa95231f6b22acb8d5b6) injects a real gravel EntityFallingSand into only the disposable decode-rewrite x80 fixture. The native controller now requires the actor to be removed and counts at least one actual entity; ten local Python regression tests pass. [New exact-source action 40669706429](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40669706429) status at ledger checkpoint: **CI_IN_PROGRESS**. Do not transfer b0a4f45's qualifying runtime/JAR hash to ef01e15 until independently verified. Negative Python test logs that describe a native placement rejection are synthetic and **not** a new game occurrence. Historical intermittent gravel-placement cause remains unresolved.

All 24 `rc_publication_cleared` values remain false; no new GitHub RC or CurseForge upload was made. Next: resolve new native adversary receipt and JAR identity, then installed providers, aliases across loaded/unloaded boundaries, multiplayer and sustained performance. Full evidence: `docs/acceptance/rc-dev-legacy-gravel-actor-2026-10-28.json`.

**Native follow-up acceptance:** [Exact-source run 40669706429](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40669706429) finished successfully for [1.6.4 source ef01e15](https://github.com/Shnewbs/Foundations-PL4/commit/ef01e150801e08b8d0e7aa95231f6b22acb8d5b6), **368/368** ordinary native cases, **1536** decode assertions across **40** JVMs, **188** provider checks and **60** historical checks. The rewrite *write* and *crash-write* native logs each show `spawned=true removed=true`, exactly **one real falling-gravel actor removed** at x80; all four gravity guards were observed per write phase. [Retained artifact 12807334156](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40669706429/artifacts/12807334156) ZIP SHA-256 `a4f99e8b4f36a8080a96a69b6ed3c9a222c9695c0e47aace91d10999707ff378`; **193** entries, ZIP CRC good, all 44 process log hashes and 20 decode world archives reverified. Packaged production SRG SHA-256 remains byte-identical: `8c01867a248fb1ac4b980a608b62486e50ed68900c8dab17c6e70d06e3b7e472`. This proves one controlled actual actor cleanup path, **not** the original spontaneous x80 placement root cause or RC acceptance. All 24 clearances remain false; no release was created.


## 2026-10-28 — exact-source environmental build deferral

A pinned `1.12.2` Forge native matrix job failed before any scenarios because libraries.minecraft.net timed out downloading `log4j-core:2.15.0` (run [40705534234](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40705534234), failed artifact 12824385408). Main's hourly-build controller now defers only that reviewed source/branch for a maximum of three hours, with explicit expiry, force override, exact-source invalidation and negative regression tests. An upstream download failure is **not** a source failure or successful native artifact. The same matrix's 1.6.4 and 1.21.1 target jobs succeeded independently. Evidence: [`docs/acceptance/rc-dev-hourly-external-network-deferral-2026-10-28.json`](docs/acceptance/rc-dev-hourly-external-network-deferral-2026-10-28.json). Installed providers, client, multiplayer, migration and performance gates remain open. All 24 `rc_publication_cleared` flags are false; no RC or CurseForge upload from this work.


## 2026-10-28 — complete modern native-JAR manifest acceptance

The main hourly recorder now rejects empty, incomplete, additional or malformed `rc-development-gate.json` JAR hashes: every `build/libs/*.jar` must match exactly one 64-character native-run SHA-256. The initial [commit 0d0df67](https://github.com/Shnewbs/Foundations-PL4/commit/0d0df6745e495c27497def372c18ad9b7086d733) uncovered a real Windows manifest path separator failure in [CI 40723840668](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40723840668). [Commit 7799450](https://github.com/Shnewbs/Foundations-PL4/commit/77994504219253ea2b69b6e63b3a6ecb17705225) uses POSIX-normalized relative paths; [17/17 corrected Windows policy tests](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40724186346) pass. The earlier integrity-source pinned [1.21.1 run 40723840470](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40723840470) passed 192/192 and retained [artifact 12832421076](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40723840470/artifacts/12832421076), ZIP SHA-256 `73f28db769389df7789ea4ac90a72657ba5d136acd20c3379a84858a9026f5dc`, production JAR SHA-256 `4325adacd93add2a26ef71435f47fe8d1e1e2722cd5832523c79c3929cf8955b`. Final corrected-source [pinned build 40724186230](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40724186230) was still pending at checkpoint and is **not** yet called qualified. This protects exact-source build evidence, not installed optional providers, current clients, multiplayer, arbitrary save migration, sustained performance or RC clearance. The 1.12.2 upstream Forge installer deferral and original 1.6.4 natural x80 gravel failure remain open. All 24 RC clearances stay false; no new GitHub RC or CurseForge upload. Evidence: [`docs/acceptance/rc-dev-modern-native-artifact-manifest-2026-10-28.json`](docs/acceptance/rc-dev-modern-native-artifact-manifest-2026-10-28.json).


## October 28, 2026 — Retained released-beta world-byte and two-client upgrade evidence

[Implementation ec532e31](https://github.com/Shnewbs/Foundations-PL4/commit/ec532e31d9fd48a384efb0e4c9de83b47a0cf9f3) captures deterministic disposable saved-world ZIPs, source and log/checkpoint hashes, real-client screenshots and restore-copy fingerprints. Eight adversarial Python tests passed. [Migration 40777636720](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40777636720) completed with two real loopback clients, preserved controller identity and UUIDs, 17+17 player items and 94 remaining barrel items, with independent artifact re-verification. [Main verify 40777637169](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40777637169) passed 194/194 native GameTests. The seeded source is the immutable v0.2b.0 tag commit, **not the published JAR executed as an installed binary**. All 24 RC clearances remain false; no GitHub/CurseForge RC was created. Next: installed exact-release/production-JAR upgrade, optional providers, aliases, full multiplayer/client performance and natural 1.6.4 x80 gravel cause. [Full receipt](docs/acceptance/rc-dev-1211-retained-world-upgrade-integrity-2026-10-28.json).


## October 28, 2026 — installed 1.21.1 production JAR dedicated-server acceptance

The immutable published `v0.2b.0` JAR and the separate `aa9b439` native-qualified development production JAR have **each actually loaded and started** in an isolated NeoForge 1.21.1 dedicated server through the standalone probe, not PL4 source substitution. Previous run [40793491930](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40793491930) failed at shutdown because the Gradle wrapper did not relay console `stop`. [Implementation c763ea0](https://github.com/Shnewbs/Foundations-PL4/commit/c763ea0e576bf395f69f8223338ccba36c5e39a1) uses an ephemeral authenticated loopback RCON `stop`, enforces clean process exit and safely reaps its isolated process group on failure. [Run 40799159079](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40799159079) is **successful** in both independent installed-JAR jobs, with 16/16 server smoke plus 15/15 candidate provenance regression tests, source/JAR/ModList/ServerStartedEvent attestation and retained hashed logs. [Released beta evidence](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40799159079/artifacts/12864418744) and [candidate evidence](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40799159079/artifacts/12865117103) are source-scoped. Full machine-readable evidence: `docs/acceptance/rc-dev-1211-installed-jar-server-graceful-shutdown-2026-10-28.json`. **Not covered:** actual two-client installed-binary-to-installed-binary world upgrade, client screenshots, installed optional providers, multiplayer, arbitrary worlds or performance. All 24 RC flags remain false, no new release/upload.


### 2026-10-29 05:21 UTC — Installed native cable graph + raw Anvil identity acceptance (not RC clearance)

Exact current control source `2a615ba6b6e02818a6d29ba998b74141b8e9c248` passed [installed run 40822624071](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40822624071): released 1.21.1 beta `v0.2b.0` → pre-qualified `aa9b439` development JAR, seven real DATA_CABLE centre parts and two passive transfer endpoints each phase, actual native NetworkEngine membership, blocked-arm sever + reconnect, and all nine owner/UUID identities checked directly in saved Anvil region data across the world upgrade. 87/87 local policy regressions passed. [Artifact 12875096843](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40822624071/artifacts/12875096843) has GitHub ZIP SHA-256 `7b724f464691c8e790b61e0d7b66f017bc992dc888ee10144a7131682b686528` (25/25 entries CRC-valid; 19-file beta and 22-file candidate world archives CRC-valid). Beta and candidate server startup/loading were verified; main checkout native verify 40822624075 still executing at this checkpoint. Physical network topology is **not** proof that the game's scheduled automatic tick delivered items over this graph: earlier 64+64 real item transfers still used manually supplied endpoint Refs. No two genuine clients, installed optional providers, arbitrary-world migrations or sustained load qualified. All 24 RC clearance flags remain false. Legacy branch publishing untouched; no new RC release/CurseForge upload. Next: actual native tick-driven item transfer through discovered topology, then two-client acceptance.

## October 29, 2026 — Exact installed-production-JAR two-real-client migration (scoped PASS)

[Implementation and correction sequence](https://github.com/Shnewbs/Foundations-PL4/commit/3e315e4c48d117c5c90c9754e621161e83f73a39) adds an independent two-client native NeoForge 1.21.1 migration workflow. Production PL4 code comes **only** from the installed, immutable published `v0.2b.0` JAR and a separately native-qualified pinned development JAR. The source-only test module compiles its own probes in distinct Java packages, never packages or compiles production PL4 source, and pins exact JAR SHA-256 and source. The first run [40834122264](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40834122264) failed on a real NeoForge split-package module collision; a separate probe namespace fixed that. Two subsequent policy test failures were over-strict escaped-source assertions, not native game failures.

The [successful installed-native workflow 40834904056](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40834904056) executed **two actual graphical clients in each JAR phase**, live wireless storage requests, two independent player UUIDs, enforced operator/foreign permission checks and replay protection. The saved beta world was copied with exact byte-fingerprint before loading in the candidate; two client inventories each retained 17 diamonds, while the original barrel retained 94 (128 conserved), with all original multipart owner/node identities preserved. Four 960×720 client screenshots show the native Wireless Storage UI; Minecraft warning/toast overlays obscure part of the top-right, so a broader client-visual polish acceptance is still open. Independent retained [artifact 12880626553](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40834904056/artifacts/12880626553) SHA-256 `e15519e18281481149742932f77f293aa241ac7ade708677fbef45b0a8420e71`, 44/44 archive CRC and both nested world ZIPs CRC valid; direct raw playerdata and Anvil NBT confirmed 17+17 player items, 94 barrel diamonds and saved multipart UUID stability. The baseline source world has 26 files (`382dbbbf...`), copied unchanged to candidate before reopening; final world has 30 files (`922e9cf0...`). 82/82 offline regressions and separate [main native verification 40834903952](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40834903952) 194/194 GameTests passed. [Publisher 40834904089](https://github.com/Shnewbs/Foundations-PL4/actions/runs/40834904089) skipped GitHub/CurseForge publication. **0/24 RC clearances; no new release or CurseForge upload.** Scope remains offline loopback; native third-party providers, arbitrarily migrated worlds, sustained performance, integrated cable transfer under real clients, online authentication and the other 23 targets still require acceptance. [Full evidence](docs/acceptance/rc-dev-1211-installed-binary-two-real-client-migration-2026-10-29.json).

