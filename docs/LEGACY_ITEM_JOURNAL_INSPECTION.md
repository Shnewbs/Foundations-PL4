# Legacy item uncertainty inspection

Current implementation: exact Minecraft 1.6.4 and 1.12.2 development sources. 1.6.4 uses normalized player names; 1.12.2 uses account UUID strings. Qualification is scoped in the linked diagnostic checkpoint.

- Stand within eight blocks of a loaded PL4 host you own (or inspect as an operator) and use `/pl4legacy journal x y z [page]`. This lists unresolved tokens at that host's current adjacent target, four per page, with recorded owner and current dimension scoping.
- Hold a sealed PL4 item receipt and use `/pl4legacy receipt`. The recorded owner or an operator may inspect it. A receipt from another dimension directs inspection to that dimension; it does not query a coincident target in the current world.
- New journal handoffs save an active token, owner, dimension, target and phase (`wireless`, `extract`, `insert`) under `ItemJournalContext` on the tile and its removal receipt. Historical receipts without that context remain unlinked; operator inspection cannot reconstruct their original destination.
- The command reports opaque journals, mismatched contexts and absent tokens without clearing anything. An absent token is not recovery clearance. No release, replay, receipt redemption or item minting command is provided.

## Recovery evidence to collect

Preserve a complete world backup and the original receipt. Record its token/context, dimension, target inventory, held escrow and observed provider outcome. Inspect the actual source and destination without replaying the uncertain transfer. Keep unresolved fences in place until conservation and ownership are established by an explicit reviewed recovery procedure. The existing generated-world upgrade tests do not cover unresolved pre-upgrade automatic faults, arbitrary modpack worlds or full-process/crash semantics.

Journal listings are bounded by the 4096-record journal cap. A removal receipt links the tile's active uncertain handoff; other overlapping tokens at a target remain independent and are visible through target inspection. Read-only diagnosis does not resolve them.
