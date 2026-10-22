"""Keep automatic publication steps disabled while branch metadata says development."""
from pathlib import Path
import argparse
import re

GUARD = "env.PL4_ALLOW_PUBLICATION == 'true'"
READ = '''      - name: Read publication maturity from exact checked-out source
        shell: bash
        run: |
          python - <<'PY'
          import json, os
          from pathlib import Path
          status = json.loads(Path('BUILD_STATUS.json').read_text())
          allowed = status.get('release_channel') != 'development' and not status['version'].endswith('-dev')
          with open(os.environ['GITHUB_ENV'], 'a') as output:
              output.write('PL4_ALLOW_PUBLICATION=' + str(allowed).lower() + '\\n')
          PY
'''


def guarded(text):
    if GUARD in text:
        return text
    groups = re.split(r"(?=^      - )", text, flags=re.M)
    if not any("gh release " in group or "tools/upload_curseforge.py" in group for group in groups):
        return text
    out = []
    for group in groups:
        if "gh release " in group or "tools/upload_curseforge.py" in group:
            match = re.search(r"^        if: (.+)$", group, re.M)
            if match:
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
                or path.name in {"port-26.yml", "publish-port-26.yml"}):
            continue
        before = path.read_text()
        after = guarded(before)
        if after != before:
            path.write_text(after)
            print("GUARDED", path)


if __name__ == "__main__":
    main()
