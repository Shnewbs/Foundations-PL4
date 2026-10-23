# Legacy Wireless Storage development

The 1.12.2 and 1.6.4 development ports include a native handheld storage screen. This is a bounded functional subset; modern search, grouped quantities, sorting, component selection, graphical editor and multipart parity are still separate work. It is not yet an RC release.

## Use

1. Craft Wireless Storage from one legacy data cable, a compass and an ender pearl.
2. Sneak-click an owned cable, item node or reader to bind the handheld to that host.
3. Hold the bound tool in the selected hotbar slot and use it within 64 blocks in the same dimension.
4. Click a storage slot to withdraw up to one stack; right-click to withdraw one item. Items go directly into your inventory.
5. Click a player-inventory slot to deposit up to one stack; right-click to deposit one item. The handheld itself cannot be deposited.
6. Use the arrows to browse 45 occupied native inventory slots per page. A busy or rapidly changing network may reject a stale click; wait for the refreshed view and click again. Reopen after the window reaches its revision limit.

A cable network exposes loaded, unpaused item export/import nodes and item readers belonging to its owner. Sneak-click each endpoint's appropriate face to choose its adjacent inventory. Native sided access, configured host/slot limits, full destinations, ownership changes, removed hosts and the global transfer switch are enforced on the server. Oversized graphs stop. Browsing does not force-load chunks.

The handheld is bound to the saved identity of a particular host. Replacing that host requires rebinding. Operators retain the existing legacy owner/operator access policy. Minecraft 1.6.4 ownership uses normalized legacy player names; it does not provide modern account UUID identity. Its configurable handheld item ID defaults to 12503 before the native item offset, with collision checks.

## Transfer uncertainty

Player withdrawals and deposits share persistent node escrow with the existing conserving item transport. A provider that mutates inventory and then throws leaves that endpoint quarantined. PL4 does not automatically retry uncertain transfers or drop uncertain escrow as spendable items. Breaking a quarantined host produces a sealed diagnostic recovery receipt; preserve it while inspecting the affected inventories. Valid partial transfers that retain items also pause the endpoint for inspection.

Each GUI action carries its current server view revision in the native window packet. Refreshes and page changes invalidate old revisions. Revisions never wrap inside an open window, and at most one item action executes per tick. Vanilla drag, throw, number-key swap and cursor pickup are disabled for this interface.

## Evidence and remaining scope

[The native acceptance record](acceptance/rc-dev-legacy-wireless-native.json) identifies both exact source commits, packaged JAR hashes and workflow artifacts. Each passed 156 native scenarios, including 36 handheld/container scenarios, a production-only dedicated-server boot and an upgrade of a generated world made with the checksum-verified published preview.3 JAR.

Native fixtures do not certify real-client rendering, dedicated multiplayer, installed optional providers, copied user/modpack worlds or sustained performance. Those remain separate checks. The [RC ledger](RC_DEVELOPMENT_STATUS.json) records current evidence and publication clearance.
