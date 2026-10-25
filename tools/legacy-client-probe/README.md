# Packaged legacy client acceptance

These probes load the retained native-qualified production JAR on stock Forge, alongside a separate test mod. The development Minecraft JAR and build-tool APIs are excluded from the game classpath. They use a private, generated, offline integrated world; no Minecraft account credentials are required.

| Target | Build / compiler | Game runtime | Forge runtime | Test mod |
|---|---|---|---|---|
| 1.12.2 | Java 8 | Java 8 | 14.23.5.2864 | `LegacyWirelessClientProbe` |
| 1.6.4 | Java 21 / Java 8 compiler, Java 7 bytecode | Java 7; installer uses Java 8 | 9.11.1.1345; build APIs 9.11.1.960 | `Legacy164WirelessClientProbe` |

`run_stock_client.py` shares checksum-verified downloads, native-library extraction and artifact checks. Set `PL4_CLIENT_MC=1.6.4` for the older profile; the default remains 1.12.2. It validates the native artifact ZIP digest, its recorded source commit and its production JAR hash before launching. Remapped 1.6.4 test-mod output is selected separately and checked for its expected harness class.

The workflows freeze the qualified source, artifact ID and archive checksum. To qualify a new payload, run the exact native gate first and update all three inputs together. Artifacts retain for 14 days; expired artifacts require a fresh native qualification. A historical report does not certify later code.

The probes check actual mouse withdrawals, deposits and paging; item conservation; stale-view rejection; native packet encode/decode across a signed-byte revision; four GUI scales; and a tooltip capture. World-display checks verify private projection, explicitly published network readings and clearing those readings on hide. The isolated profile disables vanilla tutorial overlays and focus-loss pausing so captures and server ticks continue under the virtual display. Product settings are not changed.

A passing workflow is necessary but visual evidence must also be reviewed. Store the workflow/source/artifact/JAR hashes and the exact inspected image names in `docs/acceptance`; update each target's `BUILD_STATUS.json` only within the proven scope. Preserve failures as failures.

This does not certify dedicated multiplayer, account authentication, installed optional providers, arbitrary user worlds, every display/reader/editor mode, other resolutions or sustained performance. RC publication clearance remains separate.

The current search probes also require case-insensitive filtered rows, caret/Delete editing, bounded Backspace clearing, no-match emptiness, stale pre-search replay rejection, and the native keyboard-repeat lifecycle (enabled in the search window and reset after closing). Both exact production hashes, harness hashes and ten inspected captures per port are recorded separately. The two older protocols retain a 32-printable-ASCII-character query bound and physical per-slot rows; grouping remains open. Query text is rendered outside item lighting so native UI text remains readable.
