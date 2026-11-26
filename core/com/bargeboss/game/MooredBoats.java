package com.bargeboss.game;

/** Unlit marina receivers. All motion comes from the per-game tether simulation. */
public final class MooredBoats {
    public static final float CELL_WORLD=96;
    public static final int ORIENTATIONS=32,BASE_FRAMES=224,FRAMES=896,COLUMNS=16,ROWS=56;
    public static final class Berth {
        public final float x,y,angle,length,width,ax,ay,bx,by;
        public final int frame,kind;
        final float[] hull;
        int row=-1,station,side,mooring=-1,star=-1,arm=-1;
        public final int bimini;
        Berth(float x,float y,float angle,int orientation,int kind,float ax,float ay,float bx,float by,int bimini){
            this.bimini=bimini;this.x=x;this.y=y;this.angle=angle;this.kind=kind;this.ax=ax;this.ay=ay;this.bx=bx;this.by=by;
            length=length(kind);width=width(kind);frame=kind*ORIENTATIONS+(Math.round(angle*ORIENTATIONS/(2*(float)Math.PI))+ORIENTATIONS)%ORIENTATIONS;
            float c=(float)StrictMath.cos(angle),s=(float)StrictMath.sin(angle);hull=new float[8];
            for(int i=0;i<4;i++){float u=(i<2?-1:1)*length*.48f,v=(i==0||i==3?-1:1)*width*.48f;hull[i*2]=x+c*u-s*v;hull[i*2+1]=y+s*u+c*v;}
        }
    }
    public static float length(int kind){return new float[]{39,44,47,40,46,49,56}[kind];}
    public static float width(int kind){return kind==6?14:kind>=3?13:16;}
    public static float angle(int orientation){if(orientation>=30)return (orientation-30+.5f)*(float)Math.PI/4;float[] r=MontroseHarbor.berthRows()[orientation/2];return (float)StrictMath.atan2(r[3]-r[1],r[2]-r[0])+(orientation%2==0?-1:1)*(float)Math.PI/2;}
    public static final Berth[] BERTHS=make();
    private static Berth[] make(){java.util.ArrayList<Berth> out=new java.util.ArrayList<Berth>();float[][] rows=MontroseHarbor.berthRows();
        for(int row=0;row<rows.length;row++){float[] r=rows[row];float dx=r[2]-r[0],dy=r[3]-r[1],len=Physics.length(dx,dy);dx/=len;dy/=len;
            for(float at=45;at+15<len-8;at+=30)for(int side=-1;side<=1;side+=2){
                if(!MontroseHarbor.fingerSide(row,side))continue;
                float px=r[0]+dx*at,py=r[1]+dy*at;
                if(row==1 && side==-1 && py>285)continue; // Mission yacht and its approach remain clear.
                int kind=(row*7+(int)(at/30)*3+(side==1?2:0))%6,orientation=row*2+(side==1?1:0);
                float a=angle(orientation),c=(float)StrictMath.cos(a),s=(float)StrictMath.sin(a),offset=length(kind)*.5f+9;
                float x=MontroseHarbor.mapX(px)+c*offset,y=MontroseHarbor.mapY(py)+s*offset;
                float ax=MontroseHarbor.mapX(px-dx*15)+c*10,ay=MontroseHarbor.mapY(py-dy*15)+s*10;
                float finger=(row<2?42:row<6?28:32)*MontroseHarbor.MAP_SCALE-14;
                float bx=ax+c*finger,by=ay+s*finger;
                Berth b=new Berth(x,y,a,orientation,kind,ax,ay,bx,by,BiminiArt.palette(row,(int)(at/30),side));boolean valid=true;
                for(int i=0;i<8;i+=2)if(!MontroseHarbor.contains(b.hull[i],b.hull[i+1]))valid=false;
                b.row=row;b.station=(int)at;b.side=side;
                if(valid)out.add(b);
            }
        }
        for(int starIndex=0;starIndex<MontroseHarbor.STAR_CENTERS.length;starIndex++)for(int arm=0;arm<8;arm++){
            float[] star=MontroseHarbor.STAR_CENTERS[starIndex];
            int kind=(arm+(int)star[0])%6;if(kind<0)kind+=6;float a=angle(30+arm),c=(float)StrictMath.cos(a),s=(float)StrictMath.sin(a),near=arm*(float)Math.PI/4,nc=(float)StrictMath.cos(near),ns=(float)StrictMath.sin(near);
            Berth berth=new Berth(star[0]+c*59,star[1]+s*59,a,30+arm,kind,star[0]+nc*31,star[1]+ns*31,star[0]+nc*59,star[1]+ns*59,BiminiArt.palette((int)star[0],arm,1));
            boolean valid=true;for(int i=0;i<8;i+=2)if(!MontroseHarbor.contains(berth.hull[i],berth.hull[i+1]))valid=false;
            berth.star=starIndex;berth.arm=arm;
            if(valid)out.add(berth);
        }
        int[] balls={1,9,17};
        for(int i=0;i<balls.length;i++){float[] ball=MontroseHarbor.MOORINGS[balls[i]];int kind=3+i;
            Berth b=new Berth(ball[0],ball[1]+length(kind)*.5f+15,-(float)Math.PI/2,0,kind,ball[0],ball[1],ball[0],ball[1],0);b.mooring=balls[i];out.add(b);}
        out.sort((a,b)->Float.compare(a.y-a.x,b.y-b.x));return out.toArray(new Berth[out.size()]);
    }
    public static int count(Game g){return g.montroseStage()?g.mooredFleet.boats.length:0;}
    public static float depth(Game g,int i){MooredFleet.FloatBoat f=g.mooredFleet.sorted(i);Berth b=BERTHS[i];return g.layout.camera.screenY(f==null?b.x:f.x,f==null?b.y:f.y);}
    public static float bob(int ticks,int i){return .35f*(float)StrictMath.sin(ticks*.026+i*1.73);}
    public static void draw(Game g,Painter p,int i){MooredFleet.FloatBoat f=g.mooredFleet.sorted(i);Berth b=f==null?BERTHS[i]:f.berth;
        if(f!=null && f.sinkTicks>=HullDamage.SINK)return;
        float wx=f==null?b.x:f.x,wy=f==null?b.y:f.y,angle=b.angle+(f==null?0:f.yaw),heave=f==null?0:f.heave;
        // A sinking hull settles under its float line and is gone by the plunge.
        float alpha=1;if(f!=null && f.sinkTicks>0){heave-=HullDamage.sinkDepth(f.sinkTicks,b.width*.5f);alpha=1-Physics.clamp((f.sinkTicks-HullDamage.PLUNGE+12)/24f,0,1);if(alpha<=0)return;}
        RiverCamera c=g.layout.camera;float x=c.screenX(wx,wy),y=c.screenY(wx,wy)-heave*c.scale,size=CELL_WORLD*c.scale;
        if(x+size*.5f<0||x-size*.5f>g.layout.width||y+size*.3f<0||y-size*.7f>g.layout.height)return;
        float detail=SceneDetail.boat(g,b.length,wx,wy);
        // Fade yaw detail toward the nearest authored resting pose. At distance
        // this becomes one cached time-graded sprite, while position/heave remain live.
        float rest=b.frame%ORIENTATIONS,full=angle*ORIENTATIONS/(2*(float)Math.PI);
        float delta=full-rest;delta-=ORIENTATIONS*(float)StrictMath.floor((delta+ORIENTATIONS*.5f)/ORIENTATIONS);
        float heading=rest+delta*detail,whole=(float)StrictMath.floor(heading),mix=heading-whole;
        int a=Math.floorMod((int)whole,ORIENTATIONS),next=(a+1)%ORIENTATIONS,base=b.kind*ORIENTATIONS;
        if(detail>.001f && g.settings.waterDetail>0){
            float reach=b.length*.65f+6;
            float[] rippleClip=ShoreOcclusion.region(g,c,wx-reach,wy-reach,wx+reach,wy+reach,Float.NEGATIVE_INFINITY,p.supportsUnionClips());
            if(rippleClip.length>0)p.pushClipOutside(rippleClip);
            try{VesselRipples.draw(g,new RiverPainter(p,c),b.frame+(int)b.x,wx,wy,angle,b.length,b.width,0,true,detail);}
            finally{if(rippleClip.length>0)p.popClip();}
        }
        if(b.mooring>=0){
            int ball=b.mooring+2;float bx=NavigationLights.x(g,ball),by=NavigationLights.y(g,ball);
            float tipX=wx+(float)StrictMath.cos(angle)*b.length*.45f,tipY=wy+(float)StrictMath.sin(angle)*b.length*.45f;
            float sx=c.screenX(tipX,tipY),sy=c.screenY(tipX,tipY)-(heave+2)*c.scale,ex=c.screenX(bx,by),ey=c.screenY(bx,by)-c.scale;
            p.line(sx,sy,(sx+ex)*.5f,(sy+ey)*.5f+1.5f*c.scale,.8f*c.scale,0x998d9c99);
            p.line((sx+ex)*.5f,(sy+ey)*.5f+1.5f*c.scale,ex,ey,.8f*c.scale,0x998d9c99);
        }
        dissolve(p,SpriteAtlas.MOORED_BOATS,base+a,base+next,x,y,size,size,mix,alpha);
        dissolve(p,SpriteAtlas.MOORED_BIMINI,BiminiArt.frame(b.bimini,base+a),BiminiArt.frame(b.bimini,base+next),
                x,y,size*BiminiArt.WIDTH/128,size*BiminiArt.HEIGHT/128,mix,alpha);
        // Struck: a hot flash over the hull for a few frames.
        if(f!=null){int since=g.ticks-f.damagedTick;if(since>=0 && since<16){float k=1-since/16f,len=b.length*c.scale;
            p.material(x,y-4*c.scale,len*1.3f,0,0,len*.8f,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffffc890,.85f*k));
            p.material(x,y-5*c.scale,len*.6f,0,0,len*.4f,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffffffff,k));}}
        if(detail<=.001f)return;
        MooredBoatLight.Lights lights=MooredBoatLight.lights(g,wx,wy,heave);
        for(int n=0;n<lights.count;n++){
            float energy=lights.energy[n]*detail*alpha,second=energy*mix,first=energy*(1-mix)/(1-second);
            p.normalSprite(base+a,x,y,size,size,lights.x[n],lights.y[n],lights.z[n],lights.radius[n],lights.color[n],first);
            if(second>.001f)p.normalSprite(base+next,x,y,size,size,lights.x[n],lights.y[n],lights.z[n],lights.radius[n],lights.color[n],second);
        }
    }
    // Offline comparison helpers only. Production boats always use sprites.
    static float liveWeight(Game g,float length,float x,float y){
        if((g.renderExclusions&RenderExclusions.LIVE_MARINA_BOATS)!=0)return 0;
        return SceneDetail.fade(length*g.layout.camera.scale,48,72)*(1-SceneDetail.fade(Physics.length(x-g.tug.x,y-g.tug.y),180,210));
    }
    static void drawHull(Game g,Painter p,int kind,int palette,float x,float y,float heave,float angle,float pitch,float roll,float alpha,MooredBoatLight.Lights lights){
        LiveLight shade=LIVE_LIGHT.get();shade.profile=g.lighting();shade.lights=lights;shade.palette=palette;shade.alpha=alpha;shade.length=length(kind);shade.width=width(kind);
        HarborBoatModel.get(kind).rigid.draw(g,p,x,y,heave,angle,pitch,roll,shade);
    }
    private static final ThreadLocal<LiveLight> LIVE_LIGHT=new ThreadLocal<LiveLight>(){protected LiveLight initialValue(){return new LiveLight();}};
    private static final class LiveLight implements ArticulatedModel.VertexLighting {
        LightingProfile profile;MooredBoatLight.Lights lights;int palette;float alpha,length,width;
        public boolean smooth(int material){return material==BoatMaterial.HULL;}
        public int vertex(int material,int albedo,float x,float y,float z,float nx,float ny,float nz,float[] m){
            if(material==BoatMaterial.HULL){
                nx=x/(length*length*.25f);ny=y/(width*width*.25f);nz=.012f;
                float inv=1/(float)StrictMath.sqrt(nx*nx+ny*ny+nz*nz);nx*=inv;ny*=inv;nz*=inv;
            }
            float xx=m[0]*x+m[1]*y+m[2]*z,yy=m[3]*x+m[4]*y+m[5]*z,zz=m[6]*x+m[7]*y+m[8]*z;
            float normalX=m[0]*nx+m[1]*ny+m[2]*nz,normalY=m[3]*nx+m[4]*ny+m[5]*nz,normalZ=m[6]*nx+m[7]*ny+m[8]*nz;
            int rgb=BoatMaterial.shade(profile,lights,material,albedo,xx,yy,zz,normalX,normalY,normalZ,1+.014f*(float)StrictMath.sin(x*.27f+y*.41f+z*.31f));
            return ((int)(255*alpha)<<24)|(rgb&0xffffff);
        }
        public int shade(int ambient,int albedo,float x,float y,float z,float nx,float ny,float nz){
            if(albedo==HarborBoatModel.FABRIC)albedo=BiminiArt.color(palette);
            int rgb=MarinaSurfaceLight.vessel(profile,lights,albedo,x,y,z,nx,ny,nz);
            return ((int)(255*alpha)<<24)|(rgb&0xffffff);
        }
    }

    /** Opaque-overlap dissolve avoids an offscreen additive layer per channel/boat.
     * Adjacent silhouettes can retain a narrow outgoing edge until the next heading. */
    private static void dissolve(Painter p,int atlas,int a,int b,float x,float y,float width,float height,float mix,float alpha){
        if(mix<.001f){p.sprite(atlas,a,x,y,width,height,0,alpha);return;}
        if(mix>.999f){p.sprite(atlas,b,x,y,width,height,0,alpha);return;}
        float second=alpha*mix,first=alpha*(1-mix)/(1-second);
        p.sprite(atlas,a,x,y,width,height,0,first);
        p.sprite(atlas,b,x,y,width,height,0,second);
    }
    static boolean occupied(float x,float y){for(Berth b:BERTHS)if(Math.abs(x-b.x)<b.length && Math.abs(y-b.y)<b.length && PolygonWater.contains(b.hull,x,y))return true;return false;}
    private MooredBoats(){}
}
