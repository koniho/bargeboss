#!/usr/bin/env python3
"""Export reviewed whole-hold cargo registration; never modifies raster artwork."""
import csv, math, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
STEMS=['cargo-steel-isometric-v1','cargo-container-isometric-v2','cargo-coal-isometric-v3']
lines=['package com.bargeboss.game;', '', '/** Whole-hold cargo: common 3D guide, waterline datum and registered 32-pose art. */', 'public final class CargoArt {', '    public static final float HOLD_LENGTH=62,HOLD_WIDTH=24,FLOOR=3.05f,RIM=11;', '    private CargoArt() {}', '    public static boolean atlas(int atlas){return atlas>=SpriteAtlas.STEEL && atlas<=SpriteAtlas.BULK;}']
scales=[]; axes=[[],[]]
for stem in STEMS:
 rows=list(csv.DictReader((ROOT/'docs/art-studies'/f'{stem}-registration.csv').open()))
 assert [int(r['frame']) for r in rows]==list(range(32))
 scale=float(rows[0]['uniform_scale']); assert scale>0 and all(float(r['uniform_scale'])==scale for r in rows)
 scales.append(f'{scale:.8f}f')
 for ai,axis in enumerate(['x','y']):
  values=[float(r['pivot_'+axis]) for r in rows]
  assert all(math.isfinite(v) and 0<v<1 for v in values)
  axes[ai].append('{'+','.join(f'{v:.8f}f' for v in values)+'}')
lines+=['    private static final float[] SCALE={'+','.join(scales)+'};']
for name,axis in zip(['X','Y'],axes):lines+=['    private static final float[][] '+name+'={'+','.join(axis)+'};']
lines+=['    public static float scale(int atlas){return SCALE[atlas-SpriteAtlas.STEEL];}', '    public static float pivotX(int atlas,int frame){return X[atlas-SpriteAtlas.STEEL][frame];}', '    public static float pivotY(int atlas,int frame){return Y[atlas-SpriteAtlas.STEEL][frame];}', '    public static float quantity(Body b){return Physics.clamp(1-b.unloadingTicks/(float)Game.UNLOAD_TICKS,0,1);}', '}']
(ROOT/'core/com/bargeboss/game/CargoArt.java').write_text('\n'.join(lines)+'\n')
metadata=[]
for atlas,kind,stem in zip([2,3,4],['steel','container','coal'],STEMS):
 metadata.append(dict(atlas=atlas,source=stem+'-32.png',coordinateSpace='body-local-xyz',cargoOrigin=[0,0,3.05],lights=[],smokestacks=[],tires=[],shadows=dict(file=f'docs/art-studies/cargo-shadows-{kind}-v1.png',source=stem+'-32.png',contactSigma=1.0,castSigma=3.2,cell=128,inner=96,pad=16,receivingPlane='hold-floor',runtime='hull-only water shadow; cargo masks delivered separately')))
(ROOT/'docs/art-studies/cargo-effects-v1.json').write_text(json.dumps(dict(schemaVersion=1,frames=32,geometry='tools/BargePoseGuide.java',featureInventoryVerified=True,cargo=metadata),indent=2)+'\n')
