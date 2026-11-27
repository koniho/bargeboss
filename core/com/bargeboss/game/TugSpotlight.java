package com.bargeboss.game;

/** Tug-relative searchlight; bounded water mesh and deterministic sight tests. */
public final class TugSpotlight {
    public static final float RANGE=320,HALF_ANGLE=.28f;
    public static final float HEIGHT=32,POWER=2.4f;
    static final int FIXTURE=99,COLOR=0xffffedc4;
    public boolean on;
    /** Soft angular penumbra shared by every receiver, independent of camera/visibility. */
    static float sector(float angle,float dx,float dy){
        float edge=HALF_ANGLE-Math.abs(Physics.angle((float)StrictMath.atan2(dy,dx)-angle));
        float t=Physics.clamp(edge/.10f,0,1);return t*t*(3-2*t);
    }
    float cone(Game g,float x,float y){
        if(!on)return 0;float dx=x-g.tug.x,dy=y-g.tug.y;
        if(dx*dx+dy*dy>=RANGE*RANGE)return 0;
        return sector(angle(g),dx,dy);
    }
    float energy(Game g,float x,float y,float z){
        float dz=z-HEIGHT,dx=x-g.tug.x,dy=y-g.tug.y;
        return POWER*cone(g,x,y)*VesselShoreLight.falloff(dx*dx+dy*dy+dz*dz,RANGE);
    }
    float face(Game g,float x,float y,float z,float nx,float ny,float nz){
        float e=energy(g,x,y,z);if(e==0)return 0;
        float dx=g.tug.x-x,dy=g.tug.y-y,dz=HEIGHT-z;
        return e*Math.max(0,(nx*dx+ny*dy+nz*dz)/(float)StrictMath.sqrt(Math.max(.001f,dx*dx+dy*dy+dz*dz)));
    }
    public float relativeAngle;
    void reset(){on=false;relativeAngle=0;}
    float angle(Game g){return g.tug.angle+relativeAngle;}
    void aim(Game g,float worldAngle){relativeAngle=Physics.angle(worldAngle-g.tug.angle);on=true;}
    boolean illuminates(Game g,float x,float y){
        if(!on)return false;float dx=x-g.tug.x,dy=y-g.tug.y;
        return dx*dx+dy*dy<RANGE*RANGE && Math.abs(Physics.angle((float)StrictMath.atan2(dy,dx)-angle(g)))<HALF_ANGLE
                && Towline.clear(g,g.tug.x,g.tug.y,x,y);
    }
    private float lampOffset(Game g,boolean vertical){
        float size=2*(g.tug.half+g.tug.radius)/TugArt.HULL_FRACTION;
        return TugLightArt.offset(0,g.tug.angle,true,vertical)*size-(vertical?WaterSurface.lift(g,g.tug):0);
    }
    void water(Game g,Painter canvas){
        if(!on)return;float scale=g.layout.camera.scale;
        SpotlightBeam.draw(g,canvas,g.tug.x,g.tug.y,angle(g),RANGE,HALF_ANGLE,
                lampOffset(g,false)*scale,lampOffset(g,true)*scale,0xffffedb1);
    }
    void lamp(Game g,Painter world){
        if(!on || !(world instanceof RiverPainter))return;RiverPainter r=(RiverPainter)world;RiverCamera c=r.camera;
        float size=2*(g.tug.half+g.tug.radius)/TugArt.HULL_FRACTION;
        float x=c.screenX(g.tug.x,g.tug.y)+TugLightArt.offset(0,g.tug.angle,true,false)*size*c.scale;
        float y=c.screenY(g.tug.x,g.tug.y)+(TugLightArt.offset(0,g.tug.angle,true,true)*size-WaterSurface.lift(g,g.tug))*c.scale;
        SoftCue.dot(r.canvas(),x,y,3*c.scale,0xddfff3c8);
    }
}
