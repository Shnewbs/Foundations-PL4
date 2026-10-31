# External-only build, test and publication policy (2026-10-31)

This repository's 32 GitHub Actions workflows are retained as **manual-only historical tooling**. Their automatic `push`, `pull_request`, `release`, `schedule`, and `workflow_run` triggers were removed on October 31, 2026. **Do not invoke GitHub Actions** for PL4 hourly builds, tests, releases or CurseForge uploads. ChatGPT's independent hourly work is not a GitHub Actions job.

## Source and branch discipline

- Work directly on existing `main` and exact-version `mc/<version>` branches. No new feature branches, force pushes, tag rewrites or automatic branch merges.
- Keep the existing 24-target ledger and separate acceptance per exact Minecraft/loader/Java combination. 26.4 remains a compatibility watch until a supported official NeoForge coordinate exists.
- Coordinate with `docs/RC_AUTOMATION_STATE.json`'s guarded writer lease; do not overwrite a lease owned by another worker.
- Source commits are not release qualification. Do not reuse native receipts from an earlier SHA to certify a changed candidate.

## Local/external validation

1. On a dependency-capable **external/local** builder, check out the exact source commit and run `python3 tools/run_offline_checks.py`, `python3 -m unittest discover -s tools -p 'test_*.py'`, and any component-specific tests.
2. Run the exact branch's Gradle wrapper with its pinned JDK and loader: `./gradlew --no-daemon --console=plain clean build runGameTestServer` for supported native-GameTest branches. Use `tools/ports/validate_rc_candidate.py` and the version-specific runner for ports whose native test harness differs.
3. Execute the independent **installed** optional-provider probe against the exact packaged production JAR, with clean process exit, nonce and file-hash evidence. Separately qualify real-client rendering, authenticated multiplayer, arbitrary-world migration and sustained TPS/heap; do not equate offline or GameTest success with RC clearance.
4. Capture source SHA, loader/JDK, JAR SHA-256, native logs and acceptance receipts. Keep each artifact bound to its exact source and target.

## Direct publication (only after all release gates pass)

- Create an immutable exact-version tag, upload the verified runtime JAR, source archive, release notes and SHA-256 manifest to a GitHub Release using an authenticated **local GitHub CLI/API client**, not Actions. Confirm each uploaded asset's name, size and checksum against the local candidate before considering it delivered.
- For supported modern ports, use `tools/direct_curseforge_upload.py` with `CURSEFORGE_API_TOKEN` from the local process environment. Follow `docs/DIRECT_CURSEFORGE_UPLOAD.md`. CurseForge project: `1716767`. Verify the returned file ID, target version and JAR SHA-256; an accepted API receipt does not establish moderation approval.
- Never upload an unverified JAR or move an existing release tag. Do not delete a local candidate until the matching GitHub asset and CurseForge receipt (when applicable) are verified, or a verified replacement is retained. Preserve unique world saves, test logs and evidence.
- A failed or ambiguous upload must be reconciled against the destination before retrying, to avoid duplicate publications.

## Current execution limitation

The ChatGPT execution container used on October 31, 2026 cannot resolve `github.com` or `services.gradle.org`, has no cached Gradle 9.2.1 distribution/dependencies, and cannot perform a native build or independent installed-provider run. The connected GitHub source API can still push text changes. Until a dependency-capable local builder is available, release publication remains blocked; do not re-enable Actions as a workaround.
