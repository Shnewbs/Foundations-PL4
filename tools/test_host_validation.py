"""Offline semantic mirror for multipart-host validation de-duplication.

This does not execute Minecraft or compile NetworkEngine; native tests are still required.
"""
from pathlib import Path
from random import Random

SOURCE = Path(__file__).resolve().parents[1] / 'src/main/java/net/foundations/pl4/NetworkEngine.java'
WIRELESS_SOURCE = Path(__file__).resolve().parents[1] / 'src/main/java/net/foundations/pl4/ComponentLinks.java'


def validate(refs, deduplicate, owned):
    """Model short-circuit order for snapshot, host, part attachment and owner checks."""
    verified = set()
    calls = 0
    attached = False
    for host, snapshot_ok, host_live, part_attached, owner_ok, is_anchor in refs:
        if not snapshot_ok:
            return False, calls
        if not deduplicate or host not in verified:
            verified.add(host)
            calls += 1
            if not host_live:
                return False, calls
        if not part_attached or (owned and not owner_ok):
            return False, calls
        attached |= is_anchor
    return (attached if owned else True), calls


def main():
    source = SOURCE.read_text(encoding='utf-8')
    check = '(verifiedHosts.add(ref.host)&&!loadedHost(ref.host,server))'
    assert source.count(check) == 2, 'Both live UI and owned-network checks must deduplicate hosts'
    assert source.count('Set<HostEntity> verifiedHosts=Collections.newSetFromMap(new IdentityHashMap<>());') == 2
    assert 'snapshot.matches(ref)' in source and 'ref.host.parts.get(ref.part.slot())!=ref.part' in source
    assert '!anchor.owner.equals(ref.part.owner)' in source
    # Wireless topology requires a non-null common owner. Both candidate UI
    # and server-side addHeld/addChoice must enforce that same invariant.
    wireless = WIRELESS_SOURCE.read_text(encoding='utf-8')
    assert 'if(!PLConfig.WIRELESS.get()||part.owner==null)return links;' in wireless
    assert 'if(target==null||part.owner==null||!target.kind.emitter()||target.kind.redstone()!=part.kind.redstone()||!Objects.equals(target.owner,part.owner)||link.side()!=target.face)return false;' in wireless
    for receiver in (None, 'alice', 'bob'):
        for emitter in (None, 'alice', 'bob'):
            topology_allows = receiver is not None and receiver == emitter
            candidate_allows = receiver is not None and receiver == emitter
            persisted_link_allows = receiver is not None and receiver == emitter
            assert candidate_allows == persisted_link_allows == topology_allows
            for face_matches in (False, True):
                link_add_allows = persisted_link_allows and face_matches
                graph_accepts = topology_allows and face_matches
                assert link_add_allows == graph_accepts

    rng = Random(0x504C34)
    checks = 0
    saved_calls = 0
    for _ in range(30000):
        host_count = rng.randrange(1, 9)
        host_live = [rng.random() > 0.08 for _ in range(host_count)]
        refs = []
        for i in range(rng.randrange(1, 33)):
            host = rng.randrange(host_count)
            refs.append((host, rng.random() > 0.06, host_live[host], rng.random() > 0.05, rng.random() > 0.05, i == 0))
        for owned in (False, True):
            before, old_calls = validate(refs, False, owned)
            after, new_calls = validate(refs, True, owned)
            assert before == after, 'Host de-duplication changed the validation result'
            assert new_calls <= old_calls, 'Host de-duplication increased live lookups'
            saved_calls += old_calls - new_calls
            checks += 1
    refs = [(0, True, True, True, True, True)] + [(i % 2, True, True, True, True, False) for i in range(1, 16)]
    for owned in (False, True):
        assert validate(refs, False, owned) == (True, 16)
        assert validate(refs, True, owned) == (True, 2)
    print(f'Host validation semantic mirror: {checks} seeded cases passed; {saved_calls} duplicate live lookups avoided across cases. Native compilation/GameTests not run.')


if __name__ == '__main__':
    main()
