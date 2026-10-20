#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET
p=Path(__file__).resolve().parents[1]
manifest=ET.parse(p/'app/src/main/AndroidManifest.xml').getroot()
assert manifest.tag=='manifest'
java='\n'.join(f.read_text() for f in (p/'app/src/main/java').rglob('*.kt'))
assert 'class MainActivity' in java
assert 'android.webkit' not in java
# Check source imports and instantiated types rather than comments.
assert 'import android.webkit.' not in java
assert 'WebView(' not in java
assert 'class NativeVideo' in java and 'ExoPlayer.Builder' in java
assert 'class CatalogRepository' in java and 'BingeBangParser.parseShelf' in java
assert 'versionCode = 2' in (p/'app/build.gradle.kts').read_text()
print('Native UI, parser, version and manifest structure: PASS')
