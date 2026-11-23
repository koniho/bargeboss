package com.bargeboss.game;

/** Distinct surveyed structures: west/east marina tubular lights and Wilson Avenue lattice light.
 * Fixed shared geometry, prepared once; all light anchors belong to their model root. */
final class MontroseBeaconArt {
    static final int WEST=0,EAST=1,HORN=2;
    private static final float HEIGHT=76;
    static final float LIGHT_RADIUS=220, LIGHT_ENERGY=.85f;
    private static final float[][] MAP={{369,1258},{548,1290},{2264,-1410}};
    private static final ArticulatedModel[] MODELS=new ArticulatedModel[3];
    private static final ArticulatedModel.Pose[] POSES=new ArticulatedModel.Pose[3];
    private static final float[][] LAMPS=new float[3][];
    static {
        for(int i=0;i<3;i++){
            MODELS[i]=build(i);POSES[i]=MODELS[i].pose(0);MODELS[i].prepare(POSES[i]);
            LAMPS[i]=POSES[i].point(0,0,0,HEIGHT);
        }
    }
    private MontroseBeaconArt(){}
    /** Trigger immutable model/BSP construction during stage loading, before active draws. */
    static void prepare(){}
    static float x(int id){return MontroseHarbor.mapX(MAP[id][0]);}
    static float y(int id){return MontroseHarbor.mapY(MAP[id][1]);}
    static float rise(int id){return id==HORN?MontroseHarbor.LAND_RISE:MontroseHarbor.DOCK_RISE;}
    static int color(int id){return id==EAST?NavigationLights.RED:NavigationLights.GREEN;}
    static float power(int ticks){return NavigationLights.flash(ticks,0,180);}
    static float lampZ(int id){return LAMPS[id][2];}
    static void sample(Game g,float[] weights,float x,float y){
        float energy=power(g.visualTicks)*LIGHT_ENERGY;if(energy<=0)return;
        for(int id=0;id<3;id++)SurfaceLighting.add(weights,x,y,x(id),y(id),lampZ(id)*ArticulatedModel.Z_SCALE,LIGHT_RADIUS,energy);
    }
    static float[] lamp(int id){return LAMPS[id].clone();}
    static int faces(int id){return MODELS[id].sourceFaceCount();}
    static boolean visible(Game g,int id){RiverCamera c=g.layout.camera;float px=c.screenX(x(id),y(id)),py=c.screenY(x(id),y(id)),pad=110*c.scale;
        return px+pad>=0 && px-pad<=g.layout.width && py+pad>=0 && py-pad<=g.layout.height;}
    private static ArticulatedModel build(int id){
        ArticulatedModel.Builder b=new ArticulatedModel.Builder();
        int n=b.node(id==HORN?"wilson-avenue-beacon":"montrose-inlet-"+id,-1,x(id),y(id),rise(id)/ArticulatedModel.Z_SCALE,0,0);
        int steel=0xff75504d,white=0xffa2aba6,band=id==WEST?0xff397262:0xff99594e;
        if(id==HORN){
            // Weathered red equipment cabinet and anti-climb apron below four tapered legs.
            b.box(n,0,0,9,11,11,18,0xff674a4a);
            b.box(n,0,0,20,23,23,1.4f,0xff51595b);
            for(int side=0;side<4;side++){
                float sx=side<2?-1:1,sy=side%2==0?-1:1;
                b.beam(n,sx*8,sy*8,0,sx*3.6f,sy*3.6f,68,1.25f,steel);
            }
            for(int level=0;level<4;level++){
                float z=20+level*12,h=12,lo=8-4.4f*z/68,hi=8-4.4f*(z+h)/68;
                for(int side=0;side<4;side++){
                    float ax=side==0?-lo:side==1?lo:side==2?lo:-lo,ay=side<2?-lo:lo;
                    float bx=side==0?hi:side==1?hi:side==2?-hi:-hi,by=side<1?-hi:side<3?hi:-hi;
                    b.beam(n,ax,ay,z,bx,by,z+h,.65f,steel);
                }
            }
            b.box(n,0,0,68,15,15,1.2f,0xff654b4a);
            rail(b,n,7,68,6,steel,4);
            b.box(n,0,0,72,2.2f,2.2f,6,0xff69736e);
        }else{
            // Photographed inlet pair: square plinth, tapered white tube, two colored bands.
            b.box(n,0,0,4.5f,14,14,9,0xff746c5c);
            float[] z={9,38,47,49,58,69};
            for(int i=0;i<z.length-1;i++)b.cylinder(n,z[i],z[i+1]-z[i],radius(z[i]),radius(z[i+1]),8,i==1 || i==3?band:white);
            b.cylinder(n,69,1.2f,5,5,8,white);
            rail(b,n,4.8f,70,4.2f,white,8);
            b.cylinder(n,70,5,1.2f,1.2f,8,0xff6a7775);
        }
        // Small unlit lens; emissive core is drawn only during the nominal flash.
        b.cylinder(n,75,2,1,1,8,id==EAST?0xff663c39:0xff305b4f);
        return b.build();
    }
    private static float radius(float z){return 3.7f-(z-9)*.023f;}
    private static void rail(ArticulatedModel.Builder b,int n,float radius,float z,float height,int color,int sides){
        for(int i=0;i<sides;i++){
            double a=(i+.5)*2*StrictMath.PI/sides,aa=(i+1.5)*2*StrictMath.PI/sides;
            float x=radius*(float)StrictMath.cos(a),y=radius*(float)StrictMath.sin(a),xx=radius*(float)StrictMath.cos(aa),yy=radius*(float)StrictMath.sin(aa);
            b.beam(n,x,y,z,x,y,z+height,.55f,color);b.beam(n,x,y,z+height,xx,yy,z+height,.55f,color);
        }
    }
    static void draw(Game g,Painter p,int id){
        if(!visible(g,id))return;
        MODELS[id].draw(g,p,POSES[id]);RiverCamera c=g.layout.camera;float[] a=LAMPS[id];
        float lx=c.screenX(a[0],a[1]),ly=c.screenY(a[0],a[1])-a[2]*ArticulatedModel.Z_SCALE*c.scale,power=power(g.visualTicks);
        SceneryEffects.glow(p,lx,ly,3*c.scale,color(id),power,g.visualTicks,id,1);
        if(power>0){p.circle(lx,ly,1.1f*c.scale,SoftCue.alpha(color(id),power));p.circle(lx,ly,.45f*c.scale,SoftCue.alpha(0xffe4f4e6,power));}
    }
    /** Contact is actual foundation geometry, not a shadow of the old decorative lighthouse. */
    static void ground(Game g,Painter p,int id){
        RiverCamera c=g.layout.camera;Painter world=new RiverPainter(ScreenView.offset(p,0,-rise(id)*c.scale),c);
        float x=x(id),y=y(id),r=id==HORN?10:8;
        world.poly(new float[]{x-r,y-r,x+r,y-r,x+r,y+r,x-r,y+r},0x240b171e);
    }
    static void reflection(Game g,Painter canvas){
        if(!visible(g,HORN))return;float power=power(g.visualTicks);if(power<=0)return;
        Painter world=new RiverPainter(canvas,g.layout.camera);
        WaterReflection.draw(g,world,x(HORN),y(HORN),18,55,SoftCue.alpha(color(HORN),power*.6f),20130);
    }
}
