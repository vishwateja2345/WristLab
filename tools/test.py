#!/usr/bin/env python3
"""Deterministic JVM regressions; no Android SDK or connected devices required."""
from pathlib import Path
import shutil
from build import ROOT, java_tool, run

SUITES = (
    ('watch', 'apps/watch', 'studio/teja/gesturelab',
     ('SessionPolicy', 'BackGesture', 'StepScroll', 'ControlEngine', 'PulseDetector', 'AirGestures'), 'EngineTest'),
    ('phone', 'apps/phone', 'studio/teja/airbridge', ('SwipePlan', 'SwipeGate'), 'BridgeTest'),
    ('deck', 'experiments/watch-deck', 'studio/teja/watchdeck', ('DeckState',), 'DeckStateTest'),
)

for name, directory, package, classes, test in SUITES:
    source = ROOT / directory
    output = ROOT / 'build/tests' / name
    if output.exists():
        shutil.rmtree(output)
    output.mkdir(parents=True)
    sources = [source / 'src' / package / (c + '.java') for c in classes]
    print(f'Testing {name}', flush=True)
    run([java_tool('javac'), '--release', '8', '-d', output, *sources, source / 'tests' / (test + '.java')])
    run([java_tool('java'), '-cp', output, test])
