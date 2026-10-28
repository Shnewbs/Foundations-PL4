"""Fail-closed, transportable evidence for the released-beta two-client world upgrade.

Only verifies the disposable offline-mode probe world and its source-compiled test
clients. This is NOT a released-JAR, online-auth, arbitrary-save or RC approval.
"""
import argparse
import hashlib
import json
import zipfile
from pathlib import Path

NAMES = ('PL4ProbeA', 'PL4ProbeB')
FILES = ('server.log', 'PL4ProbeA.log', 'PL4ProbeB.log',
         'server-multiplayer-proof.json',
         'PL4ProbeA-client-proof.json', 'PL4ProbeB-client-proof.json',
         'PL4ProbeA-storage.png', 'PL4ProbeB-storage.png')


def digest(path):
    h = hashlib.sha256()
    with Path(path).open('rb') as f:
        for buf in iter(lambda: f.read(1024 * 1024), b''):
            h.update(buf)
    return h.hexdigest()


def world_entries(root):
    root = Path(root)
    if not root.is_dir():
        raise ValueError('Expected actual saved-world directory: ' + str(root))
    entries = []
    for path in sorted(root.rglob('*')):
        if path.is_symlink():
            raise ValueError('World snapshot must not contain symlinks')
        if path.is_file():
            entries.append((path.relative_to(root).as_posix(), path))
    if not entries or not any(name == 'level.dat' for name, _ in entries):
        raise ValueError('Incomplete native world snapshot (level.dat missing)')
    return entries


def world_fingerprint(root):
    return fingerprint_items([(name, path.read_bytes()) for name, path in world_entries(root)])


def fingerprint_items(items):
    rows = [[name, hashlib.sha256(data).hexdigest(), len(data)] for name, data in items]
    if len({row[0] for row in rows}) != len(rows) or any('/../' in '/'+row[0]+'/' or row[0].startswith('/') for row in rows):
        raise ValueError('Invalid or duplicate world archive paths')
    return {'sha256': hashlib.sha256(json.dumps(rows, separators=(',', ':'), ensure_ascii=True).encode()).hexdigest(),
            'file_count': len(rows), 'byte_count': sum(row[2] for row in rows)}


def snapshot_world(root, archive):
    items = [(name, path.read_bytes()) for name, path in world_entries(root)]
    with zipfile.ZipFile(archive, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for name, data in items:
            info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, data)
    return fingerprint_items(items)


def zip_world_fingerprint(path):
    with zipfile.ZipFile(path, 'r') as z:
        if z.testzip() is not None:
            raise ValueError('Damaged saved-world archive')
        return fingerprint_items([(n, z.read(n)) for n in z.namelist()])


def record_phase(output, world, source_commit, source_archive_sha256, phase, copied_input=None):
    output = Path(output)
    if phase not in ('seed', 'restore') or len(source_commit) != 40 or len(source_archive_sha256) != 64:
        raise ValueError('Invalid phase or source provenance')
    archive_name = 'saved-world-' + phase + '.zip'
    world = snapshot_world(world, output / archive_name)
    files = {}
    for name in (*FILES, archive_name):
        path = output / name
        if not path.is_file() or path.stat().st_size == 0:
            raise ValueError('Missing real probe output: ' + name)
        files[name] = {'sha256': digest(path), 'bytes': path.stat().st_size}
    manifest = {'schema': 1, 'phase': phase, 'source_commit': source_commit,
                'source_archive_sha256': source_archive_sha256, 'world': world,
                'copied_input_world': copied_input, 'files': files}
    (output / 'phase-evidence.json').write_text(json.dumps(manifest, indent=2, sort_keys=True) + '\n')
    return manifest


def verify_phase(output, source_commit, phase):
    output = Path(output)
    m = json.loads((output / 'phase-evidence.json').read_text())
    if m['schema'] != 1 or m['phase'] != phase or m['source_commit'] != source_commit:
        raise ValueError('Wrong exact-source or phase manifest')
    if len(m['source_archive_sha256']) != 64 or set(m['files']) != {*FILES, 'saved-world-' + phase + '.zip'}:
        raise ValueError('Incomplete source or artifact attestation')
    for name, record in m['files'].items():
        path = output / name
        if not path.is_file() or path.stat().st_size != record['bytes'] or digest(path) != record['sha256']:
            raise ValueError('Missing or altered real probe artifact: ' + name)
    if zip_world_fingerprint(output / ('saved-world-' + phase + '.zip')) != m['world']:
        raise ValueError('Saved-world bytes do not match retained evidence')
    for name in NAMES:
        if (output / (name + '-storage.png')).read_bytes()[:8] != b'\x89PNG\r\n\x1a\n':
            raise ValueError('Screenshot does not have native PNG signature: ' + name)
        if m['files'][name + '-storage.png']['bytes'] <= 1000:
            raise ValueError('Incomplete screenshot: ' + name)
    server = json.loads((output / 'server-multiplayer-proof.json').read_text())
    if (server['source_commit'] != source_commit or server['phase'] != phase or
        server['real_client_names'] != list(NAMES) or len(set(server['real_client_uuids'])) != 2 or
        server['fake_players'] is not False or server['foreign_non_operator_denied'] is not True or
        server['operator_access_explicitly_granted'] is not True or
        server['saved_world_restore'] is not (phase == 'restore') or
        server['initial_items'] != 128 or server['items_per_player'] != [17, 17] or
        server['remaining_items'] != 94 or
        server['duplicate_network_requests_ignored'] is not (phase == 'seed')):
        raise ValueError('Server proof does not meet exact two-client conservation and access contract')
    if not isinstance(server['node_identity'], str) or not server['node_identity']:
        raise ValueError('Missing persisted node identity')
    for index, name in enumerate(NAMES):
        proof = json.loads((output / (name + '-client-proof.json')).read_text())
        if (proof['source_commit'] != source_commit or proof['phase'] != phase or
            proof['name'] != name or proof['uuid'] != server['real_client_uuids'][index] or
            proof['items_received'] != 17 or
            proof['identical_packets_sent'] != (2 if phase == 'seed' else 0)):
            raise ValueError('Client identity and packet proof mismatch: ' + name)
    return m, server


def verify_upgrade(seed, restore, baseline, candidate):
    if baseline == candidate or len(baseline) != 40 or len(candidate) != 40:
        raise ValueError('Distinct immutable 40-character source revisions required')
    before, a = verify_phase(seed, baseline, 'seed')
    after, b = verify_phase(restore, candidate, 'restore')
    if (a['node_identity'] != b['node_identity'] or
        a['real_client_uuids'] != b['real_client_uuids'] or
        after['copied_input_world'] != before['world']):
        raise ValueError('Saved source world / identity was not preserved across exact-source upgrade')
    return {'baseline': baseline, 'candidate': candidate,
            'seed_world_sha256': before['world']['sha256'],
            'restored_world_sha256': after['world']['sha256'],
            'seed_manifest_sha256': digest(Path(seed) / 'phase-evidence.json'),
            'restore_manifest_sha256': digest(Path(restore) / 'phase-evidence.json'),
            'scope': 'Disposable offline-mode source-compiled two-client upgrade; NOT released-JAR, installed-provider, online-auth, arbitrary-world, sustained-performance or RC clearance'}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('seed', type=Path); p.add_argument('restore', type=Path)
    p.add_argument('--baseline', required=True); p.add_argument('--candidate', required=True)
    args = p.parse_args()
    receipt = verify_upgrade(args.seed, args.restore, args.baseline, args.candidate)
    (args.restore / 'upgrade-evidence.json').write_text(json.dumps(receipt, indent=2, sort_keys=True) + '\n')
    print('PASS: independently verified exact-source two-client migration, source world bytes, ownership, packets and proof hashes.')


if __name__ == '__main__':
    main()
