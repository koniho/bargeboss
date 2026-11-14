package com.bargeboss.game;

/** Animated hull/water contact, registered to the painted lower silhouette in every view. */
public final class VesselWaterline {
    private static final int SEGMENTS=24;
    private static final float[][][] EDGE=edges();
    private VesselWaterline() {}
    private static float[][][] edges() {
        float[][][] result=new float[2][32][(SEGMENTS+1)*2];
        for(int atlas=0;atlas<2;atlas++)for(int frame=0;frame<32;frame++) {
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
                result[atlas][frame][2*i]=((column+.5f)/64-SpriteAtlas.pivotX(atlas,frame))*SpriteAtlas.widthScale(atlas,frame);
                result[atlas][frame][2*i+1]=(sum/Math.max(1,weight)/64-SpriteAtlas.pivotY(atlas,frame))*SpriteAtlas.heightScale(atlas,frame);
            }
        }
        return result;
    }
    /** Outer ripples go beneath the sprite; the thin contact crest follows the hull pass. */
    public static void draw(Game g,Painter p,Body b,boolean tug,float size,boolean contact) {
        boolean river=p instanceof RiverPainter;RiverCamera c=river?((RiverPainter)p).camera:null;
        Painter canvas=river?((RiverPainter)p).canvas():p;
        float scale=river?c.scale:1,x=river?c.screenX(b.x,b.y):b.x,y=river?c.screenY(b.x,b.y):b.y;
        float[] clip=river?ShoreOcclusion.vessel(g,c,b,true):ShoreOcclusion.EMPTY;
        if(clip.length>0)canvas.pushClipOutside(clip);
        try {
            float heading=b.angle/(float)(StrictMath.PI/16),whole=(float)StrictMath.floor(heading);
            int a=(((int)whole+(river?RiverCamera.DIRECTION_OFFSET:0))%32+32)%32,bb=(a+1)%32;
            float mix=Physics.clamp((heading-whole-.4f)*5,0,1);mix=mix*mix*(3-2*mix);
            float[] from=EDGE[tug?0:1][a],to=EDGE[tug?0:1][bb];
            float motion=Math.min(1,b.speed()/65),trough=Math.min(1,Math.max(0,-b.heaveVelocity)/7+b.wakeKickTicks/24f);
            float tick=g.visualTicks,phase=tick*.055f+b.id*1.73f;
            float bob=WaterSurface.lift(g,b),age=((g.visualTicks+b.id*17)%90+90)%90/90f;
            // Contact rides the hull. Detached ripples spread on the surface with reduced heave.
            float spread=contact?0:2+age*(5+motion*4),lift=contact?bob:bob*(1-age);
            float previousX=0,previousY=0;
            for(int i=0;i<=SEGMENTS;i++) {
                float u=i/(float)SEGMENTS,ex=from[2*i]+(to[2*i]-from[2*i])*mix;
                float ey=from[2*i+1]+(to[2*i+1]-from[2*i+1])*mix;
                float wave=(float)StrictMath.sin(phase+u*13)*(.25f+trough*.35f);
                float xx=x+(ex*size+(u-.5f)*spread)*scale,yy=y+(ey*size-lift+spread*.45f+wave)*scale;
                if(i>0) {
                    float taper=(float)StrictMath.sin((u-.5f/SEGMENTS)*StrictMath.PI);
                    float crest=.55f+.45f*(float)StrictMath.sin(phase*1.4+u*21);
                    float strength=taper*(.48f+motion*.3f+trough*.22f);
                    if(contact) {
                        canvas.line(previousX,previousY,xx,yy,3.4f*scale,SoftCue.alpha(0xff70c5bd,strength*.16f));
                        canvas.line(previousX,previousY,xx,yy,(.65f+crest*.35f)*scale,SoftCue.alpha(0xffc2e7dc,strength*(.36f+crest*.34f)));
                    } else canvas.line(previousX,previousY,xx,yy,1.1f*scale,
                            SoftCue.alpha(0xff80bcb7,strength*.25f*(1-age)*(1-age)));
                }
                previousX=xx;previousY=yy;
            }
        } finally {if(clip.length>0)canvas.popClip();}
    }
}
