# Foundations PL4 0.0.1a.R16 — Transfer Node parity/hardening

R16 follows the R15 final-alpha candidate after review found that the bounded transfer engine could move resources between Transfer Nodes but did not let ordinary PL4 Nodes participate as the passive resource endpoints used by Practical Logistics 2-style networks.

Normal Nodes now expose their attached item/fluid/FE capabilities to the transfer pool. REMOVE / EXPORT drives resources out of its local target and prefers explicit ADD peers before passive Nodes. ADD / IMPORT fills its local target from explicit REMOVE peers or passive Nodes. Normal Node to normal Node never moves anything without a Transfer Node driver.

ADD / REMOVE is deliberately explicit-peer only for this alpha. The historical PL2 directional channel/filter UI is not fully restored, so letting a bidirectional node automatically drive every passive endpoint would create ambiguous loops. A same-cycle receive fence also prevents newly inserted resources from being immediately re-extracted during the same transfer run.

The existing simulate-before-extract and persistent escrow model is retained for items, fluids and FE. EU/J telemetry is still read-only and is not converted into FE transport.

Eight new native item-path GameTests raise the registered total from 106 to 114. Native fluid/FE acceptance still requires actual compatible tanks and batteries in the user's environment.

No save fields, multipart slots, recipes or packet protocol change in R16.
