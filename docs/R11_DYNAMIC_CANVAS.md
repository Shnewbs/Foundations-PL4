# Foundations PL4 0.0.1a.R11 — Dynamic Large Canvas + PL2 Editor Polish

**SOURCE CANDIDATE — no compiled R11 runtime JAR is included.** Target: Minecraft 1.21.1, NeoForge 21.1.250, Java 21. Baseline: exact delivered R10 source.

## Why this revision exists

The R10 screenshots showed a large joined display physically spanning many monitor tiles while the custom content and editor still behaved like a single fixed 248x120 board. The result was a small/letterboxed design surface and controls crowded against the wiring side. R11 makes the joined rectangle itself the logical workspace.

## Dynamic joined-display canvas

Large Display rectangles derive their logical width/height from the actual connected rectangle at a fixed bounded density (`180` logical units per usable block). A 6x2 board therefore receives much more horizontal design space than a 2x2 board while matching the physical aspect ratio instead of stretching one legacy canvas.

Ordinary Display Screens, Mini Displays and holographic canvases retain their established logical coordinate systems. The change is scoped to joined Large Displays.

### Migration

R9/R10 typed elements were stored in a 248x120 coordinate space. R11 persists the coordinate-space width/height with the shared display settings. When the joined rectangle changes size, each element's X/Y/width/height is scaled proportionally from the old logical space into the new one. Relative position and relative size are preserved, clamped to the new canvas, and the migrated settings are mirrored to the entire rectangle with a new layout revision.

Growing or shrinking the display can therefore change the absolute logical coordinates without arbitrarily moving an element to another visual region. Downgrading after this migration is unsafe without restoring a matching world backup.

## Editor polish

The in-world editor now uses the controller's persisted dynamic width/height for creation, dragging, resizing and page controls. The eight PL2-style toolbar buttons are anchored just inside the visible left edge of the monitor instead of being placed outside the display where they could overlap cables/readers. Page arrows sit along the actual lower edge of the expanded canvas.

Pointer picking still uses the captured model-view-projection matrix from the renderer. R11 does not substitute guessed world axes.

## Rendering

The Large Display renderer uses the same dynamic logical dimensions and scale as the server-side layout space. AUTO_LIST uses the full dynamic region; CUSTOM elements and editor overlays share that same surface. Existing shallow depth ordering remains: display surface -> pictures -> counters -> editor overlay.

## Preserved systems

R4 GUI layer ordering, R5 hammer, R6 FE/J/EU read-only telemetry and KubeJS recipe compatibility, R7 compact reader/display wiring, R8 expansion/hologram/Field Guide, R9 typed graphical elements, and R10 item transforms/tutorial onboarding remain.

## Verification boundary

R11's dependency-free dynamic-canvas/migration/editor rules pass 786 assertions and all retained offline suites pass after the intentional large-canvas geometry change. Java 21 AST parsing passes 71 production sources. Five new native GameTests are registered but were not executed. Native Gradle execution stops before compilation because `services.gradle.org` cannot resolve in this environment. No graphical client acceptance is claimed.
