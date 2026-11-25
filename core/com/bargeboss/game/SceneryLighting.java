package com.bargeboss.game;

import java.util.Arrays;

/** Frame-local world lights for every live 3-D receiver. Fixed storage, spatial queries,
 * physical model heights and exact moving-source anchors; no texture/pose caches. */
final class SceneryLighting implements ArticulatedModel.FaceLighting {
    private static final int CAPACITY=2048,BUCKETS=2048;
    private static final float CELL=256;
    private static final ThreadLocal<SceneryLighting> LOCAL=new ThreadLocal<SceneryLighting>(){
        protected SceneryLighting initialValue(){return new SceneryLighting();}
    };
    private final float[] x=new float[CAPACITY],y=new float[CAPACITY],z=new float[CAPACITY],radius=new float[CAPACITY],power=new float[CAPACITY],heading=new float[CAPACITY];
    private final int[] color=new int[CAPACITY],sector=new int[CAPACITY],next=new int[CAPACITY],heads=new int[BUCKETS],col=new int[CAPACITY],row=new int[CAPACITY];
    private Game game;private int tick,preset,count;private float tugX,tugY,tugAngle;
    private SceneryLighting(){Arrays.fill(heads,-1);}
    static SceneryLighting get(Game g){SceneryLighting s=LOCAL.get();
        if(s.game!=g || s.tick!=g.visualTicks || s.preset!=g.lighting().id || s.tugX!=g.tug.x || s.tugY!=g.tug.y || s.tugAngle!=g.tug.angle)s.collect(g);
        return s;
    }
    /** Called once per production frame; direct model review/tests may call this too. */
    static void begin(Game g){LOCAL.get().collect(g);}
    /** Exact floor for in-range cells without Android's bit-level StrictMath.floor. */
    private static int cell(float v){float q=v/CELL;int i=(int)q;return q<i?i-1:i;}
    private static int hash(int x,int y){return (x*73856093^y*19349663)&(BUCKETS-1);}
    void add(float px,float py,float pz,float r,float energy,int tint){add(px,py,pz,r,energy,tint,0,-1);}
    private void add(float px,float py,float pz,float r,float energy,int tint,float angle,int fixture){
        if(energy<=0)return;
        if(r>CELL || r<=0 || count==CAPACITY)throw new IllegalStateException("Scenery light budget/radius");
        int i=count++;x[i]=px;y[i]=py;z[i]=pz;radius[i]=r;power[i]=energy;color[i]=tint;heading[i]=angle;sector[i]=fixture;
        col[i]=cell(px);row[i]=cell(py);int bucket=hash(col[i],row[i]);next[i]=heads[bucket];heads[bucket]=i;
    }
    private void collect(Game g){game=g;tick=g.visualTicks;preset=g.lighting().id;tugX=g.tug.x;tugY=g.tug.y;tugAngle=g.tug.angle;count=0;Arrays.fill(heads,-1);
        if((g.renderExclusions&RenderExclusions.SCENERY_POINT_LIGHTS)!=0)return;
        if(g.montroseStage()){
            for(int i=0;i<MontroseHarbor.PEDESTALS.length;i++){float[] p=MontroseHarbor.PEDESTALS[i];add(p[0],p[1],MarinaPedestals.LAMP_Z,90,.16f*MarinaPedestals.brightness(g,i),0xffffd68d);}
            for(int i=0;i<3;i++)add(MontroseBeaconArt.x(i),MontroseBeaconArt.y(i),MontroseBeaconArt.lampZ(i),MontroseBeaconArt.LIGHT_RADIUS,MontroseBeaconArt.LIGHT_ENERGY*MontroseBeaconArt.power(tick),MontroseBeaconArt.color(i));
        }else if(!g.calumetStage() && !g.surveyStage()){
            for(int side=0;side<2;side++)for(int i=HarborAtmosphere.firstLamp(g);i<=5;i++){
                float px=HarborAtmosphere.lampBank(side),py=HarborAtmosphere.lampY(i);
                if(g.tutorial!=null && TrainingChannel.contains(px,py))continue;
                add(px,py,(17+GroundArt.LAND_RISE)/ArticulatedModel.Z_SCALE,155,.9f*SceneryEffects.intensity(tick,i*1.31f+side*.77f),0xffffbd79);
            }
        }
        vessel(g,g.tug);if(g.teacherBoat()!=null)vessel(g,g.teacherBoat());
        for(Body b:g.cargo)if(b!=null)vessel(g,b);
        for(Feeder f:g.feeders)if(f.phase!=Feeder.IDLE)vessel(g,f.boat);
        if(g.trafficActive())vessel(g,g.trafficBoat);
        for(int i=0;i<g.passages.bridges.length;i++)g.passages.bridges[i].lights(g,this,g.passages.amount(i),g.passages.passable(i));
        DockView.lights(g,this);
        if(g.montroseStage())MontroseBeach.lights(this);
        if(g.indianaStage())IndianaView.lights(this);
        if(g.chicagoStage())ChicagoView.lights(g,this);
        if(g.bridgeStage())BridgeView.lights(g,this);
        if(g.lockStage())LockView.lights(g,this);
        if(g.tutorial!=null){
            for(int i=0;i<FireworksLesson.PILES;i++)add(FireworksLesson.x(i),FireworksLesson.y(i),GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE+52,210,1.7f*FireLight.energy(g,i)/g.lighting().lamps,FireLight.color(g,i));
            YardActivity.lights(g,this);
        }
    }
    private void vessel(Game g,Body b){if(!b.active)return;
        if(b.berth<0)add(b.x,b.y,22,100,.65f*SceneryEffects.workIntensity(tick,b.id*.71f),0xffffbc6b);
        else if(!b.yacht)for(int end=0;end<2;end++){float u=VesselLighting.bargeLocalX(b,end);add(b.x+u*(float)StrictMath.cos(b.angle),b.y+u*(float)StrictMath.sin(b.angle),5,65,.16f*SceneryEffects.workIntensity(tick,b.id*.71f),0xffffbc6b);}
        for(int i=0;i<VesselNavigationLights.sourceCount(b);i++)if(VesselNavigationLights.emitting(g,b,i)){
            float energy=b.cruiser?(i==VesselNavigationLights.ANCHOR?.12f:.22f):.55f;
            add(VesselNavigationLights.sourceX(g,b,i),VesselNavigationLights.sourceY(g,b,i),VesselNavigationLights.height(b,i),55,energy,VesselNavigationLights.color(i),VesselNavigationLights.sourceHeading(g,b),i);
        }
    }
    public int shade(int ambient,int albedo,float px,float py,float pz,float nx,float ny,float nz){
        float red=0,green=0,blue=0;int cx=cell(px),cy=cell(py);
        for(int yy=cy-1;yy<=cy+1;yy++)for(int xx=cx-1;xx<=cx+1;xx++)for(int i=heads[hash(xx,yy)];i>=0;i=next[i]){
            if(col[i]!=xx || row[i]!=yy)continue;
            float dx=x[i]-px,dy=y[i]-py,dz=z[i]-pz,d2=dx*dx+dy*dy+dz*dz,r=radius[i];
            if(d2>=r*r)continue;float dot=nx*dx+ny*dy+nz*dz;if(dot<=0)continue;
            float e=power[i]*game.lighting().lamps*VesselShoreLight.falloff(d2,r)*dot/(float)StrictMath.sqrt(Math.max(.001f,d2));
            if(sector[i]>=0)e*=VesselShoreLight.sector(heading[i],sector[i],-dx,-dy);
            red+=e*(color[i]>>16&255)/255f;green+=e*(color[i]>>8&255)/255f;blue+=e*(color[i]&255)/255f;
        }
        float beam=game.spotlight.face(game,px,py,pz,nx,ny,nz);
        red+=beam;green+=beam*.93f;blue+=beam*.77f;
        // Unlit faces keep their ambient exactly (each channel already <=255).
        if(red==0 && green==0 && blue==0)return ambient;
        return ambient&0xff000000|Math.min(255,(ambient>>16&255)+Math.round((albedo>>16&255)*red))<<16
            |Math.min(255,(ambient>>8&255)+Math.round((albedo>>8&255)*green))<<8|Math.min(255,(ambient&255)+Math.round((albedo&255)*blue));
    }
    /** Arbitrary rigid face after animation; XYZ is in physical model units. */
    static int polygon(Game g,float[] xyz,int albedo,int material){
        float x=0,y=0,z=0;int n=xyz.length/3;
        for(int i=0;i<xyz.length;i+=3){x+=xyz[i];y+=xyz[i+1];z+=xyz[i+2];}
        float ax=xyz[3]-xyz[0],ay=xyz[4]-xyz[1],az=xyz[5]-xyz[2],bx=xyz[6]-xyz[0],by=xyz[7]-xyz[1],bz=xyz[8]-xyz[2];
        float nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx,len=(float)StrictMath.sqrt(nx*nx+ny*ny+nz*nz);
        return face(g,albedo,material,x/n,y/n,z/n,nx/len,ny/len,nz/len);
    }
    static int face(Game g,int albedo,int material,float x,float y,float z,float nx,float ny,float nz){
        LightingProfile p=g.lighting();float f=p.face(nx,ny,nz);
        int shaded=SceneLighting.scale(albedo,Math.round(f*(p.sunColor>>16&255)),Math.round(f*(p.sunColor>>8&255)),Math.round(f*(p.sunColor&255)));
        int ambient=material<0?p.scenery(shaded):p.surface(shaded,material);
        return get(g).shade(ambient,albedo,x,y,z,nx,ny,nz);
    }
}
