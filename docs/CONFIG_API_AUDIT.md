# Server config API compatibility audit

Audited official NeoForge Maven metadata and every distinct resolved FML ModConfig.Type source enum on 2026-10-01 UTC. See CONFIG_API_AUDIT.json for all build-to-loader mappings.

| Minecraft track | Published NeoForge builds checked | Result for this bug |
| --- | ---: | --- |
| 1.21.1 | 249 | All retain SERVER; not affected |
| 26.1 | 159 | All retain SERVER; not affected |
| 26.2 | 89 | All retain SERVER; not affected |
| 26.3 | 38 | 26.3.0.37-beta through .39-beta use FML 12.0.8 and SYNCED |
| 26.4 | 0 | No published build available to verify |

Total: 535 published builds, 26 distinct FML versions. This is a config API audit, not whole-mod gameplay certification on every Minecraft version.

The first PL4 26.3 port referenced SERVER directly and fails construction on .37–.39. Port.2 chooses SYNCED when present, otherwise SERVER, and fails explicitly if neither exists. It preserves server-to-client syncing and the foundations_pl4-server.toml filename. Config storage locations follow the installed loader's own semantics.

Native builds and all 155 required GameTests passed on NeoForge 26.3.0.23-beta and 26.3.0.39-beta in workflow run 36814114202. Regression tests cover legacy and renamed enums, preference for SYNCED when both names exist, and refusal to substitute unsynced LOCAL/COMMON configs.

Reproduce the API inventory with `python tools/audit_config_types.py`. It writes config-api-audit.json from https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml, each build's POM, and the resolved loader sources JAR. Client visual acceptance and installed third-party energy providers remain separate checks.
