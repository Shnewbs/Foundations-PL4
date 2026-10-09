# Foundations PL4 0.1b

Standalone logistics, live machine displays and automation for Minecraft 26.1.2 / NeoForge. No Sonar Core or MCMultiPart runtime dependency.

**Further API testing is still required.** This beta includes a dedicated Wireless Storage screen, new directional item/fluid filters, multi-condition Signallers and provider API safeguards, alongside the 0.0.2a editor, routing and telemetry work. It does not claim full PL2 parity or installed-mod compatibility certification.

Download the matching runtime JAR from [GitHub Releases](https://github.com/Shnewbs/Foundations-PL4/releases). Sources JARs are for development. See [0.1b release notes](docs/releases/0.1b.md), [API testing matrix](docs/API_TESTING.md), [integration status](docs/INTEGRATION_STATUS.md) and the [Field Guide](docs/FIELD_GUIDE.md).

## Minecraft release targets

Separate releases are maintained for **1.21.1, 26.1.2 and 26.3**. Each is compiled and tested against its own Minecraft APIs. See [release targets](docs/RELEASE_TARGETS.md) for branches, loader versions and publication rules.

## Build and validate

Use Java 25:

```sh
./gradlew --no-daemon --console=plain clean build runGameTestServer
python tools/run_offline_checks.py
```

Windows: use `gradlew.bat` for Gradle. The runtime artifact is `build/libs/FoundationsPL4-26.1.2-0.1b.jar`. Publication runs native checks before distributing artifacts; client visuals and installed API acceptance are separate.

## World compatibility

Back up before upgrading. Schema 2 gains optional fields; existing filters inherit their previous behavior and Signallers keep their single-condition mode until statements are added. Do not downgrade configured worlds: older versions discard filters, statements and channel metadata. ADD/REMOVE remains explicit-peer only.

The master roadmap retains historical audits and unfinished work. See its current reconciliation before interpreting old checkboxes.
