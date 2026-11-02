"""Mirrored paging parity and source guard for lazy ComponentLinks labels.

This is a source-level regression, not native Minecraft compilation or GameTests.
"""
from pathlib import Path
import random

ROOT = Path(__file__).resolve().parents[1]
source = (ROOT / "src/main/java/net/foundations/pl4/ComponentLinks.java").read_text(encoding="utf-8")
body = source.split("public static void refresh(", 1)[1].split("private static String choiceId(", 1)[0]
assert "List<Part.Link> choices=candidates(player,host,part);" in body
assert "if(!query.isEmpty())choices.removeIf(" in body
assert "for(int i=start;i<choices.size()&&i-start<16;i++)" in body
assert ".stream().filter(" not in body and ".stream().skip(" not in body

def refresh(links, query, page, lazy):
    calls = 0
    def label(item):
        nonlocal calls
        calls += 1
        return item[1]
    choices = list(links)
    query = query.lower()
    if lazy:
        if query:
            choices[:] = [x for x in choices if query in label(x).lower()]
    else:
        choices = [x for x in choices if query in label(x).lower()]
    count = len(choices)
    page = max(0, min(page, max(0, (count - 1) // 16)))
    start = page * 16
    selected = choices[start:start + 16]
    result = [(x[0], label(x)) for x in selected]
    return count, page, result, calls

rng = random.Random(0xC0FFEE)
for n in range(10000):
    count = rng.choice((0, 1, 2, 15, 16, 17, 31, 32, 33, 63, 64, 65, 127, 128, 129, 256, 512))
    links = [(str(rng.randrange(150)), rng.choice(("Emitter", "Receiver", "Array", "stone", "alpha", "alpha beta", "GAMMA", "")) + " " + str(i % 17)) for i in range(count)]
    query = rng.choice(("", "", "", "a", "e", "emitter", "alpha", "GAMMA", "  ", "no-match", "17"))
    page = rng.choice((-1, 0, 1, 2, 3, 4, 7, 32, 1000000))
    old = refresh(links, query, page, False)
    new = refresh(links, query, page, True)
    assert old[:3] == new[:3], (n, old, new)
    if not query:
        assert new[3] == len(new[2])
    else:
        assert new[3] == old[3]
print("PASS: 10,000 mirrored ComponentLinks refresh cases; off-page labels skipped")
