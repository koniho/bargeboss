#!/usr/bin/env python3
"""Fail before upload if the artifact is for another app or a developer build."""
import os
import plistlib
import subprocess
import struct
import sys
import xml.etree.ElementTree as ET
from zipfile import ZipFile

if sys.argv[1:] == ['android']:
    manifest = subprocess.check_output([
        'java', '-jar', 'build/tools/bundletool-all-1.18.3.jar',
        'dump', 'manifest', '--bundle=build/BargeBoss.aab'], text=True)
    root = ET.fromstring(manifest)
    expected = ET.parse('AndroidManifest.xml').getroot()
    ns = '{http://schemas.android.com/apk/res/android}'
    assert root.get('package') == 'com.bargeboss.game'
    for key in ('versionCode', 'versionName'):
        assert root.get(ns + key) == expected.get(ns + key), key
    assert root.find('application').get(ns + 'debuggable', 'false') == 'false'
    assert not root.findall('uses-permission'), 'Unexpected Android permission'
    with ZipFile('build/BargeBoss.aab') as bundle:
        for name in bundle.namelist():
            if not name.endswith('.dex'):
                continue
            dex = bundle.read(name)
            # Inspect definitions, not reflection-name strings in the production host.
            string_offset = struct.unpack_from('<I', dex, 60)[0]
            type_offset = struct.unpack_from('<I', dex, 68)[0]
            class_count, class_offset = struct.unpack_from('<II', dex, 96)
            for index in range(class_count):
                type_id = struct.unpack_from('<I', dex, class_offset + index * 32)[0]
                string_id = struct.unpack_from('<I', dex, type_offset + type_id * 4)[0]
                offset = struct.unpack_from('<I', dex, string_offset + string_id * 4)[0]
                while dex[offset] & 0x80:
                    offset += 1
                offset += 1
                descriptor = dex[offset:dex.index(b'\0', offset)]
                assert descriptor not in (b'Lcom/bargeboss/game/AndroidProfileSession;',
                                          b'Lcom/bargeboss/game/ReplayInput;'), descriptor
elif sys.argv[1:] == ['ios']:
    with ZipFile('ios/build/BargeBoss.ipa') as ipa:
        info = plistlib.loads(ipa.read('Payload/BargeBoss.app/Info.plist'))
    assert info['CFBundleIdentifier'] == 'com.bargeboss.game.ios'
    assert info['CFBundleDisplayName'] == 'Barge Boss'
    assert info['CFBundleVersion'] == os.environ['IOS_BUILD_NUMBER']
    assert info['UIDeviceFamily'] == [1, 2]
else:
    raise SystemExit('Usage: verify-release.py android|ios')
print('Release artifact identity and production settings verified.')
