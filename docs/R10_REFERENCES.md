# R10 implementation references

- Baseline: attached FoundationsPL4-0.0.1a.R9-PL2-Display-Editor-SOURCE.zip. Only this source is an updater baseline; earlier records are historical.
- Visual direction: the user's main-hand, dropped-panel and Field Guide screenshots. Retain released PL2 mesh/UV/texture identity and the separate Foundations guide design. The promotional image and unfinished PL3 are not replacement model references.
- NeoForge 1.21.1 model format: https://docs.neoforged.net/docs/1.21.1/resources/client/models/ . Item `display` contexts can define translation/rotation/scale independently of placed models; `gui_light` is item presentation. Reviewed during R10 implementation.
- Pinned PL2 large-display model: https://raw.githubusercontent.com/SonarSonic/Practical-Logistics-2/4772196103d35c78c33f03c288a7b47aac267197/src/main/resources/assets/practicallogistics2/models/block/largedisplayscreen.json . Existing mesh, UVs and original textures are preserved in world; the R10 item representation centres a separate copy.
- Upstream author credit, pinned manifests and licenses remain in NOTICE, LICENSE, LICENSE-SonarCore and the earlier audit/reference documents. No new dependency or third-party asset is introduced by R10.

The online references document the format and original asset, not native acceptance of this port. Geometry/pose envelope tests use the packaged item JSON and an independent rotation matrix; they do not capture Minecraft camera placement, glint, shader or lighting output.
