# 0.0.2a.R1 - multi-selection editor slice

The display editor now supports additive selection with Shift-click. Selected elements retain visible outlines, and Delete removes the whole selection through one revision-fenced layout transaction.

The server validates every selected UUID and applies the deletion atomically, so a stale editor cannot partially delete a selection. Single-element selection and the existing duplicate tool remain unchanged. Grouping, clipboard transfer, and undo/redo remain subsequent R1 work.
