"""Prepare consistent release metadata; never modifies historical release notes."""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
VERSION = re.compile(r"\d+\.\d+(?:\.\d+)?[ab](?:\.R\d+(?:\.\d+)*)?")


def plan(root: Path, version: str) -> dict[Path, str]:
    if not VERSION.fullmatch(version):
        raise ValueError("Expected an alpha/beta version such as 0.2a or 0.2a.R1")
    notes = root / 'docs/releases' / (version + '.md')
    if not notes.is_file():
        raise ValueError('Write release notes before preparing a version')
    build = (root / 'build.gradle').read_text(encoding='utf-8')
    matches = re.findall(r"^version = '([^']+)'$", build, re.M)
    if len(matches) != 1:
        raise ValueError('Expected exactly one Gradle project version')
    old = matches[0]
    changes = {root / 'build.gradle': build.replace("version = '" + old + "'", "version = '" + version + "'", 1)}
    guide_path = root / 'src/main/resources/assets/foundations_pl4/guide/en_us.json'
    guide_text = guide_path.read_text(encoding='utf-8')
    guide = json.loads(guide_text)
    old_edition = guide['edition']
    suffix = 'Foundations PL4 ' + old
    if not old_edition.endswith(suffix):
        raise ValueError('Guide edition is not aligned with the current project version')
    edition = old_edition[:-len(old)] + version
    field_path = root / 'docs/FIELD_GUIDE.md'
    field = field_path.read_text(encoding='utf-8')
    if old_edition not in field:
        raise ValueError('Markdown guide edition does not match the JSON guide')
    field = field.replace(old_edition, edition, 1)
    guide['edition'] = edition
    patch_path = root / 'docs/releases' / (version + '-guide.json')
    if patch_path.is_file():
        patches = json.loads(patch_path.read_text(encoding='utf-8'))
        if not isinstance(patches, list):
            raise ValueError('Guide updates must be a list')
        seen = set()
        for patch in patches:
            key = (patch['chapter'], patch['heading'])
            body = patch['body']
            if key in seen or not isinstance(body, str) or not 0 < len(body) <= 8192 or re.search(r'[\x00-\x08\x0b\x0c\x0e-\x1f§]', body):
                raise ValueError('Invalid or duplicate guide update')
            seen.add(key)
            sections = [section for chapter in guide['chapters'] if chapter['id'] == key[0]
                        for section in chapter['sections'] if section['heading'] == key[1]]
            if len(sections) != 1:
                raise ValueError('Expected exactly one matching guide section: ' + str(key))
            previous = sections[0]['body']
            if previous != body:
                if field.count(previous) != 1:
                    raise ValueError('Guide body is missing or ambiguous in Markdown: ' + str(key))
                field = field.replace(previous, body, 1)
                sections[0]['body'] = body
    changes[guide_path] = json.dumps(guide, indent=2, ensure_ascii=False) + '\n'
    changes[field_path] = field
    source = root / 'src/main/java/net/foundations/pl4'
    packet = (source / 'PLPackets.java').read_text(encoding='utf-8')
    protocol = re.findall(r'event\.registrar\("(\d+)"\)', packet)
    if len(protocol) != 1:
        raise ValueError('Expected exactly one literal payload protocol')
    registered = re.findall(r'e\.register\((\w+)\.class\)', (source / 'PLGameTests.java').read_text(encoding='utf-8'))
    if not registered or len(set(registered)) != len(registered):
        raise ValueError('Missing or duplicate native test registrations')
    count = sum((source / (name + '.java')).read_text(encoding='utf-8').count('@GameTest(') for name in registered)
    if count <= 0:
        raise ValueError('No registered native tests found')
    status_path = root / 'BUILD_STATUS.json'
    status = json.loads(status_path.read_text(encoding='utf-8'))
    status.update(version=version, release_channel='beta' if re.search(r'\db(?:\.|$)', version) else 'alpha',
                  payload_protocol=protocol[0], expected_native_tests=count,
                  further_api_testing_required=True, client_visual_acceptance='PENDING',
                  release_notes='docs/releases/' + version + '.md')
    changes[status_path] = json.dumps(status, indent=2, ensure_ascii=False) + '\n'
    readme = root / 'README.md'
    changes[readme] = readme.read_text(encoding='utf-8').replace(old, version)
    return changes


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('version')
    parser.add_argument('--check', action='store_true', help='Validate and print changes without writing')
    args = parser.parse_args()
    try:
        changes = plan(ROOT, args.version)
        for path, text in changes.items():
            if path.read_text(encoding='utf-8') == text:
                continue
            print(path.relative_to(ROOT))
            if not args.check:
                temporary = path.with_name(path.name + '.version-tmp')
                temporary.write_text(text, encoding='utf-8')
                temporary.replace(path)
    except (OSError, ValueError, KeyError) as error:
        parser.exit(1, 'Version preparation failed: ' + str(error) + '\n')


if __name__ == '__main__':
    main()
