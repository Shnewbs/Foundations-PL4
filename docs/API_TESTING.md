# API and functional-fallback acceptance matrix

**Further API testing is still required.** Installed-provider acceptance below remains pending for each Minecraft track independently. An adapter being registered, a reflected signature resolving, or a synthetic fixture passing is not proof that an installed modpack works. Native build/test results belong to their exact release commits and are not silently inherited by another target.

## Existing integration coverage

| API / integration | Existing automated coverage | Further installed testing required |
|---|---|---|
| Target-native item/fluid/energy access | Native vanilla inventory routes, bounds, persistent escrow and transfer rules; 26.x has its own transfer adapters | Sided modded wrappers, capability replacement, full destinations, inaccurate simulation, unload/reload and repeated delivery |
| InfoProviders v1 | Registration limits/lifecycle, bad callbacks, bounded sinks, wrong-dimension rejection, immutable registry diagnostics | Real add-ons, common-setup lifecycle, dedicated server class loading, server-thread use and reload behavior |
| Mekanism Joules | Synthetic reflected telemetry/transfer and conversion fixtures | Exact installed API version, sided source/sink, simulation agreement and saved escrow |
| GTCEu EU | Synthetic extraction/insertion, pushed input, voltage and conversion fixtures | Installed voltage/amperage tiers, overvoltage policy, multiblocks and loss/duplication checks |
| Electrodynamics / Voltaic | Synthetic optional transfer/conversion contracts | Exact capability signatures, real providers, simulation, capacity and restart behavior |
| Create | Synthetic read-only RPM/stress contracts | Real shaft networks, overstress updates and client visuals; no kinetic generator/motor implementation claim |
| AE2 | Synthetic read-only grid-energy contracts and grid deduplication | Powered/unpowered grids, security changes and real service lifecycle; no AE2 inventory/crafting access claim |
| Other native-energy mods | Standard target API path where implemented | Each installed mod and exposed side; no blanket compatibility certification |
| Jade / JEI / EMI / KubeJS and version-appropriate alternatives | Existing source only where present | Complete missing features and verify exact ecosystem versions before claiming parity |
| Wireless Storage 0.2a | Network aggregation, component variants, filters, topology/access revalidation and replay scenarios in the 0.2a native fixtures | Real storage wrappers, large networks, simultaneous users, reconnects and real-client UI acceptance |

## Required alternatives when an optional API is unavailable

These are functional acceptance requirements, not a claim that all fallbacks already exist. See [MULTIVERSION_PORT_PLAN.md](MULTIVERSION_PORT_PLAN.md) for the approved targets. Minecraft 1.6.4 is experimental; 1.7.10 is excluded.

| Task | Required no-optional-mod path | Status for the broader multi-version milestone |
|---|---|---|
| Live recipe lookup | PL4 browser or guide integration reflecting actual registered recipes and reload changes | Implementation/validation pending; static guide prose alone is insufficient |
| Inspection and diagnostics | PL4 inspector/status view exposing the information needed to configure and diagnose its own components | Existing screens/provider data are a starting point; task-completeness and per-port validation pending |
| Recipe/automation configuration | Documented PL4 declarative rules and native extension hooks, independent of a scripting mod | Existing configuration and hooks are a starting point; missing feature coverage and per-port validation pending; not arbitrary scripting parity |
| Inventory/fluid operations | Native sided access, PL4 routing and own item-storage workflow | Existing current-track implementation; target-specific native and installed-wrapper results must be recorded separately; network fluid-storage UI is not claimed |
| Power | Correct version-native adapters and explicit diagnostics for unavailable foreign systems | Target-specific implementation/testing required; no guessed units, conversion ratios or emulated absent networks |
| Performance/stability | Bounded work, lifecycle cleanup, caching and sustained-load tests without mandatory optimization mods | Existing bounds/caching are a starting point; per-port performance evidence pending |

A required user task is not complete merely because its original optional API is unavailable. Provide and test a compatible alternative or PL4-owned implementation. External-system-only information, such as Create stress when Create does not exist for that target, is explicitly not applicable; do not fabricate it. Third-party binaries remain optional installs, not automatically bundled dependencies.

For each task and Minecraft target record: primary adapter, selected alternative or built-in fallback, exact dependency versions, dependency absence behavior, client/server result, scenario logs and unresolved limits. Record external API availability and fallback completeness independently. Test conflicting providers and incompatible API versions without crashing the core or registering duplicate UI hooks.

## Acceptance procedure

1. Record PL4 commit/build, Minecraft, loader, build/runtime Java, dependency versions/checksums and server configuration. Use a copied world.
2. Run a clean client and dedicated-server baseline with no optional integrations. Exercise recipe lookup, inspection, configuration, cable/display operation, native stores and saves through PL4-owned paths. A missing required task remains a blocker.
3. Run each available integration or alternative alone, then in the combined target pack. Do not co-install mutually exclusive replacements. Confirm missing or incompatible integrations leave core PL4 loadable and give clear diagnostics.
4. Exercise empty/full/partial stores, permitted and blocked faces, both route directions, native units and explicitly enabled conversions. Count resources before and after; simulated calls must not mutate state. Test item components/NBT variants and repeated requests.
5. Replace or remove a provider during use. Unload/reload chunks and restart the dedicated server with nonempty escrow. Confirm no loss, duplication, stale handler access or forced chunk loads.
6. Test ownership restrictions with two players, simultaneous storage actions, reconnects and dimension changes. Recheck real-client rendering, GUI scaling, display editing and sustained network performance.
7. Record evidence per target and selected fallback. Keep pending cells pending until that real scenario passes. Native tests, helper/unit tests, real-client tests and installed-provider tests remain distinct categories. Use an equivalent server-scenario harness on legacy targets without modern GameTests.

InfoProviders.API_VERSION remains 1. registeredIds() returns an immutable deterministic snapshot including foundations_pl4:vanilla. sample() requires the server thread and returns no data for a target in another dimension. Callbacks remain trusted, synchronous and read-only by contract; they are not sandboxed.
