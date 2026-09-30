# R7 upstream comparison and implementation boundary

Reference is SonarSonic Practical-Logistics-2 commit `4772196103d35c78c33f03c288a7b47aac267197`. Files were inspected via public source during this revision. Original upstream license and notices remain in the package.

- Separate display face slots: `src/main/java/sonar/logistics/base/utils/slots/EnumDisplayFaceSlot.java`.
- Reader input/output distinction and same-face display lookup: `src/main/java/sonar/logistics/core/tiles/readers/base/TileAbstractReader.java`, especially `canConnect`, `onFirstTick`.
- Reader/display appearance: `src/main/java/sonar/logistics/core/tiles/readers/base/BlockAbstractReader.java`, `onScreenChanged`.
- Actual retained model: `src/main/resources/assets/practicallogistics2/models/block/readerwithdisplay.json`; its base begins at y=1, leaving the thin screen layer free.
- Internal/external endpoint selection: `src/main/java/sonar/logistics/core/tiles/connections/data/handling/CableConnectionHelper.java`.
- Meaning of NETWORK versus VISUAL: `src/main/java/sonar/logistics/api/core/tiles/connections/EnumCableConnection.java`.
- Node target side: `src/main/java/sonar/logistics/core/tiles/nodes/node/TileNode.java`.

Browse the pinned tree at https://github.com/SonarSonic/Practical-Logistics-2/tree/4772196103d35c78c33f03c288a7b47aac267197 . Raw source is available under https://raw.githubusercontent.com/SonarSonic/Practical-Logistics-2/4772196103d35c78c33f03c288a7b47aac267197/ followed by each path.

R7 reimplements the listed compact/neighbor/typed-visibility paths in the current standalone host system; it does not embed old MCMultiPart or Sonar Core code. New host indexing uses 0–5 ordinary, 6 centre, 7–12 display, because R6's saved kind/face records permit reindexing without identity changes.

A conservative R7 rule chooses one input for a face endpoint: an existing local centre takes precedence, including a blocked/wrong-family centre. It cannot be bypassed by an external input. Exposed endpoints need an unobstructed back corridor and a compatible exposed cable port. This is an explicit current implementation choice, not a claim that every original multipart exception has been copied.

R7 supports compact pairing for the flat normal/mini/large panels. Holographic components receive separate storage slots but their original mounting/projection/editor parity is not completed. Covers/edge/corner slots, arbitrary multipart providers, every legacy channel/subnetwork rule, full original GSI selection/actions, integrations and interactive wireless storage remain on PARITY.md. Current remote display selection is the PL4 text/bar reader-selection system, not the original full GSI provider system.
