# Foundations PL4 — Minecraft 1.16.5 / Forge

Native development port **0.2a-port.1**, **Forge 36.2.42**, **Java 17 required**. This PL4 port is not compatible with Java 8.

Retains the cable, reader, storage, transfer/escrow, ownership and display rules; native TileEntity persistence/ticking, sided Forge capabilities, SimpleChannel packets, legacy model rendering and biome-event sapphire generation implement this target's platform behavior.

`bash gradlew build` produces the runtime and sources. The separate `src/portTest` module adapts all 191 existing behavior fixtures to a disposable dedicated server, because Minecraft 1.16.5 lacks modern GameTests. It is never shipped in the runtime JAR. Run only with `bash gradlew runServer -PportScenarios` in an isolated checkout and accept the EULA in `run-scenarios` before using that development harness.

**Further API testing is still required.** Native scenario results, client visuals, installed optional providers, multiplayer and performance acceptance are separate. All planned optional-API fallback features are not yet complete. Back up worlds and use matching client/server versions; worlds and JARs are not interchangeable between Minecraft targets.
