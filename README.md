# Foundations PL4 - Minecraft 1.16.4 / Forge

Independent **0.2a-port.1** alpha source target. **Forge 35.1.37 / Java 17 required. Further API testing is still required.**

This branch targets 1.16.4 specifically, while `mc/1.16.5` and `mc/1.18.2` retain their own builds. The full-feature 1.16.5 source port is the starting point, not the limited legacy transport subset. Branch creation and compilation are not proof of gameplay or optional-provider stability.

Run `bash gradlew build` for the runtime and sources. The isolated `src/portTest` harness executes 191 adapted server scenarios using `bash gradlew runServer -PportScenarios` in a disposable checkout. It is never bundled as part of the runtime. The branch workflow records actual target identity and gates publication.

See [release scope and acceptance limits](docs/releases/0.2a-port.1.md). Client visuals, ordinary installed-launcher boot, live multiplayer, installed optional APIs and sustained performance checks remain pending. No Java 8 or broad 1.16.x binary compatibility is asserted. Exact per-version coverage, not relabelled artifacts, is the goal.
