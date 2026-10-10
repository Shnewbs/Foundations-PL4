# Historical Java 8 parity migration

The one-time coordinator workflow was retired after the independently versioned 1.16.4
and 1.16.5 releases. These preparation templates record the migration history; they are
not current release entry points and must not be reapplied over subsequent target fixes.

Current runtime conversion and verification live in each target branch under
`tools/java8/`. Native build/release workflows are `.github/workflows/forge-native.yml`
on `mc/1.16.4` and `mc/1.16.5`. They verify the packaged Java 8 output on installed Forge,
retain exact sources and checksums, and refuse to overwrite changed published runtime inputs.

Minecraft 1.16.4 requires the explicit ModLauncher 8.1.3 launch profile documented on its
branch and shipped with its release. It is not stock Forge 35/current Java 8 acceptance.
Minecraft 1.16.5 uses the tested stock Forge 36.2.42 library set.

Client visuals, installed optional APIs, multiplayer and sustained performance testing
remain distinct from the native-server gates. Further API testing is still required.
