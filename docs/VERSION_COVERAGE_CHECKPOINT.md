# Version coverage checkpoint - October 9, 2026

Scope: cover final Minecraft versions absent from the original Practical Logistics / Practical Logistics 2 project listings, rather than restricting PL4 to the initial popular-version shortlist. 1.18.2 is explicitly retained. 1.16.4 is independent from 1.16.5. User-approved 1.6.4 remains experimental; 1.7.10 remains excluded.

## Full inventory

The audit at 2026-10-09T22:29:59Z inventoried 85 numerical final Java Edition releases from 1.6.4 through the officially listed released targets. It excluded snapshots and future entries. Nine have a published PL4 runtime asset, six more are present in the original PL2 listings, and one (1.7.10) is excluded by the user. The other 69 are unbuilt PL4 coverage gaps, including 1.16.4.

Of the 69 unbuilt gaps, 43 have exact-version Forge artifacts listed in official Maven metadata. The other 26 need alternative-loader/toolchain feasibility work; absence from Forge metadata does not prove that no other loader exists. Branch presence and published-asset presence are recorded separately from native/runtime/API/visual/multiplayer/performance acceptance.

Evidence: https://github.com/Shnewbs/Foundations-PL4/actions/runs/37999529512 . The read-only audit workflow passed its nine exact-target inventory tests and retained the Mojang manifest, Forge metadata, paginated branch and release snapshots, their hashes, and VERSION_COVERAGE.json. Artifact SHA256: `058822076d8dd7dae3b903cfaa0a42e46454a1e10eafba1dbb7940394505e7c8`. Downloaded input hashes were verified independently. This snapshot counts the already-published legacy preview-3 assets but does not certify their acceptance merely by counting them.

## Explicit requested targets

**1.18.2:** published `mc1.18.2-v0.2a-port.2` remains available, with the previous 193-native-GameTest evidence. The runtime and source ZIP were rechecked against their published hashes. No new 1.18.2 source/runtime change or new installed-API acceptance is claimed in this scope pass.

- Runtime SHA256: `d5c6747e1731b7fd17e1f9f19ec31e981a3f1ae6d368eaa4af47f2c2385c00ac`.
- Source ZIP SHA256: `2800132c69a576aaef5cc7c5b6469dda40495eaa4694939ea5bdcc8f1b5eff01`.
- Release: https://github.com/Shnewbs/Foundations-PL4/releases/tag/mc1.18.2-v0.2a-port.2 .

**1.16.4:** created `mc/1.16.4` from the modern-feature 1.16.5 source, targeted exact Minecraft 1.16.4 / Forge 35.1.37 and a distinct protocol, and fixed the missing Forge-35 empty-list config helper. Native compilation, reobfuscation and packaging now pass. The original 191 server scenarios are retained, with two new config regression fixtures.

However, the native run at source `74969c5387e3d7ce1377ee09c169078ee2ab152b` fails before tests in Forge 35's mod scanner with `Record requires ASM8`. The explicit ModLauncher 8.1.3 / ASM 9.2 / Java 17 profile resolves the earlier launcher failure but cannot change Forge's visitor contract by itself. **Zero of the 193 scenarios executed. No 1.16.4 release is published.** Its branch BUILD_STATUS.json and docs/JAVA17_RUNTIME.md record the blocker and the remaining proper record backport/compatibility-conversion work. A successful compiler exit is not playable-port acceptance.

Latest diagnostic: https://github.com/Shnewbs/Foundations-PL4/actions/runs/37999800213 . Publication remains gated on working native and installed-runtime validation, with client, multiplayer and third-party API checks separate.

## Next uncovered anchor targets

1.8.9, 1.11.2, 1.13.2, 1.14.4, 1.15.2 and 1.17.1 remain priority gap implementations. Other final point releases are in the machine-readable audit rather than silently omitted. They are not completed builds yet.

All targets retain the requirement for appropriate optional-API alternatives or PL4-owned core fallbacks, with explicit feature-specific acceptance. No unavailable power API receives invented conversion ratios, no command/config fallback is described as a complete graphical/script replacement, and no missing target gets a renamed neighboring binary. **Further API testing is still required.**
