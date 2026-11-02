"""Deterministic signaller selection parity and source-contract checks.

UUIDs outrank labels; ambiguous duplicate labels must not select a reader.
Display selection delegates to the same resolver rather than duplicating allocation-heavy streams.
This is a source/mirror regression, NOT a compiled Minecraft or native GameTest.
"""
from pathlib import Path
import random


def select(readers, selection):
    if not selection:
        return 0 if readers else None
    for i, (identity, _) in enumerate(readers):
        if identity == selection:
            return i
    matches = [i for i, (_, label) in enumerate(readers) if label == selection]
    return matches[0] if len(matches) == 1 else None


def main():
    root = Path(__file__).resolve().parents[1]
    signaller = (root / "src/main/java/net/foundations/pl4/SignallerLogic.java").read_text()
    display = (root / "src/main/java/net/foundations/pl4/DisplayNetworks.java").read_text()
    network = (root / "src/main/java/net/foundations/pl4/NetworkEngine.java").read_text()
    required = (
        (signaller, "static NetworkEngine.Ref readerRef(List<NetworkEngine.Ref> readers,String selection)"),
        (signaller, "for(var ref:readers)if(ref.part().identity.toString().equals(selection))return ref;"),
        (signaller, "if(named!=null)return null;"),
        (signaller, "var ref=readerRef(readers,selection);"),
        (signaller, "return ref==null?null:ref.part();"),
        (display, "return SignallerLogic.readerRef(readers,selector);"),
        (network, "if(p.kind==Kind.SIGNALLER)reader=SignallerLogic.readerRef(readers,p.selected);"),
        (signaller, "return evaluate(p,readers,p.statements.isEmpty()?readerRef(readers,p.selected):null);"),
        (signaller, "static int evaluate(Part p,List<NetworkEngine.Ref> readers,NetworkEngine.Ref selected)"),
        (signaller, "Part reader=selected==null?null:selected.part();"),
        (network, "setSignal(r,SignallerLogic.evaluate(p,readers,reader));"),
        (network, "else if(p.selected.isEmpty())reader=readers.isEmpty()?null:readers.getFirst();"),
    )
    for source, marker in required:
        assert marker in source, "Missing signaller resolver source contract: " + marker
    fixtures = (
        ([("a", "b"), ("b", "other")], "b", 1),
        ([("a", "dup"), ("b", "dup")], "dup", None),
        ([("a", "one"), ("b", "two")], "one", 0),
        ([("a", "one"), ("b", "two")], "", 0),
        ([], "", None),
        ([("a", "one")], "absent", None),
        ([("a", "one"), ("a", "two")], "a", 0),
    )
    for readers, selected, expected in fixtures:
        assert select(readers, selected) == expected, (readers, selected, expected)
    rng = random.Random(263201)
    checks = 0
    for _ in range(6000):
        readers = [(f"uuid-{i}", rng.choice(["", "dup", "other", "uuid-0", "uuid-2"]))
                   for i in range(rng.randrange(30))]
        for selection in ("", "dup", "other", "uuid-0", "uuid-2", "missing"):
            uuids = [i for i, (identity, _) in enumerate(readers) if identity == selection]
            labels = [i for i, (_, label) in enumerate(readers) if label == selection]
            expected = (0 if readers else None) if not selection else (
                uuids[0] if uuids else (labels[0] if len(labels) == 1 else None))
            actual = select(readers, selection)
            assert actual == expected, (readers, selection, expected, actual)
            checks += 1
    print(f"PASS signaller/display shared resolver, UUID precedence, ambiguous-label denial and {checks} mirrored cases (not native)")


if __name__ == "__main__":
    main()
