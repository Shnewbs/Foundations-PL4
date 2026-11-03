"""Component-link candidate sorting: cached IDs must preserve stable choice order.

Source guard and seeded semantic mirror only. Native NeoForge compilation and GameTests
are separate release gates.
"""
from functools import cmp_to_key
from pathlib import Path
from random import Random

ROOT = Path(__file__).resolve().parents[1]
source = (ROOT / "src/main/java/net/foundations/pl4/ComponentLinks.java").read_text(encoding="utf-8")
body = source.split("private static List<Part.Link> candidates(", 1)[1].split("public static void refresh(", 1)[0]
assert "if(links.size()>1)" in body
assert "Map<Part.Link,String> keys=new IdentityHashMap<>();" in body
assert "for(Part.Link link:links)keys.put(link,choiceId(link));" in body
assert "links.sort(Comparator.comparing(keys::get));" in body
assert "links.sort(Comparator.comparing(ComponentLinks::choiceId))" not in body

rng = Random(0x504C34)
old_calls = new_calls = 0
for case in range(10000):
    size = 2048 if case % 20 == 0 else rng.randrange(130)
    # Repeated choice IDs are deliberate: sort must retain the original order on ties.
    links = [(rng.randrange(100), rng.randrange(100), i) for i in range(size)]
    def choice_id(link):
        return f"{link[0]:04d}:{link[1]:04d}"
    def compare(a, b):
        global old_calls
        old_calls += 2
        return (choice_id(a) > choice_id(b)) - (choice_id(a) < choice_id(b))
    baseline = sorted(links, key=cmp_to_key(compare))
    keys = {id(link): choice_id(link) for link in links} if size > 1 else {}
    new_calls += size if size > 1 else 0
    cached = sorted(links, key=lambda link: keys[id(link)]) if size > 1 else list(links)
    assert baseline == cached, f"Component link order changed at case {case}"
print(f"PASS: 10,000 stable-order cases; ID evaluations {old_calls} -> {new_calls} in mirrored sorting. Native build not run.")
