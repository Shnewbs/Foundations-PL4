# 0.1a API acceptance matrix

**Further API testing is still required.** Every installed-provider result below is pending for each Minecraft track independently. An adapter being registered, a reflected signature resolving, or a synthetic fixture passing is not proof that a supported modpack works.

| API / integration | Existing automated coverage | Further installed testing required |
|---|---|---|
| NeoForge item/fluid/FE | Native vanilla inventory routes, bounds, persistent escrow and transfer rules | Sided modded wrappers, capability replacement, full destinations, inaccurate simulation, unload/reload and repeated delivery |
| InfoProviders v1 | Registration limits/lifecycle, bad callbacks, bounded sinks, wrong-dimension rejection, immutable registry diagnostics | Real add-ons, common-setup lifecycle, dedicated server class loading, server-thread use and reload behavior |
| Mekanism Joules | Synthetic reflected telemetry/transfer and conversion fixtures | Exact installed API version, sided source/sink, simulation agreement and saved escrow |
| GTCEu EU | Synthetic extraction/insertion, pushed input, voltage and conversion fixtures | Installed voltage/amperage tiers, overvoltage policy, multiblocks and loss/duplication checks |
| Electrodynamics / Voltaic | Synthetic optional transfer/conversion contracts | Exact capability signatures, real providers, simulation, capacity and restart behavior |
| Create | Synthetic read-only RPM/stress contracts | Real shaft networks, overstress updates and client visuals; no kinetic generator/motor implementation claim |
| AE2 | Synthetic read-only grid-energy contracts and grid deduplication | Powered/unpowered grids, security changes and real service lifecycle; no AE2 inventory/crafting access claim |
| Other FE mods | Standard capability path | Each installed mod and exposed side; no blanket compatibility certification |
| Jade / JEI / EMI / KubeJS | Existing source only where present | Complete missing features and verify matching ecosystem versions before claiming parity |

## Acceptance procedure

1. Record PL4 build, Minecraft, loader, Java, dependency versions and server config. Use a copied world.
2. Run each supported integration alone, then in the combined target pack. Confirm missing integrations leave PL4 loadable.
3. Exercise empty/full/partial stores, permitted and blocked faces, both route directions, native units and explicitly enabled conversions. Count resources before and after; simulated calls must not mutate state.
4. Replace or remove a provider during use. Unload/reload chunks and restart the dedicated server with nonempty escrow. Confirm no loss, duplication, stale handler access or forced chunk loads.
5. Test ownership restrictions with two players, reconnects and dimension changes. Test unsupported API versions and record the resulting diagnostics.
6. Record result and logs per track. Keep pending cells pending until the real scenario passes. Native tests are a separate evidence category.

API changes in 0.1a: InfoProviders.API_VERSION is 1; registeredIds() returns an immutable deterministic snapshot including foundations_pl4:vanilla. sample() requires the server thread and returns no data for a target in another dimension. Callbacks remain trusted, synchronous, read-only by contract, and are not sandboxed.
