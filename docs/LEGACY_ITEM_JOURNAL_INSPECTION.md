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
