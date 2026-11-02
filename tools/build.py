#!/usr/bin/env python3
"""Portable SDK-only builds. Never installs APKs or changes a device."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
WATCH_ACTIVE = ('MainActivity', 'SessionService', 'SessionPolicy', 'PhoneBridgeClient', 'BackGesture')


def run(args, **kwargs):
    subprocess.run([str(a) for a in args], check=True, **kwargs)


def java_tool(name):
    home = os.environ.get('JAVA_HOME')
    candidate = Path(home) / 'bin' / name if home else None
    if candidate and candidate.is_file():
        return str(candidate)
    found = shutil.which(name)
    if not found:
        raise SystemExit(f'Missing {name}: set JAVA_HOME to a full JDK 17 or newer.')
    return found


def sdk_paths():
    value = os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME')
    if not value:
        raise SystemExit('Set ANDROID_SDK_ROOT to your Android SDK directory.')
    sdk = Path(value).expanduser().resolve()
    bt = sdk / 'build-tools' / os.environ.get('ANDROID_BUILD_TOOLS', '36.0.0')
    platform = os.environ.get('ANDROID_PLATFORM')
    if platform:
        jar = sdk / 'platforms' / platform / 'android.jar'
    else:
        jars = list((sdk / 'platforms').glob('android-*/android.jar'))
        def key(p):
            return tuple(int(n) for n in re.findall(r'\d+', p.parent.name))
        if not jars:
            raise SystemExit('Install SDK platform 36+ using sdkmanager.')
        jar = max(jars, key=key)
    for path in (jar, bt / 'aapt2', bt / 'd8', bt / 'zipalign', bt / 'apksigner'):
        if not path.is_file():
            raise SystemExit(f'Missing SDK file: {path}')
    return bt, jar


def signing():
    names = ('WRISTLAB_KEYSTORE', 'WRISTLAB_KEY_ALIAS', 'WRISTLAB_STORE_PASS', 'WRISTLAB_KEY_PASS')
    configured = [bool(os.environ.get(n)) for n in names]
    if any(configured):
        if not all(configured):
            raise SystemExit('Custom signing needs all four WRISTLAB_* signing variables; see docs/BUILD.md.')
        key = Path(os.environ[names[0]]).expanduser().resolve()
        if not key.is_file():
            raise SystemExit('WRISTLAB_KEYSTORE does not exist.')
        return key, os.environ[names[1]], names[2], names[3]
    # Standard Android debug credentials, never a production/private signing secret.
    key = ROOT / 'build' / 'debug-signing.jks'
    key.parent.mkdir(parents=True, exist_ok=True)
    os.environ['WRISTLAB_DEBUG_PASS'] = 'android'
    if not key.exists():
        run([java_tool('keytool'), '-genkeypair', '-keystore', key, '-storetype', 'JKS',
             '-alias', 'androiddebugkey', '-storepass:env', 'WRISTLAB_DEBUG_PASS',
             '-keypass:env', 'WRISTLAB_DEBUG_PASS', '-keyalg', 'RSA', '-keysize', '2048',
             '-validity', '10000', '-dname', 'CN=WristLab Development,O=Android,C=US'])
    return key, 'androiddebugkey', 'WRISTLAB_DEBUG_PASS', 'WRISTLAB_DEBUG_PASS'


def build_target(target, source, bt, jar, credentials):
    manifest = ET.parse(source / 'AndroidManifest.xml').getroot()
    minimum = manifest.find('uses-sdk').get(ANDROID + 'minSdkVersion')
    version = manifest.get(ANDROID + 'versionName')
    folder = ROOT / 'build' / target
    if folder.exists():
        shutil.rmtree(folder)
    folder.mkdir(parents=True)
    output_dir = ROOT / 'build' / 'apks'
    output_dir.mkdir(parents=True, exist_ok=True)
    output = output_dir / f'wristlab-{target}-{version}.apk'
    # Remove any older version of this target to avoid releasing stale output.
    for old in output_dir.glob(f'wristlab-{target}-*.apk'):
        old.unlink()
    print(f'Building {target} ({version})', flush=True)
    run([bt / 'aapt2', 'compile', '--dir', source / 'res', '-o', folder / 'resources.zip'])
    run([bt / 'aapt2', 'link', '-I', jar, '--manifest', source / 'AndroidManifest.xml',
         '-o', folder / 'resources.apk', folder / 'resources.zip'])
    unsigned = folder / 'unsigned.apk'
    shutil.copy2(folder / 'resources.apk', unsigned)
    sources = []
    if (source / 'src').is_dir():
        if target == 'watch':
            sources = [source / 'src/studio/teja/gesturelab' / (name + '.java') for name in WATCH_ACTIVE]
        else:
            sources = sorted((source / 'src').rglob('*.java'))
    if sources:
        classes, dex = folder / 'classes', folder / 'dex'
        classes.mkdir()
        dex.mkdir()
        run([java_tool('javac'), '--release', '8', '-classpath', jar, '-d', classes, *sources])
        run([java_tool('jar'), 'cf', folder / 'classes.jar', '-C', classes, '.'])
        run([bt / 'd8', '--lib', jar, '--min-api', minimum, '--output', dex, folder / 'classes.jar'])
        with zipfile.ZipFile(unsigned, 'a', zipfile.ZIP_DEFLATED) as z:
            for file in dex.glob('*.dex'):
                z.write(file, file.name)
    run([bt / 'zipalign', '-f', '-p', '4', unsigned, folder / 'aligned.apk'])
    key, alias, store_env, key_env = credentials
    run([bt / 'apksigner', 'sign', '--ks', key, '--ks-key-alias', alias,
         '--ks-pass', 'env:' + store_env, '--key-pass', 'env:' + key_env,
         '--v4-signing-enabled', 'false', '--out', output, folder / 'aligned.apk'])
    run([bt / 'apksigner', 'verify', '--verbose', output])
    run([bt / 'zipalign', '-c', '-p', '4', output])
    print(f'Wrote {output.relative_to(ROOT)}', flush=True)
    return output


def main():
    faces = json.loads((ROOT / 'faces/catalog.json').read_text())
    targets = {'watch': ROOT / 'apps/watch', 'phone': ROOT / 'apps/phone',
               'deck': ROOT / 'experiments/watch-deck'}
    targets.update({f['id']: ROOT / f['path'] for f in faces})
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('target', choices=['all', 'faces', *targets])
    args = parser.parse_args()
    if args.target == 'all':
        selected = list(targets)
    elif args.target == 'faces':
        selected = [f['id'] for f in faces]
    else:
        selected = [args.target]
    bt, jar = sdk_paths()
    credentials = signing()
    files = [build_target(t, targets[t], bt, jar, credentials) for t in selected]
    # Include every built APK, even if a single-target invocation followed a full build.
    checksum = ROOT / 'build/apks/SHA256SUMS'
    checksum.write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest() + '  ' + p.name + '\n'
                                for p in sorted(checksum.parent.glob('*.apk'))))
    print(f'{len(files)} target(s) built; checksums: build/apks/SHA256SUMS')


if __name__ == '__main__':
    main()
