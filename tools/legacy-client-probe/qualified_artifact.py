"""Validate pinned native evidence before exposing a production JAR to a client."""
import hashlib
import json
import re
import zipfile


def runtime_from_archive(path, archive_sha256, minecraft, source, fixtures, name):
    if not re.fullmatch(r'[0-9a-f]{64}', archive_sha256 or ''):
        raise ValueError('Missing pinned native archive digest')
    if hashlib.sha256(path.read_bytes()).hexdigest() != archive_sha256:
        raise ValueError('Native archive checksum mismatch')
    with zipfile.ZipFile(path) as archive:
        if archive.read('verification-logs/SOURCE_COMMIT.txt').decode().strip() != source:
            raise ValueError('Artifact source differs from checked source')
        native = json.loads(archive.read('verification-logs/clean-native-summary.json'))
        if (native['minecraft'] != minecraft or native['verification_commit'] != source
                or native['total'] != fixtures or native['passed'] != fixtures
                or native['failed'] != 0 or fixtures <= 0):
            raise ValueError('Native scenario qualification does not match source/target/count')
        payload = archive.read('build/libs/' + name)
    digest = hashlib.sha256(payload).hexdigest()
    if digest != native['runtime_sha256']:
        raise ValueError('Qualified production artifact checksum mismatch')
    return payload, digest
