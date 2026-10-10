# 1.16.2 build/runtime separation

This target compiles modern source with Java 17 and converts the mod/fixtures to Java 8 before Forge reobfuscation. Do not launch it on Java 17 merely because Java 17 builds it.

Use Forge 33.0.61, Java 8 and the explicit ModLauncher 8.1.3 profile supplied in `tools/java8/forge35_profile.py`. The profile filename is historical; its contents validate the exact 1.16.2 target. It does not patch Forge or supply a client launcher profile.

Installed scenarios and production-only startup must pass before publication. Native pass status is in attached CI summaries, not inferred from compilation. Further API testing is still required.
