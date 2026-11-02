from pathlib import Path
import re
import json
R=Path(__file__).resolve().parents[1]
def read(p):return (R/p).read_text()
engine=read('src/main/java/net/foundations/pl4/TransferEngine.java')
network=read('src/main/java/net/foundations/pl4/NetworkEngine.java')
rules=read('src/main/java/net/foundations/pl4/core/TransferRules.java')
partscreen=read('src/main/java/net/foundations/pl4/client/PartScreen.java')
book=json.loads((R/'src/main/resources/assets/foundations_pl4/guide/en_us.json').read_text())
transfer=next(c for c in book['chapters'] if c['id']=='transfer')
body=' '.join(s['body'] for s in transfer['sections'])
checks={
 'current version metadata is parameterized':'version="${version}"' in read('src/main/resources/META-INF/neoforge.mods.toml') and re.search(r"^version = '([^']+)'$",read('build.gradle'),re.M) is not None,
 'normal node endpoints':'Kind.NODE||r.part().kind==Kind.TRANSFER_NODE' in engine,
 'route rules used':'TransferRules.canRoute' in engine,
 'remove phase':'TransferRules.drivesRemove' in engine,
 'add phase':'TransferRules.drivesAdd' in engine,
 'same cycle receive fence':'received.contains(target(source))' in engine,
 'explicit peer priority':'TransferRules.endpointClass' in engine,
 'peer-only bidirectional':'ADD_REMOVE&&sinkMode==ADD_REMOVE' in rules and 'sourceMode==REMOVE' in rules and 'sinkMode==ADD' in rules,
 'item escrow':'pendingItem' in engine,
 'fluid escrow':'pendingFluid' in engine,
 'energy escrow':'pendingEnergy' in engine,
 'normal node guide':'normal Node' in body and 'passive endpoint' in body,
 'FE boundary documented':'EU' in body and 'Joule' in body and 'read-only telemetry' in body,
 'clear mode labels':'ADD / IMPORT' in partscreen and 'REMOVE / EXPORT' in partscreen,
 'R16 native tests registered':'R16GameTests.class' in read('src/main/java/net/foundations/pl4/PortTestInstance.java'),
 'thirty-two transfer, automation and storage native tests':read('src/main/java/net/foundations/pl4/R16GameTests.java').count('@PortGameTest(')==32,
 'silent host part-set changes invalidate reader caches':'hostPartCount==ref.host.parts.size()' in network and 'snapshot.matches(ref)' in network and 'readerSeesSilentlyAddedAndRemovedOwnedNode' in read('src/main/java/net/foundations/pl4/R16GameTests.java'),
 'physical cable partitions newly foreign owners': bool(re.search(
     r'private\s+static\s+boolean\s+sharedOwner\s*\(\s*Ref\s+source\s*,\s*Ref\s+destination\s*\)\s*\{\s*return\s+source\.part\.owner\s*!=\s*null\s*&&\s*source\.part\.owner\.equals\s*\(\s*destination\.part\.owner\s*\)\s*;\s*\}',
     network)) and bool(re.search(
     r'if\s*\(\s*sharedOwner\s*\(\s*a\s*,\s*b\s*\)\s*\)\s*sets\.union\s*\(\s*edge\.a\(\)\s*,\s*edge\.b\(\)\s*\)\s*;',
     network)),
 'wireless links retain owner and exact emitter identity': bool(re.search(
     r'!sharedOwner\s*\(\s*r\s*,\s*target\s*\)\s*\|\|\s*!matchesWirelessTarget\s*\(\s*link\s*,\s*target\s*\)',
     network)),
 'live reader alias revocation fixture is registered':'readerExportsDropRevokedAndDetachedInventoryAliases' in read('src/main/java/net/foundations/pl4/R16GameTests.java') and 'currentOwnedGroup(server,reader,group)' in read('src/main/java/net/foundations/pl4/NetworkEngine.java'),
 'R16 verifier wired':"verify-r16-rules.gradle" in read('build.gradle'),
 'guide edition matches current version':book['edition'].endswith(re.search(r"^version = '([^']+)'$",read('build.gradle'),re.M).group(1)),
}
failed=[k for k,v in checks.items() if not v]
if failed:raise SystemExit('R16 source guard failed: '+', '.join(failed))
print(f'PASS R16 source guards: {len(checks)} of {len(checks)}.')
