"""Deterministic re-authored Foundations-style book sprites; no original upstream PNG is modified."""
from pathlib import Path
import json,random
from PIL import Image,ImageDraw
R=Path(__file__).resolve().parents[1];A=R/'src/main/resources/assets/foundations_pl4'
D=A/'textures/gui/sprites/field_guide';D.mkdir(parents=True,exist_ok=True)
SIZE=256
for name,base in [('cover',(43,47,48)),('page',(222,216,193))]:
    rng=random.Random(804 if name=='cover' else 816)
    image=Image.new('RGBA',(SIZE,SIZE));pix=image.load()
    for y in range(SIZE):
        for x in range(SIZE):
            n=rng.randint(-5,5) if name=='cover' else rng.randint(-2,2)
            pix[x,y]=tuple(max(0,min(255,c+n)) for c in base)+(255,)
    d=ImageDraw.Draw(image);last=SIZE-1
    if name=='cover':
        d.rectangle((0,0,last,last),outline=(18,23,25),width=2);d.rectangle((2,2,last-2,last-2),outline=(97,105,100));d.rectangle((4,4,last-4,last-4),outline=(25,32,34))
        for p in range(7,SIZE-7,5):
            d.line((p,3,p+1,3),fill=(173,167,142));d.line((p,last-3,p+1,last-3),fill=(173,167,142));d.line((3,p,3,p+1),fill=(173,167,142));d.line((last-3,p,last-3,p+1),fill=(173,167,142))
        for x,y in [(1,1),(SIZE-6,1),(1,SIZE-6),(SIZE-6,SIZE-6)]:
            d.rectangle((x,y,x+4,y+4),fill=(83,113,118),outline=(24,56,64));d.point((x+2,y+2),fill=(134,200,207))
        border=7
    else:
        d.rectangle((0,0,last,last),outline=(136,120,91));d.rectangle((1,1,last-1,last-1),outline=(176,162,128));d.rectangle((2,2,last-2,last-2),outline=(205,194,163));d.line((3,last-2,last-3,last-2),fill=(153,146,125))
        border=4
    image.save(D/(name+'.png'))
    (D/(name+'.png.mcmeta')).write_text(json.dumps({'gui':{'scaling':{'type':'nine_slice','width':SIZE,'height':SIZE,'border':border}},'texture':{'blur':False,'clamp':False}},indent=2)+'\n')
# Physical field-guide item; original guide.png remains available but is no longer the selected item skin.
item=Image.new('RGBA',(32,32));d=ImageDraw.Draw(item)
d.polygon([(6,3),(25,1),(28,4),(28,28),(9,31),(5,27)],fill=(17,29,34),outline=(10,16,20))
d.polygon([(8,7),(26,5),(26,26),(9,29)],fill=(214,207,181),outline=(135,135,115))
for y in [24,26,28]:d.line((9,y,26,y-2),fill=(160,155,130))
d.polygon([(6,3),(24,1),(26,4),(26,25),(8,28),(5,26)],fill=(41,62,70),outline=(14,32,40))
d.line((9,4,9,26),fill=(98,114,106),width=1)
d.polygon([(22,3),(24,3),(24,22),(23,20),(22,22)],fill=(80,193,217))
d.rectangle((13,10,21,18),outline=(111,219,230),width=1);d.rectangle((16,13,18,15),fill=(185,239,234))
d.line((11,20,22,18),fill=(140,174,163));d.line((12,22,21,20),fill=(140,174,163))
(A/'textures/item/field_guide.png').parent.mkdir(parents=True,exist_ok=True);item.save(A/'textures/item/field_guide.png')
(A/'models/item/plguide.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'foundations_pl4:item/field_guide'}},indent=2)+'\n')
lang=A/'lang/en_us.json';values=json.loads(lang.read_text());values['item.foundations_pl4.plguide']='Foundations PL4 Field Guide';lang.write_text(json.dumps(values,indent=2,ensure_ascii=False)+'\n')
print('R8 guide assets generated: cover, page, field-guide item')
