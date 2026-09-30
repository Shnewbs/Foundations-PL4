# Foundations PL4 0.0.1a.R16 acceptance

Use Java 21, Minecraft 1.21.1 and NeoForge 21.1.250. Test on a copied world.

## Build

Run `gradlew.bat --no-daemon --console=plain clean build runGameTestServer`.

Expected runtime: `build/libs/FoundationsPL4-1.21.1-0.0.1a.R16.jar`.

All 114 registered GameTests must pass.

## Item routing

1. Existing pair: chest -> REMOVE Transfer Node -> data network -> ADD Transfer Node -> chest. Confirm exact item conservation.
2. Passive source: chest -> normal Node -> data network -> ADD / IMPORT Transfer Node -> chest. Confirm transfer without a second Transfer Node at the source.
3. Passive destination: chest -> REMOVE / EXPORT Transfer Node -> data network -> normal Node -> chest. Confirm transfer without a second Transfer Node at the destination.
4. Put both an ADD peer and an ordinary Node destination on the same network at equal priority. The explicit ADD peer should receive first.
5. Fill every destination slot. Confirm the source is not extracted and escrow remains empty.
6. Apply an item/tag filter on ADD. Confirm only matching resources import from a normal Node.
7. Set one Transfer Node to ADD / REMOVE with only passive ordinary Nodes present. Confirm it does not blindly pump the passive pool. Pair it with an explicit ADD or REMOVE peer and confirm the explicit direction works.

## Fluids

Repeat passive-source and passive-destination tests with a real NeoForge fluid tank. Confirm amounts are conserved, filters apply and a full tank does not drain the source.

## FE

Repeat passive-source and passive-destination tests with FE batteries/machines. Confirm transfer never exceeds the configured per-cycle rate. Native EU/J-only devices must not be silently converted; use an FE bridge if the mod exposes one.

## Persistence

Create pending escrow using a deliberately blocked destination, save/quit/reload, then unblock it. Confirm the escrow is delivered once without re-extracting the source. Operator-remove a Transfer Node with pending state and confirm the dropped configured item preserves escrow.

## Freeze gate

Do not freeze 0.0.1a until the R15 hologram visual checks, R14 display/editor checks, R16 transfers above, and all 114 GameTests pass together.
