"""Audit published NeoForge loader config enums; writes config-api-audit.json."""
import urllib.request,xml.etree.ElementTree as E,concurrent.futures,json,zipfile,io,pathlib,re
base='https://maven.neoforged.net/releases/'
metadata=E.fromstring(urllib.request.urlopen(base+'net/neoforged/neoforge/maven-metadata.xml',timeout=30).read())
versions=[x.text for x in metadata.findall('.//version') if x.text.startswith(('21.1.','26.1.','26.2.','26.3.','26.4.'))]
ns={'m':'http://maven.apache.org/POM/4.0.0'}
def read(v):
 u=f'{base}net/neoforged/neoforge/{v}/neoforge-{v}.pom'
 for attempt in range(3):
  try:
   root=E.fromstring(urllib.request.urlopen(u,timeout=30).read())
   dep=next(d for d in root.findall('m:dependencies/m:dependency',ns) if d.findtext('m:artifactId',namespaces=ns)=='loader')
   return v,dep.findtext('m:version',namespaces=ns)
  except Exception:
   if attempt==2:raise
with concurrent.futures.ThreadPoolExecutor(max_workers=24) as pool: deps=dict(pool.map(read,versions))
results={}
for loader in sorted(set(deps.values())):
 u=f'{base}net/neoforged/fancymodloader/loader/{loader}/loader-{loader}-sources.jar'
 with zipfile.ZipFile(io.BytesIO(urllib.request.urlopen(u,timeout=30).read())) as z:
  s=z.read('net/neoforged/fml/config/ModConfig.java').decode()
  enum=s[s.index('public enum Type'):]
  names=re.findall(r'^\s*(STARTUP|COMMON|LOCAL|CLIENT|SERVER|SYNCED)\s*[,;]',enum,re.M)
  results[loader]=names
  print('LOADER',loader,names,flush=True)
report={'versions':deps,'config_types':results}
pathlib.Path('config-api-audit.json').write_text(json.dumps(report,indent=2))
for prefix in ['21.1.','26.1.','26.2.','26.3.','26.4.']:
 rows=[(v,l) for v,l in deps.items() if v.startswith(prefix)]
 print(prefix,'builds',len(rows),'loaders',sorted(set(l for _,l in rows)),flush=True)
print('26.3 SYNCED builds:',[v for v,l in deps.items() if v.startswith('26.3.') and 'SYNCED' in results[l]])
