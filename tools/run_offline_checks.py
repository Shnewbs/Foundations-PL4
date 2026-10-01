"""Run JDK-only rules, Java parsing and resource/source guards without Gradle or external downloads."""
from pathlib import Path
import subprocess,sys,tempfile
ROOT=Path(__file__).resolve().parents[1]

def run(*args):
    print('+ '+' '.join(map(str,args)),flush=True)
    subprocess.run(list(map(str,args)),cwd=ROOT,check=True)

def main():
    with tempfile.TemporaryDirectory(prefix='foundations-pl4-r5-checks-') as tmp:
        core=ROOT/'src/main/java/net/foundations/pl4/core'
        classes=[core/(n+'.java') for n in ['MechanicalEnergyAccess','ConnectionRules','DisjointSets','HammerMotion','HammerGeometry','DisplayLayout','DisplayFacing','EnergyValues','EnergyConversion','ReflectiveEnergyTransfer','ReflectiveElectrodynamicTransfer','ReflectiveEnergyAccess','MultipartTopology','DisplayPlacement','CanvasContinuity','HologramProjection','GuideLayout','GuideBook','DisplayElements','LayoutTransactions','EditorSelection','DisplayPicking','MonitorPresentation','GuideNavigation','DynamicCanvasLayout','EditorChrome','PartItemDataRules','EditorPalette','TransferRules']]
        run('javac','--release','21','-encoding','UTF-8','-d',tmp,*classes,ROOT/'src/main/java/net/foundations/pl4/Kind.java',ROOT/'tools/MechanicalEnergyRegressionTests.java',ROOT/'tools/EnergyConversionRegressionTests.java',ROOT/'tools/R7RegressionTests.java',ROOT/'tools/R8RegressionTests.java',ROOT/'tools/R9RegressionTests.java',ROOT/'tools/R10RegressionTests.java',ROOT/'tools/R11RegressionTests.java',ROOT/'tools/R12RegressionTests.java',ROOT/'tools/R13RegressionTests.java',ROOT/'tools/R14RegressionTests.java',ROOT/'tools/R17RegressionTests.java',ROOT/'tools/R16RegressionTests.java',ROOT/'tools/R5RegressionTests.java',ROOT/'tools/VerifyJavaSyntax.java',ROOT/'tools/R6RegressionTests.java')
        run('java','-cp',tmp,'MechanicalEnergyRegressionTests')
        run('java','-cp',tmp,'EnergyConversionRegressionTests')
        run('java','-cp',tmp,'R5RegressionTests')
        run('java','-cp',tmp,'R6RegressionTests')
        run('java','-cp',tmp,'R7RegressionTests')
        run('java','-cp',tmp,'R8RegressionTests')
        run('java','-cp',tmp,'R9RegressionTests')
        run('java','-cp',tmp,'R10RegressionTests')
        run('java','-cp',tmp,'R11RegressionTests')
        run('java','-cp',tmp,'R12RegressionTests')
        run('java','-cp',tmp,'R13RegressionTests')
        run('java','-cp',tmp,'R14RegressionTests')
        run('java','-cp',tmp,'R17RegressionTests')
        run('java','-cp',tmp,'R16RegressionTests')
        run('java','-cp',tmp,'VerifyJavaSyntax',ROOT)
    run(sys.executable,ROOT/'tools/test_curseforge_upload.py')
    run(sys.executable,ROOT/'tools/verify_r5_assets.py')
    run(sys.executable,ROOT/'tools/test_screen_layers.py')
    run(sys.executable,ROOT/'tools/verify_r6.py')
    run(sys.executable,ROOT/'tools/verify_r7.py')
    run(sys.executable,ROOT/'tools/test_r7_mutants.py')
    run(sys.executable,ROOT/'tools/verify_r8.py')
    run(sys.executable,ROOT/'tools/test_r8_mutations.py')
    run(sys.executable,ROOT/'tools/verify_r9.py')
    run(sys.executable,ROOT/'tools/test_r9_mutations.py')
    run(sys.executable,ROOT/'tools/verify_r10.py')
    run(sys.executable,ROOT/'tools/test_r10_mutations.py')
    run(sys.executable,ROOT/'tools/verify_r11.py')
    run(sys.executable,ROOT/'tools/verify_r11_final_hotfix.py')
    run(sys.executable,ROOT/'tools/verify_r12.py')
    run(sys.executable,ROOT/'tools/verify_r13.py')
    run(sys.executable,ROOT/'tools/verify_r14.py')
    run(sys.executable,ROOT/'tools/verify_r17.py')
    run(sys.executable,ROOT/'tools/verify_r16.py')
    print('OFFLINE CHECKS PASSED. Native mod compilation, GameTests, graphical rendering and Windows updater execution remain separate checks.',flush=True)

if __name__=='__main__':
    try:main()
    except (OSError,subprocess.CalledProcessError) as exc:
        print(f'OFFLINE CHECK FAILED: {exc}',file=sys.stderr);sys.exit(1)
