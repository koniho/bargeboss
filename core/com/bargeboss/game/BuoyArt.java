package com.bargeboss.game;

/** Registered single-view floats. Only wave roll rotates these sprites, never vessel heading.
 * Source PNGs remain unchanged; the moving water plane hides their submerged lower body.
 * Per-variant inventory: lantern (neutral glass), no smoke outlets, no tire effect anchors.
 * The solar panel and lifting/mooring rings are not emitters. */
public final class BuoyArt {
    public static final int RED=0,GREEN=1,YELLOW=2,COUNT=3,SOURCE_SIZE=1254;
    public static final float PRACTICE_SIZE=48,MARKER_SIZE=28,CHANNEL_SIZE=16,MAX_ROLL=.105f;
    private static final String[] FILES={"buoy-red-v1.png","buoy-green-v1.png","buoy-yellow-v1.png"};
    // Measured from each original PNG, in pixels; all lantern centers were inspected separately.
    private static final float[] WATER_X={619,620,619},WATER_Y={1109,1109,1109};
    private static final float[] LAMP_X={627,627,627},LAMP_Y={270,270,270};
    private BuoyArt(){}
    public static String file(int frame){return FILES[frame];}
    public static float pivotX(int frame){return WATER_X[frame]/SOURCE_SIZE;}
    public static float pivotY(int frame){return WATER_Y[frame]/SOURCE_SIZE;}
    public static float lampX(int frame){return LAMP_X[frame]/SOURCE_SIZE;}
    public static float lampY(int frame){return LAMP_Y[frame]/SOURCE_SIZE;}
    public static int smokeCount(int frame){return 0;}
    public static int tireCount(int frame){return 0;}
    public static final class Pose {
        public final float x,y,size,roll,cos,sin,waterY;
        Pose(float x,float y,float size,float roll,float waterY){this.x=x;this.y=y;this.size=size;this.roll=roll;this.waterY=waterY;cos=(float)StrictMath.cos(roll);sin=(float)StrictMath.sin(roll);}
        public float pointX(int frame,float u,float v){return x+size*((u-pivotX(frame))*cos-(v-pivotY(frame))*sin);}
        public float pointY(int frame,float u,float v){return y+size*((u-pivotX(frame))*sin+(v-pivotY(frame))*cos);}
    }
    public static Pose pose(Game g,float wx,float wy,float size){
        RiverCamera c=g.layout.camera;int tick=g.visualTicks;
        float surface=g.water.height(wx,wy,tick,null),heave=g.water.height(wx,wy,tick-4,null)*.85f;
        float across=g.water.height(wx+6,wy+6,tick,null)-g.water.height(wx-6,wy-6,tick,null);
        float roll=Physics.clamp(across*.07f+.018f*(float)StrictMath.sin(tick*.032+wx*.017+wy*.009),-MAX_ROLL,MAX_ROLL);
        return new Pose(c.screenX(wx,wy),c.screenY(wx,wy)-heave*c.scale,size*c.scale,roll,
                c.screenY(wx,wy)-surface*c.scale+size*c.scale*.028f);
    }
    /** Horizontal surface is deliberately independent of the sprite's roll and delayed heave. */
    static void hull(Painter p,int frame,Pose pose){
        float left=pose.x-pose.size,right=pose.x+pose.size,bottom=pose.y+pose.size,top=pose.waterY;
        p.pushClipOutside(new float[]{left,top,right,top,right,bottom,left,top,right,bottom,left,bottom});
        try{p.spriteAffine(SpriteAtlas.BUOY,frame,pose.x,pose.y,pose.size*pose.cos,pose.size*pose.sin,-pose.size*pose.sin,pose.size*pose.cos,1);}
        finally{p.popClip();}
    }
    public static void draw(Game g,Painter p,float wx,float wy,float size,int frame,int color,float power,int number){
        Pose pose=pose(g,wx,wy,size);RiverCamera c=g.layout.camera;
        p.pushClipOutside(ShoreOcclusion.region(g,c,wx-size,wy-size,wx+size,wy+size,c.screenY(wx,wy)));
        try{
            hull(p,frame,pose);
            float lx=pose.pointX(frame,lampX(frame),lampY(frame)),ly=pose.pointY(frame,lampX(frame),lampY(frame));
            SceneryEffects.glow(p,lx,ly,pose.size*.15f,color,power,g.visualTicks,wx*.013f);
            if(power>0){p.circle(lx,ly,pose.size*.032f,SoftCue.alpha(color,.75f*power));p.circle(lx,ly,pose.size*.012f,SoftCue.alpha(0xfffff5dc,power));}
            if(number>0)Lettering.center(p,""+number,pose.pointX(frame,.5f,.783f),pose.pointY(frame,.5f,.783f)-pose.size*.0335f,pose.size*.067f,0xff263035);
        }finally{p.popClip();}
    }
    /** Independent contact/cast alpha, flattened onto water before the depth-sorted hull pass. */
    public static void shadows(Game g,Painter p,float wx,float wy,float size,int frame){
        RiverCamera c=g.layout.camera;Pose pose=pose(g,wx,wy,size);float[] light=new float[3];ShadowView.light(g,wx,wy,light);
        p.pushClipOutside(ShoreOcclusion.region(g,c,wx-size,wy-size,wx+size,wy+size,Float.NEGATIVE_INFINITY));
        try{
            int atlas=ShadowArt.BUOY;float s=pose.size*SpriteAtlas.widthScale(atlas,frame);
            // Roll the source about its registered pivot before flattening onto the receiving plane.
            p.spriteAffine(atlas,frame,pose.x,pose.waterY,s*pose.cos,s*.12f*pose.sin,-s*pose.sin,s*.12f*pose.cos,.18f);
            float skew=-light[0]*.25f,flatten=.18f-light[1]*.25f;
            p.spriteAffine(atlas,frame+COUNT,pose.x,pose.waterY,s*(pose.cos+skew*pose.sin),s*flatten*pose.sin,
                    s*(-pose.sin+skew*pose.cos),s*flatten*pose.cos,.13f+.06f*light[2]);
        }finally{p.popClip();}
    }
}
