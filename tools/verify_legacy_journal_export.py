"""Verify captured operator-only opaque journal chat pages without touching a save."""
import hashlib
import json
import re
import sys
from pathlib import Path

FORMAT = "native-nbt-uncompressed-v1"
MAX_BYTES = 16384
MANIFEST = re.compile(r"PL4 opaque export native-nbt-uncompressed-v1; dimension (-?\d+); bytes (\d+); sha256 ([0-9a-f]{64})")
PAGE = re.compile(r"Page (\d+)/(\d+) hex ([0-9a-f]+)")
NOTICE = "Original target/phase unknown. Read only; all targets remain fenced. No recovery clearance."

def verify(blocks):
    if not isinstance(blocks, list) or not 1 <= len(blocks) <= 128:
        raise ValueError("Expected 1..128 complete three-line chat blocks")
    manifest = None
    pages = {}
    for block in blocks:
        if not isinstance(block, list) or len(block) != 3 or not all(isinstance(line, str) for line in block):
            raise ValueError("Expected complete chat block")
        if any(len(line) > 512 for line in block):
            raise ValueError("Chat line exceeds bound")
        m = MANIFEST.fullmatch(block[0])
        p = PAGE.fullmatch(block[1])
        if not m or not p or block[2] != NOTICE:
            raise ValueError("Malformed export block")
        current = (int(m[1]), int(m[2]), m[3])
        if manifest is not None and current != manifest:
            raise ValueError("Mixed dimension or changed snapshot")
        manifest = current
        dimension, size, digest = current
        if not -(2**31) <= dimension < 2**31 or not 1 <= size <= MAX_BYTES:
            raise ValueError("Manifest bounds")
        number, count = int(p[1]), int(p[2])
        if count != (size + 127) // 128 or not 1 <= number <= count or number in pages:
            raise ValueError("Missing, duplicate or invalid page")
        expected = min(128, size - (number - 1) * 128) * 2
        if len(p[3]) != expected:
            raise ValueError("Page byte length mismatch")
        pages[number] = p[3]
    if len(pages) != count:
        raise ValueError("Incomplete export")
    raw = bytes.fromhex("".join(pages[n] for n in range(1, count + 1)))
    if len(raw) != size or hashlib.sha256(raw).hexdigest() != digest:
        raise ValueError("Snapshot checksum mismatch")
    return dict(format=FORMAT, dimension=dimension, bytes=size, sha256=digest,
                native_nbt_hex=raw.hex(), original_target="unknown", original_phase="unknown",
                recovery_clearance=False,
                scope="Verified captured bytes only; no provider identity, ownership or outcome authentication")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: python tools/verify_legacy_journal_export.py captured-chat-blocks.json")
    path = Path(sys.argv[1])
    if path.stat().st_size > 131072:
        raise SystemExit("Transcript exceeds bound")
    print(json.dumps(verify(json.loads(path.read_text())), indent=2))
