# Legacy Wireless Storage development

The 1.12.2 and 1.6.4 development ports include a native handheld storage screen. This is a bounded functional subset; modern search, grouped quantities, component selection, graphical editor and multipart parity are still separate work. It is not yet an RC release.

## Use

1. Craft Wireless Storage from one legacy data cable, a compass and an ender pearl.
2. Sneak-click an owned cable, item node or reader to bind the handheld to that host.
3. Hold the bound tool in the selected hotbar slot and use it within 64 blocks in the same dimension.
4. Click a storage slot to withdraw up to one stack; right-click to withdraw one item. Items go directly into your inventory.
5. Click a player-inventory slot to deposit up to one stack; right-click to deposit one item. The handheld itself cannot be deposited.
6. Click **Sort** to cycle between network traversal order, item name and descending per-slot stack count. Rows remain separate physical slots, including NBT variants; this is not grouped storage. Sorting resets the page and invalidates old clicks. The name order uses the server-provided display name.
7. Use the arrows to browse 45 occupied native inventory slots per page. A busy or rapidly changing network may reject a stale click; wait for the refreshed view and click again. Reopen after the window reaches its revision limit.

A cable network exposes loaded, unpaused item export/import nodes and item readers belonging to its owner. Sneak-click each endpoint's appropriate face to choose its adjacent inventory. Native sided access, configured host/slot limits, full destinations, ownership changes, removed hosts and the global transfer switch are enforced on the server. Oversized graphs stop. Browsing does not force-load chunks.

The handheld is bound to the saved identity of a particular host. Replacing that host requires rebinding. Operators retain the existing legacy owner/operator access policy. Minecraft 1.6.4 ownership uses normalized legacy player names; it does not provide modern account UUID identity. Its configurable handheld item ID defaults to 12503 before the native item offset, with collision checks.

## Transfer uncertainty

Player withdrawals and deposits share persistent node escrow with the existing conserving item transport. A provider that mutates inventory and then throws leaves that endpoint quarantined. PL4 does not automatically retry uncertain transfers or drop uncertain escrow as spendable items. Breaking a quarantined host produces a sealed diagnostic recovery receipt; preserve it while inspecting the affected inventories. Valid partial transfers that retain items also pause the endpoint for inspection.

Each GUI action carries its current server view revision in the native window packet. Refreshes, sorting and page changes invalidate old revisions. Revisions never wrap inside an open window, and at most one item action executes per tick. Vanilla drag, throw, number-key swap and cursor pickup are disabled for this interface.

## Evidence and remaining scope

[The native acceptance record](acceptance/rc-dev-legacy-wireless-native.json) identifies both exact source commits, packaged JAR hashes and workflow artifacts. Each passed 156 native scenarios, including 36 handheld/container scenarios, a production-only dedicated-server boot and an upgrade of a generated world made with the checksum-verified published preview.3 JAR.

Native fixtures do not certify real-client rendering, dedicated multiplayer, installed optional providers, copied user/modpack worlds or sustained performance. Those remain separate checks. The [RC ledger](RC_DEVELOPMENT_STATUS.json) records current evidence and publication clearance.

## Scoped stock-client check

The [1.12.2 client record](acceptance/rc-dev-1.12.2-stock-wireless-client.json) and [1.6.4 client record](acceptance/rc-dev-1.6.4-stock-wireless-client.json) each identify an exact native-qualified production JAR on its stock Forge runtime, with a separate client test mod. They cover mouse-driven withdrawals, deposits and paging, stale view rejection, packet codec round-trips, four GUI scales and a tooltip at 1280×960 in private offline integrated worlds. World-display checks cover private text, explicit publication of a network reading and clearing that reading on hide. The renderers keep screen text independent of world lighting and restore the previous texture-unit state. Consult each record for actual results and reviewed captures; other display/editor modes, dedicated multiplayer, installed providers and sustained load remain separate.

The acceptance workflow is frozen to its qualified source, artifact ID and archive checksum. Its temporary build artifacts retain for 14 days. Requalify the intended source through the native gate and update those inputs before testing a new candidate or after artifact expiry; the stored reports do not silently certify later code.

## Sorting development candidate

The next 1.12.2 and 1.6.4 source candidates add three server-authoritative sort modes and 12 native sorting/control cases each (168 total declared cases per port). The exact 1.12.2 candidate passed 168 native cases; the repaired 1.6.4 candidate passed 170. Both passed production startup, generated preview.3 migration and scoped stock-client sorting/order/replay checks. All 16 reviewed captures and current production JAR hashes are linked from the RC ledger; prior 156-case evidence certifies only its recorded JARs. The 1.6.4 binding comparison now ignores only the NBT root transport name changed by ItemStack saving, retaining the complete payload fence. Search, grouping, editor/multipart parity and the remaining acceptance gates stay open.
