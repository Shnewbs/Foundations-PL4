# R1 alignment and distribution

Use Arrange [A] in the world display editor. Shift-click adds elements to the current-page selection. Choose one of six alignment controls, or one of two equal-spacing controls.

A single element aligns to canvas edges/centres. Multiple elements align within their existing combined bounds. Distribution needs at least three elements, sorts them along the chosen axis, retains the outer anchors and rounds intermediate positions to whole pixels (gaps may differ by one pixel). It rejects insufficient space instead of introducing overlap.

All operations are one atomic server transaction fenced by the current layout revision. Ownership, reach and joined-canvas permissions use the existing packet gate. Unknown/missing/duplicate identities, other-page selections and overflow are rejected. Undo restores the previous layout through the existing validated snapshot transaction.

Visual acceptance: test a single normal display and a joined Large Display; align single and multiple elements, distribute uneven sizes, undo/redo and reload the world. Test no selection and fewer than three selections, and confirm that another page retains its layout.
