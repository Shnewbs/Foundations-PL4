"""Apply the reviewed performance baseline without replacing target-specific APIs.

This prepares source only. Native and installed acceptance must run against the
result; this command does not promote a release or inherit another port's PASS.
"""
from pathlib import Path
import argparse


def replace_once(source, before, after):
    if source.count(before) != 1:
        raise ValueError(f"Source drift: expected one occurrence of {before!r}")
    return source.replace(before, after, 1)


def network(source):
    if "private static Map<Part,Group> cachedMembership=Map.of();" in source:
        if "cachedStorageNodes.get(anchor)" not in source:
            raise ValueError("Incomplete network membership optimization")
        return source
    source = replace_once(source,
        "    private static List<Group> cachedGroups=List.of();",
        "    private static List<Group> cachedGroups=List.of();\n"
        "    private static Map<Part,Group> cachedMembership=Map.of();\n"
        "    private static Map<Part,List<Ref>> cachedStorageNodes=Map.of();")
    source = replace_once(source,
        "cachedGroups=List.of();cachedServer=null;",
        "cachedGroups=List.of();cachedMembership=Map.of();cachedStorageNodes=Map.of();cachedServer=null;")
    source = replace_once(source,
        "        cachedRefs=List.copyOf(refs);cachedHosts=List.copyOf(arms.keySet());cachedGroups=List.copyOf(complete);",
        "        Map<Part,Group> membership=new IdentityHashMap<>();\n"
        "        Map<Part,List<Ref>> storageNodes=new IdentityHashMap<>();\n"
        "        for(Group group:complete){\n"
        "            List<Ref> storageRefs=group.parts.stream().filter(r->r.part.kind==Kind.NODE||r.part.kind==Kind.TRANSFER_NODE).toList();\n"
        "            for(Ref r:group.parts){membership.put(r.part,group);storageNodes.put(r.part,storageRefs);}\n"
        "        }\n"
        "        cachedRefs=List.copyOf(refs);cachedHosts=List.copyOf(arms.keySet());cachedGroups=List.copyOf(complete);\n"
        "        cachedMembership=membership;cachedStorageNodes=storageNodes;")
    source = replace_once(source,
        "        for(Group group:cachedGroups)if(!group.redstone&&group.hostCount<=PLConfig.MAX_NETWORK.get()&&group.readers.stream().anyMatch(r->r.part==reader))return group.targets;\n        return List.of();",
        "        Group group=cachedMembership.get(reader);\n"
        "        return group!=null&&!group.redstone&&group.hostCount<=PLConfig.MAX_NETWORK.get()&&reader.kind.reader()?group.targets:List.of();")
    source = replace_once(source,
        "        for(Group group:cachedGroups)if(!group.redstone&&group.hostCount<=PLConfig.MAX_NETWORK.get()&&group.parts.stream().anyMatch(r->r.part==anchor))\n"
        "            return group.parts.stream().filter(r->r.part.kind==Kind.NODE||r.part.kind==Kind.TRANSFER_NODE).toList();\n        return List.of();",
        "        Group group=cachedMembership.get(anchor);\n"
        "        return group!=null&&!group.redstone&&group.hostCount<=PLConfig.MAX_NETWORK.get()\n"
        "            ?cachedStorageNodes.get(anchor):List.of();")
    return source


def transfers(source):
    if "Map<NetworkEngine.Ref,TargetKey> targets=new IdentityHashMap<>();" in source:
        if "updateStatus(ref,endpoints,targets)" not in source:
            raise ValueError("Incomplete transfer identity optimization")
        return source
    source = replace_once(source,
        "        boolean nativeEnergy=NativeEnergyTransfers.needed(plan);",
        "        boolean nativeEnergy=NativeEnergyTransfers.needed(plan);\n"
        "        Map<NetworkEngine.Ref,TargetKey> targets=new IdentityHashMap<>();\n"
        "        for(NetworkEngine.Ref endpoint:endpoints)targets.put(endpoint,target(endpoint));")
    for name, argument in [("sinks", "ref"), ("sinks", "source"), ("sources", "sink")]:
        source = replace_once(source, f"{name}(endpoints,{argument},plan.routingCursor())",
                              f"{name}(endpoints,{argument},plan.routingCursor(),targets)")
    source = replace_once(source, "updateStatus(ref,endpoints)", "updateStatus(ref,endpoints,targets)")
    for name, argument, key in [("sinks", "source", "origin"), ("sources", "sink", "destination")]:
        signature = f"    static List<NetworkEngine.Ref> {name}(List<NetworkEngine.Ref> endpoints,NetworkEngine.Ref {argument},long cursor){{"
        source = replace_once(source, signature,
            signature + f"\n        return {name}(endpoints,{argument},cursor,new IdentityHashMap<>());\n    }}\n"
            f"    private static List<NetworkEngine.Ref> {name}(List<NetworkEngine.Ref> endpoints,NetworkEngine.Ref {argument},long cursor,Map<NetworkEngine.Ref,TargetKey> targets){{\n"
            f"        TargetKey {key}=targets.computeIfAbsent({argument},TransferEngine::target);")
    source = replace_once(source, "!sameTarget(source,s)", "!origin.equals(targets.computeIfAbsent(s,TransferEngine::target))")
    source = replace_once(source, "!sameTarget(s,sink)", "!destination.equals(targets.computeIfAbsent(s,TransferEngine::target))")
    source = replace_once(source,
        "private static boolean sameTarget(NetworkEngine.Ref a,NetworkEngine.Ref b){return target(a).equals(target(b));}",
        "private static boolean sameTarget(NetworkEngine.Ref a,NetworkEngine.Ref b,Map<NetworkEngine.Ref,TargetKey> targets){return targets.get(a).equals(targets.get(b));}")
    source = replace_once(source,
        "private static void updateStatus(NetworkEngine.Ref ref,List<NetworkEngine.Ref> endpoints){",
        "private static void updateStatus(NetworkEngine.Ref ref,List<NetworkEngine.Ref> endpoints,Map<NetworkEngine.Ref,TargetKey> targets){")
    for arguments in ["ref,other", "source,ref", "ref,sink"]:
        source = replace_once(source, f"sameTarget({arguments})", f"sameTarget({arguments},targets)")
    return source


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    args = parser.parse_args()
    java = args.root / "src/main/java/net/foundations/pl4"
    if (args.root / "src/legacy/java").is_dir():
        raise ValueError("Legacy runtime uses different source; a modern-source edit would not port it")
    changes = []
    for name, transform in [("NetworkEngine.java", network), ("TransferEngine.java", transfers)]:
        path = java / name
        before = path.read_text()
        after = transform(before)
        if transform(after) != after:
            raise ValueError("Performance migration is not idempotent")
        changes.append((path, before, after))
    # Validate both files before writing either one.
    for path, before, after in changes:
        if before != after:
            path.write_text(after)
            print("UPDATED", path)
        else:
            print("ALREADY ALIGNED", path)


if __name__ == "__main__":
    main()
