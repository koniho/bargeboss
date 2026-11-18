package com.bargeboss.game;

/** Cyclic hull displacement: calm rings blend to fluffy foam above hull speed. */
public final class VesselRipples {
    private VesselRipples() {}
    public static void draw(Game g,Painter p,int seed,float x,float y,float angle,float length,float width,float speed,boolean moored){
        draw(g,p,seed,x,y,angle,length,width,speed,moored,1);
    }
    static void draw(Game g,Painter p,int seed,float x,float y,float angle,float length,float width,float speed,boolean moored,float opacity){
        float c=(float)StrictMath.cos(angle),s=(float)StrictMath.sin(angle);
        float motion=Physics.clamp(speed/60,0,1),spread=moored?5:9+motion*9;
        float foam=moored?0:Physics.clamp((speed-WaterFoam.SHED_SPEED)/10,0,1);
        float moving=moored?0:Physics.clamp((speed-1)/7,0,1);
        float cruise=moored?0:Physics.clamp((speed-WaterFoam.SHED_SPEED*.4f)/(WaterFoam.SHED_SPEED*.6f),0,1);
        for(int i=0;i<2;i++){
            float age=Math.floorMod(g.visualTicks+seed*37+i*90,180)/180f;
            float fade=(float)StrictMath.sin(age*StrictMath.PI);
            float alpha=fade*fade*(moored?.09f:.16f+motion*.18f);
            alpha*=opacity;
            if(!moored && i==1)alpha*=SceneDetail.fade(length*g.layout.camera.scale,36,80);
            if(alpha<.003f)continue;
            // The transparent center starts inside the footprint and expands outwards.
            float a=length*(1.10f+.30f*cruise)+age*spread*(1+cruise);
            float b=width*(1.18f-.12f*cruise)+age*spread*(1-.3f*cruise);
            if(foam<1){
                float aft=moving*(length*(.06f+.12f*cruise)+age*speed*.12f);
                float xx=x-c*aft,yy=y-s*aft;
                boolean clipped=moving>0;
                if(clipped){
                    // Cut the bow-facing portion of the ellipse in local hull space.
                    // Slow motion leaves broad arcs; cruise leaves narrower stern arcs.
                    float cut=length*(.65f-.65f*moving-.12f*cruise),reach=2*(a+b);
                    float ax=xx+c*cut-s*reach,ay=yy+s*cut+c*reach;
                    float bx=xx+c*reach-s*reach,by=yy+s*reach+c*reach;
                    float cx=xx+c*reach+s*reach,cy=yy+s*reach-c*reach;
                    float dx=xx+c*cut+s*reach,dy=yy+s*cut-c*reach;
                    p.pushClipOutside(new float[]{ax,ay,bx,by,dx,dy,bx,by,cx,cy,dx,dy});
                }
                try{p.spriteAffine(SpriteAtlas.RIPPLES,Math.floorMod(seed+i,4),xx,yy,c*a,s*a,-s*b,c*b,alpha*(1-foam));}
                finally{if(clipped)p.popClip();}
            }
            if(foam>0){
                // Broad, porous foam islands billow beyond the hull footprint instead
                // of drawing a clean concentric line. Keep two staggered soft cycles.
                float fluffyLength=length*1.5f+age*spread,fluffyWidth=width*1.8f+age*spread;
                p.spriteAffine(SpriteAtlas.FOAM,2+Math.floorMod(seed+i,2),x,y,
                    c*fluffyLength,s*fluffyLength,-s*fluffyWidth,c*fluffyWidth,alpha*foam);
            }
        }
    }
}
