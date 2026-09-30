# 0.0.2a.R1 - display undo/redo

The display editor now supports `Ctrl+Z` (undo), `Ctrl+Y` (redo), and `Ctrl+Shift+Z` (redo) for element edits.

The editor keeps a bounded (20-deep) client-side history of whole-layout snapshots. Before every mutating commit (add, update, delete, forward, backward, clear) it pushes the current element list onto the undo stack and clears the redo stack. Undo pops the previous snapshot, pushes the current one to the redo stack, and restores it; redo is the mirror operation. View-only actions (page navigation, mode switch) are intentionally excluded from history since they are not layout edits.

Restoring a snapshot goes through a new revision-fenced `replace` transaction rather than replaying individual add/delete edits:

- `LayoutTransactions.applyReplace` (in `core`, kept dependency-free like the rest of that package) validates the incoming element count against `MAX_ELEMENTS`, rejects duplicate IDs, and enforces the existing eight-reader-binding cap before accepting the whole list atomically.
- `PLPackets.editLayout` decodes the snapshot JSON (`ElementJson.encodeList`/`decodeList`) and re-checks reader visibility for every element in the snapshot, exactly like a normal add/update, before calling `applyReplace`.
- The `LayoutEdit` packet's value field was widened from 4096 to 65536 UTF-8 bytes to fit a full 32-element layout snapshot; the existing per-tick edit rate limit still applies.

Undo/redo is local to the editor screen and, like copy/paste, never trusts serialized client data without going through the same reader-visibility and structural validation as every other layout edit.
