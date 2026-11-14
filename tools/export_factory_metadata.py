#!/usr/bin/env python3
"""Export source-inspected factory anchors and Java arrays; never edits PNG pixels."""
import json, math, pathlib, shutil

ROOT = pathlib.Path(__file__).resolve().parents[1]
STUDY = ROOT / 'docs/art-studies/factory-v1'
specs = json.loads((STUDY / 'specs.json').read_text())
audit = json.loads((STUDY / 'grounding-audit.json').read_text())
# Source pixel centers reviewed against the individual generated PNGs.
# Window banks are single fixtures; mullions do not get independent emitters.
lights = [
 [('lamp.work',424,705,3),('furnace.left',444,849,5),('furnace.front-left',547,886,5),('furnace.front-right',718,885,5),('furnace.right',814,846,5)],
 [('window.left-back',387,789,4),('window.left-front',534,867,4),('window.right-front',740,898,4),('window.right-back',871,820,4)],
 [('furnace.left',333,930,5),('furnace.middle',471,992,5),('furnace.right',610,1060,5)],
 [('furnace.front',483,957,6),('window.left',353,837,3),('window.front',613,939,3),('window.clerestory-1',576,569,3),('window.clerestory-2',689,615,3),('window.clerestory-3',802,663,3)],
 [('lamp.door',469,835,2),('opening.loading',471,932,4),('window.1',603,879,4),('window.2',677,840,4),('window.3',750,800,4),('window.4',815,760,4)],
 [('lamp.door',461,854,2),('window.1',622,838,4),('window.2',710,787,4),('window.3',794,740,4)],
 [('lamp.door',469,884,2),('window.front',671,885,4),('window.rear',787,817,4)],
 [('lamp.cabin',740,776,2),('window.cabin',805,761,3)],
 [('lamp.rear-tank',436,650,2),('lamp.front-tank',796,823,2)],
 [('lamp.left',491,466,2),('lamp.right',791,591,2)],
 [('lamp.hopper',335,662,2)],
 [('window.roof-1',465,668,3),('window.roof-2',548,715,3),('window.roof-3',629,764,3),('lamp.loading',855,890,2)],
 [('window.left',541,815,3),('window.right',713,813,3)],
 [('lamp.left',533,762,2),('lamp.right',721,764,2),('window.left',537,815,3),('window.right',718,814,3)],
 [('lamp.door',522,800,2),('window.side',720,826,3)],
 [('window.upper',556,795,3),('window.lower',556,918,3),('lamp.door',709,829,2)]
]
smokes = [
 [('stack.main',625,213,5),('stack.secondary',847,322,4)],
 [('stack.left',627,200,4),('stack.right',783,287,4)],
 [('stack.left',678,157,4),('stack.right',865,230,4)],
 [('stack.main',633,188,4)],
 [('exhaust.roof',752,577,3)],
 [('exhaust.roof',678,599,3)],
 [('stack.main',678,368,4)],
 [('exhaust.cabin',746,601,2)],
 [], [('stack.main',623,187,4)], [], [],
 [('exhaust.roof',628,519,2)], [], [('exhaust.roof',627,524,2)], []
]
heights=[175,145,120,145,85,80,150,65,100,155,60,85,70,55,65,95]
rows=[]
for i,s in enumerate(specs):
    a=next(a for a in audit['assets'] if a['id']==s['id'])
    if not a['reviewStatus'].startswith('accepted'):
        raise ValueError('Unaccepted source: '+s['id'])
    w,h=a['dimensions']; px,py=a['inferredGroundPivotPixels']; size=a['uniformWorldSize']
    version=a['file'].rsplit('-v',1)[1].split('.')[0]
    filename='factory-'+s['id']+'-v'+version+'.png'
    row={'id':s['id'],'index':i,'file':filename,'sourceStudy':a['file'],
         'prompt':'docs/art-prompts/factory-v1/'+s['id']+'-v'+version+'.txt',
         'dimensions':[w,h],'sourceRect':[0,0,w,h], 'tileOccupancy':s['tiles'],
         'groundPivotUV':[px/w,py/h], 'uniformWorldSize':size,
         'authoredGuideHeight':heights[i], 'visibleHeightAboveGroundPivot':a['visibleHeightAboveGroundPivot'],
         'foundationCornersPixels':a['inferredFoundationCornersPixels'],
         'foundationWorldCoordinates':a['foundationWorldCoordinates'],
         'edgeAnglesDegrees':a['edgeAnglesDegrees'], 'alphaBounds':a['alpha128BoundsInclusive'],
         'registrationReview':a['registrationReview'], 'lights':[],'smoke':[]}
    for category,source in [('lights',lights[i]),('smoke',smokes[i])]:
        for name,x,y,radius in source:
            # A single image constrains a projection, not a unique 3-D location.
            # Choose local y=0, solve local x and z; source UV is runtime authority.
            dx=(x-px)*size/w; dy=(y-py)*size/h
            lx=dx/math.sqrt(.5); z=-dx/math.sqrt(3)-dy
            color='#7b8589' if category=='smoke' else '#ffb34f'
            row[category].append({'id':s['id']+'.'+name,'kind':'smoke' if category=='smoke' else name.split('.')[0],
             'sourcePixels':[x,y],'sourceUV':[x/w,y/h],'radiusWorld':radius,'color':color,
             'localXYZ':[round(lx,3),0,round(z,3)],'outwardNormal':[0,0,1] if category=='smoke' else [-1,0,0],
             'visibility':'visible in fixed view',
             'sourceReview':'pixel-reviewed on labeled overlay of final source; re-review required after source edits',
             'localReview':'inferred single-view estimate with localY=0; not measured 3-D geometry',
             'normalReview':'inferred up for exhaust; nominal facade normal for light spill'})
    rows.append(row)
    shutil.copyfile(STUDY/a['file'],ROOT/'assets/sprites'/filename)
metadata={'schema':'bargeboss.scenery.factory.v1','camera':{'yaw':45,'elevation':35.2643897,'groundAxes':[30,-30]},
 'tileWorldSize':64,'groundRise':'GroundArt.LAND_RISE, applied in shared Java during tutorial',
 'sourcePolicy':'Immutable imagegen originals, full-frame uniform scaling, true alpha. Freestanding modules with service-pad occupancy; not seamless tiles.',
 'anchorPolicy':'Source UV positions are runtime authority. Local XYZ/normal are explicitly inferred from one view and must be rebuilt for another camera.',
 'assets':rows}
(STUDY/'factory-anchors.json').write_text(json.dumps(metadata,indent=2)+'\n')
def floats(values): return '{'+','.join(f'{v:.7f}f' for v in values)+'}'
def nested(values): return '{\n        '+',\n        '.join(floats(v) for v in values)+'\n    }'
data='''package com.bargeboss.game;

/** Generated source registration. Rebuild with tools/export_factory_metadata.py. */
final class FactoryArtData {
    private FactoryArtData() {}
'''
data+='    static final String[] FILES={'+','.join('"'+r['file']+'"' for r in rows)+'};\n'
for field,values in [('PX',[r['groundPivotUV'][0] for r in rows]),('PY',[r['groundPivotUV'][1] for r in rows]),('SIZE',[r['uniformWorldSize'] for r in rows])]:
    data+='    static final float[] '+field+'='+floats(values)+';\n'
for field,values in [('BOUNDS',[[b/1254 for b in r['alphaBounds']] for r in rows]),
                     ('LIGHTS',[[q for a in r['lights'] for q in [*a['sourceUV'],a['radiusWorld']]] for r in rows]),
                     ('SMOKE',[[q for a in r['smoke'] for q in [*a['sourceUV'],a['radiusWorld']]] for r in rows])]:
    data+='    static final float[][] '+field+'='+nested(values)+';\n'
data+='}\n'
(ROOT/'core/com/bargeboss/game/FactoryArtData.java').write_text(data)
print('Exported',len(rows),'factory modules;',sum(len(r['lights']) for r in rows),'lights;',sum(len(r['smoke']) for r in rows),'exhausts')
