"""Carry the reviewed 1.21.1 channel/status correction across modern ports."""
from pathlib import Path
import argparse
from align_performance import replace_once

RULE = '''    /** The same direction and channel decision is used for transfers and endpoint status. */
    public static boolean canRouteOnChannels(boolean sourceTransfer,int sourceMode,String output,
                                              boolean sinkTransfer,int sinkMode,String input){
        return channelsMatch(output,input)&&canRoute(sourceTransfer,sourceMode,sinkTransfer,sinkMode);
    }

'''
REGRESSIONS = '''        check(TransferRules.canRouteOnChannels(true,TransferRules.REMOVE,"",false,0,""),"Legacy default channels can route");
        check(TransferRules.canRouteOnChannels(true,TransferRules.REMOVE,"ore",true,TransferRules.ADD,"ore"),"Matching named channels can route");
        check(!TransferRules.canRouteOnChannels(true,TransferRules.REMOVE,"ore",true,TransferRules.ADD,"fluid"),"Endpoint status and movement exclude mismatched channels");
        check(!TransferRules.canRouteOnChannels(true,TransferRules.REMOVE,"",true,TransferRules.ADD,"ore"),"Named channels do not leak into the default pool");
        check(!TransferRules.canRouteOnChannels(true,TransferRules.ADD,"ore",true,TransferRules.REMOVE,"ore"),"Matching channels cannot bypass direction rules");
'''


def rules(source):
    if "public static boolean canRouteOnChannels(" in source:
        if "return channelsMatch(output,input)&&canRoute(sourceTransfer,sourceMode,sinkTransfer,sinkMode);" not in source:
            raise ValueError("Unreviewed channel routing implementation")
        return source
    return replace_once(source, "    /** Explicit Transfer Nodes win equal-priority ties over passive Nodes. */", RULE + "    /** Explicit Transfer Nodes win equal-priority ties over passive Nodes. */")


def engine(source):
    if "routable(ref,other,targets)" in source:
        return source
    for name, argument, identity, condition in [
        ("sinks", "source", "origin", "source,s"), ("sources", "sink", "destination", "s,sink")]:
        prefix = f"    private static List<NetworkEngine.Ref> {name}(List<NetworkEngine.Ref> endpoints,NetworkEngine.Ref {argument},long cursor,Map<NetworkEngine.Ref,TargetKey> targets){{\n"
        start = source.index(prefix)
        end = source.index("\n    }", start) + len("\n    }")
        expected = source[start:end]
        if f"TargetKey {identity}=targets.computeIfAbsent(" not in expected:
            raise ValueError("Missing cached identity baseline")
        source = source[:start] + prefix + f"        return rotateTies(endpoints.stream().filter(s->routable({condition},targets)).toList(),cursor);\n    }}" + source[end:]
    anchor = "    private static PairKey pair(NetworkEngine.Ref source,NetworkEngine.Ref sink)"
    helper = '''    private static boolean routable(NetworkEngine.Ref source,NetworkEngine.Ref sink,Map<NetworkEngine.Ref,TargetKey> targets){
        return source!=sink&&!sameTarget(source,sink,targets)&&TransferRules.canRouteOnChannels(
            source.part().kind==Kind.TRANSFER_NODE,source.part().transferMode,source.part().outputChannel,
            sink.part().kind==Kind.TRANSFER_NODE,sink.part().transferMode,sink.part().inputChannel);
    }
'''
    source = replace_once(source, anchor, helper + anchor)
    source = replace_once(source,
        "private static boolean sameTarget(NetworkEngine.Ref a,NetworkEngine.Ref b,Map<NetworkEngine.Ref,TargetKey> targets){return targets.get(a).equals(targets.get(b));}",
        "private static boolean sameTarget(NetworkEngine.Ref a,NetworkEngine.Ref b,Map<NetworkEngine.Ref,TargetKey> targets){return targets.computeIfAbsent(a,TransferEngine::target).equals(targets.computeIfAbsent(b,TransferEngine::target));}")
    before = "other->other!=ref&&other.part().kind==Kind.TRANSFER_NODE&&!sameTarget(ref,other,targets)&&(TransferRules.canRoute(true,p.transferMode,true,other.part().transferMode)||TransferRules.canRoute(true,other.part().transferMode,true,p.transferMode))"
    source = replace_once(source, before, "other->other.part().kind==Kind.TRANSFER_NODE&&(routable(ref,other,targets)||routable(other,ref,targets))")
    source = replace_once(source,
        "source->source!=ref&&!sameTarget(source,ref,targets)&&TransferRules.canRoute(source.part().kind==Kind.TRANSFER_NODE,source.part().transferMode,true,p.transferMode)",
        "source->routable(source,ref,targets)")
    source = replace_once(source,
        "sink->sink!=ref&&!sameTarget(ref,sink,targets)&&TransferRules.canRoute(true,p.transferMode,sink.part().kind==Kind.TRANSFER_NODE,sink.part().transferMode)",
        "sink->routable(ref,sink,targets)")
    return source.replace("needs ADD or REMOVE peer", "needs matching ADD or REMOVE peer")


def regressions(source):
    if "Endpoint status and movement exclude mismatched channels" in source:
        return source
    anchor = '        check(!TransferRules.canRoute(true,TransferRules.ADD,true,TransferRules.REMOVE),"Reverse direction is invalid");\n'
    return replace_once(source, anchor, anchor + REGRESSIONS)


def changes(root):
    java = root / "src/main/java/net/foundations/pl4"
    transformations = [(java / "core/TransferRules.java", rules), (java / "TransferEngine.java", engine),
                       (root / "tools/R16RegressionTests.java", regressions)]
    result = {}
    for path, transform in transformations:
        before = path.read_text()
        after = transform(before)
        if transform(after) != after:
            raise ValueError("Channel/status migration is not idempotent")
        result[path] = after
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    args = parser.parse_args()
    for path, after in changes(args.root).items():
        if path.read_text() != after:
            path.write_text(after)
            print("ALIGNED", path)


if __name__ == "__main__":
    main()
