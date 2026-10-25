"""Keep automatic publication steps disabled while branch metadata says development."""
from pathlib import Path
import argparse
import re

GUARD = "env.PL4_ALLOW_PUBLICATION == 'true'"
# Embed the shared policy so old port workflows need no new runtime files.
READ = '''      - name: Read publication maturity from exact checked-out source
        shell: bash
        run: |
          python - <<'PY'
          import json, os
          from pathlib import Path
          """Shared release maturity and explicit RC clearance policy."""
          import re


          def release_type(version):
              text = version.lower()
              if re.search(r"(?:^|[.\\-_])(?:dev|snapshot|nightly)(?:[.\\-_]|$)", text):
                  raise ValueError("Development snapshots are not qualified releases")
              if re.search(r"beta|(?:^|[.\\-_])rc\\d*(?:[.\\-_]|$)|\\db(?:[.\\-_]|$)", text):
                  return "beta"
              if re.search(r"alpha|preview|experimental|(?:^|[.\\-_])port(?:[.\\-_]|$)|\\da(?:[.\\-_]|$)", text):
                  return "alpha"
              return "release"


          def is_rc(version):
              return bool(re.search(r"(?:^|[.\\-_])rc\\d*(?:[.\\-_]|$)", version, re.I))


          def require_publication(status, minecraft=None, version=None):
              if not isinstance(status, dict):
                  raise ValueError("Missing exact-source build status")
              actual = status.get("version", "")
              if not actual or (version is not None and actual != version):
                  raise ValueError("Build status and release version disagree")
              if minecraft is not None and status.get("minecraft") != minecraft:
                  raise ValueError("Build status and exact Minecraft target disagree")
              maturity = release_type(actual)
              if status.get("release_channel", "").lower() == "development":
                  raise ValueError("Development channel is not publishable")
              if is_rc(actual) and status.get("rc_publication_cleared") is not True:
                  raise ValueError("RC publication has not been explicitly cleared")
              return maturity

          status = json.loads(Path('BUILD_STATUS.json').read_text())
          try:
              require_publication(status)
              allowed = True
          except ValueError as error:
              allowed = False
              print('Publication held:', error)
          with open(os.environ['GITHUB_ENV'], 'a') as output:
              output.write('PL4_ALLOW_PUBLICATION=' + str(allowed).lower() + '\\n')
          PY
'''


def guarded(text):
    # Replace obsolete maturity readers as well as adding missing guards.
    text = re.sub(r"^      - name: Read publication maturity from exact checked-out source\n.*?(?=^      - |\Z)", "", text, flags=re.M | re.S)
    groups = re.split(r"(?=^      - )", text, flags=re.M)
    if not any("gh release " in group or "tools/upload_curseforge.py" in group for group in groups):
        return text
    if "uses: actions/checkout@" not in text:
        raise ValueError("Frozen publisher has no current-source checkout; review manually")
    out = []
    for group in groups:
        if "gh release " in group or "tools/upload_curseforge.py" in group:
            match = re.search(r"^        if: (.+)$", group, re.M)
            if match and GUARD in match[1]:
                pass
            elif match:
                condition = match[1].strip()
                if condition.startswith("${{") and condition.endswith("}}"):
                    condition = condition[3:-2].strip()
                group = group[:match.start()] + f"        if: {GUARD} && ({condition})" + group[match.end():]
            else:
                lines = group.splitlines(keepends=True)
                lines.insert(1, f"        if: {GUARD}\n")
                group = "".join(lines)
        if "uses: actions/checkout@" in group:
            lines = group.splitlines(keepends=True)
            end = len(lines)
            for index, line in enumerate(lines[1:], 1):
                if line.strip() and len(line) - len(line.lstrip()) < 8:
                    end = index
                    break
            lines.insert(end, READ)
            group = "".join(lines)
        out.append(group)
    result = "".join(out)
    # Workflow maintenance does not change the runtime and should not rerun
    # every native suite. Runtime/source pushes still take the normal gates.
    if "  push:\n" in result and "    paths:" not in result and "    paths-ignore:" not in result:
        result = result.replace("  push:\n", "  push:\n    paths-ignore: ['.github/workflows/**']\n", 1)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    args = parser.parse_args()
    for path in (args.root / ".github/workflows").glob("*.yml"):
        # Historical cross-version controllers and release-receipt reconciliation
        # operate on immutable releases, not this branch's current runtime source.
        if not (path.name == "release.yml" or re.fullmatch(r"forge.*native\.yml", path.name)
                or path.name in {"forge-port.yml", "legacy112-native.yml", "legacy164-native.yml", "port-26.yml"}):
            continue
        before = path.read_text()
        after = guarded(before)
        if after != before:
            path.write_text(after)
            print("GUARDED", path)


if __name__ == "__main__":
    main()
