#!/usr/bin/env bash
# Forge 1.6.4's installer installs into CWD and ignores a trailing destination.
set -euo pipefail
root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
mkdir -p "$root/run-legacy"
cd "$root/run-legacy"
"${JAVA_HOME_8_X64:?Java 8 is required for this installer}/bin/java" -jar "$root/forge-installer.jar" --installServer
