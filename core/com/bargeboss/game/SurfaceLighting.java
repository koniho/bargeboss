package com.bargeboss.game;

/** World-space lamp sampling and four screen-space surface responses, shared by every host.
 * No per-frame image processing, nearest-light switching or camera-dependent light strength. */
public final class SurfaceLighting {
    private SurfaceLighting(){}
    public static final float MAX_SHADE=.68f;
    /** Darkness is removed from faces reached by light; overlapping masks partition the surface. */
    static float shade(float[] light,int face){
        float total=0;for(float value:light)total+=value;
        float reach=light[face]*3f+.65f*(light[(face+1)%4]+light[(face+3)%4])+.15f*total;
        return MAX_SHADE*(1-Physics.clamp(reach,0,1));
    }
    private static float ambience(Game g){return g.mode==Game.TITLE?.45f:1;}

    /** Order: right, down, left, up in the upright sprite's coordinate system. */
    static void add(float[] weights,float x,float y,float lx,float ly,float height,float radius,float energy){
        float dx=lx-x,dy=ly-y,d2=dx*dx+dy*dy;
        if(d2>=radius*radius || energy<=0)return;
        float fall=1-d2/(radius*radius);fall=fall*fall*energy;
        float sx=RiverCamera.Q*(dx+dy),sy=RiverCamera.Q*RiverCamera.DEPTH*(dy-dx)-height;
        addProjected(weights,sx,sy,fall);
    }
    private static void addProjected(float[] weights,float sx,float sy,float fall){
        float length=Math.max(1,Physics.length(sx,sy));sx/=length;sy/=length;
        float sum=Math.abs(sx)+Math.abs(sy);
        if(sum<.001f){weights[3]+=fall;return;}
        weights[sx>=0?0:2]+=Math.abs(sx)/sum*fall;
        weights[sy>=0?1:3]+=Math.abs(sy)/sum*fall;
    }
    static float[] sample(Game g,float x,float y,Body receiver){
        float[] weights=new float[4];
        if(g.mode==Game.TITLE){ // Title has its own sunlit composition rather than gameplay quay lamps.
            add(weights,x,y,x-65,y-90,55,220,.65f);return weights;
        }
        for(int side=0;g.tutorial!=null && side<2;side++)for(int lamp=HarborAtmosphere.firstLamp(g);lamp<=5;lamp++){
            float lx=HarborAtmosphere.lampBank(side),ly=HarborAtmosphere.lampY(lamp);
            if(g.tutorial!=null && TrainingChannel.contains(lx,ly))continue;
            add(weights,x,y,lx,ly,17+(g.tutorial!=null?GroundArt.LAND_RISE:0),155,
                    .9f*SceneryEffects.intensity(g.visualTicks,lamp*1.31f+side*.77f));
        }
        if(g.tutorial!=null)FactorySources.sample(g,weights,x,y);
        if(g.montroseStage())MarinaPedestals.sample(g,weights,x,y);
        vessel(g,weights,x,y,g.tug,receiver);
        if(g.teacherBoat()!=null)vessel(g,weights,x,y,g.teacherBoat(),receiver);
        for(Feeder f:g.feeders)if(f.phase!=Feeder.IDLE)vessel(g,weights,x,y,f.boat,receiver);
        for(Body b:g.cargo)if(b!=null && b.active)vessel(g,weights,x,y,b,receiver);
        if(g.lockStage())for(int side=0;side<2;side++)for(int gate=0;gate<2;gate++)
            add(weights,x,y,side==0?Physics.LEFT:Physics.RIGHT,LockView.gateY(gate),32,125,.65f);
        // Bound overlapping lamps together; never flash white where pools overlap.
        float total=0;for(float w:weights)total+=w;
        if(total>1)for(int i=0;i<4;i++)weights[i]/=total;
        return weights;
    }
    /** Lazy spatial index: nearby authored furnace/window anchors, not a scan of the whole yard. */
    private static final class FactorySources {
        private static final float CELL=192,RADIUS=180;
        private static final java.util.Map<Long,java.util.List<float[]>> GRID=make();
        private static long key(int x,int y){return ((long)x<<32)|(y&0xffffffffL);}
        private static int cell(float n){return (int)StrictMath.floor(n/CELL);}
        private static java.util.Map<Long,java.util.List<float[]>> make(){
            java.util.Map<Long,java.util.List<float[]>> map=new java.util.HashMap<Long,java.util.List<float[]>>();
            for(int i=0;i<TrainingChannelView.FACTORY_PLACEMENTS;i++){
                int asset=TrainingChannelView.factoryCell(i);float x=TrainingChannelView.factoryX(i),y=TrainingChannelView.factoryY(i),size=FactoryArt.size(asset);
                long key=key(cell(x),cell(y));java.util.List<float[]> list=map.get(key);
                if(list==null){list=new java.util.ArrayList<float[]>();map.put(key,list);}
                float phase=asset*1.71f+x*.011f+y*.007f;
                for(int k=0;k<FactoryArt.lightCount(asset);k++)list.add(new float[]{x,y,
                        (FactoryArt.lightX(asset,k)-FactoryArt.pivotX(asset))*size,
                        (FactoryArt.lightY(asset,k)-FactoryArt.pivotY(asset))*size-GroundArt.LAND_RISE,phase,asset,k});
            }
            return map;
        }
        static void sample(Game g,float[] weights,float x,float y){
            int col=cell(x),row=cell(y);
            for(int yy=row-1;yy<=row+1;yy++)for(int xx=col-1;xx<=col+1;xx++){
                java.util.List<float[]> list=GRID.get(key(xx,yy));if(list==null)continue;
                for(float[] lamp:list){
                    float dx=lamp[0]-x,dy=lamp[1]-y,d2=dx*dx+dy*dy;if(d2>=RADIUS*RADIUS)continue;
                    float fall=1-d2/(RADIUS*RADIUS),activity=FactoryActivity.activity((int)lamp[5],(int)lamp[6],g.visualTicks,lamp[4]);
                    float power=(.12f+.12f*activity)*SceneryEffects.intensity(g.visualTicks,lamp[4]+lamp[6]*1.37f);
                    addProjected(weights,RiverCamera.Q*(dx+dy)+lamp[2],RiverCamera.Q*RiverCamera.DEPTH*(dy-dx)+lamp[3],fall*fall*power);
                }
            }
        }
    }
    private static void vessel(Game g,float[] w,float x,float y,Body b,Body receiver){
        if(b==receiver || !b.active)return;
        float energy=SceneryEffects.workIntensity(g.visualTicks,b.id*.71f);
        if(b.berth<0)add(w,x,y,b.x,b.y,18,100,.45f*energy);
        else for(int end=0;end<2;end++){
            float local=VesselLighting.bargeLocalX(b,end);
            add(w,x,y,b.x+local*(float)StrictMath.cos(b.angle),b.y+local*(float)StrictMath.sin(b.angle),5,65,.16f*energy);
        }
    }
    public static void directional(Game g,Painter p,int source,float angle,float x,float y,float size,float alpha,Body body){
        directionalAt(g,p,source,angle,x,y,size,alpha,body,body.x,body.y);
    }
    static void directionalAt(Game g,Painter p,int source,float angle,float x,float y,float size,float alpha,Body body,float worldX,float worldY){
        int atlas=SurfaceLightArt.forSource(source);if(atlas<0 || alpha<=0)return;
        RiverPainter river=p instanceof RiverPainter?(RiverPainter)p:null;
        Painter canvas=river==null?p:river.canvas();RiverCamera c=river==null?null:river.camera;
        float heading=angle/(float)(Math.PI/16),whole=(float)StrictMath.floor(heading);
        int a=(((int)whole+(river==null?0:RiverCamera.DIRECTION_OFFSET))%32+32)%32,b=(a+1)%32;
        float mix=Physics.clamp((heading-whole-.4f)*5,0,1);mix=mix*mix*(3-2*mix);
        float sx=river==null?x:c.screenX(x,y),sy=river==null?y:c.screenY(x,y),s=river==null?size:size*c.scale;
        float[] weights=sample(g,worldX,worldY,body);
        for(int d=0;d<4;d++){
            float shade=shade(weights,d)*ambience(g)*alpha;
            if(shade>.004f)canvas.spriteBlend(atlas,SurfaceLightArt.frame(source,d,a),SurfaceLightArt.frame(source,d,b),sx,sy,s,s,0,mix,shade);
        }
    }
    /** Affine receiver path also supports future explicitly enabled static/rigid scenery. */
    public static void affine(Game g,Painter p,int source,int frame,float wx,float wy,float x,float y,float ax,float ay,float bx,float by,float roll){
        int atlas=SurfaceLightArt.forSource(source);if(atlas<0)return;
        float[] world=sample(g,wx,wy,null),local=new float[4];
        // Rotate light directions into the rolling sprite's axes; its world lighting stays put.
        for(int d=0;d<4;d++){
            double angle=d*Math.PI/2-roll;float dx=(float)StrictMath.cos(angle),dy=(float)StrictMath.sin(angle),sum=Math.abs(dx)+Math.abs(dy);
            local[dx>=0?0:2]+=world[d]*Math.abs(dx)/sum;local[dy>=0?1:3]+=world[d]*Math.abs(dy)/sum;
        }
        for(int d=0;d<4;d++){
            float shade=shade(local,d)*ambience(g);
            if(shade>.004f)p.spriteAffine(atlas,SurfaceLightArt.frame(source,d,frame),x,y,ax,ay,bx,by,shade);
        }
    }
}
