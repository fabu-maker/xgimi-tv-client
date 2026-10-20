#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET
p=Path(__file__).resolve().parents[1]
manifest=ET.parse(p/'app/src/main/AndroidManifest.xml').getroot()
assert manifest.tag=='manifest'
java='\n'.join(f.read_text() for f in (p/'app/src/main/java').rglob('*.kt'))
assert 'class MainActivity' in java
assert 'android.webkit' not in (p/'app/src/main/java/dev/projectortv/app/MainActivity.kt').read_text()
# Check source imports and instantiated types rather than comments.
assert 'import android.webkit.' not in (p/'app/src/main/java/dev/projectortv/app/MainActivity.kt').read_text()
assert 'WebView(' in (p/'app/src/main/java/dev/projectortv/player/SitePlaybackSession.kt').read_text()
assert 'class NativeVideo' in java and 'ExoPlayer.Builder' in java
assert 'class CatalogRepository' in java and 'BingeBangParser.parseShelf' in java
assert 'class AccountSession' in java and 'AccountRoutes' in java
assert 'versionCode = 4' in (p/'app/build.gradle.kts').read_text()
print('Native UI with isolated account/player sessions, parser, version and manifest: PASS')
