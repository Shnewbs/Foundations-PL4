# 26.x port status

Development tracks: main/mc/1.21.1 and mc/26.3. mc/26.1 and mc/26.2 are dormant scaffolds, not separate planned development tracks.

The mc/26.3 port starts from released R1.6, including editor tools, immediate cable geometry, energy conversion and pushed EU input.

Pinned development target: Minecraft 26.3, NeoForge 26.3.0.23-beta, ModDevGradle 2.0.147, Java 25, Gradle 9.2.1, following the official NeoForge 26.3 MDK.

Goal: a common 26.x artifact tested on 26.1, 26.2 and 26.3; 26.4 follows when available. Metadata remains restricted to 26.3 while the native port is being compiled. Broader compatibility must be demonstrated through actual startup, linkage, server and client tests, not inferred by widening version ranges.

Port CI is validation-only and cannot publish a release. Current changes establish the toolchain and compile diagnostics; native migration is in progress. No 26.x runtime is released yet.

Source: https://github.com/NeoForgeMDKs/MDK-26.3-ModDevGradle
