# Foundations PL4 0.0.1a.R15 acceptance

Use a copied world and matching R15 client/server builds.

## Holographic displays

1. Test a normal projector on a wall, floor and ceiling. Its canvas should sit visibly clear of the projector and adjacent multipart cable hardware.
2. Repeat with Advanced. Its plane should sit farther out than the normal version.
3. Walk around each plane. Text must remain readable from both sides without mirroring.
4. For floor/ceiling mounts, cycle all four Settings > View directions and verify text stays upright.
5. Open the editor close to the projection and verify HUD help, toolbar interaction and element positioning still work.

## Guide

Read First inventory monitor, From list to block icon, Energy monitor, Large display, Hologram and Hammer tutorials. The tone should read like a human walkthrough while retaining the concrete checkpoints.

## Regression

- Hammer GUI, KubeJS recipe and hopper/pipe automation.
- FE and EU energy readers.
- Large joined displays and multipart display hosts.
- Custom item/block/fluid/bar elements; no z-fighting regressions.
- Broken default parts stack; configured Operator-saved parts preserve meaningful settings.
- Run all 106 GameTests.

If these pass, archive R15 as the 0.0.1a baseline and begin 0.0.2a.
