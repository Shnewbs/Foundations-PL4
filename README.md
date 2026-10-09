# Foundations PL4 - Minecraft 26.1.2 - 0.2a

Standalone NeoForge alpha port. Java 25. Further API testing is still required.

Network-wide Wireless Storage with search/sorting, cross-inventory withdrawals,
offhand deposits, permission revalidation and component-safe variants. Receiver
and Entity Node settings provide validated component selection.

Build with `bash gradlew build runGameTestServer`. The release workflow verifies
all 192 registered native tests, offline regressions and standalone packaging
before publishing the target-specific JAR, source JAR, source ZIP and checksums.
A successful compile is not installed-mod or real-client acceptance.

Use only the JAR for this exact Minecraft version. Both client and server must
use protocol 5 builds. Back up worlds before upgrading. No Sonar Core or
MCMultiPart runtime dependency is required. See [release notes](docs/releases/0.2a.md)
and [field guide](docs/FIELD_GUIDE.md).
