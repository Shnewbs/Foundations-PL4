# Foundations PL4 0.0.1a.R7 — Compact readers, display slots and typed connections

**SOURCE CANDIDATE. No compiled R7 runtime JAR is bundled.**

Target: Minecraft 1.21.1 / NeoForge 21.1.250 / Java 21. Baseline: exact recovered R6 Display-Energy source. No separate Sonar Core, MCMultiPart or Foundations Framework is required.

## Why this revision exists

The R5/R6 implementation required every device to have a cable centre in its own block. It also stored readers and displays in the same six face slots. That restricted valid mounting/connection paths and forced the cable detour shown in the user's screenshot. R7 corrects those specific paths; it is not a new claim of complete historical 1:1 parity.

## Compact mounting

Each host now has six ordinary face slots (0–5), one cable centre (6) and six display slots (7–12). A normal, mini or large panel can occupy the same face as a reader. The original `readerwithdisplay` mesh and matching shorter collision shape are used while the panel is attached. Removing the panel restores the ordinary reader skin. Other overlapping combinations remain rejected; separate logical slots do not disable collision checking.

Click a reader's outward face with the panel item to mount the panel in that SAME host and face. A fresh paired panel faces outward. Existing saved item settings are retained when an Operator-removed item is reused; explicitly choose Front: outward if that saved panel has an inward setting.

Right-click the panel to configure the display. **Shift-right-click it with an empty hand to configure the reader underneath.** Sneak-using the Operator continues to remove the targeted component; a front hit removes the panel, not the hidden reader. The item count is not consumed until placement succeeds.

## Corrected wiring contract

A reader's front is its mounting face. Its network input is behind it; its visual output is in front.

```text
Machine -- Node -- DATA cable -- [ Reader | Display ]
                              compact same-face pair
```

A device uses a compatible cable centre in its own host when one exists. A **bare endpoint can instead connect to an exposed cable in the adjacent block behind it**. The endpoint does not receive a free/invisible cable centre. Actual connector stems fill the exposed endpoint's gap; collision, selection and rendering use the server's derived connection state.

A cable centre present in the host is authoritative: a disabled or incompatible local centre does not fall back to an outside input through that centre. An intervening face part, disabled cable port or incompatible cable family prevents the connection. Sideways contact is not a reader port. These conservative endpoint choices are documented rather than labelled exhaustive upstream topology parity.

For remote displays, a cable on a reader's exposed front can carry that reader's **visual data** to a separate display bus. That visual link NEVER joins the reader's machine network to the output bus. A Node or Transfer Node on that output bus does not gain access to the input bus's inventories. A screen already mounted over the front blocks a physical cable through its face. A panel immediately across the reader's front block boundary can also receive the direct visual feed when no intervening ordinary face part claims that connection.

Directly paired panels see their attached reader. Other cable-mounted panels can still select available readers on their own bus, including valid visual exports. Leave **Reader name** blank for automatic selection, or set it to the available reader's unique name/UUID. A saved selection naming a different reader is not silently erased; clear or change it deliberately.

All screen sizes use the same reader-source selection path. Nonrectangular large panels remain individually usable instead of retaining stale shared rows. Existing same-owner, same-plane, same-front 16x16 rectangular joining remains; joining panels does not join their electrical networks.

## Existing worlds and update safety

R6 persists each part's kind and face, not a numeric map key. R7 reconstructs display entries in slots 7–12 while preserving part UUIDs, owner, labels, selected reader, front setting, elements, energy selection, links and transfer escrow. No world-wide reset or automatic remount is performed. Existing cable-based working layouts can remain until deliberately simplified.

Host save schema becomes 2 and payload protocol becomes 2. Client and server must run the same R7 build. Loading an R7 world back into R6 is NOT safe: R6 has the old slot scheme and seven-entry load limit. Source rollback is not world rollback. Test on a copied/backed-up world and retain its matching old mod.

The updater changes only its listed source delta. It checks starting version and affected-file hashes, backs up under `.foundations_update_backups`, and offers build/test choices. It does not install a JAR, modify a world, delete runtime state or install KubeJS scripts.

## Preserved scope

R4 GUI layering, R5 hammer/recipes and R6 display-facing/energy-reader functionality remain. All 113 original PNGs and 35 original recipes are byte-identical to R6. The read-only FE/Mekanism-J/GTCEu-EU telemetry and existing KubeJS JSON recipe example remain unchanged; this revision adds neither native energy transfers nor a dedicated KubeJS scripting API.

The topology remains cached, rebuilt on lifecycle/config/part changes, not discovered by the renderer every frame. The new planner looks up six neighbor positions instead of scanning every pair of loaded parts. No in-game FPS, memory, MSPT or large-server benchmark is claimed.

## Verification boundary

The actual dependency-free planner passed 9,665 assertions, including six orientations, all 64 port masks, compact pairs, external endpoints, visual/network isolation, wrong-family/blocked-port rejection and a 4,096-cable chain. Tests also reject three deliberately reintroduced regressions. Retained R5/R6 rule tests, Java syntax parsing, asset checks and GUI-layer guards pass.

The full Gradle attempt failed at the distribution download with `UnknownHostException: services.gradle.org`, BEFORE Minecraft/NeoForge API compilation. R7 has no compiled runtime JAR here. The 58 native GameTests, real rendering/gameplay, optional-mod integration and Windows updater execution are not run. Python updater simulations are not execution of PowerShell. See VALIDATION.md and R7_ACCEPTANCE.md.
