# Server and modpack transfer caps

PL4 registers `foundations_pl4-server.toml` as a NeoForge SERVER config. Set the server's config file, not a player's local preference. NeoForge 1.21.1 supports the config directory and per-world overrides in `serverconfig`; an existing world override takes precedence. Stop the server before editing and restart to apply predictably.

For an existing world, check `world/serverconfig/foundations_pl4-server.toml` (singleplayer: `saves/<world>/serverconfig/`). For pack/server defaults, use `config/foundations_pl4-server.toml` when no world override exists. The generated file includes comments for every new cap. See the [NeoForge 1.21.1 configuration documentation](https://docs.neoforged.net/docs/1.21.1/misc/config/).

Example sections to merge into the generated file (do not duplicate existing tables):

```toml
[network]
updateTicks = 20

[transfer]
enabled = true
itemsPerCycle = 64
millibucketsPerCycle = 1000
fePerCycle = 10000

[transfer.networkCaps]
itemsPerCycle = 128
millibucketsPerCycle = 4000
fePerCycle = 50000
```

`transfer` limits new extraction per Transfer Node per cycle. `transfer.networkCaps` limits total successful delivery per connected data network per cycle, shared across all its Transfer Nodes, ADD/REMOVE paths, passive endpoints, and pending escrow retries. A resource is charged once on successful destination insertion, not twice at extraction and insertion. Each separate connected component gets its own budget; wireless links joining components share that budget too. Existing endpoint eligibility, filters, priorities and ordering remain in effect.

Shared caps default to **0**, meaning uncapped; per-node limits and `transfer.enabled` still apply. To disable all transfers, set `enabled = false`. Item values count individual items, not stacks. Fluid values are millibuckets; energy values are FE-equivalent for native Mekanism J, GregTech EU and Electrodynamics J as well as FE. See [native energy and conversion](ENERGY_TRANSFER.md) for ratios, packet thresholds and conservative fractional budget charging.

At the default 20 ticks per cycle and 20 TPS, the example shared cap allows at most 128 items, 4000 mB and 50000 FE per second per network. At 10 ticks per cycle it allows twice that per second at 20 TPS. These are ceilings, not guaranteed throughput; server TPS and provider acceptance also affect actual delivery. Cable placement responsiveness is independent of these limits.

Ranges: shared items 0..10,000,000; fluids 0..1,000,000,000; FE 0..2,147,483,647. Config changes are read on the next network transfer cycle after NeoForge reloads the config; a graph rebuild is unnecessary. Restarting remains the predictable editing workflow.

Validation: native item tests cover shared drivers, ADD imports, source/destination escrow, independent networks, cycle reset and retained per-node limits. All three resource caps use the same tested bounded budget contract; actual third-party fluid/energy providers still need modpack acceptance.
