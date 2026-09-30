# 0.0.2a.R1 - display text styles

This continuation implements the first self-contained slice of the roadmap's display/GSI parity work:

- per-element text scale from 25% through 400%;
- separate left, centre, and right text alignment controls;
- persisted line wrapping, bounded by the element width and height;
- rendering in both live canvases and the editor preview;
- persistence through part NBT and element JSON/packet data.

Existing elements and older worlds default to 100% scale and left alignment. Values are clamped at the data boundary, so malformed or out-of-range client input cannot create unbounded text geometry.

This is the first `0.0.2a.R1` continuation after the R17 freeze. The frozen `0.0.1a` native hologram and transfer acceptance gate remains unchanged.
