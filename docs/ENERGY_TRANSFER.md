# Native energy and explicit conversion

R1.5 adds optional native Mekanism J, GregTech CEu EU, and Electrodynamics/Voltaic Joule transfer adapters alongside FE. These are four explicit transport choices: `FE`, `J` (Mekanism), `EU`, and `ED_J` (Electrodynamics). The node UI shows `Mek J` and `ED Joules` to distinguish providers. Missing or unsupported API versions are omitted from the node choices. Other energy systems and Create rotation are not implemented by this build.

## Node settings

Open a Transfer Node's **Settings** tab and scroll to its energy controls.

- **Conversion Off**: choose Input; Output follows it atomically. This is same-unit native transfer.
- **Conversion On**: choose Input and Output separately. Cross-unit routes require the server's conversion option.
- **REMOVE / EXPORT**: Input is the attached machine's energy; Output is the unit offered to the network.
- **ADD / IMPORT**: Input is the unit expected from the network; Output is the attached machine's energy.
- **EU voltage**: packet voltage for insertion into an EU machine, default 32 V; range 1..1,048,576. A voltage above the receiver's rating is rejected before extraction/insertion.

An export's network Output must match an import's network Input. Ordinary Nodes remain passive **FE** endpoints. For native J/EU/ED connections use Transfer Nodes on both machines. Existing FE nodes retain their settings and transfer behavior. Cables need no converter setting.

Examples:

| Source | Destination | Result |
|---|---|---|
| REMOVE, Conversion Off, Input Mek J | ADD, Conversion Off, Input Mek J | Native Mekanism J |
| REMOVE, Conversion On, Mek J → FE | Ordinary Node on an FE battery | Mekanism J converted to FE |
| Ordinary Node on an FE battery | ADD, Conversion On, FE → EU, suitable EU voltage | FE converted to EU packets |
| REMOVE, Conversion On, ED Joules → FE | ADD, Conversion On, FE → Mek J | Electrodynamics → FE → Mekanism; two conversion boundaries |

Server caps govern all paths, including retries. Player edits remain subject to the existing owner, interaction, distance and clicked-part identity checks. Drain escrow before changing energy mode, types or EU voltage; the controls lock while it exists. Keep a compatible destination connected to drain it.

## Server/modpack policy

Merge this section into `foundations_pl4-server.toml`, without duplicating existing tables. See [config locations and transfer caps](TRANSFER_CONFIG.md).

```toml
[energyTransfer]
mekanismJoules = true
gregtechEU = true
electrodynamicsJoules = true
conversionEnabled = true
fePer1000J = 400
fePerEU = 4
fePerElectrodynamicsJ = 1
conversionEfficiencyPermille = 1000
```

Whitespace before keys is optional. Defaults are pack policy, not a universal equivalence: 2.5 Mekanism J = 1 FE; 1 EU = 4 FE; 1 Electrodynamics J = 1 FE. Electrodynamics is independent of Mekanism and defaults to the 1:1 FE behavior in Voltaic's wrapper. All ratios are positive integers up to 1,000,000. Ratios apply in both directions; using one accounting value prevents a lossless round trip from producing energy.

`conversionEnabled = false` disables cross-unit routes while retaining enabled native same-unit transfers. Individual adapter switches disable that provider's transport independently of its reader. Efficiency ranges from 1..1000 thousandths; 900 means 90% at **each** cross-unit node boundary. Same-unit transport has no configured conversion loss. Existing nodes default to Conversion Off and FE → FE.

Both per-node `transfer.fePerCycle` and shared `transfer.networkCaps.fePerCycle` use **FE-equivalent** accounting for native energy. Shared caps count consumed delivery credits once. Fractional deliveries round the budget charge upward to the next whole FE; this is conservative and may reduce throughput for tiny transfers. Extraction is bounded by each driver's raw native FE-equivalent budget. A native source quantum can exceed the simulated delivery amount; the excess stays in escrow. A low cap must accommodate the destination's smallest unit/packet. For example, at 4 FE/EU a 32 V EU packet costs 128 FE, so a shared cap below 128 cannot deliver it.

The transfer interval remains `network.updateTicks`; this does not affect snappy cable placement. EU output/input voltage and amperage are bounded for the transfer burst, not multiplied by the sampling interval. Electrodynamics ampacity uses its API's `J / V * 20` burst convention. Large intervals can therefore underutilize a provider's continuous power capacity.

## Persistence and provider boundaries

Accounting uses integer millionths of FE. Mekanism API quantities are whole J; EU quantities are whole EU, with whole voltage packets on insertion. Electrodynamics' public API uses doubles: PL4 represents microjoules, credits actual withdrawals downward, and charges actual deposits upward. This cannot create energy from fractional responses; conservative rounding can lose **less than one microjoule per actual partial provider operation**. Requests are rounded downward before passing doubles to the API.

Escrow persists its network unit, exact credits, and all three ratio values across save/load and node drops. Even sub-FE escrow retains a drop payload and the energy routing settings needed to recover it. If a ratio changes while escrow exists, delivery/top-up pauses with a diagnostic until the original saved ratios are restored. Drain escrow before changing pack ratios. Efficiency changes affect future conversion boundaries, not already converted escrow credits.

Mekanism uses the sided strict-energy capability's documented simulate/execute methods. EU extraction respects the sided output permission and output voltage/amperage allowance; insertion uses `acceptEnergyFromNetwork` with rated voltage and bounded whole amperage. It never fills via `changeEnergy` or writes energy NBT. Electrodynamics uses the sided `voltaic:electrodynamicblock` capability and its `TransferPack` simulation methods, matching the machine's exact advertised operating voltage; PL4 never deliberately invokes overvoltage. Adapters are optional, resolve public interfaces, and cache handlers only within one network cycle. Unloaded targets are not force-loaded. These guarantees assume providers honor their public API contracts.

Energy Readers stay read-only and report existing FE/Mek J/EU telemetry in separate native totals; transport settings do not change their readings. Electrodynamics native reader telemetry is not added by this build.

## Validation

The new core suite runs 30,025 deterministic arithmetic/API-contract assertions, including rational round trips, configured loss, saturation, side/voltage/ampere gates, Mekanism remainder semantics, and Electrodynamics fractional/ampacity behavior. Eleven native server fixtures cover the production route engine, native J and ED amounts, export/import fractions, shared drivers/caps, policy and loss, whole EU packets, partial acceptance, ratio changes, persistence/drop recovery, and link targets. Provider contracts use API-shaped fixtures; actual installed Mekanism, GTCEu and Electrodynamics/Voltaic packs and the new client settings still require in-game acceptance.

Public API references: [Mekanism strict energy](https://github.com/mekanism/Mekanism/blob/1.21.x/src/api/java/mekanism/api/energy/IStrictEnergyHandler.java), [GTCEu energy container](https://github.com/GregTechCEu/GregTech-Modern/blob/1.21/src/main/java/com/gregtechceu/gtceu/api/capability/IEnergyContainer.java), [Voltaic electrodynamics capability](https://github.com/aurilisdev/Voltaic/blob/1.21.1/src/main/java/voltaic/api/electricity/ICapabilityElectrodynamic.java) and [TransferPack](https://github.com/aurilisdev/Voltaic/blob/1.21.1/src/main/java/voltaic/prefab/utilities/object/TransferPack.java).

## R1.6: push-only GregTech sources

Transfer Nodes now expose a sided GregTech EU input for REMOVE / EXPORT nodes configured with Input EU and Energy On. This allows sources such as GregTech's creative energy block, which pushes packets but refuses storage extraction, to feed the persistent conversion escrow. Connect the node to the machine with its mounting face and set **EU voltage** to the source's packet voltage (32 LV, 128 MV, etc.). Higher voltages are rejected, never silently accepted. Configure EU → FE with Conversion On and use an ADD / IMPORT FE node or ordinary FE Node at the destination. The creative source must be enabled in its own GUI.

Incoming packets respect server conversion policy, stored ratio profiles and the per-node buffer cap; delivery respects shared network caps. The buffer remains bounded between network update cycles, survives saves and node drops, and refuses additions after removal or a ratio change. Stored machines still support the existing sided pull adapter. Other push-only providers are not covered by this EU receiver.

Settings now have a visible draggable scrollbar; clicking the track also scrolls. The Data tab reports missing source capabilities, blocked/empty sources, unmatched destination routes and receivers that refuse the offered packet. Actual installed-mod visual and power checks remain required.
