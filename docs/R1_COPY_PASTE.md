# 0.0.2a.R1 - display copy and paste

The display editor now supports `Ctrl+C` and `Ctrl+V` for the selected element. Paste creates a fresh element ID, applies the existing four-pixel deterministic offset, preserves the full typed/style specification, and sends it through the same revision-fenced server add transaction as the duplicate action.

The clipboard is intentionally local to the editor screen and never accepts serialized client data from the network. `Ctrl+D` remains a compatibility shortcut for paste/duplicate.
