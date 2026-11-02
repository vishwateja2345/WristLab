#!/usr/bin/env python3
"""Regression checks for safe scaffolding and independent project discovery."""
import json
from pathlib import Path
import shutil
import tempfile
import unittest
import xml.etree.ElementTree as ET
from catalog import ROOT, load_projects, validate
from new_project import scaffold


class ProjectTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        (self.root / 'catalog').mkdir()
        (self.root / 'catalog/projects.json').write_text('{"projects":[]}')
        (self.root / 'projects').mkdir()

    def tearDown(self):
        self.temp.cleanup()

    def test_standalone_app_and_catalog(self):
        directory = scaffold('new-watch', 'A & B "Watch"', 'watch-app', self.root)
        projects = load_projects(self.root)
        validate(projects, self.root)
        self.assertEqual(projects[0]['build_target'], 'project-new-watch')
        self.assertEqual(projects[0]['origin'], 'community')
        manifest = ET.parse(directory / 'AndroidManifest.xml').getroot()
        self.assertEqual(manifest.findall('uses-permission'), [])
        self.assertEqual(manifest.findall('.//service'), [])
        strings = ET.parse(directory / 'res/values/strings.xml').getroot()
        self.assertEqual(strings.find('string').text, 'A & B "Watch"')

    def test_no_overwrite(self):
        folder = scaffold('my-tool', 'My tool', 'tool', self.root)
        (folder / 'keep.txt').write_text('keep me')
        with self.assertRaises(ValueError):
            scaffold('my-tool', 'Replacement', 'tool', self.root)
        self.assertEqual((folder / 'keep.txt').read_text(), 'keep me')

    def test_path_traversal_rejected(self):
        with self.assertRaises(ValueError):
            scaffold('../escape', 'Invalid', 'watch-app', self.root)
        self.assertEqual(list((self.root / 'projects').iterdir()), [])

    def test_template_directories_are_not_projects(self):
        folder = self.root / 'projects/_template'
        folder.mkdir()
        (folder / 'project.json').write_text('this intentionally is not JSON')
        self.assertEqual(load_projects(self.root), [])

    def test_catalog_rejects_readme_outside_project(self):
        scaffold('test-app', 'Test', 'phone-app', self.root)
        projects = load_projects(self.root)
        projects[0]['readme'] = '../../catalog/projects.json'
        with self.assertRaises(ValueError):
            validate(projects, self.root)

    def test_independent_face_retains_original_font_assets(self):
        shutil.copytree(ROOT / 'faces/terminal', self.root / 'faces/terminal')
        folder = scaffold('new-face', 'My Face', 'watch-face', self.root)
        projects = load_projects(self.root)
        validate(projects, self.root)
        manifest = ET.parse(folder / 'AndroidManifest.xml').getroot()
        self.assertEqual(manifest.get('package'), 'org.example.wristlab.p_new_face')
        self.assertTrue((folder / 'res/raw/watchface.xml').is_file())
        self.assertTrue(list((folder / 'res/font').glob('*.ttf')))

    def test_catalog_detects_undeclared_permission_change(self):
        folder = scaffold('permission-test', 'Permission Test', 'watch-app', self.root)
        file = folder / 'AndroidManifest.xml'
        tree = ET.parse(file)
        permission = ET.SubElement(tree.getroot(), 'uses-permission')
        permission.set('{http://schemas.android.com/apk/res/android}name', 'android.permission.INTERNET')
        tree.write(file)
        with self.assertRaises(ValueError):
            validate(load_projects(self.root), self.root)


if __name__ == '__main__':
    unittest.main()
