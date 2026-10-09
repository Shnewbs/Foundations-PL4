# Foundations PL4 - Minecraft 1.15.2 feature port

Target: Forge 31.2.62, Java 8 runtime output built from Java 17 source. Version 0.2a-port.1.

This branch carries the full modern-derived feature implementation from the tested 1.16.5
port: multipart cables and topology, readers/displays/editor, wireless network item storage,
transfer routing, ownership, persisted escrow and the field guide. It is not the reduced
legacy transport subset. Exact 1.15.2 API adaptation and installed-runtime validation are
in progress; source presence is not completed feature acceptance.

Build-time JvmDowngrader conversion retains the minimal relocated JDK compatibility helpers,
license and matching upstream sources. No runtime agent is used. Only a successfully tested
converted, reobfuscated runtime may be released; unconverted intermediates are not installable
release files. The full 191-scenario suite is retained in a separate test module.

**Further API testing is still required.** Client graphics, optional-provider combinations,
multiplayer and sustained performance acceptance remain separate. No full PL2 parity claim.
