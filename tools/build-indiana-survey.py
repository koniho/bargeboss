#!/usr/bin/env python3
"""Build the checked-in Indiana survey from OSM extracts, never from runtime network data.
USGS imagery is a visual reference; this tool only processes geographic geometry.
"""
import json, math, pathlib, xml.etree.ElementTree as ET
ROOT=pathlib.Path(__file__).resolve().parents[1]
SRC=ROOT/'out/indiana-reference'
import sys
canonical=ROOT/'docs/maps/indiana-survey-v1.json'
if '--refresh-osm' in sys.argv:
    nodes={}; ways={}
    for filename in sorted(SRC.glob('*.xml')):
        root=ET.parse(filename).getroot()
        for n in root.findall('node'): nodes[n.get('id')]=[float(n.get('lon')),float(n.get('lat'))]
        for w in root.findall('way'): ways[w.get('id')]={'nodes':[n.get('ref') for n in w.findall('nd')],'tags':{n.get('k'):n.get('v') for n in w.findall('tag')}}
    # Lake shore, southeast to west, including the two projecting steelworks peninsulas.
    ids=[]
    for wid,reverse in [('48373571',False),('156338291',False),('156338286',False),('1266094606',False),('42908083',True),('156338287',False)]:
        part=ways[wid]['nodes'][::(-1 if reverse else 1)]
        if ids: assert ids[-1]==part[0],(wid,ids[-1],part[0])
        ids += part[1:] if ids else part
    # Replace the canal's artificial mouth chord with its complete river-bank ring.
    a=ways['1164985031']['nodes'];b=ways['156314929']['nodes'];ring=a+b[1:-1]
    start=ring.index('20241926');end=ring.index('20242014')
    long=ring[start:]+ring[:end+1]
    i=ids.index('20241926');j=ids.index('20242014');assert i<j
    ids=ids[:i]+long+ids[j+1:]
    points=[nodes[n] for n in ids]
    points += [[-87.60,41.78],[-87.30,41.78],[-87.30,41.60]]
    # Keep entire harbor plus both branches north of the Grand Calumet junction; remote
    # river miles and neighboring Lake Michigan harbors are outside this stage.
    BBOX=(-87.500,41.632,-87.400,41.705)
    def clip(poly,axis,bound,greater):
        out=[];a=poly[-1];inside=lambda p:(p[axis]>=bound if greater else p[axis]<=bound)
        for b in poly:
            ia,ib=inside(a),inside(b)
            if ia!=ib:
                t=(bound-a[axis])/(b[axis]-a[axis]);out.append([a[k]+t*(b[k]-a[k]) for k in range(2)])
            if ib:out.append(b)
            a=b
        return out
    for axis,bound,greater in [(0,BBOX[0],True),(0,BBOX[2],False),(1,BBOX[1],True),(1,BBOX[3],False)]:points=clip(points,axis,bound,greater)
    N=111132*2;E=111320*math.cos(math.radians(41.66))*2
    world=lambda p:[(p[1]-41.64)*N,(p[0]+87.46)*E]
    worldpts=[world(p) for p in points]
    def d2(p,a,b):
        dx=b[0]-a[0];dy=b[1]-a[1];t=max(0,min(1,((p[0]-a[0])*dx+(p[1]-a[1])*dy)/(dx*dx+dy*dy))) if dx*dx+dy*dy else 0
        return (p[0]-a[0]-t*dx)**2+(p[1]-a[1]-t*dy)**2
    # Iterative vertex removal, <=1.5 metre deviation. Endpoints, mouth, basin and
    # branch topology retained; unlike a raster trace this preserves real angles.
    changed=True
    while changed:
        changed=False
        for i in range(len(worldpts)):
            if d2(worldpts[i],worldpts[i-1],worldpts[(i+1)%len(worldpts)])<9:
                worldpts.pop(i);points.pop(i);changed=True;break
    # Route is the charted center line, plus lake approach and south branch center.
    route=[[-87.4446,41.690],[-87.4431,41.6810],[-87.4398,41.6747],[-87.4388,41.6700],[-87.4419071,41.6675907]]
    route+=list(reversed([nodes[n] for n in ways['443404991']['nodes'][:-1]]))
    route+=list(reversed([nodes[n] for n in ways['264421807']['nodes'][:-1]]))
    route += [[-87.4713,41.6450],[-87.47127,41.6410],[-87.47139,41.6372]]
    # Land-cover inventory from mapped closed polygons; only material classifications
    # are inferred. Building detail is deliberately left for a later art increment.
    land=[];bridges=[];roads=[]
    for wid,w in ways.items():
        t=w['tags'];p=[nodes[n] for n in w['nodes'] if n in nodes]
        if not p:continue
        if max(q[0] for q in p)<BBOX[0] or min(q[0] for q in p)>BBOX[2] or max(q[1] for q in p)<BBOX[1] or min(q[1] for q in p)>BBOX[3]:continue
        kind=None
        if t.get('landuse') in ['grass','meadow','forest','recreation_ground'] or t.get('natural') in ['wood','scrub','wetland']:kind='grass'
        if t.get('landuse') in ['industrial','railway','construction','brownfield']:kind={'industrial':'works','railway':'rail','construction':'aggregate','brownfield':'aggregate'}[t['landuse']]
        if t.get('building'):kind='building'
        if t.get('amenity')=='parking':kind='concrete'
        if kind and len(p)>=4 and p[0]==p[-1]:
            # Avoid small household footprints outside the industrial corridor.
            pp=[world(q) for q in p[:-1]]
            area=abs(sum(pp[i-1][0]*pp[i][1]-pp[i][0]*pp[i-1][1] for i in range(len(pp)))*.5)
            if area>5000:land.append({'id':wid,'kind':kind,'name':t.get('name',''),'points':p[:-1]})
        if t.get('bridge') and (t.get('highway') not in [None,'footway','path'] or t.get('railway') in ['rail','disused']):
            bridges.append({'id':wid,'name':t.get('bridge:name',t.get('name','Rail bridge')),'kind':'rail' if t.get('railway') else 'road','movable':t.get('bridge')=='movable','points':p})
        if t.get('highway') in ['primary','secondary','tertiary','motorway','trunk'] or t.get('railway')=='rail':
            roads.append({'id':wid,'kind':'rail' if t.get('railway') else 'road','points':p})
    data={'origin':{'lat':41.64,'lon':-87.46},'unitsPerMetre':2,'bbox':BBOX,'coast':points,'route':route,'land':land,'bridges':bridges,'roads':roads,'sources':{'river':'https://www.openstreetmap.org/relation/15761774','lake':'https://www.openstreetmap.org/relation/1205149','imagery':'https://basemap.nationalmap.gov/arcgis/rest/services/USGSImageryOnly/MapServer','retrieved':'2026-11-18'},'attribution':'© OpenStreetMap contributors, ODbL 1.0. USGS/USDA imagery used only for visual reference.'}
    if canonical.exists():
        data['land'] += [row for row in json.loads(canonical.read_text())['land'] if row.get('reviewBase')]
    out=ROOT/'docs/maps/indiana-survey-v1.json';out.parent.mkdir(exist_ok=True);out.write_text(json.dumps(data,indent=2)+'\n')
else:
    data=json.loads(canonical.read_text())
    BBOX=data['bbox']; points=data['coast']; route=data['route']
    land=data['land']; bridges=data['bridges']; roads=data['roads']
    N=111132*2; E=111320*math.cos(math.radians(41.66))*2
    world=lambda p:[(p[1]-41.64)*N,(p[0]+87.46)*E]
    worldpts=[world(p) for p in points]
    def d2(p,a,b):
        dx=b[0]-a[0];dy=b[1]-a[1]
        t=max(0,min(1,((p[0]-a[0])*dx+(p[1]-a[1])*dy)/(dx*dx+dy*dy))) if dx*dx+dy*dy else 0
        return (p[0]-a[0]-t*dx)**2+(p[1]-a[1]-t*dy)**2
def arr(p):return '{'+','.join(f'{v:.2f}f' for q in p for v in world(q))+'}'
# Bake land-use tiles once offline; runtime does not scan hundreds of GIS polygons.
CELL=128
MINX=math.floor((BBOX[1]-41.64)*N/CELL)*CELL;MAXX=math.ceil((BBOX[3]-41.64)*N/CELL)*CELL
MINY=math.floor((BBOX[0]+87.46)*E/CELL)*CELL;MAXY=math.ceil((BBOX[2]+87.46)*E/CELL)*CELL
COLS=round((MAXX-MINX)/CELL);ROWS=round((MAXY-MINY)/CELL)
kinds={'grass':0,'concrete':1,'works':2,'aggregate':3,'rail':4,'building':5,'road':6}
def contains(poly,x,y):
    inside=False;a=poly[-1]
    for b in poly:
        if (a[1]>y)!=(b[1]>y) and x<(b[0]-a[0])*(y-a[1])/(b[1]-a[1])+a[0]:inside=not inside
        a=b
    return inside
prepared=[]
for row in land:
    pp=[world(q) for q in row['points']];prepared.append((kinds[row['kind']],pp,min(q[0] for q in pp),min(q[1] for q in pp),max(q[0] for q in pp),max(q[1] for q in pp),row.get("reviewBase",False)))
prepared.sort(key=lambda p:(0 if p[6] else 1,p[0]))
grid=[]
for row in range(ROWS):
    chars=[]
    for col in range(COLS):
        x=MINX+(col+.5)*CELL;y=MINY+(row+.5)*CELL;kind=0
        for k,pp,l,t,r,b,_ in prepared:
            if l<=x<=r and t<=y<=b and contains(pp,x,y):kind=k
        chars.append(str(kind))
    grid.append(''.join(chars))
# Retain only bridges across this navigable polygon and coalesce neighboring rail tracks.
selected=[]
for bridge in bridges:
    pp=[world(q) for q in bridge['points']];mid=pp[len(pp)//2] if len(pp)>2 else [(pp[0][i]+pp[-1][i])*.5 for i in range(2)]
    if not contains(worldpts,*mid):continue
    if any(sum((mid[i]-a[0][i])**2 for i in range(2))<60**2 for a in selected):continue
    selected.append((mid,bridge))
bridges=[r for _,r in selected]
# Sources retain all mapped features for later detailing; active guide lines only near water.
near=[]
for road in roads:
    pp=[world(q) for q in road['points']]
    if min(d2(p,worldpts[i-1],worldpts[i]) for p in [pp[0],pp[len(pp)//2],pp[-1]] for i in range(len(worldpts)))<700**2:near.append(road)
roads=near
lines=['package com.bargeboss.game;','','/** Survey-derived immutable source data. See docs/maps/indiana-survey-v1.json. */','final class IndianaSurvey {','    private IndianaSurvey(){}','    static final float[] COAST='+arr(points)+';','    static final float[][] ROUTE={'+','.join(arr([p]) for p in route)+'};',f'    static final int CELL={CELL},MIN_X={MINX},MAX_X={MAXX},MIN_Y={MINY},MAX_Y={MAXY},COLS={COLS},ROWS={ROWS};','    static final String[] COVER={'+','.join('"'+row+'"' for row in grid)+'};']
for name,rows in [('ROADS',roads),('BRIDGES',bridges)]:
    chunks=[]
    for off in range(0,len(rows),40):
        m=name.lower()+str(off);chunks.append(m+'()');lines.append('    private static float[][] '+m+'(){return new float[][]{'+','.join(arr(r['points']) for r in rows[off:off+40])+'};}')
    lines.append('    static final float[][] '+name+'=join(new float[][][]{'+','.join(chunks)+'});')
    lines.append('    static final int[] '+name+'_KIND={'+','.join(str(kinds[r['kind']]) for r in rows)+'};')
lines.append('    private static float[][] join(float[][][] parts){int n=0;for(float[][] p:parts)n+=p.length;float[][] a=new float[n][];int i=0;for(float[][] p:parts)for(float[] r:p)a[i++]=r;return a;}')
lines.append('}')
(ROOT/'core/com/bargeboss/game/IndianaSurvey.java').write_text('\n'.join(lines)+'\n')
print('coast',len(points),'land',len(land),'roads',len(roads),'bridges',len(bridges),'tiles',COLS,ROWS)
for b in bridges:print(b['name'],b['points'][0],b['points'][-1])
