package com.bargeboss.game;

/** Registered single-view floats. Only wave/contact roll rotates these sprites, never vessel heading.
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
        public final float x,y,size,roll,cos,sin,waterY,phase;
        Pose(float x,float y,float size,float roll,float waterY){this(x,y,size,roll,waterY,0);}
        Pose(float x,float y,float size,float roll,float waterY,float phase){this.x=x;this.y=y;this.size=size;this.roll=roll;this.waterY=waterY;this.phase=phase;cos=(float)StrictMath.cos(roll);sin=(float)StrictMath.sin(roll);}
        public float pointX(int frame,float u,float v){return x+size*((u-pivotX(frame))*cos-(v-pivotY(frame))*sin);}
        public float pointY(int frame,float u,float v){return y+size*((u-pivotX(frame))*sin+(v-pivotY(frame))*cos);}
    }
    public static Pose pose(Game g,float wx,float wy,float size){
        return pose(g,wx,wy,size,0);
    }
    public static Pose pose(Game g,float wx,float wy,float size,float contactRoll){
        RiverCamera c=g.layout.camera;int tick=g.visualTicks;
        float surface=g.water.height(wx,wy,tick,null),heave=g.water.height(wx,wy,tick-10,null)*1.1f;
        float across=g.water.height(wx+6,wy+6,tick,null)-g.water.height(wx-6,wy-6,tick,null);
        float roll=Physics.clamp(across*.07f+.018f*(float)StrictMath.sin(tick*.032+wx*.017+wy*.009),-MAX_ROLL,MAX_ROLL);
        roll=Physics.clamp(roll+contactRoll,-.26f,.26f);
        return new Pose(c.screenX(wx,wy),c.screenY(wx,wy)-heave*c.scale,size*c.scale,roll,
                c.screenY(wx,wy)-surface*c.scale-size*c.scale*.022f,tick*.045f+wx*.017f+wy*.009f);
    }
    /** Front of the cylindrical float meets an elliptical water meniscus, independent of roll. */
    static float surfaceY(Pose pose,float x){
        float u=Physics.clamp((x-pose.x)/(pose.size*.275f),-1,1);
        float bow=(float)StrictMath.sqrt(Math.max(0,1-u*u));
        return pose.waterY-pose.size*.033f*(1-bow)
                +pose.size*.004f*bow*(float)StrictMath.sin(pose.phase+u*4);
    }
    static float[] submerged(Pose pose){
        float[] triangles=new float[18*12];float bottom=pose.y+pose.size;
        for(int i=0;i<18;i++){
            float left=pose.x+pose.size*(i==0?-1:-.4f+(i-1)*.05f);
            float right=pose.x+pose.size*(i==17?1:-.4f+i*.05f);
            float a=surfaceY(pose,left),b=surfaceY(pose,right);int j=i*12;
            triangles[j]=left;triangles[j+1]=a;triangles[j+2]=right;triangles[j+3]=b;triangles[j+4]=right;triangles[j+5]=bottom;
            triangles[j+6]=left;triangles[j+7]=a;triangles[j+8]=right;triangles[j+9]=bottom;triangles[j+10]=left;triangles[j+11]=bottom;
        }
        return triangles;
    }
    /** Curved surface is deliberately independent of the sprite's roll and delayed heave. */
    static void hull(Painter p,int frame,Pose pose){
        p.pushClipOutside(submerged(pose));
        try{p.spriteAffine(SpriteAtlas.BUOY,frame,pose.x,pose.y,pose.size*pose.cos,pose.size*pose.sin,-pose.size*pose.sin,pose.size*pose.cos,1);}
        finally{p.popClip();}
    }
    /** Quiet wet contact plus irregular sprite foam; no opaque painted water rectangle. */
    static void contact(Painter p,Pose pose){
        for(int i=0;i<12;i++){
            float a=-.26f+i*.52f/12,b=-.26f+(i+1)*.52f/12;
            float x=pose.x+a*pose.size,xx=pose.x+b*pose.size;
            float y=surfaceY(pose,x),yy=surfaceY(pose,xx);
            float taper=(float)StrictMath.sin((i+.5f)*StrictMath.PI/12);
            p.line(x,y,xx,yy,pose.size*.035f,SoftCue.alpha(0xff153d43,.36f*taper));
            p.line(x,y,xx,yy,pose.size*.016f,SoftCue.alpha(0xff91ccc5,.36f*taper));
        }
        for(int patch=0;patch<5;patch++){
            float u=-.22f+patch*.11f,age=pose.phase*.18f+patch*.23f;
            age-=(float)StrictMath.floor(age);
            float envelope=(float)StrictMath.sin(age*StrictMath.PI);
            float x=pose.x+u*pose.size,y=surfaceY(pose,x)+pose.size*.008f;
            float tangent=(surfaceY(pose,x+1)-surfaceY(pose,x-1))*.5f;
            VesselFoam.patch(p,patch%2,x,y,(float)StrictMath.atan(tangent),pose.size*.22f,pose.size*.07f,.24f+.36f*envelope*envelope);
        }
        // Broken front crests spread away from the float; no bright closed target ring.
        for(int ring=0;ring<2;ring++){
            float age=pose.phase*.13f+ring*.5f;age-=(float)StrictMath.floor(age);
            float radius=pose.size*(.28f+age*.14f),height=pose.size*(.045f+age*.07f);
            float opacity=.18f*(float)StrictMath.sin(age*StrictMath.PI);
            for(int i=0;i<8;i++){
                double a=(.05+i*.1125)*StrictMath.PI,b=(.05+(i+.78)*.1125)*StrictMath.PI;
                float x=pose.x+radius*(float)StrictMath.cos(a),y=pose.waterY-pose.size*.033f+height*(float)StrictMath.sin(a);
                float xx=pose.x+radius*(float)StrictMath.cos(b),yy=pose.waterY-pose.size*.033f+height*(float)StrictMath.sin(b);
                float alpha=opacity*(.6f+.4f*(float)StrictMath.sin(i*1.7+pose.phase));
                p.line(x,y,xx,yy,pose.size*.025f,SoftCue.alpha(0xff639f9d,alpha*.3f));
                p.line(x,y,xx,yy,pose.size*.007f,SoftCue.alpha(0xff88c8bd,alpha));
            }
        }
    }
    public static void draw(Game g,Painter p,float wx,float wy,float size,int frame,int color,float power,int number){
        draw(g,p,wx,wy,size,frame,color,power,number,0);
    }
    public static void draw(Game g,Painter p,float wx,float wy,float size,int frame,int color,float power,int number,float contactRoll){
        Pose pose=pose(g,wx,wy,size,contactRoll);RiverCamera c=g.layout.camera;
        p.pushClipOutside(ShoreOcclusion.region(g,c,wx-size,wy-size,wx+size,wy+size,c.screenY(wx,wy)));
        try{
            hull(p,frame,pose);
            p.pushClipOutside(submerged(pose));
            try{SurfaceLighting.affine(g,p,SpriteAtlas.BUOY,frame,wx,wy,pose.x,pose.y,
                    pose.size*pose.cos,pose.size*pose.sin,-pose.size*pose.sin,pose.size*pose.cos,(float)StrictMath.atan2(pose.sin,pose.cos));}
            finally{p.popClip();}
            contact(p,pose);
            float lx=pose.pointX(frame,lampX(frame),lampY(frame)),ly=pose.pointY(frame,lampX(frame),lampY(frame));
            SceneryEffects.glow(p,lx,ly,pose.size*.15f,color,power,g.visualTicks,wx*.013f);
            if(power>0){p.circle(lx,ly,pose.size*.032f,SoftCue.alpha(color,.75f*power));p.circle(lx,ly,pose.size*.012f,SoftCue.alpha(0xfffff5dc,power));}
            if(number>0)Lettering.center(p,""+number,pose.pointX(frame,.5f,.783f),pose.pointY(frame,.5f,.783f)-pose.size*.0335f,pose.size*.067f,0xff263035);
        }finally{p.popClip();}
    }
    /** Independent contact/cast alpha, flattened onto water before the depth-sorted hull pass. */
    public static void shadows(Game g,Painter p,float wx,float wy,float size,int frame){
        shadows(g,p,wx,wy,size,frame,0);
    }
    public static void shadows(Game g,Painter p,float wx,float wy,float size,int frame,float contactRoll){
        RiverCamera c=g.layout.camera;Pose pose=pose(g,wx,wy,size,contactRoll);float[] light=new float[3];ShadowView.light(g,wx,wy,light);
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
