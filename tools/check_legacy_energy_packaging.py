"""Fail closed on runtime/fixture contamination and omitted energy resources."""
from pathlib import Path
import json,zipfile,sys,hashlib,re
target=sys.argv[1];root=Path.cwd();version='0.2a-legacy-preview.3'
jar=root/'build/libs'/('FoundationsPL4-'+target+'-'+version+('-srg.jar' if target=='1.6.4' else '.jar'))
with zipfile.ZipFile(jar) as z:
    names=set(z.namelist())
    for cls in ['ConservingEnergy','LegacyEnergy','LegacyEnergyPlatform','LegacyEnergyAccess']:
        assert 'net/foundations/pl4/legacy/'+cls+'.class' in names,cls
    assert not any(n.startswith(('net/minecraft/','net/minecraftforge/','cofh/','ic2/')) or '/tests/' in n for n in names)
    if target=='1.12.2':
        for name in ['legacy_energy_export','legacy_energy_import','legacy_energy_buffer']:
            for directory in ['blockstates','models/block','models/item']:assert 'assets/foundations_pl4/'+directory+'/'+name+'.json' in names
    else:
        assert not any(b'net/minecraftforge/energy/' in z.read(n) for n in names if n.endswith('.class'))
log=(root/'verification-logs/build.log').read_text(errors='replace')
checks={label:int(re.search(pattern,log).group(1)) for label,pattern in [('energy',r'PASS legacy energy production rules: (\d+)'),('items',r'PASS legacy production rules: (\d+)'),('fluids',r'PASS fluid production rules: (\d+)')]}
summary=json.loads((root/'verification-logs/clean-native-summary.json').read_text())
summary.update(portable_assertions=checks,energy_runtime_sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),version=version)
(root/'verification-logs/energy-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
print('PASS energy runtime packaging and production rules:',checks)
