package com.bargeboss.game;

/** Shared water-effect placement; native shaders only shade these small clipped patches. */
public final class WaterFoam {
    private WaterFoam() {}
    public static float phase(int tick,int seed) {
        return Math.floorMod(tick+seed*31,240)*(float)(StrictMath.PI*2/240);
    }
    /** Strong leading pressure crest, derived from actual forward speed, not throttle. */
    public static void bow(Game g,Painter p,Body b) {
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        float forward=b.vx*c+b.vy*s,motion=Physics.clamp((forward-3)/55,0,1);
        if(motion<=0)return;
        float nose=b.half+b.radius*.86f,spread=b.radius*(1.3f+motion*.35f),trail=12+motion*18;
        for(int side=-1;side<=1;side+=2) {
            float ax=-c*trail-s*side*spread,ay=-s*trail+c*side*spread;
            float length=Physics.length(ax,ay),width=5+motion*7;
            float x=b.x+c*nose+ax*.48f,y=b.y+s*nose+ay*.48f;
            p.waterFoam(x,y,ax,ay,-ay/length*width,ax/length*width,phase(g.visualTicks,b.id)+side,4,.22f+motion*.62f);
        }
    }
    /** Bounded textured/geometry fallback; preserves placement, speed response and clipping. */
    public static void fallback(Painter p,float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha) {
        float width=Physics.length(bx,by);
        int segments=kind==5?1:8; // Crests already arrive as short arc segments.
        for(int i=0;i<segments;i++) {
            float u=i/(float)segments,v=(i+1)/(float)segments,mid=(u+v)*.5f;
            float a=crest(u,phase,kind),b=crest(v,phase,kind);
            float fade=(float)StrictMath.sin(mid*StrictMath.PI);
            int opacity=(int)(255*alpha*fade*(.55f+.25f*(float)StrictMath.sin(mid*23+phase)));
            p.line(x+ax*(u-.5f)+bx*a,y+ay*(u-.5f)+by*a,x+ax*(v-.5f)+bx*b,y+ay*(v-.5f)+by*b,
                    width*.16f,(Math.min(255,opacity)<<24)|0xc1eee4);
        }
    }
    private static float crest(float u,float phase,int kind) {
        float x=u*2-1;
        return (kind==4?.15f*x*x-.06f:0)+.045f*(float)StrictMath.sin(x*7+phase);
    }
}
