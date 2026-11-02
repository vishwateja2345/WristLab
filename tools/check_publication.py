#!/usr/bin/env python3
"""Check public artifact hygiene and the active manifest/resource contract."""
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
failures = []
files = []
private = re.compile(r'/(?:Users|home)/[^/\s]+/|192\.168\.\d+\.\d+|(?:[0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}|gh[pousr]_[A-Za-z0-9]{20,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----')
for p in ROOT.rglob('*'):
    rel = p.relative_to(ROOT)
    if any(part in ('build', '.git', '__pycache__', '.venv') for part in rel.parts) or not p.is_file():
        continue
    files.append(p)
    if p.suffix.lower() in ('.apk', '.idsig', '.jks', '.keystore', '.p12', '.pem', '.key'):
        failures.append(f'Private/generated artifact: {rel}')
    if p.suffix in ('.java', '.py', '.sh', '.md', '.json', '.yml', '.xml', '.svg') or p.name in ('NOTICE', '.gitignore'):
        text = p.read_text()
        if private.search(text):
            failures.append(f'Possible private path, identifier or credential: {rel}')
    if p.suffix in ('.xml', '.svg'):
        try:
            ET.parse(p)
        except ET.ParseError as e:
            failures.append(f'Invalid XML {rel}: {e}')

contracts = {
    'apps/watch': {'android.permission.BLUETOOTH_CONNECT', 'android.permission.VIBRATE'},
    'apps/phone': {'android.permission.BLUETOOTH_CONNECT'},
}
for path, expected in contracts.items():
    manifest = ET.parse(ROOT / path / 'AndroidManifest.xml').getroot()
    actual = {e.get(ANDROID + 'name') for e in manifest.findall('uses-permission')}
    if actual != expected:
        failures.append(f'Active permissions changed: {path}: {actual}')
    if manifest.findall('.//receiver'):
        failures.append(f'Manifest receiver added to active app: {path}')

access = ET.parse(ROOT / 'apps/phone/res/xml/swipe_service.xml').getroot()
if access.get(ANDROID + 'canRetrieveWindowContent') != 'false':
    failures.append('Phone helper may retrieve screen content')
if access.get(ANDROID + 'accessibilityEventTypes', ''):
    failures.append('Phone helper subscribes to Accessibility events')

catalog = json.loads((ROOT / 'faces/catalog.json').read_text())
for face in catalog:
    folder = ROOT / face['path']
    manifest = ET.parse(folder / 'AndroidManifest.xml').getroot()
    app = manifest.find('application')
    if app.get(ANDROID + 'hasCode') != 'false' or manifest.findall('uses-permission'):
        failures.append(f'Face has executable code or permissions: {face["id"]}')
    properties = {e.get(ANDROID + 'name'):e.get(ANDROID + 'value') for e in app.findall('property')}
    if properties.get('com.google.wear.watchface.format.version') != '2':
        failures.append(f'Not WFF2: {face["id"]}')
    wff = ET.parse(folder / 'res/raw/watchface.xml').getroot()
    for e in wff.iter():
        if e.tag == 'Image' and e.get('resource'):
            resource = e.get('resource')
            # Configuration-controlled image selections are resolved by WFF at runtime.
            if not resource.startswith('[') and not list((folder / 'res/drawable').glob(resource + '.*')):
                failures.append(f'Missing image {resource}: {face["id"]}')

if failures:
    raise SystemExit('\n'.join(failures))
print(f'Publication checks passed: {len(files)} files, {len(catalog)} WFF2 faces; active permission contract intact.')
