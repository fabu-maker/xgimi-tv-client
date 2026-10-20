#!/usr/bin/env python3
"""Local static sanity checks. Does not replace Android SDK compile, lint or device tests."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
manifest = root / 'app/src/main/AndroidManifest.xml'
app = root / 'app/src/main/java/dev/projectortv/app/MainActivity.kt'
nav = root / 'app/src/main/java/dev/projectortv/core/Navigation.kt'
gradle = root / 'app/build.gradle.kts'

for source in (manifest, app, nav, gradle):
    assert source.is_file(), f'Missing source: {source}'
for path in (root / 'app/src/main/res').rglob('*.xml'):
    ET.parse(path)
ET.parse(manifest)

source = manifest.read_text()
ns = '{http://schemas.android.com/apk/res/android}'
tree = ET.fromstring(source)
assert any(x.attrib.get(ns+'name') == 'android.hardware.touchscreen' and x.attrib.get(ns+'required') == 'false' for x in tree.findall('uses-feature'))
assert any(x.attrib.get(ns+'name') == 'android.software.leanback' for x in tree.findall('uses-feature'))
assert 'android.intent.category.LEANBACK_LAUNCHER' in source
assert 'android.permission.INTERNET' in source
assert 'usesCleartextTraffic="false"' in source
assert 'allowBackup="false"' in source

app_source = app.read_text()
for mandatory in ('shouldOverrideUrlLoading', 'isForMainFrame', 'onRenderProcessGone',
                  'onShowCustomView', 'onHideCustomView', 'onReceivedError',
                  'mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW',
                  'allowFileAccess = false', 'javaScriptCanOpenWindowsAutomatically = false'):
    assert mandatory in app_source, f'Missing mechanism: {mandatory}'
assert 'minSdk = 28' in gradle.read_text()
assert 'targetSdk = 35' in gradle.read_text()
assert (root / 'app/src/main/res/drawable-nodpi/tv_banner.png').exists()
assert (root / '.github/workflows/android-ci.yml').exists()
print('PASS: Android manifest/resources parse, TV flags, security mechanism presence, build config and CI spec')
