package com.bargeboss.game;

/** Tug-relative searchlight; bounded water mesh and deterministic sight tests. */
public final class TugSpotlight {
    public static final float RANGE=480,HALF_ANGLE=.28f;
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
    private static int color(float range,int edge){float a=(1-range)*(edge==0 || edge==4?0:edge==2?.27f:.14f);return ((int)(255*a)<<24)|0xffedb1;}
    void water(Game g,Painter canvas){
        if(!on)return;RiverCamera c=g.layout.camera;Painter p=new RiverPainter(canvas,c);
        // Four softly feathered wedges, ten distance bands: no stamps, full-screen masks or blur passes.
        for(int edge=0;edge<4;edge++){
            double a=angle(g)+(edge-2)*HALF_ANGLE/2,b=a+HALF_ANGLE/2;
            float ax=(float)StrictMath.cos(a),ay=(float)StrictMath.sin(a),bx=(float)StrictMath.cos(b),by=(float)StrictMath.sin(b);
            for(int band=0;band<10;band++){
                float r=band/10f,rr=(band+1)/10f;
                float x=g.tug.x+ax*r*RANGE,y=g.tug.y+ay*r*RANGE,xx=g.tug.x+bx*r*RANGE,yy=g.tug.y+by*r*RANGE;
                float ex=g.tug.x+ax*rr*RANGE,ey=g.tug.y+ay*rr*RANGE,fx=g.tug.x+bx*rr*RANGE,fy=g.tug.y+by*rr*RANGE;
                if(!g.inWater(ex,ey) || !g.inWater(fx,fy))break;
                p.shadedTriangle(x,y,color(r,edge),ex,ey,color(rr,edge),fx,fy,color(rr,edge+1));
                p.shadedTriangle(x,y,color(r,edge),fx,fy,color(rr,edge+1),xx,yy,color(r,edge+1));
            }
        }
    }
    void lamp(Game g,Painter world){
        if(!on || !(world instanceof RiverPainter))return;RiverPainter r=(RiverPainter)world;RiverCamera c=r.camera;
        float size=2*(g.tug.half+g.tug.radius)/TugArt.HULL_FRACTION;
        float x=c.screenX(g.tug.x,g.tug.y)+TugLightArt.offset(0,g.tug.angle,true,false)*size*c.scale;
        float y=c.screenY(g.tug.x,g.tug.y)+(TugLightArt.offset(0,g.tug.angle,true,true)*size-WaterSurface.lift(g,g.tug))*c.scale;
        SoftCue.dot(r.canvas(),x,y,3*c.scale,0xddfff3c8);
    }
}
