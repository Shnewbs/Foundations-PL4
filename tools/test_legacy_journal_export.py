import copy
import hashlib
import unittest
from verify_legacy_journal_export import verify, NOTICE, verify_native_log

def fixture(size=300, dimension=0):
    raw = bytes(n % 251 for n in range(size))
    manifest = f"PL4 opaque export native-nbt-uncompressed-v1; dimension {dimension}; bytes {size}; sha256 {hashlib.sha256(raw).hexdigest()}"
    count = (size+127)//128
    return [[manifest, f"Page {n+1}/{count} hex {raw[n*128:(n+1)*128].hex()}", NOTICE] for n in range(count)]

class ExportTests(unittest.TestCase):
    def test_reconstruct_out_of_order(self):
        blocks = fixture()
        result = verify(list(reversed(blocks)))
        self.assertEqual(result["native_nbt_hex"], bytes(n % 251 for n in range(300)).hex())
        self.assertIs(result["recovery_clearance"], False)
        self.assertEqual(result["original_target"], "unknown")
    def test_full_cap(self):
        self.assertEqual(verify(fixture(16384, -1))["bytes"], 16384)
    def test_missing_page(self):
        with self.assertRaises(ValueError): verify(fixture()[:-1])
    def test_duplicate_page(self):
        blocks=fixture(); blocks[-1]=copy.deepcopy(blocks[0])
        with self.assertRaises(ValueError): verify(blocks)
    def test_mixed_snapshot(self):
        blocks=fixture(); blocks[1]=fixture(301)[1]
        with self.assertRaises(ValueError): verify(blocks)
    def test_mixed_dimension(self):
        blocks=fixture(); blocks[1]=fixture(dimension=-1)[1]
        with self.assertRaises(ValueError): verify(blocks)
    def test_mutated_hex(self):
        blocks=fixture(); blocks[0][1]=blocks[0][1][:-2]+"ff"
        with self.assertRaises(ValueError): verify(blocks)
    def test_truncated_page(self):
        blocks=fixture(); blocks[-1][1]=blocks[-1][1][:-2]
        with self.assertRaises(ValueError): verify(blocks)
    def test_misleading_notice(self):
        blocks=fixture(); blocks[0][2]="Recovery cleared"
        with self.assertRaises(ValueError): verify(blocks)
    def test_reject_oversize(self):
        with self.assertRaises(ValueError): verify(fixture(16385))
    def test_line_and_manifest_bounds(self):
        for blocks in ([[["x"*513,"",""]]], [fixture()[0][:2]], "bad", []):
            with self.assertRaises(ValueError): verify(blocks)

    def test_native_log_reconstruction(self):
        text="\n".join("INFO fixture PL4 OPAQUE EXPORT BLOCK "+str(n+1)+" "+line for n, block in enumerate(fixture()) for line in block)
        self.assertEqual(verify_native_log(text)["bytes"],300)
    def test_native_log_requires_complete_unique_blocks(self):
        for text in ["no export", "PL4 OPAQUE EXPORT BLOCK 0 bad", "PL4 OPAQUE EXPORT BLOCK 1 bad"]:
            with self.assertRaises(ValueError): verify_native_log(text)
