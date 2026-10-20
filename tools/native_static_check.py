#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET
p=Path(__file__).resolve().parents[1]
manifest=ET.parse(p/'app/src/main/AndroidManifest.xml').getroot()
assert manifest.tag=='manifest'
main=(p/'app/src/main/java/dev/projectortv/app/TvFacadeActivity.kt').read_text()
bridge=(p/'app/src/main/java/dev/projectortv/app/SitePageBridge.kt').read_text()
assert '.TvFacadeActivity' in (p/'app/src/main/AndroidManifest.xml').read_text()
assert 'SitePageBridge(' in main and 'PosterLoader(' in main
assert 'My Space' not in main or 'Mein Bereich' in main
assert 'document.querySelectorAll' in bridge and 'evaluateJavascript' in bridge
assert 'CookieManager.getInstance().flush()' in bridge
assert 'BingeBangParser.parseShelf' not in main
assert 'versionCode = 5' in (p/'app/build.gradle.kts').read_text()
assert 'https://bingebang.st' in bridge
print('TV facade, persistent provider session, Android manifest and release version: PASS')
