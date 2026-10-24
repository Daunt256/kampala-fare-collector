"""Offline checks. These do not replace an Android SDK build or a device test."""
from pathlib import Path
import json, os, shutil, subprocess, tempfile, xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'app/src/main/java/com/abaho/farecollector'
NS = {'a': 'http://schemas.android.com/apk/res/android'}

def run(args):
    subprocess.run([str(v) for v in args], cwd=ROOT, check=True)

def main():
    for path in (ROOT/'app/src/main').rglob('*.xml'):
        ET.parse(path)
    manifest = ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot()
    android = '{http://schemas.android.com/apk/res/android}'
    permissions = {node.attrib[android+'name'] for node in manifest.findall('uses-permission')}
    assert permissions == {'android.permission.INTERNET'}, permissions
    catalog=json.loads((ROOT/'config/catalog.json').read_text())
    expected_packages={package for provider in catalog['providers'] for package in provider['packages']}
    packages = {node.attrib[android+'name'] for node in manifest.findall('queries/package')}
    assert packages == expected_packages
    config = ET.parse(ROOT/'app/src/main/res/xml/accessibility_service.xml').getroot()
    assert set(config.attrib[android+'packageNames'].split(',')) == packages
    assert config.attrib[android+'canPerformGestures'] == 'false'
    assert manifest.find('application').attrib[android+'allowBackup'] == 'false'
    json.loads((ROOT/'apps-script/appsscript.json').read_text())
    node = os.environ.get('CODEX_PRIMARY_RUNTIME_NODE') or shutil.which('node')
    java = shutil.which('java')
    if not java or not node:
        raise SystemExit('Install JDK 17 and Node.js to run all checks.')
    with tempfile.TemporaryDirectory(prefix='fare-check-') as tmp:
        run([java, 'com.sun.tools.javac.Main', '-encoding', 'UTF-8', '-d', tmp, JAVA/'FareLogic.java', ROOT/'tests/FareLogicTest.java'])
        run([java, '-cp', tmp, 'FareLogicTest'])
        # Parse every Java source with javac; no fake Android framework is supplied.
        parser = Path(tmp)/'ParseJava.java'
        parser.write_text('''import javax.tools.*;import com.sun.source.util.*;import java.util.*;
public class ParseJava { public static void main(String[] args) throws Exception {
JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();DiagnosticCollector<JavaFileObject> ds=new DiagnosticCollector<>();
try(StandardJavaFileManager fm=compiler.getStandardFileManager(ds,null,null)){
JavacTask task=(JavacTask)compiler.getTask(null,fm,ds,Arrays.asList("-proc:none","-encoding","UTF-8"),null,fm.getJavaFileObjects(args));task.parse();
for(Diagnostic<?> d:ds.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR)throw new Exception(d.toString());}
System.out.println("PASS: Java syntax checked (Android type checking still requires the SDK)");}}
''')
        run([java, 'com.sun.tools.javac.Main', '-d', tmp, parser])
        run([java, '-cp', tmp, 'ParseJava', *sorted(JAVA.glob('*.java'))])
    run([node, ROOT/'tests/receiver.test.cjs', ROOT/'apps-script/Code.gs',ROOT/'config/catalog.json'])
    print('PASS: XML, manifest permissions, package scope and Apps Script manifest')

if __name__ == '__main__':
    main()
