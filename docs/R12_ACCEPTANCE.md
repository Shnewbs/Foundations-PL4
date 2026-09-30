# Foundations PL4 0.0.1a.R12 — final alpha acceptance

Use a copied/backed-up world and matching R12 client/server builds.

## 1. Native build

Run `gradlew.bat --no-daemon --console=plain clean build` with Java 21. Expected runtime artifact:

`build/libs/FoundationsPL4-1.21.1-0.0.1a.R12.jar`

## 2. Reproduce the reported bar

Open the same joined Large Display and energy-bar layout that showed striped flicker in R11. Verify:

- dark bar background is stable;
- coloured fill is stable across the entire filled region;
- frame and `stored / capacity EU` label do not shimmer;
- editor selection frame stays above saved content;
- no toolbar hitbox regression.

Inspect head-on, close, far and from a shallow side angle.

## 3. Other display types

Check one Item, Block, Inventory grid, Fluid tank/grid, Text and AUTO_LIST display. Quantities must remain above pictures and editor controls above quantities. No content should visibly float far off the screen from the side.

## 4. Joined canvas persistence

Move and resize an element into the far side of a joined Large Display, close/reopen editor, save/quit/reload. Position, size, page and shared canvas dimensions must remain stable.

## 5. Regression sweep

Recheck compact reader+display mounting, monitor expansion, hologram viewing direction, hammer GUI, energy reader and Field Guide Welcome/tutorial navigation.

## 6. GameTests

Run `gradlew.bat --no-daemon --console=plain runGameTestServer`. All 103 registered tests must pass before the `0.0.1a` baseline is archived.
