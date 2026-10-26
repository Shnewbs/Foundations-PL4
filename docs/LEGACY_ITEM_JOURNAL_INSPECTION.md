# Legacy item uncertainty inspection

Current implementation: exact Minecraft 1.6.4 and 1.12.2 development sources. 1.6.4 records normalized player names; 1.12.2 records account UUID strings.

- Use `/pl4legacy journal x y z [page]` within eight blocks of a loaded PL4 host you own, or inspect as an operator. It lists unresolved tokens at that host's current adjacent target, four per page, scoped to owner and dimension.
- Hold a sealed PL4 item receipt and use `/pl4legacy receipt`. Only the recorded owner or an operator may inspect a linked receipt. Historical unlinked receipts require operator inspection and do not reconstruct a target. Another dimension directs inspection there without querying a coincident current-world target.
- These public command entry points work without enabling cheats. The handlers retain ownership, operator and proximity checks; a public dispatcher is not permission to inspect someone else's host or receipt.
- Saved `ItemJournalContext` contains the active token, owner, dimension, target and recorded phase. Chat output only accepts `wireless`, `extract` or `insert`; missing/unrecognized phase values display `unknown`. This is reported context, not an independently verified provider outcome.
- Missing tokens, mismatched contexts and opaque saved data never grant recovery clearance. No release, replay, redemption or item minting command exists.

## Recovery evidence to collect

Preserve a complete world backup and the original receipt. Record the token/context, dimension, actual source/destination inventories, held escrow and observed provider outcome. Inspect without replaying the uncertain transfer. Keep fences until conservation and ownership are established by an explicit reviewed recovery procedure.

A removal receipt links the tile's active uncertain handoff. Other overlapping tokens at that target remain independent and are visible through target inspection. Read-only diagnostics do not resolve them. Generated-world and integrated-client tests do not certify arbitrary copied modpack worlds, unresolved pre-upgrade destination history, dedicated multiplayer or crash-atomic/full-process recovery.

## Process restart boundary accepted on October 26, 2026

The [exact-source process checkpoint](acceptance/rc-dev-legacy-item-process-restart-2026-10-26.json) launches four independent native JVMs per port: write/clean stop/read and write/completed native world plus journal save/flush/parent SIGKILL/read. Both scenarios preserve two owner-scoped tokens, linked sealed receipts and observed vanilla inventory contents after removing the original host. A replacement host remains fenced. The deliberately unregistered fault injector is replaced by a vanilla chest containing its observed contents before saving; this does not certify a third-party provider's private persistence.

An insertion callback accepting eight then throwing leaves 32 source + 8 destination items and an **uncertain sealed eight-item claim**. That claim is not eight additional spendable items. An extraction callback removing eight then throwing leaves 32 observed source items and no destination items; eight are unaccounted for, the source remains fenced, and nothing is fabricated. Neither case grants automatic replay, redemption or fence clearing.

This proves clean restart and process termination only **after the completed save boundary**. Arbitrary pre-save crashes, power loss, filesystem atomicity, external provider durability, pre-upgrade faults, copied user worlds and reviewed recovery remain open. Ordinary native totals are 312 and 290; the additional 124 lifecycle assertions per port are recorded separately.

## Historical faults with unknown routes — October 26, 2026

The [pre-upgrade import checkpoint](acceptance/rc-dev-legacy-preupgrade-fault-import-2026-10-26.json) imports an observed loaded tile that is already faulted but has no trustworthy matching operation context. It records `Scope: legacy-unknown` with the observed host position. That position is **not** the original inventory target. Original target and phase stay unknown; malformed original context/evidence remains preserved.

The imported record fences that observed owner's item transfers throughout the current dimension because the historical mutation target cannot be reconstructed. An unowned fault fences all owners in that dimension. This is visible in target/receipt inspection. A valid existing target-linked record keeps its original narrower scope. No loaded-world scan, foreign provider callback, replay, redemption, minting or fence clearing is performed. Faults in unloaded chunks have not yet been observed or imported.

Removing the imported host preserves the world barrier and drops a sealed linked receipt. A pre-existing loose unlinked receipt has no trustworthy owner/target and stays unauthenticated; operator inspection cannot manufacture its missing history. Preserve the complete save, original receipts and observed source/destination contents for reviewed recovery.

The journal now writes schema 2 and reads schema 1. Capacity exhaustion retains the previous journal and unlocated fault together as opaque data, which stays fenced after reload. Older journal-aware schema-1 readers reject schema 2 and fail closed; **pre-journal JARs do not enforce this index**. Do not open an upgraded save in an older JAR. Revert only using a complete pre-upgrade backup; no safe downgrade or arbitrary modpack migration is certified.

The generated migration test uses a verified pre-journal JAR to save faulted tiles, a fresh candidate JVM to import/remove the hosts and seal receipts, and another fresh candidate JVM to reload those receipts and barriers. Those 24 checks per port are separate from the ordinary native counts and the existing 124 current-runtime restart assertions. No generic recovery operation is exposed.

## Nested context fields

The [scalar-type checkpoint](acceptance/rc-dev-legacy-context-scalar-types-2026-10-26.json) validates child tag types as well as the context compound itself. Tokens, owners, scopes and phases are read only as native string tags; dimension and captured coordinates require native integer tags. The tested initially unlinked malformed contexts retain their raw payload and unknown-route barrier. A malformed receipt owner/dimension cannot authorize a player by coercion: it requires operator inspection and grants no recovery clearance. Bad phase tags display unknown. These checks do not add a recovery or fence-clearing command.

## Registered provider private-state restart fixtures — October 26, 2026

The [provider-persistence checkpoint](acceptance/rc-dev-legacy-provider-persistence-2026-10-26.json) keeps a registered adversarial inventory provider installed across all four native JVMs on each legacy port. Its own saved inventory, private UUID, ownership label, armed failure mode, executed mutation count and removed/accepted quantities reload together. A replacement host stays fenced and never invokes it again. Each port passes 188 separate lifecycle assertions (52 write + 42 verify for clean stop, repeated at the completed-save SIGKILL boundary); these are additional to ordinary native counts 341/319. Production-only startup, generated preview.3 migration and the 24 historical import checks also pass.

Independent inspection of both saved world archives per port checks the actual registered tile IDs, provider-private tags and inventory contents against the native reports. Reproduce it with `python3 tools/inspect_legacy_provider_world.py <item-restart-clean-world.zip> <item-restart-verify.json>` or the corresponding barrier/crash-verify files from the retained native artifact.

This fixture preserves 32 source + 8 destination spendable units after uncertain insertion, alongside a sealed 8-unit claim that cannot be counted again. Uncertain extraction leaves 32 source + 0 destination, with 8 units unaccounted for. No recovery operation or substitute inventory is created. The accepted boundary is this registered fixture at the completed-save boundary; installed optional providers, provider access-control enforcement, historical unknown-route provider-private outcomes, arbitrary crashes, copied worlds, dedicated multiplayer and reviewed recovery remain open.

## Historical provider-private outcomes — October 26, 2026

The [historical provider checkpoint](acceptance/rc-dev-legacy-historical-provider-outcomes-2026-10-26.json) runs actual mutating callbacks under the pinned pre-journal native JAR. Registered fixture providers remain installed while a candidate JVM imports the faulted hosts and removes them, then another candidate JVM reloads the sealed receipts and owner-wide barriers. Each port passes 60 checks across these three processes (16 old-write, 22 import, 22 restart), in addition to 188 current-runtime restart assertions. Direct region-NBT inspection confirms the two providers' private IDs, ownership labels, counters and inventory counts in each retained historical world.

The fixture observations do not become production route history. Imported receipts still have no original target or phase, and old loose receipts remain unauthenticated. Extraction leaves eight units unaccounted for; the sealed insertion claim is not extra spendable inventory. This closes only registered fixture private-state persistence through generated clean migration. Installed optional providers, provider access-control enforcement, arbitrary copied worlds, crash atomicity and reviewed recovery remain open; no replay, redemption, minting or fence-clear operation is added.
