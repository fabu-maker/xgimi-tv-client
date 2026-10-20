#!/usr/bin/env python3
"""Legacy entrypoint. Kept for old workflows; does not prove functional correctness."""
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
ns='{http://schemas.android.com/apk/res/android}'
app=manifest.find('application')
assert app.get(ns+'usesCleartextTraffic')=='false'
assert app.get(ns+'allowBackup')=='false'
source='\n'.join(p.read_text() for p in (root/'app/src/main/java').rglob('*.kt'))
assert 'addJavascriptInterface(' not in source
assert 'SslErrorHandler.proceed' not in source
print('Manifest/bridge static checks passed; playback and accounts not tested here.')
