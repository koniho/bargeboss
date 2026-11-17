package com.bargeboss.game;

/** Expanding painted rings in the water plane, always beneath the hull. */
public final class VesselRipples {
    private VesselRipples() {}
    public static void draw(Game g,Painter p,int seed,float x,float y,float angle,float length,float width,float speed,boolean moored){
        float c=(float)StrictMath.cos(angle),s=(float)StrictMath.sin(angle);
        float motion=Physics.clamp(speed/60,0,1),spread=moored?5:9+motion*9;
        for(int i=0;i<2;i++){
            float age=Math.floorMod(g.visualTicks+seed*37+i*90,180)/180f;
            float fade=(float)StrictMath.sin(age*StrictMath.PI);
            float alpha=fade*fade*(moored?.09f:.16f+motion*.18f);
            if(alpha<.003f)continue;
            // The transparent center starts inside the footprint and expands outwards.
            float a=length*1.10f+age*spread,b=width*1.18f+age*spread;
            p.spriteAffine(SpriteAtlas.RIPPLES,Math.floorMod(seed+i,4),x,y,c*a,s*a,-s*b,c*b,alpha);
        }
    }
}
