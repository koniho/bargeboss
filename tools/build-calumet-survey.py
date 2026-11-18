#!/usr/bin/env python3
"""Export reviewed geographic survey into shared Java. No imagery becomes runtime art."""
import json, math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
s=json.loads((ROOT/'docs/levels/calumet/survey.json').read_text());lon0,lat0=s['origin'];scale=s['unitsPerMeter']
def world(p):return [(p[1]-lat0)*111195*scale,(p[0]-lon0)*111195*math.cos(math.radians(41.70))*scale]
def dist(p,a,b):
 dx=b[0]-a[0];dy=b[1]-a[1];t=max(0,min(1,((p[0]-a[0])*dx+(p[1]-a[1])*dy)/(dx*dx+dy*dy or 1)))
 return math.hypot(p[0]-a[0]-dx*t,p[1]-a[1]-dy*t)
def rdp(p,tol):
 if len(p)<3:return p
 k=max(range(1,len(p)-1),key=lambda i:dist(p[i],p[0],p[-1]));d=dist(p[k],p[0],p[-1])
 return rdp(p[:k+1],tol)[:-1]+rdp(p[k:],tol) if d>tol else [p[0],p[-1]]
def simplify(p,tol,closed=False):
 p=[world(v) for v in p]
 if closed:
  k=max(range(len(p)),key=lambda i:math.dist(p[0],p[i]));p=rdp(p[:k+1],tol)[:-1]+rdp(p[k:]+p[:1],tol)[:-1]
 else:p=rdp(p,tol)
 out=[]
 for a in p:
  a=[round(x,1) for x in a]
  if not out or math.dist(a,out[-1])>.1:out.append(a)
 return out
coast=simplify(s['river'],4,True);route=[world(p) for p in s['route']]
def flat(p):return ','.join(f'{v:.1f}f' for point in p for v in point)
def distance_route(p):return min(dist(p,a,b) for a,b in zip(route,route[1:]))
kind={'grass':0,'worksite':1,'aggregate':2,'rail-yard':3,'woods':4,'wetland':5,'neighborhood':6}
areas=[]
for f in s['landuse']:
 p=simplify(f['coordinates'],12,True)
 if len(p)<3:continue
 center=[sum(v[i] for v in p)/len(p) for i in (0,1)]
 if distance_route(center)>1500 and all(distance_route(v)>900 for v in p):continue
 a=abs(sum(p[i][0]*p[(i+1)%len(p)][1]-p[(i+1)%len(p)][0]*p[i][1] for i in range(len(p))))/2
 if a<800:continue
 areas.append((kind[f['kind']],p))
roads=[]
for f in s['transport']:
 p=simplify(f['coordinates'],10)
 for a,b in zip(p,p[1:]):
  mid=[(a[i]+b[i])/2 for i in (0,1)]
  if distance_route(mid)<1000:roads.append([*a,*b,1 if f['kind']=='rail' else 0])
# OSM bridge center positions; bank endpoints derive from exact coastline below.
bridges=[[*world([b[1],b[2]]),b[3]] for b in s['bridges']]
# Sites use exact shore ray intersections and a 40-unit wet apron.
def inside(p):
 x,y=p;v=False
 for a,b in zip(coast,coast[1:]+coast[:1]):
  if (a[1]>y)!=(b[1]>y) and x<(b[0]-a[0])*(y-a[1])/(b[1]-a[1])+a[0]:v=not v
 return v
def center_at(lat):
 x=world([lon0,lat])[0]
 for a,b in zip(route,route[1:]):
  if b[0]<=x<=a[0]:
   t=(x-a[0])/(b[0]-a[0]);return [x,a[1]+t*(b[1]-a[1])]
 raise ValueError(lat)
def berth(lat,direction,clearance=42):
 p=center_at(lat);dx,dy=direction;n=math.hypot(dx,dy);dx/=n;dy/=n
 assert inside(p),p
 d=0
 while d<900 and inside([p[0]+dx*d,p[1]+dy*d]):d+=1
 return [p[0]+dx*(d-clearance),p[1]+dy*(d-clearance),dx,dy]
sites=[berth(41.7259,(0,-1)),berth(41.7259,(0,1)),berth(41.7148,(0,-1)),berth(41.6630,(1,0)),berth(41.6644,(1,0)),berth(41.6654,(1,0))]
# Final terminals on same slanted east-bank reach near Basin5; mark reviewed source coordinates.
# Moving upstream→downstream in the shared route, 106thStreet is the convoy crossing.
gate=center_at(41.70245);forward=[-1,0]
start=route[0];angle=math.atan2(route[1][1]-start[1],route[1][0]-start[0])
lines=['package com.bargeboss.game;','/** Generated from docs/levels/calumet/survey.json; run tools/build-calumet-survey.py. */','final class CalumetSurveyData {']
lines+=['static final float[] COAST={'+flat(coast)+'};','static final float[][] ROUTE={'+','.join('{'+flat([p])+'}' for p in route)+'};','static final float[][] JOB_SITES={'+','.join('{'+','.join(f'{v:.2f}f' for v in p)+'}' for p in sites)+'};','static final float[] CONVOY_GATE={'+','.join(f'{v:.2f}f' for v in [*gate,*forward])+'};','static final float[][] BRIDGES={'+','.join('{'+','.join(f'{v:.2f}f' for v in p)+'}' for p in bridges)+'};','static final float[][] ROADS=roads();']
lines+=['private static float[][] roads(){return new float[][]{'+','.join('{'+','.join(f'{v:.1f}f' for v in p)+'}' for p in roads)+'};}']
lines+=['static final int[] KINDS={'+','.join(str(a[0]) for a in areas)+'};','static final float[][] LAND=land();','private static float[][] land(){return new float[][]{'+','.join('a%d()'%i for i in range(len(areas)))+'};}']
for i,(k,p) in enumerate(areas):lines+=['private static float[] a%d(){return new float[]{'%i+flat(p)+'};}']
lines+=['}']
(ROOT/'core/com/bargeboss/game/CalumetSurveyData.java').write_text('\n'.join(lines)+'\n')
print('coast vertices',len(coast),'land areas',len(areas),'road segments',len(roads),'start',start,'angle',angle,'sites',sites)
# Preclassify 64-world-unit land tiles outside play, never ray-test every landuse polygon per draw.
cell=64;minx=-512;miny=-512;cols=math.ceil((max(p[0] for p in coast)+1024)/cell);rows=math.ceil((max(p[1] for p in coast)+1024)/cell)
boxes=[]
for k,p in areas:
 xs=[v[0] for v in p];ys=[v[1] for v in p];area=abs(sum(p[i][0]*p[(i+1)%len(p)][1]-p[(i+1)%len(p)][0]*p[i][1] for i in range(len(p))))
 boxes.append((area,k,p,min(xs),min(ys),max(xs),max(ys)))
boxes.sort(reverse=True,key=lambda a:a[0])
def inside_shape(x,y,p):
 v=False
 for a,b in zip(p,p[1:]+p[:1]):
  if (a[1]>y)!=(b[1]>y) and x<(b[0]-a[0])*(y-a[1])/(b[1]-a[1])+a[0]:v=not v
 return v
cover=[]
for row in range(rows):
 y=miny+(row+.5)*cell;out=[]
 for col in range(cols):
  x=minx+(col+.5)*cell;k=6 if x>8000 else 0
  for area,kind,p,l,t,r,b in boxes:
   if l<=x<=r and t<=y<=b and inside_shape(x,y,p):k=kind
  out.append(str(k))
 cover.append(''.join(out))
p=ROOT/'core/com/bargeboss/game/CalumetSurveyData.java';text=p.read_text();text=text.rsplit('}',1)[0]+'\nstatic final float CELL=64,MIN_X=-512,MIN_Y=-512;\nstatic final String[] COVER={\n'+',\n'.join('"'+r+'"' for r in cover)+'};\n}\n';p.write_text(text)
print('classified tiles',cols,rows)
