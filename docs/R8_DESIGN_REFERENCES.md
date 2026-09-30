# R8 design and API references

The source baseline is the exact packaged FoundationsPL4-0.0.1a.R7-Multipart-Connections-SOURCE.zip. All behavior claims are scoped to the classes and tests supplied here; old upstream parity work remains recorded in UPSTREAM.md / R7_UPSTREAM_CONTRACT.md.

The Foundations Soil R4_FIELD_GUIDE_TESTING.md and R5_FIELD_GUIDE_TESTING.md supplied in the user's Library establish the requested family design: two-page leather/parchment field manual, binding, four left icon tabs, safe titles, integrated search, crisp world-only background blur. R8 reauthors PL4's shell/assets; it does not import or modify Soil's source or require its runtime.

Official NeoForge 1.21.1 screen documentation, consulted for native GUI coordinates, blitSprite/scissor and gui.scaling nine-slice metadata:
https://docs.neoforged.net/docs/1.21.1/gui/screens/

The inherited Screen background ordering used by this project follows the existing R4 guarded renderBackground hook. Generic documentation examples do not replace version-specific native acceptance. The original textures/recipes and license notices remain preserved. New chapters are written for the actual standalone port, not copied from the outdated PL2 guide language entries.
