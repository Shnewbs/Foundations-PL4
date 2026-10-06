# R3 implementation and acceptance

## Implemented in source

- Display Layers: atomic group/ungroup, lock/unlock, hide/show; hidden layers remain accessible in Layers. World selection expands groups. Locked elements reject normal transforms and deletion. Locks are editing aids, not an authorization boundary: authorized undo/import can replace them.
- Element backgrounds, borders and page-link actions. Element metadata survives NBT, templates, JSON snapshots and undo/redo. Copies receive independent group IDs.
- Eight named page slots (32 characters each). Names propagate across joined displays and survive reload. Page renaming is separate from element undo/template history.
- Server-resolved page links: right-click a visible linked element; frontmost elements block links below them. Existing ownership, interaction, reach and canvas checks apply. Sneaking bypasses links (covered readers retain their existing sneak access).
- Exact, case-sensitive input/output channel names on passive Nodes and Transfer Nodes. Blank matches blank for old worlds. All three resource paths use the same channel selection. Channel edits are blocked while the node owns item/fluid/energy escrow.
- Equal-priority driver and destination rotation uses a network-cycle cursor, independent of item slot selection. Priority ordering and explicit-peer preference remain.

## Validation

Dependency-free editor/channel tests and native packet/persistence, action picking, route-isolation and repeated-cycle fairness fixtures accompany these changes. Native compilation and execution must pass on both Minecraft tracks before release. Client visuals and installed third-party power mods need separate acceptance.

## Save compatibility

Host schema remains 2. New metadata is optional and defaults to existing behavior. Older versions discard page names, organization, styles, links and transfer channels; reverting can reconnect routes that R3 separates. Back up the world before upgrading and do not downgrade a configured production world.

## Remaining larger R3 scope

Wireless/Array/Entity Node/remote storage interfaces, Signaller statement lists, dedicated Clock controls, installed power-adapter acceptance, complete directional PL2 filter semantics, and real-client/multiplayer profiling are not complete in this batch. Existing ADD/REMOVE peer-only safety remains. No claim of full PL2 parity is made.
