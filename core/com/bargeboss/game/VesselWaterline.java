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
    /** Sparse contact filaments; detached foam lives independently in WaterSurface.foam. */
    public static void draw(Game g,Painter p,Body b,boolean tug,float size,boolean contact) {
        if(!contact)return;
        boolean river=p instanceof RiverPainter;RiverCamera c=river?((RiverPainter)p).camera:null;
        Painter canvas=river?((RiverPainter)p).canvas():p;
        float scale=river?c.scale:1,x=river?c.screenX(b.x,b.y):b.x,y=river?c.screenY(b.x,b.y):b.y;
        float[] clip=river?ShoreOcclusion.vessel(g,c,b,true):ShoreOcclusion.EMPTY;
        if(clip.length>0)canvas.pushClipOutside(clip);
        try {
            float heading=b.angle/(float)(StrictMath.PI/16),whole=(float)StrictMath.floor(heading);
            int a=(((int)whole+(river?RiverCamera.DIRECTION_OFFSET:0))%32+32)%32,bb=(a+1)%32;
            float mix=Physics.clamp((heading-whole-.4f)*5,0,1);mix=mix*mix*(3-2*mix);
            float[] from=EDGE[b.yacht?2:tug?0:1][a],to=EDGE[b.yacht?2:tug?0:1][bb];
            float speed=b.speed(),motion=Math.min(1,speed/65),bob=WaterSurface.lift(g,b);
            float travelX=river?RiverCamera.Q*(b.vx+b.vy):b.vx;
            // Patches travel toward the trailing end; their envelopes leave clear gaps between crests.
            for(int patch=0;patch<7;patch++) {
                float age=Math.floorMod(g.visualTicks+patch*13+b.id*7,78)/78f;
                float u=(patch+.5f+(travelX<0?age-.5f:.5f-age)*.65f)/7;
                float index=u*SEGMENTS;int i=Math.min(SEGMENTS-1,(int)index);float t=index-i;
                float ex=offset(from,to,i,false,t,mix),ey=offset(from,to,i,true,t,mix);
                float tx=offset(from,to,i+1,false,0,mix)-offset(from,to,i,false,0,mix);
                float ty=offset(from,to,i+1,true,0,mix)-offset(from,to,i,true,0,mix);
                float leading=.35f+.65f*(travelX>=0?u:1-u);
                float envelope=(float)StrictMath.sin(age*StrictMath.PI);
                float alpha=(.035f+motion*.94f*leading)*envelope*envelope;
                float length=(14+motion*16)*scale,width=(10+motion*20)*scale;
                VesselFoam.patch(canvas,patch%2,x+ex*size*scale,y+(ey*size-bob+.5f)*scale,
                        (float)StrictMath.atan2(ty,tx),length,width,alpha,WaterFoam.phase(g.visualTicks,b.id+patch));
            }
        } finally {if(clip.length>0)canvas.popClip();}
    }
    private static float offset(float[] a,float[] b,int i,boolean vertical,float t,float blend) {
        int j=i*2+(vertical?1:0),k=Math.min(i+1,SEGMENTS)*2+(vertical?1:0);
        float x=a[j]+(b[j]-a[j])*blend,y=a[k]+(b[k]-a[k])*blend;
        return x+(y-x)*t;
    }
}
