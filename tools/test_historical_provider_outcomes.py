import copy
from test_legacy_provider_outcomes import ProviderOutcomeTests
import hourly_builds as builds

class HistoricalProviderTests(ProviderOutcomeTests):
    def validate(self):
        rows=[dict(report=self.report['phase_reports'][p]) for p in ['write','verify','crash-verify']]
        builds.validate_historical_providers(dict(processes=rows),self.mc)
    def test_receipt_token_mismatch(self):
        # Historical provider observations must not manufacture an original operation token.
        for p in self.report['phase_reports'].values():
            for r in p['provider_outcomes']:r.pop('token')
        self.validate()
