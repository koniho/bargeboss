package com.bargeboss.game;

/** Bounded local materials. No scene reads, time source, gameplay or native decisions. */
public final class EffectMaterial {
    public static final int GLOW=0,DOT=1,RING=2;
    private EffectMaterial(){}
    public static void fallback(Painter p,float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color) {
        if((color>>>24)==0 || amount<=0)return;
        if(kind==RING) {
            int n=64;float ringRadius=.49f-detail*.5f;
            for(int i=0;i<n*amount;i++) {
                double a=-StrictMath.PI/2+i*StrictMath.PI*2/n,b=-StrictMath.PI/2+Math.min(i+1,n*amount)*StrictMath.PI*2/n;
                float u=(float)StrictMath.cos(a)*ringRadius,v=(float)StrictMath.sin(a)*ringRadius;
                float uu=(float)StrictMath.cos(b)*ringRadius,vv=(float)StrictMath.sin(b)*ringRadius;
                p.line(x+ax*u+bx*v,y+ay*u+by*v,x+ax*uu+bx*vv,y+ay*uu+by*vv,
                    detail*(Physics.length(ax,ay)+Physics.length(bx,by))*.5f,color);
            }
        } else {
            // Fixed concentric contours approximate the shader falloff on reference hosts.
            for(int shell=16;shell>0;shell--) {
                float radius=shell/32f;
                float coverage=kind==DOT?.045f: .018f+(1-shell/16f)*.07f;
                float[] q=new float[48];
                for(int i=0;i<24;i++){double a=i*StrictMath.PI/12;float u=(float)StrictMath.cos(a)*radius,v=(float)StrictMath.sin(a)*radius;q[i*2]=x+ax*u+bx*v;q[i*2+1]=y+ay*u+by*v;}
                p.poly(q,SoftCue.alpha(color,coverage*amount));
            }
        }
    }
}
