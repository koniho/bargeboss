package com.bargeboss.game;

/** Animated hull/water contact, registered to the painted lower silhouette in every view. */
public final class VesselWaterline {
    private static final int SEGMENTS=24;
    private static final float[][][] EDGE=edges();
    private VesselWaterline() {}
    private static float[][][] edges() {
        float[][][] result=new float[3][32][(SEGMENTS+1)*2];
        for(int slot=0;slot<3;slot++)for(int frame=0;frame<32;frame++) {
            int atlas=slot==2?SpriteAtlas.YACHT:slot;
            long[] rows=OcclusionMasks.rows(atlas)[frame];int[] bottom=new int[64];int first=64,last=-1;
            for(int x=0;x<64;x++) {
                bottom[x]=-1;
                for(int y=63;y>=0;y--)if((rows[y]&(1L<<x))!=0){bottom[x]=y;break;}
                // Ignore isolated fittings above the hull's registered waterline region.
                if(bottom[x]/64f>=SpriteAtlas.pivotY(atlas,frame)-.12f){first=Math.min(first,x);last=x;}
            }
            for(int i=0;i<=SEGMENTS;i++) {
                float column=first+(last-first)*i/(float)SEGMENTS;int center=Math.round(column);
                float sum=0,weight=0;
                for(int x=Math.max(first,center-1);x<=Math.min(last,center+1);x++)if(bottom[x]>=0) {
                    float w=x==center?2:1;sum+=(bottom[x]+1)*w;weight+=w;
                }
                result[slot][frame][2*i]=((column+.5f)/64-SpriteAtlas.pivotX(atlas,frame))*SpriteAtlas.widthScale(atlas,frame);
                result[slot][frame][2*i+1]=(sum/Math.max(1,weight)/64-SpriteAtlas.pivotY(atlas,frame))*SpriteAtlas.heightScale(atlas,frame);
            }
        }
        return result;
    }
    /** Screen-space water intersection. Its height is independent of hull heave. */
    static float[] boundary(Game g,Painter p,Body b,boolean tug,float size) {
        RiverCamera c=p instanceof RiverPainter?((RiverPainter)p).camera:null;
        float scale=c==null?1:c.scale,x=c==null?b.x:c.screenX(b.x,b.y),y=c==null?b.y:c.screenY(b.x,b.y);
        float heading=b.angle/(float)(StrictMath.PI/16),whole=(float)StrictMath.floor(heading);
        int a=(((int)whole+(c==null?0:RiverCamera.DIRECTION_OFFSET))%32+32)%32,bb=(a+1)%32;
        float mix=Physics.clamp((heading-whole-.4f)*5,0,1);mix=mix*mix*(3-2*mix);
        float[] from=EDGE[b.yacht?2:tug?0:1][a],to=EDGE[b.yacht?2:tug?0:1][bb];
        float surface=g.water.height(b.x,b.y,g.visualTicks,b)*.35f;
        float[] edge=new float[(SEGMENTS+1)*2];
        for(int i=0;i<=SEGMENTS;i++) {
            float wave=.4f*(float)StrictMath.sin(g.visualTicks*.048+b.id*.7+i*.38);
            edge[2*i]=x+offset(from,to,i,false,0,mix)*size*scale;
            edge[2*i+1]=y+(offset(from,to,i,true,0,mix)*size-2.2f-surface-wave)*scale;
        }
        return edge;
    }
    /** Disjoint trapezoids hide every hull/lit/glow pixel below the same contact curve. */
    static float[] submerged(float[] edge,float extent) {
        float bottom=-Float.MAX_VALUE;for(int i=1;i<edge.length;i+=2)bottom=Math.max(bottom,edge[i]+extent);
        float[] mask=new float[(SEGMENTS+2)*12];
        for(int i=0;i<SEGMENTS+2;i++) {
            int a=Math.max(0,i-1)*2,b=Math.min(SEGMENTS,i)*2;
            float x=edge[a]-(i==0?extent:0),xx=edge[b]+(i==SEGMENTS+1?extent:0),y=edge[a+1],yy=edge[b+1];
            int j=i*12;mask[j]=x;mask[j+1]=y;mask[j+2]=xx;mask[j+3]=yy;mask[j+4]=xx;mask[j+5]=bottom;
            mask[j+6]=x;mask[j+7]=y;mask[j+8]=xx;mask[j+9]=bottom;mask[j+10]=x;mask[j+11]=bottom;
        }
        return mask;
    }
    static void contact(Painter canvas,float[] edge,float scale) {
        for(int i=0;i<SEGMENTS;i++) {
            int j=i*2;
            canvas.line(edge[j],edge[j+1],edge[j+2],edge[j+3],.85f*scale,0x786eaaa9);
        }
    }
    public static void draw(Game g,Painter p,Body b,boolean tug,float size,boolean contact) {
        if(!contact)return;
        RiverPainter river=p instanceof RiverPainter?(RiverPainter)p:null;
        Painter canvas=river==null?p:river.canvas();
        float[] clip=river==null?ShoreOcclusion.EMPTY:ShoreOcclusion.vessel(g,river.camera,b,true);
        if(clip.length>0)canvas.pushClipOutside(clip);
        try{contact(canvas,boundary(g,p,b,tug,size),river==null?1:river.camera.scale);}
        finally{if(clip.length>0)canvas.popClip();}
    }
    private static float offset(float[] a,float[] b,int i,boolean vertical,float t,float blend) {
        int j=i*2+(vertical?1:0),k=Math.min(i+1,SEGMENTS)*2+(vertical?1:0);
        float x=a[j]+(b[j]-a[j])*blend,y=a[k]+(b[k]-a[k])*blend;
        return x+(y-x)*t;
    }
}
