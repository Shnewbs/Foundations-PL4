"""Validate the prepared 1.16.1 target and apply its reviewed native API backport.
The one-time 1.16.4-to-1.16.1 metadata conversion is already committed on this branch.
"""
from pathlib import Path
import json
from backport_1161_api import backport
ROOT=Path(__file__).resolve().parents[1]
def prepare(root):
    request=json.loads((root/'PORT_TARGET.json').read_text())
    status=json.loads((root/'BUILD_STATUS.json').read_text())
    if request['minecraft']!='1.16.1' or status['minecraft']!='1.16.1' or status['loader_version']!='32.0.108':
        raise ValueError('Refusing a different or unprepared target')
    build=(root/'build.gradle').read_text()
    if "minecraft 'net.minecraftforge:forge:1.16.1-32.0.108'" not in build:
        raise ValueError('Exact Minecraft/Forge build coordinates changed')
    if json.loads((root/'src/main/resources/pack.mcmeta').read_text())['pack']['pack_format']!=5:
        raise ValueError('Incorrect 1.16.1 resource format')
    backport(root)
    # Removing the unsupported event subscription leaves an indented blank line.
    p=root/'src/main/java/net/foundations/pl4/FoundationsPL4.java'
    p.write_text('\n'.join(line.rstrip() for line in p.read_text().splitlines())+'\n',encoding='utf-8')
    print('1.16.1 source prepared; compilation and installed-runtime acceptance remain independent gates')
if __name__=='__main__':prepare(ROOT)
