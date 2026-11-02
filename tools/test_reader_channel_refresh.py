"""Reader-channel refresh parity, bounded page retention and lazy labels.

Deterministic mirrors and source guards only; NOT native Minecraft acceptance.
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


def bounded(targets, selected, query, page, aliases):
    """One-pass, at-most-128-retained-links mirror of the Java page algorithm."""
    target_label = "Selected target disconnected"
    seen, visible_page, tail_page = set(), [], []
    description_calls = match_count = 0
    query = query.lower()
    requested_page = max(0, page)
    requested_start = requested_page * 64

    def describe(t):
        nonlocal description_calls
        description_calls += 1
        alias = aliases.get(t[0], "")
        return t[1] if not alias.strip() else alias + " · " + t[1]

    for t in targets:
        identity, _, available = t
        if identity in seen:
            continue
        seen.add(identity)
        label = None
        if identity == selected:
            label = describe(t)
            target_label = label + ("" if available else " [unloaded]")
        matched = not query or query in identity or query in (label if label is not None else describe(t)).lower()
        if not matched:
            continue
        if requested_start <= match_count < requested_start + 64:
            visible_page.append(t)
        if match_count % 64 == 0:
            tail_page.clear()
        tail_page.append(t)
        match_count += 1

    page = max(0, min(page, max(0, (match_count-1)//64)))
    choices = []
    for t in (visible_page if page == requested_page else tail_page):
        label = describe(t)
        if not t[2]:
            label += " [unloaded]"
        choices.append((t[0], label))
    return (target_label, match_count, page, choices), description_calls, len(visible_page)+len(tail_page)


def main():
    root = Path(__file__).resolve().parents[1]
    source = (root / "src/main/java/net/foundations/pl4/ReaderChannels.java").read_text()
    required = (
        'List<Part.Link> visiblePage=new ArrayList<>(64), tailPage=new ArrayList<>(64);',
        'long requestedStart=(long)requestedPage*64;',
        'boolean matched=query.isEmpty()||id.contains(query)',
        'if(matchCount%64==0)tailPage.clear();',
        'List<Part.Link> pageTargets=reader.targetPage==requestedPage?visiblePage:tailPage;',
    )
    for marker in required:
        assert marker in source, f"Missing bounded refresh contract: {marker}"

    rng = random.Random(261104)
    queries = ("", "foo", "bar", "entity", "dimension", "id-3", "alias", "absent", "x")
    cases = 0
    for _ in range(10000):
        size = rng.randrange(0, 180)
        targets = [(f"id-{rng.randrange(0, 120)}", f"dimension-{rng.randrange(5)} entity-{rng.randrange(4)}", bool(rng.getrandbits(1))) for _ in range(size)]
        aliases = {f"id-{i}": rng.choice(("", "Alias foo", "bar", "x")) for i in range(0, 120, 7)}
        selected = rng.choice(("", "missing", "id-3", "id-22", "id-110"))
        query = rng.choice(queries)
        page = rng.choice((rng.randrange(-3, 8), 2**31-1, -(2**31)))
        baseline, _ = refresh(targets, selected, query, page, aliases, False)
        improved, _ = refresh(targets, selected, query, page, aliases, True)
        paged, _, retained = bounded(targets, selected, query, page, aliases)
        assert baseline == improved == paged, (selected, query, page, baseline, improved, paged)
        assert retained <= 128, retained
        cases += 1

    targets = [(f"id-{i}", f"dimension-0 entity-{i}", True) for i in range(256)]
    old, before = refresh(targets, "id-240", "", 0, {}, False)
    new, after = refresh(targets, "id-240", "", 0, {}, True)
    assert old == new and before == 320 and after == 65, (before, after)
    targets = [(f"id-{i}", f"dimension-0 entity-{i}", True) for i in range(8192)]
    baseline, _ = refresh(targets, "id-8000", "", 2**31-1, {}, False)
    improved, _ = refresh(targets, "id-8000", "", 2**31-1, {}, True)
    paged, bounded_calls, retained = bounded(targets, "id-8000", "", 2**31-1, {})
    assert baseline == improved == paged and retained <= 64 and bounded_calls == 65
    print(f"PASS {cases} mirrored refresh cases; 8192 targets retain {retained} page entries (not native)")


if __name__ == "__main__":
    main()
