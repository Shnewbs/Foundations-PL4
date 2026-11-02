"""Reader-channel refresh parity and unfiltered description-work regression.

Checks a deterministic mirror and source-contract markers; NOT a native Minecraft test.
"""
from pathlib import Path
import random


def refresh(targets, selected, query, page, aliases, optimized):
    target_label = "Selected target disconnected"
    seen = set()
    matches = []
    description_calls = 0
    query = query.lower()

    def describe(t):
        nonlocal description_calls
        description_calls += 1
        alias = aliases.get(t[0], "")
        location = t[1]
        return location if not alias.strip() else alias + " · " + location

    for t in targets:
        identity, _, available = t
        if identity in seen:
            continue
        seen.add(identity)
        if optimized:
            label = None
            if identity == selected:
                label = describe(t)
                target_label = label + ("" if available else " [unloaded]")
            if not query or query in identity:
                matches.append(t)
            elif query in (label if label is not None else describe(t)).lower():
                matches.append(t)
        else:
            label = describe(t)
            if identity == selected:
                target_label = label + ("" if available else " [unloaded]")
            if not query or query in label.lower() or query in identity:
                matches.append(t)

    page = max(0, min(page, max(0, (len(matches)-1)//64)))
    choices = []
    for t in matches[page*64:page*64+64]:
        label = describe(t)
        if not t[2]:
            label += " [unloaded]"
        choices.append((t[0], label))
    return (target_label, len(matches), page, choices), description_calls


def main():
    root = Path(__file__).resolve().parents[1]
    source = (root / "src/main/java/net/foundations/pl4/ReaderChannels.java").read_text()
    required = (
        'String label=null;',
        'if(id.equals(reader.targetChannel)){',
        'if(query.isEmpty()||id.contains(query))matches.add(t);',
        '(label==null?description(reader,t):label).toLowerCase(Locale.ROOT).contains(query)',
    )
    for marker in required:
        assert marker in source, f"Missing optimized refresh contract: {marker}"

    rng = random.Random(261104)
    queries = ("", "foo", "bar", "entity", "dimension", "id-3", "alias", "absent", "x")
    cases = 0
    for _ in range(5000):
        size = rng.randrange(0, 180)
        targets = [(f"id-{rng.randrange(0, 120)}", f"dimension-{rng.randrange(5)} entity-{rng.randrange(4)}", bool(rng.getrandbits(1))) for _ in range(size)]
        aliases = {f"id-{i}": rng.choice(("", "Alias foo", "bar", "x")) for i in range(0, 120, 7)}
        selected = rng.choice(("", "missing", "id-3", "id-22", "id-110"))
        query = rng.choice(queries)
        page = rng.randrange(-3, 8)
        baseline, _ = refresh(targets, selected, query, page, aliases, False)
        improved, _ = refresh(targets, selected, query, page, aliases, True)
        assert baseline == improved, (targets, selected, query, page, baseline, improved)
        cases += 1

    targets = [(f"id-{i}", f"dimension-0 entity-{i}", True) for i in range(256)]
    old, before = refresh(targets, "id-240", "", 0, {}, False)
    new, after = refresh(targets, "id-240", "", 0, {}, True)
    assert old == new and before == 320 and after == 65, (before, after)
    print(f"PASS {cases} mirrored refresh cases; 256-target no-filter description calls {before}->{after} (not native)")


if __name__ == "__main__":
    main()
