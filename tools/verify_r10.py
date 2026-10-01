"""R10 resource/geometry and source-wiring checks. NOT a native Minecraft/client test."""
from pathlib import Path
import json,math,copy
from r10_item_assets import kind_names,corners,resolve
R=Path(__file__).resolve().parents[1]
CONTEXTS={'gui','ground','fixed','head','firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand'}

def matrix(rotation):
    x,y,z=map(math.radians,rotation);cx,sx=math.cos(x),math.sin(x);cy,sy=math.cos(y),math.sin(y);cz,sz=math.cos(z),math.sin(z)
    # Explicit Rx*Ry*Rz (rotationXYZ), independent of generator's successive rotation loop.
    return [[cy*cz,-cy*sz,sy],[cx*sz+sx*sy*cz,cx*cz-sx*sy*sz,-sx*cy],[sx*sz-cx*sy*cz,sx*cz+cx*sy*sz,cx*cy]]

def check_models(root):
    models=root/'src/main/resources/assets/foundations_pl4/models';count=0
    for name in kind_names(root):
        item=json.loads((models/'item'/f'{name}.json').read_text());original=resolve(models,'foundations_pl4:block/part_'+name)
        assert item['parent']=='minecraft:block/block',name
        assert set(item['display'])==CONTEXTS,name
        assert item['textures']==original.get('textures',{}),name
        pts=corners(original['elements']);origin=[(min(p[i] for p in pts)+max(p[i] for p in pts))/2 for i in range(3)]
        expected=copy.deepcopy(original['elements'])
        for e in expected:
            for key in ['from','to']:e[key]=[round(e[key][i]+8-origin[i],7) for i in range(3)]
            if 'rotation' in e:e['rotation']['origin']=[round(e['rotation']['origin'][i]+8-origin[i],7) for i in range(3)]
        assert item['elements']==expected,('UV/model shape changed',name)
        vertices=corners(item['elements'])
        for i in range(3):assert abs((min(p[i] for p in vertices)+max(p[i] for p in vertices))/2-8)<1e-6,('uncentred',name)
        for ctx,transform in item['display'].items():
            assert set(transform)=={'rotation','translation','scale'},(name,ctx)
            assert all(math.isfinite(v) and 0<v<=3 for v in transform['scale']),(name,ctx)
            assert all(math.isfinite(v) and abs(v)<=2 for v in transform['translation']),(name,ctx)
            m=matrix(transform['rotation']);v=[[sum(m[i][j]*(p[j]-8)*transform['scale'][j] for j in range(3)) for i in range(3)] for p in vertices]
            extent=max(max(p[i] for p in v)-min(p[i] for p in v) for i in range(3))
            target=13 if ctx=='gui' else 8 if ctx=='fixed' else 5.6 if ctx=='head' else 4 if ctx=='ground' else 5.2 if ctx.startswith('firstperson') else 5
            assert extent<=target+.0001,(name,ctx,extent,target)
            assert extent>=target*.55,(name,ctx,'unreadably small')
        assert item['display']['firstperson_lefthand']==item['display']['firstperson_righthand'],('left hand applies engine mirror to same local pose',name)
        assert item['display']['thirdperson_lefthand']==item['display']['thirdperson_righthand'],name
        count+=1
    for name in ['plguide','hammer','operator','transceiver','wirelessstorage']:
        item=json.loads((models/'item'/f'{name}.json').read_text());assert item['parent'].startswith('minecraft:item/'),name
    return count

def check_guide(root):
    a=root/'src/main/resources/assets/foundations_pl4';book=json.loads((a/'guide/en_us.json').read_text());ids=[c['id'] for c in book['chapters']]
    tutorials=['tutorial_inventory','tutorial_graphics','tutorial_energy','tutorial_expansion','tutorial_hologram','tutorial_hammer']
    assert ids[0]=='start' and len(ids)==28 and len(set(ids))==28
    assert book['chapters'][0]['title']=='Welcome to Foundations PL4'
    assert ids[1:7]==tutorials
    assert all(c['category']=='start' for c in book['chapters'][:7])
    text=(root/'docs/FIELD_GUIDE.md').read_text()
    for c in book['chapters']:
        for s in c['sections']:assert s['body'] in text,(c['id'],s['heading'])
    for term in ['17 stone','3 dirt','22 stone','Block model','Static resource blank','power_demo','two clear air blocks','AUTO_LIST','CUSTOM','16 by 16','Tutorial >']:assert term in text,term
    return len(ids)

def check_wiring(root):
    j=root/'src/main/java/net/foundations/pl4'
    source=lambda p:(j/p).read_text()
    guide=source('client/GuideScreen.java');renderer=source('client/HostRenderer.java');painter=source('client/DisplayPainter.java');editor=source('client/DisplayEditorScreen.java')
    assert 'GuideNavigation.section(book,category,chapter)' in guide
    assert 'category=chapter.category()' in guide and 'savedOnly=false' in guide
    assert 'footerJumpX' in guide and 'Tutorial >' in guide and '"start"' in guide
    assert 'row.chapterNumber=i+1' in guide and 'GuideNavigation.heading(chapter.category())' in guide
    assert 'DynamicCanvasLayout.large' in renderer and 'editor!=null' in renderer
    assert 'MonitorPresentation.automatic(part.displayMode,editing)' in painter
    assert 'MonitorPresentation.amount(row)' in painter and 'table.left()' in painter
    assert 'DisplayElements.plan(spec,rows)' in painter and 'No mode or world data is mutated' in painter
    assert ('drawHelp(canvas)' in editor) or ('drawHudHelp(g)' in editor)
    assert 'event.registrar("4")' in source('PLPackets.java'),'No packet/schema change intended'
    assert source('R10GameTests.java').count('@PortGameTest(')==5
    assert 'R10GameTests.class' in source('PortTestInstance.java')
    return True

def main():
    n=check_models(R);chapters=check_guide(R);check_wiring(R)
    print(f'PASS R10 resources: {n} item-only centred meshes, {n*8} bounded/mirrored context transforms; flat tools/guide preserved.')
    print(f'PASS R10 guide: {chapters} chapters, Welcome and six sequenced tutorials; category, footer and CUSTOM preview source wiring.')
    print('Scope: static resource/geometry checks and source wiring, NOT Minecraft rendering, API compilation or GameTests.')
if __name__=='__main__':main()
