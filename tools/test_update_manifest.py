"""Exercise the packaged manifest with a separate Python filesystem model of the updater contract.
This does NOT execute/parse PowerShell, a Windows folder picker or BAT file.
Usage: python tools/test_update_manifest.py BASELINE_SOURCE UPDATE_ROOT
"""
from pathlib import Path
import tempfile,shutil,json,hashlib,re,sys

def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest() if p.is_file() else None

def files(root):return {str(p.relative_to(root)):p.read_bytes() for p in root.rglob('*') if p.is_file() and '.foundations_update_backups' not in p.parts}

def path(root,relative):
    assert ':' not in relative and not re.match(r'^[\\/]',relative) and '..' not in re.split(r'[\\/]',relative),'Unsafe path'
    p=root/relative
    assert p.resolve().is_relative_to(root.resolve()),'Escape path'
    for ancestor in [p,*p.parents]:
        if ancestor==root.parent:break
        assert not ancestor.is_symlink(),'Link path'
    return p

def apply(root,update,fail_after=None,concurrent=None,backup_fail=False):
    package=json.loads((update/'updater/manifest.json').read_text());version=re.search(r"(?m)^version\s*=\s*['\"]([^'\"]+)", (root/'build.gradle').read_text())[1]
    assert "archivesName = 'FoundationsPL4-1.21.1'" in (root/'build.gradle').read_text()
    entries=package['baselines'][version]['files'];payload=update/'source'
    for e in entries:
        assert digest(path(root,e['path']))==e['before_sha256'],'Baseline mismatch'
        if e['after_sha256'] is not None:assert digest(path(payload,e['path']))==e['after_sha256'],'Payload mismatch'
    backup=root/'.foundations_update_backups/simulation/files'
    for e in entries:
        if e['before_sha256'] is not None:
            copy=path(backup,e['path']);copy.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(root/e['path'],copy)
            if backup_fail:copy.write_bytes(b'bad backup')
            assert digest(copy)==e['before_sha256'],'Bad backup'
    applied=[]
    try:
        for i,e in enumerate(entries):
            dest=path(root,e['path'])
            if concurrent is not None and i==concurrent:dest.write_bytes(b'concurrent local edit')
            assert digest(dest)==e['before_sha256'],'Concurrent edit'
            applied.append(e)
            if e['after_sha256'] is None:dest.unlink()
            else:dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(payload/e['path'],dest)
            if fail_after is not None and i==fail_after:raise OSError('Simulated disk failure')
            assert digest(dest)==e['after_sha256'],'Post-write hash'
    except Exception:
        for e in reversed(applied):
            dest=path(root,e['path'])
            if e['before_sha256'] is None:dest.unlink(missing_ok=True)
            else:shutil.copy2(backup/e['path'],dest)
            assert digest(dest)==e['before_sha256'],'Bad rollback'
        raise
    return entries

def main():
    baseline=Path(sys.argv[1]).resolve();update=Path(sys.argv[2]).resolve();entries=json.loads((update/'updater/manifest.json').read_text())['baselines']['0.0.1a.R9']['files']
    results=[]
    with tempfile.TemporaryDirectory(prefix='pl4-r10-updater-sim-') as t:
        base=Path(t)
        for case in ['apply','local-change','missing-file','new-file-collision','tampered-payload','wrong-version','disk-failure','late-concurrent-edit','bad-backup','preserve-unrelated','traversal-rejected','symlink-rejected']:
            root=base/case/'Project with spaces & !';root.parent.mkdir(parents=True);shutil.copytree(baseline,root,ignore=shutil.ignore_patterns('__pycache__','.gradle','build'))
            expected=files(root);u=update;changed=next(e for e in entries if e['before_sha256'] is not None and e['path']!='build.gradle');added=next(e for e in entries if e['before_sha256'] is None);kwargs={}
            if case=='local-change':(root/changed['path']).write_bytes(b'local edit');expected=files(root)
            if case=='missing-file':(root/changed['path']).unlink();expected=files(root)
            if case=='new-file-collision':p=root/added['path'];p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(b'local file');expected=files(root)
            if case=='tampered-payload':u=base/'tampered-update';shutil.copytree(update,u);(u/'source'/changed['path']).write_bytes(b'tampered')
            if case=='wrong-version':p=root/'build.gradle';p.write_text(p.read_text().replace('0.0.1a.R9','0.0.1a.R8'));expected=files(root)
            if case=='disk-failure':kwargs['fail_after']=len(entries)//2
            if case=='late-concurrent-edit':idx=len(entries)-2;kwargs['concurrent']=idx;expected[entries[idx]['path']]=b'concurrent local edit'
            if case=='bad-backup':kwargs['backup_fail']=True
            if case=='preserve-unrelated':p=root/'config/custom-local.json';p.parent.mkdir(exist_ok=True);p.write_bytes(b'{"local":true}');expected=files(root)
            if case=='traversal-rejected':
                for name in ['../escape','nested/../../escape','C:/escape','/escape','..\\escape']:
                    try:path(root,name)
                    except AssertionError:pass
                    else:raise AssertionError(name)
                assert files(root)==expected;results.append(case);continue
            if case=='symlink-rejected':
                link=root/'linked';link.symlink_to(base,target_is_directory=True)
                try:path(root,'linked/outside')
                except AssertionError:pass
                else:raise AssertionError('Link accepted')
                link.unlink();assert files(root)==expected;results.append(case);continue
            try:apply(root,u,**kwargs)
            except (AssertionError,KeyError,OSError):
                assert case not in ['apply','preserve-unrelated'],case
                assert files(root)==expected,(case,'Project did not remain/return to expected state')
            else:
                assert case in ['apply','preserve-unrelated'],case
                for e in entries:
                    if e['after_sha256'] is None:expected.pop(e['path'],None)
                    else:expected[e['path']]=(update/'source'/e['path']).read_bytes()
                assert files(root)==expected,(case,'Mismatch after application')
            results.append(case)
    print(json.dumps({'status':'PASS','model':'Python filesystem simulation, NOT PowerShell/Windows execution','cases':results,'file_delta':len(entries)},indent=2))
if __name__=='__main__':main()
