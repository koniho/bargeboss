package com.bargeboss.game;

/** Analytic water shared by hulls, floats and crests. No host state or runtime textures. */
public final class SeaWaves {
    private SeaWaves(){}
    private static final float[] DX={0,.35f},DY={-1,-.9367497f};
    private static final float[] K={(float)(2*StrictMath.PI/220),(float)(2*StrictMath.PI/95)};
    private static final float[] OMEGA={(float)(2*StrictMath.PI/3.4),(float)(2*StrictMath.PI/2.1)};
    private static final float[] AMPLITUDE={2.4f,.8f};
    static float smooth(float a,float b,float x){float t=Physics.clamp((x-a)/(b-a),0,1);return t*t*(3-2*t);}
    /** Authored exposure, not a bathymetric/refraction simulation. Narrow entrance admits chop. */
    public static float exposure(float x,float y){
        float mx=MontroseHarbor.planX(x),my=MontroseHarbor.planY(y);
        float lake=Math.max(smooth(1100,1420,my),smooth(1650,2050,mx));
        float entrance=smooth(700,1250,my)*(1-smooth(180,450,Math.abs(mx-350)));
        return .12f+.88f*Math.max(lake,entrance);
    }
    static float value(Game g,float x,float y,float tick,int component){
        float sum=0;
        for(int i=0;i<2;i++){
            float dx,dy,k,omega,amplitude;
            if(g.montroseStage()){dx=DX[i];dy=DY[i];k=K[i];omega=OMEGA[i];amplitude=AMPLITUDE[i]*exposure(x,y);}
            else {
                float kx=i==0?.017f:-.013f,ky=i==0?.009f:.021f;
                k=Physics.length(kx,ky);dx=-kx/k;dy=-ky/k;omega=(i==0?.048f:.031f)/Physics.DT;amplitude=i==0?.65f:.22f;
            }
            float h=amplitude*(float)StrictMath.sin(k*(dx*x+dy*y)-omega*tick*Physics.DT);
            if(!g.montroseStage())h=-h;
            sum+=component==0?h:h*omega*(component==1?dx:dy);
        }
        return sum;
    }
    /** View-bounded crest strips use exactly the phase used by value(). Shared shore clip is active. */
    static void draw(Game g,Painter p){
        if(!g.montroseStage() || !(p instanceof RiverPainter))return;
        RiverCamera c=((RiverPainter)p).camera;
        int budget=512;
        for(int wave=0;wave<2;wave++){
            float dx=DX[wave],dy=DY[wave],lo=Float.MAX_VALUE,hi=-lo,left=lo,right=-lo;
            for(int corner=0;corner<4;corner++){
                float sx=corner%2*g.layout.width,sy=corner/2*g.layout.height;
                float x=c.worldX(sx,sy),y=c.worldY(sx,sy),u=x*dx+y*dy,v=-x*dy+y*dx;
                lo=Math.min(lo,u);hi=Math.max(hi,u);left=Math.min(left,v);right=Math.max(right,v);
            }
            float phase=OMEGA[wave]*g.visualTicks*Physics.DT,period=(float)(2*StrictMath.PI);
            int first=(int)StrictMath.floor((lo*K[wave]-phase-period*.25f)/period)-1;
            int last=(int)StrictMath.ceil((hi*K[wave]-phase-period*.25f)/period)+1;
            float segment=Math.max(40,32/c.scale);
            for(int crest=first;crest<=last;crest++){
                float u=(phase+period*.25f+crest*period)/K[wave];
                if(u<lo-12 || u>hi+12)continue;
                for(float v=(float)StrictMath.floor(left/segment)*segment;v<=right;v+=segment){
                    if(--budget<0)return;
                    float x=dx*u-dy*(v+segment*.5f),y=dy*u+dx*(v+segment*.5f);
                    if(!MontroseHarbor.contains(x,y))continue;
                    float e=exposure(x,y),broken=.65f+.35f*(float)StrictMath.sin(v*.031+crest*1.7);
                    p.waterFoam(x,y,-dy*segment,dx*segment,dx*7,dy*7,
                            WaterFoam.phase(g.visualTicks,crest),5,e*broken*(wave==0?.14f:.06f));
                }
            }
        }
    }
}
