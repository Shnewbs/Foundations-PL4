"""Run installed Forge 49 native GameTests and the separate production-only smoke.

Set JAVA_HOME to a Java 17 installation before running from a clean checkout.
"""
from pathlib import Path
import shutil
from verify_installed1204 import main as native
from verify_production1204 import main as production


if __name__ == '__main__':
    native()
    production()
    logs = Path(__file__).resolve().parents[1] / 'verification-logs'
    shutil.copyfile(logs / 'installed-1204-native.log', logs / 'native.log')
    print('PASS Forge 49 installed native GameTests and standalone production runtime')
