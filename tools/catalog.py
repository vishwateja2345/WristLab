#!/usr/bin/env python3
"""List and validate bundled examples and independently contributed projects."""
import argparse
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
KINDS = ('watch-app', 'phone-app', 'watch-face', 'tool', 'integration', 'research')
STATUSES = ('draft', 'build-tested', 'device-tested', 'paused')


def load_projects(root=ROOT):
    projects = []
    for entry in json.loads((root / 'catalog/projects.json').read_text())['projects']:
        projects.append(dict(entry, origin='bundled'))
    for file in sorted((root / 'projects').glob('*/project.json')):
        if file.parent.name.startswith('_'):
            continue
        entry = json.loads(file.read_text())
        entry['path'] = str(file.parent.relative_to(root))
        entry['origin'] = 'community'
        projects.append(entry)
    return projects


def validate(projects, root=ROOT):
    errors, ids, targets = [], set(), set()
    for project in projects:
        identifier = project.get('id', '')
        if not re.fullmatch(r'[a-z][a-z0-9-]{2,49}', identifier) or identifier in ids:
            errors.append(f'Invalid or duplicate project id: {identifier}')
        ids.add(identifier)
        for field in ('name', 'summary', 'license', 'readme'):
            if not isinstance(project.get(field), str) or not project[field].strip():
                errors.append(f'{identifier}: missing {field}')
        if project.get('kind') not in KINDS or project.get('status') not in STATUSES:
            errors.append(f'{identifier}: unknown kind/status')
        if not isinstance(project.get('permissions'), list):
            errors.append(f'{identifier}: list permissions explicitly, including an empty list')
        folder = (root / project.get('path', '')).resolve()
        if root.resolve() not in folder.parents or not folder.is_dir():
            errors.append(f'{identifier}: project path must be a directory inside the repository')
            continue
        readme = (folder / project.get('readme', '')).resolve()
        if folder not in readme.parents or not readme.is_file():
            errors.append(f'{identifier}: README must exist inside its project')
        system = project.get('build_system')
        if system not in ('android-sdk', 'manual', 'none'):
            errors.append(f'{identifier}: unknown build_system')
        if system == 'android-sdk':
            target = project.get('build_target', '')
            if not re.fullmatch(r'[a-z][a-z0-9-]{2,59}', target) or target in targets:
                errors.append(f'{identifier}: invalid or duplicate build target')
            targets.add(target)
            if project['origin'] == 'community' and target != 'project-' + identifier:
                errors.append(f'{identifier}: community build target must be project-{identifier}')
            if not (folder / 'AndroidManifest.xml').is_file() or not (folder / 'res').is_dir():
                errors.append(f'{identifier}: android-sdk target needs a manifest and res directory')
            else:
                manifest = ET.parse(folder / 'AndroidManifest.xml').getroot()
                declared = {e.get('{http://schemas.android.com/apk/res/android}name') for e in manifest.findall('uses-permission')}
                permissions = project.get('permissions', [])
                if not all(isinstance(p, str) for p in permissions) or set(permissions) != declared:
                    errors.append(f'{identifier}: metadata permissions must match its Android manifest')
    if errors:
        raise ValueError('\n'.join(errors))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=('list', 'check'))
    parser.add_argument('--json', action='store_true')
    args = parser.parse_args()
    projects = load_projects()
    try:
        validate(projects)
    except ValueError as error:
        raise SystemExit(str(error))
    if args.action == 'check':
        print(f'Catalog valid: {len(projects)} projects; community projects discovered automatically.')
    elif args.json:
        print(json.dumps(projects, indent=2))
    else:
        for project in projects:
            print(f'{project["id"]:24} {project["kind"]:12} {project["status"]:14} {project["origin"]:9} {project["path"]}')


if __name__ == '__main__':
    main()
