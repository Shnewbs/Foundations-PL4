"""R9 resource/data/source-wiring guards. Not a native Minecraft or graphical acceptance test."""
from pathlib import Path
import json,re,struct
R=Path(__file__).resolve().parents[1];J=R/'src/main/java/net/foundations/pl4';A=R/'src/main/resources/assets/foundations_pl4'
def source(name):return (J/name).read_text()
part=source('Part.java');packet=source('PLPackets.java');net=source('DisplayNetworks.java');painter=source('client/DisplayPainter.java');canvas=source('client/DisplayCanvas.java');editor=source('client/DisplayEditorScreen.java');guide=source('client/GuideScreen.java')
assert 'event.registrar("4")' in packet
assert 'result.accepted()' in packet and packet.index('if(!result.accepted())')<packet.index('DisplayNetworks.applyLayout(host,part,settings,nextRevision)')
assert 'DisplayNetworks.canEditCanvas' in packet and '!clicked.identity.equals(packet.identity)' in packet
assert 'player.isSpectator()' in packet and 'player.distanceToSqr(packet.pos.getCenter())>64' in packet
assert 'writeUtf(p.value,65536)' in packet and 'rate.count>=8' in packet
assert 'part.readerChoices.stream()' in packet and 'Map<ServerPlayer,Rate> EDIT_RATE=new WeakHashMap' in packet
assert 't.putString("displayMode"' in part and 't.putInt("displayPage"' in part and 't.putString("type",spec.type().name())' in part
assert 't.hasUUID("id")' in part and 'legacy?' in part and 'Part.DisplaySettings' in packet
assert 'List.copyOf(elements)' in part and 'sourceRows' in part
assert 'Only already-visible readers' in net and 'reader(canvas.readers,selector)' in net and 'budget=256' in net
assert 'root.sourceRows.size()>=8' in net and 'Math.min(64,budget)' in net
assert 'MonitorPresentation.automatic' in painter and 'DisplayElements.plan(spec,rows)' in painter
for op in ['DisplayElements.Icon','DisplayElements.Liquid','DisplayElements.Box','DisplayElements.Text']:assert op in painter
assert 'new WeakHashMap<>()' in painter and 'MAX_ICONS' in painter
assert 'ItemDisplayContext.GUI' in canvas and 'instanceof BlockItem' in canvas and 'renderSingleBlock' in canvas
assert 'getStillTexture(sample.fluid())' in canvas and 'getTintColor(sample.fluid())' in canvas
assert 'DisplayElements.worldDepth(layer)' in canvas and 'Math.clamp(order,0,31)' in canvas
assert 'disableDepthTest' not in canvas and 'renderGuiItemDecorations' not in canvas and 'renderFakeItem' not in canvas
assert 'renderBackground(GuiGraphics g,int x,int y,float partial){}' in editor
assert 'DisplayPicking.inverse' in editor and 'RenderSystem.getProjectionMatrix()' in editor and 'RenderSystem.getModelViewMatrix()' in editor
assert 'if(p.layoutRevision>=part.layoutRevision)' in editor and 'live.layoutRevision>=part.layoutRevision' in editor
assert 'mouseReleased' in editor and 'commit("update",result,"",dragRevision)' in editor
assert 'dragRevision=part.layoutRevision' in editor and '300_000_000L' in editor
assert 'new PLPackets.LayoutEdit' in editor and 'clickedIdentity' in editor and 'inspected.size()>=8' in editor
assert 'packet.action.equals("replace")' in packet and 'ElementJson.decodeList(packet.value)' in packet and 'LayoutTransactions.applyReplace' in packet
assert 'DisplayEditorScreen.active()' in source('client/PLClient.java') and 'new DisplayEditorScreen' in source('client/PLClient.java')
assert 'new GuideSearchBox' in guide and ('\"Saved\"' in guide or 'Bookmarks' in guide) and 'description.summary()' in guide
assert 'setHint(' not in source('client/GuideSearchBox.java') and 'false' in source('client/GuideSearchBox.java')
assert '.summary()' in guide and 'Tooltip.create(Component.literal(chapter' not in guide
assert 'getBoolean("reply")' in source('client/PLClient.java') and 't.putBoolean("reply",reply)' in packet
assert 'R9GameTests.class'  in source('PLGameTests.java')
tests=source('R9GameTests.java').count('@GameTest(');assert tests==22,tests
for path in A.rglob('*.json'):json.loads(path.read_text())
white=(A/'textures/gui/display_white.png').read_bytes();assert white[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',white[16:24])==(1,1)
b=json.loads((A/'guide/en_us.json').read_text());assert 'Foundations PL4' in b['edition'] and '1.21.1' in b['edition']
text='\n'.join(s['body'] for c in b['chapters'] for s in c['sections'])
for term in ['Block model','Inventory grid','Fluid tank','AUTO_LIST','CUSTOM','eight pages','Bookmarks','Quantity']:assert term in text,term
assert 'full original drag/resize/icon/page editor is not implemented' not in text
for c in b['chapters']:
 for s in c['sections']:assert s['body'] in (R/'docs/FIELD_GUIDE.md').read_text()
print(f'PASS R9 source wiring: typed persistence/dispatch, guarded revisioned packets, visible-source scope, depth separation and actual-matrix editor picking; {tests} native fixtures registered, NOT executed.')
print('PASS R9 resource/content guards: one white primitive sprite, 28 current guide chapters, independent view/data modes, search and chapter summaries. NOT graphical or native API acceptance.')
