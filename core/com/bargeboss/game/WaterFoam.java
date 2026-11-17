package com.bargeboss.game;

/** Shared water-effect placement; native shaders only shade these small clipped patches. */
public final class WaterFoam {
    private WaterFoam() {}
    static final float SHED_SPEED=32.5f; // Half of the existing 65-unit foam speed reference.
    static float pressure(float forward){return 1-Physics.clamp((forward-SHED_SPEED*.8f)/(SHED_SPEED*.2f),0,1);}
    public static float phase(int tick,int seed) {
        return Math.floorMod(tick+seed*31,240)*(float)(StrictMath.PI*2/240);
    }
    /** Strong leading pressure crest, derived from actual forward speed, not throttle. */
    public static void bow(Game g,Painter p,Body b) {
        bow(g,p,b,0);
    }
    /** Title contact crest split at unraised water-plane depth, never at the cabin. */
    public static void bowLayer(Game g,Painter p,Body b,boolean foreground) {bow(g,p,b,foreground?1:-1);}
    private static void bow(Game g,Painter p,Body b,int layer) {
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        float forward=b.vx*c+b.vy*s,motion=Physics.clamp((forward-3)/40,0,1);
        float pressure=pressure(forward);
        if(motion<=0 || pressure<=0)return;
        float nose=b.half+b.radius*.86f,spread=b.radius*(1.5f+motion*.65f),trail=18+motion*26;
        for(int side=-1;side<=1;side+=2) {
            float ax=-c*trail-s*side*spread,ay=-s*trail+c*side*spread;
            float length=Physics.length(ax,ay),width=12+motion*18;
            float x=b.x+c*nose+ax*.48f,y=b.y+s*nose+ay*.48f;
            float bx=-ay/length*width,by=ax/length*width;
            Painter canvas=p;float[] clip=null;
            if(layer!=0 && p instanceof RiverPainter) {
                RiverPainter river=(RiverPainter)p;RiverCamera camera=river.camera;canvas=river.canvas();
                float depth=camera.screenY(b.x,b.y),left=Float.MAX_VALUE,top=left,right=-left,bottom=-left;
                for(int corner=0;corner<4;corner++) {
                    float u=(corner%2)-.5f,v=(corner/2)-.5f;
                    float xx=camera.screenX(x+ax*u+bx*v,y+ay*u+by*v),yy=camera.screenY(x+ax*u+bx*v,y+ay*u+by*v);
                    left=Math.min(left,xx);right=Math.max(right,xx);top=Math.min(top,yy);bottom=Math.max(bottom,yy);
                }
                if(layer>0?bottom<=depth:top>=depth)continue;
                if(layer>0?top<depth:bottom>depth) {
                    left-=1;right+=1;
                    float a=layer>0?top-1:depth,z=layer>0?depth:bottom+1;
                    clip=new float[]{left,a,right,a,left,z,right,a,right,z,left,z};
                    canvas.pushClipOutside(clip);
                }
            }
            try {p.waterFoam(x,y,ax,ay,bx,by,phase(g.visualTicks,b.id)+side,4,(.38f+motion*.60f)*pressure);}
            finally {if(clip!=null)canvas.popClip();}
        }
    }
    /** Bounded textured/geometry fallback; preserves placement, speed response and clipping. */
    public static void fallback(Painter p,float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha) {
        float width=Physics.length(bx,by);
        if(kind>=8){
            if(kind==9){for(int k=0;k<14;k++){float a=k*(float)Math.PI/7,b=a+.13f;float u=(float)StrictMath.cos(a)*.3f,v=(float)StrictMath.sin(a)*.3f,uu=(float)StrictMath.cos(b)*.3f,vv=(float)StrictMath.sin(b)*.3f;p.line(x+ax*u+bx*v,y+ay*u+by*v,x+ax*uu+bx*vv,y+ay*uu+by*vv,width*.055f,(Math.round(alpha*200)<<24)|0xe5fcf7);}}
            else for(int k=-1;k<=1;k++){float side=k*.10f;p.poly(new float[]{x-ax*.4f+bx*(side-.035f),y-ay*.4f+by*(side-.035f),x+ax*.3f+bx*side*.25f,y+ay*.3f+by*side*.25f,x-ax*.4f+bx*(side+.035f),y-ay*.4f+by*(side+.035f)},(Math.round(alpha*220)<<24)|0xe5fcf7);}
            return;
        }
        int segments=kind==5?1:8; // Crests already arrive as short arc segments.
        for(int i=0;i<segments;i++) {
            float u=i/(float)segments,v=(i+1)/(float)segments,mid=(u+v)*.5f;
            float a=crest(u,phase,kind),b=crest(v,phase,kind);
            float fade=(float)StrictMath.sin(mid*StrictMath.PI);
            int opacity=(int)(255*alpha*fade*(.55f+.25f*(float)StrictMath.sin(mid*23+phase)));
            p.line(x+ax*(u-.5f)+bx*a,y+ay*(u-.5f)+by*a,x+ax*(v-.5f)+bx*b,y+ay*(v-.5f)+by*b,
                    width*.23f,(Math.min(255,opacity)<<24)|0xe5fcf7);
        }
    }
    private static float crest(float u,float phase,int kind) {
        float x=u*2-1;
        return (kind==4?.15f*x*x-.06f:0)+.045f*(float)StrictMath.sin(x*7+phase);
    }
}
