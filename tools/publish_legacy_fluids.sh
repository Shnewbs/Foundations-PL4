#!/usr/bin/env bash
set -euo pipefail
set -euo pipefail
version='0.2a-legacy-preview.2'; tag="mc1.12.2-v$version"
mkdir -p release-files
cp "build/libs/FoundationsPL4-1.12.2-$version.jar" "build/libs/FoundationsPL4-1.12.2-$version-sources.jar" release-files/
git archive --format=zip --output="release-files/FoundationsPL4-1.12.2-$version-source.zip" HEAD
cp run-legacy/legacy-scenarios.json verification-logs/native.log release-files/
git rev-parse HEAD > release-files/SOURCE_COMMIT.txt
(cd release-files && sha256sum *.jar *.zip *.json *.log SOURCE_COMMIT.txt > SHA256SUMS.txt)
if git ls-remote --exit-code --tags origin "refs/tags/$tag" >/dev/null 2>&1; then echo 'Existing release is immutable; use a new revision'; exit 1; fi
git tag "$tag"; git push origin "$tag"
gh release create "$tag" release-files/* --verify-tag --prerelease --title "Foundations PL4 1.12.2 legacy preview 2 (item and fluid transport)" --notes-file "docs/releases/$version.md"

