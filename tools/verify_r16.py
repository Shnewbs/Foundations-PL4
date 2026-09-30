from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
def read(p):return (R/p).read_text()
engine=read('src/main/java/net/foundations/pl4/TransferEngine.java')
rules=read('src/main/java/net/foundations/pl4/core/TransferRules.java')
partscreen=read('src/main/java/net/foundations/pl4/client/PartScreen.java')
book=json.loads((R/'src/main/resources/assets/foundations_pl4/guide/en_us.json').read_text())
transfer=next(c for c in book['chapters'] if c['id']=='transfer')
body=' '.join(s['body'] for s in transfer['sections'])
checks={
 'R1 retains R16 code line':"0.0.2a.R1" in read('build.gradle') and '0.0.2a.R1' in read('src/main/resources/META-INF/neoforge.mods.toml'),
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
 'R16 native tests registered':'e.register(R16GameTests.class)' in read('src/main/java/net/foundations/pl4/PLGameTests.java'),
 'eight R16 native tests':read('src/main/java/net/foundations/pl4/R16GameTests.java').count('@GameTest(')==8,
 'R16 verifier wired':"verify-r16-rules.gradle" in read('build.gradle'),
 'guide R1 edition':'0.0.2a.R1' in book['edition'],
}
failed=[k for k,v in checks.items() if not v]
if failed:raise SystemExit('R16 source guard failed: '+', '.join(failed))
print(f'PASS R16 source guards: {len(checks)} of {len(checks)}.')
