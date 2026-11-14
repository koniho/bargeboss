#!/usr/bin/env python3
"""Project authored vessel effect points into registered source cells; edits no images."""
import argparse
import csv
import json
import math
from pathlib import Path
import struct


def project(guide_path, registration_path, image_path):
    with Path(guide_path).open() as stream:
        guide = list(csv.DictReader(stream))
    with Path(registration_path).open() as stream:
        registration = list(csv.DictReader(stream))
    if len(registration) != 32 or [int(r['frame']) for r in registration] != list(range(32)):
        raise ValueError('Registration must contain 32 ordered frames')
    data = Path(image_path).read_bytes()
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError('Expected PNG atlas')
    width, height = struct.unpack('>II', data[16:24])
    kinds = {'light': 'lights', 'smokestack': 'smokestacks', 'tire': 'tires'}
    frames = []
    expected = None
    for frame, reg in enumerate(registration):
        px, py, scale = (float(reg[k]) for k in ('pivot_x', 'pivot_y', 'uniform_scale'))
        if not all(math.isfinite(n) for n in (px, py, scale)) or scale <= 0:
            raise ValueError('Invalid registration')
        row = {'frame': frame, 'intendedYawDegrees': frame * 11.25,
               'waterlinePivot': [px, py], 'uniformScale': scale,
               'lights': [], 'smokestacks': [], 'tires': []}
        ids = set()
        for point in (p for p in guide if int(p['frame']) == frame):
            identity, kind = point['id'], point['kind']
            if identity in ids or kind not in kinds:
                raise ValueError('Duplicate ID or unknown kind: ' + identity)
            ids.add(identity)
            u, v, radius = (float(point[k]) for k in ('u', 'v', 'radius_uv'))
            position = [px + (u - .5) / scale, py + (v - .72) / scale]
            xyz = [float(point[k]) for k in ('x', 'y', 'z')]
            normal = [float(point[k]) for k in ('nx', 'ny', 'nz')]
            if not all(math.isfinite(n) for n in position + xyz + normal + [radius]):
                raise ValueError('Nonfinite anchor: ' + identity)
            if not all(0 <= n <= 1 for n in position) or radius <= 0:
                raise ValueError('Anchor outside source cell or invalid radius: ' + identity)
            if point['visibility'] not in ('visible', 'occluded'):
                raise ValueError('Missing visibility: ' + identity)
            anchor = {'id': identity, 'kind': kind, 'position': position,
                      'radius': radius / scale, 'guidePosition': [u, v],
                      'guideRadius': radius, 'localXYZ': xyz,
                      'localNormal': normal, 'radiusWorld': float(point['radius']),
                      'visibility': point['visibility'],
                      'visibilitySource': 'guide-depth-or-facing-estimate',
                      'reviewStatus': 'guide-projected', 'role': point['role']}
            if point['side']:
                anchor['side'] = point['side']
            if point['color']:
                anchor['color'] = point['color']
                anchor['baseIntensity'] = 1.0
            row[kinds[kind]].append(anchor)
        if expected is None:
            expected = ids
        if not ids or ids != expected:
            raise ValueError('Missing or inconsistent anchor IDs in frame ' + str(frame))
        frames.append(row)
    if len(guide) != sum(sum(len(f[k]) for k in kinds.values()) for f in frames):
        raise ValueError('Unexpected guide frame number')
    return {'schemaVersion': 1, 'atlas': str(image_path), 'imageSize': [width, height],
            'grid': [8, 4], 'coordinateSpace': 'normalized-source-cell',
            'localAxes': 'X bow, Y starboard, Z above waterline',
            'guide': str(guide_path), 'registration': str(registration_path),
            'reviewStatus': 'guide-projected-unverified',
            'featureInventoryVerified': False,
            'reviewNotes': ['Compare each point to the finished sprite before effects integration.',
                            'Generated artwork adds small cabin/roof fixtures beyond the guide; inventory and author these before claiming complete light coverage.',
                            'Visibility is a guide estimate, not measured occlusion in the generated bitmap.'],
            'frames': frames}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('guide_csv')
    parser.add_argument('registration_csv')
    parser.add_argument('atlas_png')
    parser.add_argument('output_json')
    args = parser.parse_args()
    result = project(args.guide_csv, args.registration_csv, args.atlas_png)
    output = Path(args.output_json)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2) + '\n')
    first = result['frames'][0]
    counts = {k: len(first[k]) for k in ('lights', 'smokestacks', 'tires')}
    print(f'Wrote {output}: 32 frames, per-frame counts {counts}; guide estimates, not approved pixel anchors.')


if __name__ == '__main__':
    main()
