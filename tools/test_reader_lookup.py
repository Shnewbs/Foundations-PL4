"""Source-contract and deterministic mirror for selected-reader first-match parity.

This is NOT Minecraft compilation, a native GameTest, or a performance benchmark.
"""
from pathlib import Path
import random


def main():
    root = Path(__file__).resolve().parents[1]
    source = (root / "src/main/java/net/foundations/pl4/NetworkEngine.java").read_text()
    markers = (
        "Map<String,Ref> readerLookup=null;",
        "if(p.selected.isEmpty())reader=readers.isEmpty()?null:readers.getFirst();",
        "readerLookup.putIfAbsent(candidate.part.identity.toString(),candidate);",
        "readerLookup.putIfAbsent(candidate.part.label,candidate);",
        "reader=readerLookup.get(p.selected);",
        "r.part.kind.emitter()&&r.part.owner!=null&&r.part.identity!=null",
    )
    missing = [marker for marker in markers if marker not in source]
    if missing:
        raise AssertionError("Selected-reader source contract changed: " + repr(missing))
    rng = random.Random(262604)
    labels = ("", "same", "id1", "id3", "foo", "bar", "missing")
    cases = 0
    for _ in range(5000):
        readers = [(f"id{i}", rng.choice(labels)) for i in range(rng.randrange(40))]
        lookup = {}
        for i, (identity, label) in enumerate(readers):
            lookup.setdefault(identity, i)
            lookup.setdefault(label, i)
        for selected in labels:
            original = next((i for i, (identity, label) in enumerate(readers)
                             if not selected or identity == selected or label == selected), None)
            optimized = (0 if readers else None) if not selected else lookup.get(selected)
            if original != optimized:
                raise AssertionError(f"Reader precedence changed for {selected!r}: {original} != {optimized}")
            cases += 1
    print(f"PASS reader lookup source guard and {cases} deterministic mirrored precedence cases (not native)")


if __name__ == "__main__":
    main()
