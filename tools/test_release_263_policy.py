import unittest
from release_263_policy import eligible,audit
class ExactTargetReleaseTests(unittest.TestCase):
    def setUp(self):
        self.ok=dict(minecraft="26.3",version="0.2.0-rc.1",release_channel="beta",rc_publication_cleared=True)
        self.sha="a"*40
    def check(self,status=None,event="push",ref="refs/tags/mc26.3-v0.2.0-rc.1",sha=None,head=None):
        return eligible(self.ok if status is None else status,event,ref,self.sha if sha is None else sha,
                        self.sha if head is None else head)
    def test_exact_rc(self): self.assertEqual(self.check(),"0.2.0-rc.1")
    def test_dev_stable_nightly(self):
        for v in ("0.2.0-rc.1-dev","1.0.0","1.0.0-nightly"):
            with self.subTest(v=v),self.assertRaises(ValueError):self.check(dict(self.ok,version=v))
    def test_maturity_clearance_and_neighbor(self):
        for d in (dict(release_channel="development"),dict(rc_publication_cleared=False),
                  dict(rc_publication_cleared="true"),dict(minecraft="1.21.1")):
            with self.subTest(d=d),self.assertRaises(ValueError):self.check(dict(self.ok,**d))
    def test_wrong_branch_manual_and_tags(self):
        for event,ref in (("workflow_dispatch","refs/heads/mc/26.3"),("push","refs/heads/mc/26.3"),
                          ("push","refs/tags/v0.2.0-rc.1"),("push","refs/tags/mc26.1.2-v0.2.0-rc.1")):
            with self.subTest(ref=ref),self.assertRaises(ValueError):self.check(event=event,ref=ref)
    def test_wrong_source_sha(self):
        with self.assertRaises(ValueError):self.check(head="b"*40)
        with self.assertRaises(ValueError):self.check(sha="bad")
    def test_branch_publishers(self):audit()
if __name__=="__main__":unittest.main()
