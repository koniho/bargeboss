package com.bargeboss.game;
/** One simulation-timed fire emitter, shared by glow, water and real timber faces. */
final class FireLight implements ArticulatedModel.FaceLighting {
    private final int tint;private final float power;
    FireLight(Game g){tint=color(g);power=energy(g);}
    static float energy(Game g){
        if(g.tutorial==null || g.tutorial.fireworks.heat<=0)return 0;
        float flame=.77f+.14f*(float)StrictMath.sin(g.visualTicks*.19)+.09f*(float)StrictMath.sin(g.visualTicks*.071+1.3);
        return flame*(.25f+.75f*g.tutorial.fireworks.heat/FireworksLesson.HEAT);
    }
    static int color(Game g){
        float t=.5f+.32f*(float)StrictMath.sin(g.visualTicks*.083)+.18f*(float)StrictMath.sin(g.visualTicks*.031+1.7);
        int a=t<.6f?0xffff5217:0xffffb52e,b=t<.6f?0xffffb52e:0xffffed9a;
        float f=t<.6f?t/.6f:(t-.6f)/.4f;
        int green=Math.round((a>>8&255)+( (b>>8&255)-(a>>8&255))*f),blue=Math.round((a&255)+((b&255)-(a&255))*f);
        return 0xffff0000|green<<8|blue;
    }
    static void sample(Game g,float[] weights,float x,float y){float e=energy(g);if(e>0)SurfaceLighting.add(weights,x,y,FireworksLesson.X,FireworksLesson.Y,GroundArt.LAND_RISE+48,210,1.1f*e);}
    static void reflection(Game g,Painter world){float e=energy(g);if(e>0)WaterReflection.draw(g,world,FireworksLesson.X,FireworksLesson.Y,65,135,SoftCue.alpha(color(g),e*.65f),417);}
    public int shade(int ambient,int albedo,float x,float y,float z,float nx,float ny,float nz){
        float dx=FireworksLesson.X-x,dy=FireworksLesson.Y-y,dz=GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE+52-z;
        float d2=dx*dx+dy*dy+dz*dz,lambert=Math.max(0,(nx*dx+ny*dy+nz*dz)/(float)StrictMath.sqrt(Math.max(1,d2)));
        float e=power*1.7f*VesselShoreLight.falloff(d2,210)*lambert;
        int r=Math.min(255,(ambient>>16&255)+Math.round((albedo>>16&255)*(tint>>16&255)/255f*e));
        int gr=Math.min(255,(ambient>>8&255)+Math.round((albedo>>8&255)*(tint>>8&255)/255f*e));
        int b=Math.min(255,(ambient&255)+Math.round((albedo&255)*(tint&255)/255f*e));
        return ambient&0xff000000|r<<16|gr<<8|b;
    }
}
