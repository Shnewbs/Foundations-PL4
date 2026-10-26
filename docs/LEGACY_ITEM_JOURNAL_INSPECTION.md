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

## Rejected compound contexts with existing evidence — October 26, 2026

The [rejected-context checkpoint](acceptance/rc-dev-legacy-rejected-context-evidence-2026-10-26.json) preserves a later rejected non-null compound when a tile already carries legacy evidence. A matching valid context remains idempotent. A rejected context is kept on the tile, alongside its original legacy evidence; the saved journal retains a bounded opaque envelope containing the previous journal, rejected context and existing evidence. Repeated admission does not recursively wrap that envelope or allocate more tokens.

This freezes item targets for every owner in the current dimension until reviewed reconciliation. Diagnostics report an opaque journal and grant no clearance. Preserve the complete save and receipt. No recovery, replay, redemption, minting or fence-clear operation is added.

Six new native cases per port cover preservation, changed-owner fencing, serialized journal reload, bounded repeat admission, no-clearance receipt status and tile serialization. Missing/noncompound contexts and malformed-field replacement earlier in tile decoding still require separate preservation work. Existing process restart/migration fixtures are regression checks; the new rejection path has no process-crash or graphical acceptance claim.

## Missing and noncompound context decoding — October 26, 2026

The [decode-preservation checkpoint](acceptance/rc-dev-legacy-decode-context-preservation-2026-10-26.json) keeps existing compound legacy evidence unchanged when a current context becomes noncompound. Its raw NBT type/value is saved separately in memory and written back under the original context key on tile saves and sealed removal receipts. Earlier malformed payloads and fault text in the saved evidence are not overwritten. A recorded context that disappears stays absent; the decoder marks uncertainty even when saved fault text has been cleared. Generic escrow settlement cannot clear raw/missing context uncertainty.

Both cases retain the prior journal and original evidence behind an opaque, dimension-wide item barrier. No replacement token, guessed original route, replay, redemption, minting or fence-clear operation is created. Valid imported contexts still reload idempotently. First-time malformed evidence retains its prior unknown-route import behavior.

Ten new native cases per exact legacy port cover string/integer raw payloads, missing context, earlier payload preservation, repeated admission, serialized reload, actual sealed removal receipts, valid contexts and the settlement guard. These are in-process native save/removal tests. Existing 188 restart assertions and 60 historical checks are separate regression evidence; no new-path process-crash/power-loss acceptance is inferred. Other malformed fields, arbitrary external rewrites and worlds, installed-provider authorization/durability, multiplayer and reviewed recovery remain open.

## Decode-path process evidence and copied-save collection — October 26, 2026

The dedicated decode fixture now uses separate raw and missing-context worlds, each with four native JVMs: clean write/read and completed native save/storage-save/flush, parent SIGKILL, then read. Per port, 232 assertions (32/26/32/26 per suite) preserve a retained faulted host, an actually removed host's sealed receipt, the complete original evidence including nested byte payload, raw integer/string types or missing context, prior tokens and the opaque dimension barrier. These are separate from the 188 provider restart and 60 historical migration checks. They do not establish pre-save crash, power-loss or arbitrary world atomicity.

Independent region and saved-journal NBT inspection checks all four retained decode worlds per port. The hourly controller requires those checks for future decode-fixture candidates. See [the exact-source process evidence](acceptance/rc-dev-legacy-decode-process-restart-2026-10-26.json).

For bounded observations from an **offline copied save ZIP**, use:

```sh
python3 tools/collect_legacy_item_evidence.py copied-world.zip \
  --minecraft 1.12.2 --token RECORDED-TOKEN-UUID --owner RECORDED-OWNER \
  --dimension 0 --inventory 85,200,32 > item-observations.json
```

Replace the token, owner and explicitly observed inventory coordinates with the saved evidence being reviewed. The Minecraft argument is an operator label, not proof of runtime identity. Up to eight inventory coordinates are accepted. The collector hashes the exact input archive and saved records/tiles/stacks; it reports current context token matches separately from token occurrences in preserved evidence. Already unlinked receipts at an explicitly selected inventory are observations without inferred association or route.

It prioritizes explicit coordinates and matching saved target/observed-host coordinates, then scans within fixed bounds: 128 chunks, 4096 tiles, 32 MiB selected region bytes and 64 results per category. Limits and partial scans are explicit. An observed host never becomes an inferred original mutation target. Unknown journal schemas remain opaque. Report labels are bounded; complete payloads remain identified by hashes.

Missing tokens, an absent journal, a partial scan or equal inventory contents grant **no clearance**. Saved provider identity/owner labels are observations, not proof of authorization, alias identity or durability. Sealed claims are not additional spendable items. The tool does not change the archive, invoke providers, reconcile balances, redeem, replay, mint or clear fences. Fixture-world collector checks do not certify arbitrary user/modpack saves. Reviewed reconciliation, installed-provider acceptance, other malformed fields, copied worlds, multiplayer, sustained performance and broader parity remain open.
