import datetime as dt
import unittest
from audit_version_coverage import inventory

class CoverageTests(unittest.TestCase):
    def setUp(self):
        self.now=dt.datetime(2026,10,9,tzinfo=dt.timezone.utc)
        self.manifest={'versions':[{'id':v,'type':'release','releaseTime':'2020-01-01T00:00:00Z'} for v in ['1.6.3','1.6.4','1.7.10','1.8.9','1.12.2','1.16.4','1.16.5','1.18.2']]}
        self.forge='<metadata><versioning><versions><version>1.16.4-35.1.37</version><version>1.16.5-36.2.42</version></versions></versioning></metadata>'
    def records(self,releases=None,branches=None):
        return {r['minecraft']:r for r in inventory(self.manifest,self.forge,branches or [],releases or [],self.now)}
    def test_explicit_targets_never_conflated(self):
        r=self.records(branches=[{'name':'mc/1.16.5'}]);self.assertFalse(r['1.16.4']['branch_present']);self.assertTrue(r['1.16.5']['branch_present']);self.assertIn('1.18.2',r)
    def test_exclusions_and_upstream(self):
        r=self.records();self.assertNotIn('1.6.3',r);self.assertEqual(r['1.7.10']['coverage'],'EXCLUDED_BY_USER');self.assertEqual(r['1.12.2']['coverage'],'UPSTREAM_LISTED');self.assertTrue(r['1.6.4']['experimental']);self.assertEqual(r['1.8.9']['coverage'],'MISSING_PL4_BUILD')
    def test_branch_alone_is_not_release(self):
        r=self.records(branches=[[{'name':'mc/1.16.4'}]]);self.assertEqual(r['1.16.4']['coverage'],'MISSING_PL4_BUILD')
    def test_only_uploaded_runtime_counts(self):
        rel={'tag_name':'mc1.18.2-v0.2a-port.2','published_at':'2026-10-08T00:00:00Z','prerelease':True,'draft':False,'assets':[{'name':'FoundationsPL4-1.18.2-0.2a-port.2-sources.jar','state':'uploaded'}]}
        self.assertEqual(self.records([[rel]])['1.18.2']['coverage'],'MISSING_PL4_BUILD')
        rel['assets'].append({'name':'FoundationsPL4-1.18.2-0.2a-port.2.jar','state':'uploaded'})
        r=self.records([[rel]])['1.18.2'];self.assertEqual(r['coverage'],'PL4_ARTIFACT_PRESENT');self.assertEqual(r['api_runtime_visual_acceptance'],'SEE_TARGET_EVIDENCE_NOT_INFERRED')
        rel['draft']=True;self.assertEqual(self.records([rel])['1.18.2']['coverage'],'MISSING_PL4_BUILD')
    def test_snapshots_and_future_releases_not_claimed(self):
        self.manifest['versions'] += [{'id':'26.4','type':'release','releaseTime':'2027-01-01T00:00:00Z'},{'id':'26.3-snapshot-1','type':'snapshot','releaseTime':'2026-01-01T00:00:00Z'}]
        r=self.records();self.assertNotIn('26.4',r);self.assertNotIn('26.3-snapshot-1',r)
    def test_forge_match_is_exact(self):
        r=self.records();self.assertTrue(r['1.16.4']['forge_artifacts_listed']);self.assertFalse(r['1.18.2']['forge_artifacts_listed'])
    def test_xml_declarations_rejected(self):
        self.forge='<!DOCTYPE metadata>'+self.forge
        with self.assertRaises(ValueError):self.records()
    def test_duplicates_rejected(self):
        self.manifest['versions'].append(self.manifest['versions'][-1])
        with self.assertRaises(ValueError):self.records()
    def test_future_upload_not_counted(self):
        rel={'tag_name':'future','published_at':'2027-01-01T00:00:00Z','assets':[{'name':'FoundationsPL4-1.16.4-0.2a-port.1.jar','state':'uploaded'}]}
        self.assertEqual(self.records([rel])['1.16.4']['coverage'],'MISSING_PL4_BUILD')

if __name__=='__main__':unittest.main()
