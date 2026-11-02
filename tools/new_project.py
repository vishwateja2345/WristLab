#!/usr/bin/env python3
"""Scaffold an independent project. Does not install or change any device."""
import argparse
import json
from pathlib import Path
import re
import shutil
import xml.etree.ElementTree as ET
from catalog import ROOT


def scaffold(identifier, name, kind, root=ROOT):
    if not re.fullmatch(r'[a-z][a-z0-9-]{2,39}', identifier):
        raise ValueError('Use a lowercase id starting with a letter, 3–40 letters/digits/hyphens.')
    if not name.strip() or len(name) > 70 or any(ord(c) < 32 for c in name):
        raise ValueError('Use a nonempty, single-line display name of at most 70 characters.')
    destination = root / 'projects' / identifier
    if destination.exists():
        raise ValueError('Project already exists; existing work will not be overwritten.')
    android = kind in ('watch-app', 'phone-app', 'watch-face')
    destination.mkdir(parents=True)
    package = 'org.example.wristlab.p_' + identifier.replace('-', '_')
    if kind == 'watch-face':
        # Reuse a licensed original face, not an unlicensed image or paid template.
        for part in ('res', 'AndroidManifest.xml', 'preview.png'):
            source = root / 'faces/terminal' / part
            if source.is_dir():
                shutil.copytree(source, destination / part)
            else:
                shutil.copy2(source, destination / part)
        manifest = ET.parse(destination / 'AndroidManifest.xml')
        manifest.getroot().set('package', package)
        manifest.getroot().set('{http://schemas.android.com/apk/res/android}versionCode', '1')
        manifest.getroot().set('{http://schemas.android.com/apk/res/android}versionName', '0.1-preview')
        manifest.write(destination / 'AndroidManifest.xml', encoding='utf-8', xml_declaration=True)
        strings = destination / 'res/values/strings.xml'
        tree = ET.parse(strings)
        for entry in tree.getroot():
            if entry.get('name') == 'app_name':
                entry.text = name
        tree.write(strings, encoding='utf-8', xml_declaration=True)
    elif android:
        feature = '<uses-feature android:name="android.hardware.type.watch" android:required="true"/>' if kind == 'watch-app' else ''
        (destination / 'res/values').mkdir(parents=True)
        resources = ET.Element('resources')
        ET.SubElement(resources, 'string', name='app_name').text = name
        ET.ElementTree(resources).write(destination / 'res/values/strings.xml', encoding='utf-8', xml_declaration=True)
        (destination / 'AndroidManifest.xml').write_text(f'''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{package}" android:versionCode="1" android:versionName="0.1-preview">
<uses-sdk android:minSdkVersion="31" android:targetSdkVersion="36"/>{feature}
<application android:label="@string/app_name" android:allowBackup="false" android:theme="@android:style/Theme.Material.NoActionBar">
<activity android:name=".MainActivity" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity>
</application></manifest>\n''')
        java = destination / 'src' / package.replace('.', '/') / 'MainActivity.java'
        java.parent.mkdir(parents=True)
        java.write_text(f'''// SPDX-License-Identifier: Apache-2.0
package {package};

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Minimal foreground-only starter. No service, permission or connection to Gesture Lab. */
public final class MainActivity extends Activity {{
  @Override public void onCreate(Bundle state) {{
    super.onCreate(state);
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setGravity(Gravity.CENTER);
    layout.setPadding(28, 28, 28, 28);
    layout.setBackgroundColor(Color.BLACK);
    TextView title = new TextView(this);
    title.setText({json.dumps(name, ensure_ascii=False)});
    title.setTextSize(24);
    title.setTextColor(0xff83e1ce);
    title.setGravity(Gravity.CENTER);
    layout.addView(title);
    Button close = new Button(this);
    close.setText("Close");
    close.setOnClickListener(view -> finish());
    layout.addView(close);
    setContentView(layout);
  }}
}}\n''')
    else:
        (destination / 'src').mkdir()
    metadata = dict(id=identifier, name=name, summary='Describe the concrete user problem this project solves.',
                    kind=kind, status='draft', readme='README.md', license='Apache-2.0',
                    permissions=[], build_system='android-sdk' if android else 'none')
    if android:
        metadata['build_target'] = 'project-' + identifier
    (destination / 'project.json').write_text(json.dumps(metadata, indent=2) + '\n')
    build = f'python3 tools/build.py project-{identifier}' if android else 'Document your own build/run instructions here; this project does not use the Android builder.'
    credit = '\nThis starter derives from WristLab Terminal. Retain root NOTICE and font OFL notices when redistributing. Its preview is unchanged sample artwork until you replace it.\n' if kind == 'watch-face' else ''
    (destination / 'README.md').write_text(f'''# {name}

Status: **draft starter; not tested on a device**.

## Purpose

Describe a concrete use case. This project is independent of Gesture Lab.

## Build

From the WristLab repository root:

```text
{build}
```

## Setup and stop

Document supported devices, installation, required permissions, how the wearer starts/stops it, and how to undo setup. Do not install or change devices as part of the build.

## Validation

List exact checks and actual device observations. Keep build results separate from battery or recognition measurements.

## Battery, privacy and assets

The app starter has no permissions or service. Document any changes. List asset sources and redistribution terms; retain root license/NOTICE and third-party notices.
{credit}''')
    return destination


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('id')
    parser.add_argument('--name', required=True)
    parser.add_argument('--kind', choices=('watch-app', 'phone-app', 'watch-face', 'tool', 'research'), default='watch-app')
    args = parser.parse_args()
    try:
        destination = scaffold(args.id, args.name, args.kind)
    except ValueError as error:
        raise SystemExit(str(error))
    print(f'Created {destination.relative_to(ROOT)}. Edit its README and project.json, then run tools/catalog.py check.')


if __name__ == '__main__':
    main()
