# Foundations PL4 R14 acceptance

1. Build with Java 21 and install matching R14 client/server JARs.
2. Open a Large Display editor while standing close: contextual help must remain readable in the HUD without needing to back away; only the editing player sees it.
3. Confirm the left monitor toolbar remains visible during edit mode.
4. Right-click through each layer: picker -> properties -> editor -> gameplay; Settings -> Data -> close.
5. Add an Inventory Grid: default Columns should be 3. Change to 1, 2, 4 and verify the mini preview and saved world grid both change. Repeat with Fluid Grid.
6. Open Palette, click multiple swatches, verify the preview and resulting hex field; type a custom hex afterward and verify it still saves.
7. Regression: all four resize corners, toolbar clicks, bar rendering, icon counters, dynamic large-screen positioning, NBT stackability, reader/display compact mount, hammer GUI and Field Guide.
8. Save/reload a copied world and verify R13/R14 layouts persist.
9. Run all 106 registered GameTests.
