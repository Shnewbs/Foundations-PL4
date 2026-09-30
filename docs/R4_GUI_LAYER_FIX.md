# R4 — GUI background and tooltip layer correction

Date: 2026-09-28  
Baseline: Foundations PL4 0.0.1a.R3 TextureFix  
Delivery: 0.0.1a.R4 source-only fix + portable R3-to-R4 source updater

## Diagnosis

The supplied screenshot showed sharp vanilla Data/Settings/Done buttons above a blurred PL4 title, status, row area and panel. In the inspected R3 source, both `PartScreen.render` and `GuideScreen.render` painted custom content first, then called `super.render`.

The NeoForge 1.21.1 `Screen.java` patch includes the actual inherited method entry:

```java
public void render(GuiGraphics ..., int ..., int ..., float ...) {
    this.renderBackground(...);
```

That is why simply putting `super.render` last blurs earlier custom pixels: the inherited method still has a background pass to perform. This is a pass-order problem, not a reason to enlarge text shadows or alter global video settings.

Primary reference inspected: https://raw.githubusercontent.com/neoforged/NeoForge/1.21.1/patches/net/minecraft/client/gui/screens/Screen.java.patch

The general layering guidance is also documented at https://docs.neoforged.net/docs/1.21.1/gui/screens/ . Its generic rendering example is not a substitute for checking the actual version-specific `Screen.render` behavior.

## Source changes

| File | Change |
|---|---|
| `client/PartScreen.java` | Paint panel, header, rows and field labels in the background hook, after the native background. Keep the inherited widget render. Defer row tooltips until after widgets; reset the pending tooltip every frame. |
| `client/GuideScreen.java` | Paint guide content after the native background and inherit the normal widget-render sequence. |
| `tools/VerifyScreenLayers.java` | JDK source-parser guard for both screens' background/widget/tooltip order. No Minecraft startup or type resolution. |
| `tools/test_screen_layers.py` | Eleven positive/negative checks for the guard using the production screen source. |
| `gradle/verify-screen-layers.gradle` | Run the guard as part of `check`/`build` once Gradle dependencies are available. |
| `build.gradle`, mod metadata | Advance the source revision to 0.0.1a.R4; target versions and dependencies remain the same. |

Intended per-frame order:

```text
native background (world blur/dimming)
PL4 panel, title, content and field labels
vanilla widgets
row tooltip
native deferred widget tooltip handling
```

The fix does not disable the user's background-blur setting, add a second blur pass, change depth offsets, or manually iterate/bypass Minecraft's widget list. It adds no threads, timers, network traffic, static GUI caches or new runtime dependencies. R3's existing GUI behavior outside these layering changes is retained.

## Preserved, not claimed complete

All R3 resources and all non-client-rendering production Java files are byte-identical. R3's atlas-path correction remains present. Texture/model file integrity is not equivalent to checking the block appearance in-game, and this patch does not implement original hammer animation, connected-screen geometry or full display editing.

## In-game acceptance after the first successful build

1. Open an Inventory Reader/Info Reader with background blur enabled. Title, status, rows, row-count footer and panel edges should be as sharp as the buttons; only the world behind may be blurred/dimmed.
2. Open Settings and a display's Layout tab. Labels and edit boxes should stay sharp. Existing scroll, apply, Done and Escape behavior should remain available.
3. Hover a data row near the bottom/right of the window. The tooltip should remain above the UI. Move off the row and switch tabs: no old tooltip should linger. Clicking a data row should still copy its key.
4. Open the PL4 Guide, change pages and scroll. Its text should remain sharp, with Previous/Next/Done on the normal widget layer.
5. Repeat in windowed/fullscreen modes and several GUI scales. Reopen each screen several times. Test with vanilla background blur disabled as well as enabled.
6. Check the previously broken item icons and placed parts on R3/R4 resources. The path correction is preserved, but texture/model visual parity still needs this client check.

## Build status

No R4 runtime JAR has been produced in this environment. The Gradle wrapper could not resolve `services.gradle.org` to download Gradle 9.2.1. The original R3 JAR has not been relabeled or patched in place. The Windows updater is provided to apply the source delta and run the actual build on a machine with the required dependencies/network access.
