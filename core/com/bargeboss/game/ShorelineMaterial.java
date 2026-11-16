package com.bargeboss.game;

/** Bounded reference rendering for hosts without the shared shoreline shader. */
public final class ShorelineMaterial {
    private ShorelineMaterial(){}
    static float crest(float u,float phase,float energy){return 1.9f+.65f*(float)StrictMath.sin(u*.105f-phase*1.17f)+.35f*(float)StrictMath.sin(u*.237f+phase*.79f)+Math.max(0,Math.min(1,energy))*1.1f;}
    private static float recent(float u,float phase,float energy){return Math.max(crest(u,phase,energy),Math.max(crest(u,phase-.8f,energy)-.18f,crest(u,phase-1.6f,energy)-.42f));}
    public static void fallback(Painter p,float[] xy,float[] uv,float phase,float energy,int kind,int color){
        if(xy.length<6 || xy.length!=uv.length || (color>>>24)==0)return;
        int third=4;float du=uv[2]-uv[0],dv=uv[3]-uv[1];
        while(third<uv.length-2 && Math.abs(du*(uv[third+1]-uv[1])-dv*(uv[third]-uv[0]))<.0001f)third+=2;
        float eu=uv[third]-uv[0],ev=uv[third+1]-uv[1],det=du*ev-dv*eu;
        if(Math.abs(det)<.0001f)return;
        float ax=((xy[2]-xy[0])*ev-(xy[third]-xy[0])*dv)/det,bx=((xy[third]-xy[0])*du-(xy[2]-xy[0])*eu)/det;
        float ay=((xy[3]-xy[1])*ev-(xy[third+1]-xy[1])*dv)/det,by=((xy[third+1]-xy[1])*du-(xy[3]-xy[1])*eu)/det;
        float minU=uv[0],maxU=uv[0],minV=uv[1],maxV=uv[1];
        for(int i=2;i<uv.length;i+=2){minU=Math.min(minU,uv[i]);maxU=Math.max(maxU,uv[i]);minV=Math.min(minV,uv[i+1]);maxV=Math.max(maxV,uv[i+1]);}
        for(int i=0;i<8;i++){
            float u=minU+(maxU-minU)*i/8,v=minU+(maxU-minU)*(i+1)/8,a=crest(u,phase,energy),b=crest(v,phase,energy);
            if(kind==1){a=recent(u,phase,energy);b=recent(v,phase,energy);}
            float[] q={u,kind==1?minV:Math.max(minV,a-.55f),v,kind==1?minV:Math.max(minV,b-.55f),v,Math.min(maxV,b+.55f),u,Math.min(maxV,a+.55f)};
            float alpha=kind==1?.48f:.32f+.30f*(.5f+.5f*(float)StrictMath.sin((u+v)*.215f+phase*.51f));
            for(int j=0;j<8;j+=2){float uu=q[j]-uv[0],vv=q[j+1]-uv[1];q[j]=xy[0]+ax*uu+bx*vv;q[j+1]=xy[1]+ay*uu+by*vv;}
            q=PolygonWater.clip(q,xy);if(q.length>=6)p.poly(q,((int)((color>>>24)*alpha)<<24)|(color&0xffffff));
        }
    }
}
