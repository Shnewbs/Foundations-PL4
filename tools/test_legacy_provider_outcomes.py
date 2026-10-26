import copy,unittest
import hourly_builds as builds

class ProviderOutcomeTests(unittest.TestCase):
    def setUp(self):
        self.mc='1.12.2';self.report=dict(phase_reports={})
        for phase in ['write','verify','crash-write','crash-verify']:
            tokens=['00000000-0000-0000-0000-000000000001','00000000-0000-0000-0000-000000000002']
            rows=[]
            for i,kind in enumerate(['insert','extract']):
                extraction=kind=='extract'
                rows.append(dict(provider_id='00000000-0000-0000-0000-00000000000'+str(i+3),owner='fae1b877-c6b4-4538-a26a-630190b54205',outcome=kind,token=tokens[i],source_units=32,destination_units=0 if extraction else 8,unaccounted_units=8 if extraction else 0,sealed_uncertain_claim=0 if extraction else 8,mutation_calls=1,removed_units=8 if extraction else 0,accepted_units=0 if extraction else 8))
            self.report['phase_reports'][phase]=dict(tokens=tokens,provider_outcomes=rows)
    def validate(self):builds.validate_provider_outcomes(self.report,self.mc)
    def mutate_all(self,key,value,index=0):
        for phase in self.report['phase_reports'].values():phase['provider_outcomes'][index][key]=value
    def test_current_provider_pair(self):self.validate()
    def test_legacy_owner(self):
        self.mc='1.6.4'
        for i in range(2):self.mutate_all('owner','pl4owner',i)
        self.validate()
    def test_private_state_changed_after_restart(self):
        self.report['phase_reports']['verify']['provider_outcomes'][0]['mutation_calls']=2
        with self.assertRaises(ValueError):self.validate()
    def test_missing_persisted_provider(self):
        self.report['phase_reports']['verify'].pop('provider_outcomes')
        with self.assertRaises(ValueError):self.validate()
    def test_sealed_claim_cannot_be_spendable(self):
        self.mutate_all('destination_units',16)
        with self.assertRaises(ValueError):self.validate()
    def test_unaccounted_loss_cannot_be_zeroed(self):
        self.mutate_all('unaccounted_units',0,1)
        with self.assertRaises(ValueError):self.validate()
    def test_different_owner(self):
        self.mutate_all('owner','someone-else')
        with self.assertRaises(ValueError):self.validate()
    def test_duplicate_provider_identity(self):
        self.mutate_all('provider_id','00000000-0000-0000-0000-000000000003',1)
        with self.assertRaises(ValueError):self.validate()
    def test_boolean_counter_rejected(self):
        self.mutate_all('mutation_calls',True)
        with self.assertRaises(ValueError):self.validate()
    def test_receipt_token_mismatch(self):
        self.mutate_all('token','00000000-0000-0000-0000-000000000007')
        with self.assertRaises(ValueError):self.validate()

if __name__=='__main__':unittest.main()
