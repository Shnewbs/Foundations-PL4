# Foundations PL4 0.0.1a.R16 — Transfer Node endpoint hardening

R16 restores ordinary `Node` parts as passive transfer endpoints while keeping Transfer Nodes as the only active movers.

## Routing contract

- `PASSIVE`: no movement.
- `ADD / IMPORT`: fills the target attached to this Transfer Node. It can import from explicit `REMOVE` / `ADD-REMOVE` Transfer peers and from ordinary Nodes on the same data-network component.
- `REMOVE / EXPORT`: drains the target attached to this Transfer Node. It prefers explicit `ADD` / `ADD-REMOVE` peers, then ordinary Nodes.
- `ADD / REMOVE`: explicit-peer only in this alpha. It does not blindly push to or pull from ordinary Nodes. This bounded rule remains until PL2's directional channel/filter UI is restored.
- Normal Node -> normal Node never moves resources by itself.

Items and fluids respect Transfer Node filters on the side controlled by that node. Transfer-node priority orders explicit peers; equal-priority endpoints rotate deterministically. Explicit Transfer peers win ties over passive ordinary Nodes.

## Safety

Every resource path simulates destination capacity before extraction. If real insertion accepts less than simulation promised, the driving Transfer Node retains the remainder in its persistent `pendingItem`, `pendingFluid` or `pendingEnergy` escrow. A target that received a resource during the current run is fenced from immediate re-extraction during the same run.

No chunks are force-loaded. Existing per-cycle limits remain 64 items, 1000 mB and 10000 FE by default.

## Energy boundary

Transfer Nodes move FE through NeoForge energy capabilities. R16 does not convert or transport native GregTech EU or Mekanism Joules. Those systems remain read-only Energy Reader telemetry in this alpha.

## Save/protocol compatibility

R16 adds no persisted fields. Host schema remains 2, multipart slots remain 13 and payload protocol remains 4.
