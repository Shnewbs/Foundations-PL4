# Independent installed InfoProvider probe — exact Minecraft 1.21.1

This is a **separate NeoForge test mod**, not a PL4 source-set test, production
feature, or release artifact. It uses reflection against the installed
Foundations PL4 production JAR and never compiles, shades, or packages PL4
implementation classes. It is deliberately restricted to 1.21.1/NeoForge
21.1.250 and Java 21; do not relabel it for another exact Minecraft target.

## Isolated local execution (no GitHub Actions)

Obtain the production 1.21.1 JAR from an independently retained exact-source
verification artifact and check its SHA-256. Keep the original artifact and
source/commit receipt. From the PL4 checkout:

    python tools/provider-probe/run_provider_probe.py prepare \
      --checkout . --jar /path/to/FoundationsPL4-1.21.1.jar \
      --sha256 <verified-64-hex-production-jar-sha256> \
      --source-sha <verified-40-hex-source-commit> \
      --output /path/to/new-disposable-probe-run

    python tools/provider-probe/run_provider_probe.py run \
      --output /path/to/new-disposable-probe-run \
      --timeout-seconds 1500

The script copies the pinned Gradle wrapper, standalone probe source, and
production PL4 JAR into a **new** isolated directory, configures an offline
loopback-only disposable server with ephemeral RCON shutdown credentials, builds
the separate probe JAR, runs NeoForge, checks both JARs and native proof, and
retains logs/JSON/checksums. It refuses to overwrite an existing directory.

Tests use a deliberately loaded chest fixture, but separately verify that a
far-away unloaded chunk remains unloaded before/after sampling. The installed
provider callbacks test same-position backing replacement, later-provider
registration closure, exception partial-row quarantine, and no-force-load.
A provider that closes its own registration mid-callback must also lose its
buffered rows while a later healthy provider continues. The installed proof
schema is 2, with an explicit self-close scenario. Counters, output rows,
and registration cleanup must all match. The prepared
run carries a random nonce through the server environment, native proof, and
server-log PASS marker, so an old proof or marker cannot qualify a different
preparation. The runner refuses to overwrite any prior log, proof, or receipt;
use a new prepared output directory for each native attempt. A callback
throws intentionally during exception quarantine; a logged provider warning
therefore does not mean the test failed.

The receipt is **installed-provider-only evidence**, not full RC acceptance.
It cannot qualify online authenticated multiplayer, arbitrary user-world
migration, sustained TPS/heap, client screenshots, or other exact versions.
The source SHA is a caller-supplied attribution and must be independently
matched to the retained production-JAR artifact; a JAR checksum alone does
not prove its source commit. Never publish either development JAR as an RC.

## Preflight integrity gate (October 31)

Before invoking Gradle, the runner verifies every pinned wrapper/build/Java source
hash, the exact installed PL4 JAR, a single installed mod, the run nonce and
source attribution, and the disposable server's loopback-only RCON/offline
configuration. The same checks run again when verifying the retained proof.
`verifyProbeIsolation` executes **before** `runServer`, so an unverified
standalone probe JAR cannot be launched first. Negative unit tests cover
wrapper tampering, changed online-mode, extra mods and missing hash entries.
These are source-level safeguards; they do not replace compiling and running
the independent probe on a network-capable local NeoForge installation.

## Measured native proof and immutable evidence (October 31)

The Java probe now writes the **observed** callback counts, published/discarded
rows, before/after chunk residency and registration leak count into schema 2.
It still requires every expected value before issuing a PASS marker; expected
values are not substituted into the JSON receipt. The native proof uses
exclusive file creation, as do the Python server log and retained receipt.
Symlinked proof/log inputs are rejected, and repeated identical receipt
verification remains idempotent. New negative Python tests cover an existing
log and a symlinked receipt. **These source changes have not yet been compiled
or executed against the independent installed production JAR**; a native PASS
and exact source/JAR/proof/log hashes are required before acceptance.

## Pinned native server configuration (October 31)

The preparation manifest now hashes the **entire** generated `run/server.properties`
and `run/eula.txt`, in addition to the wrapper and independent source.
Preflight and post-run verification reject any changed setting (including RCON
credentials, world settings, and simulation/view distances), not merely changes
to the previously enumerated network-isolation keys. The RCON secret itself is
never written into the manifest or receipt. Offline negative tests exercise
changes to previously unchecked server settings and the EULA. This is a
source-level integrity improvement; it is **not** an installed native PASS.

The verifier also refuses an external `--log` path: the accepted native server
log must be the non-symlinked `provider-server.log` inside the prepared runtime.
This blocks accidentally mixing proof and log files from separate runs.
\n## Strict observed proof types (October 31)\n\nThe independent native-proof verifier now rejects JSON booleans or integral-looking\nfloats in callback counts, registration leak counts, schema and API version;\nchunk residency must be actual JSON booleans. Python otherwise treats\n`true == 1` and `false == 0`, allowing fabricated numeric surrogates to\nmatch expected counters. New negative tests cover each scenario counter and\nmetadata/residency type. Four isolated contract tests passed (59 cases), but\nthe **repository Python suite and installed native probe have not run**.\n[Evidence](../../docs/acceptance/rc-dev-provider-probe-exact-counter-types-2026-10-31.json).\n