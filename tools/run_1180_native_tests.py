"""Run installed Forge 38 native GameTests and the separate production-only smoke.

This entry point uses local Forge installation, not Gradle runServer/client assets.
Set JAVA_HOME to a Java 17 installation before running it from a clean checkout.
"""
from pathlib import Path
import shutil
from verify_installed118 import main as native
from verify_production118 import main as production


if __name__ == '__main__':
    native()
    production()
    logs = Path(__file__).resolve().parents[1] / 'verification-logs'
    shutil.copyfile(logs / 'installed-118-native.log', logs / 'native.log')
    print('PASS Forge 38 installed native GameTests and standalone production runtime')
