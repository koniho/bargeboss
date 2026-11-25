package com.bargeboss.game;

import java.util.ArrayList;

/** Projected solid ground, used as a cutout rather than repainting land over scenery. */
public final class ShoreOcclusion {
    public static final float[] EMPTY=new float[0];
    private ShoreOcclusion(){}
    public static float[] vessel(Game g,RiverCamera c,Body b,boolean surface) {
        return vessel(g,c,b,surface,false);
    }
    public static float[] vessel(Game g,RiverCamera c,Body b,boolean surface,boolean union) {
        return region(g,c,b.x-160,b.y-160,b.x+160,b.y+160,
                surface?Float.NEGATIVE_INFINITY:c.screenY(b.x,b.y),union);
    }
    /** depth is the unraised ground depth of the sprite/span, never its heading or speed. */
    public static float[] region(Game g,RiverCamera c,float left,float top,float right,float bottom,float depth) {
        return region(g,c,left,top,right,bottom,depth,false);
    }
    /** Full-depth mask over the whole view (water surface, wave crests), padded by pad world
     * units. Survey levels reuse cached unit-space tiles; other stages fall back to region(). */
    public static float[] view(Game g,RiverCamera c,float pad,boolean union){
        float p=pad*c.scale,w=g.layout.width,h=g.layout.height;
        SurveyLevel level=g.level();
        if(level!=null && g.mode!=Game.TITLE && g.mode!=Game.CHALLENGES)return level.geometry().viewShore(c,-p,-p,w+p,h+p,union);
        float l=Float.MAX_VALUE,t=l,r=-l,b=-l;
        for(int k=0;k<4;k++){float x=c.worldX((k&1)*w,(k>>1)*h),y=c.worldY((k&1)*w,(k>>1)*h);l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);}
        return region(g,c,l-pad,t-pad,r+pad,b+pad,Float.NEGATIVE_INFINITY,union);
    }
    public static float[] region(Game g,RiverCamera c,float left,float top,float right,float bottom,float depth,boolean union){
        if(g.mode==Game.TITLE || g.mode==Game.CHALLENGES)return EMPTY;
        SurveyLevel level=g.level();
        if(level!=null)return level.geometry().shore(c,left,top,right,bottom,depth,union);
        if(g.montroseStage())return MontroseHarbor.shore(c,left,top,right,bottom,depth,union);
        return industrial(c,left,top,right,bottom,depth,null);
    }
    /** Survey geometry supplies raised tops directly; null selects the practice canal. */
    static float[] industrial(RiverCamera c,float left,float top,float right,float bottom,float depth,HarborPolygonGeometry survey){
        if(survey!=null)return survey.tops(c,left,top,right,bottom,depth);
        ArrayList<float[]> dry=new ArrayList<float[]>();
        subtract(dry,left,top,right,bottom,CalumetMap.water(),0);
        if(dry.isEmpty())return EMPTY;
        float rise=GroundArt.LAND_RISE*c.scale;
        ArrayList<float[]> pieces=new ArrayList<float[]>();int count=0;
        for(float[] r:dry) {
            float[] q={c.screenX(r[0],r[1]),c.screenY(r[0],r[1])-rise,
                c.screenX(r[2],r[1]),c.screenY(r[2],r[1])-rise,
                c.screenX(r[2],r[3]),c.screenY(r[2],r[3])-rise,
                c.screenX(r[0],r[3]),c.screenY(r[0],r[3])-rise};
            // A ground surface behind the vessel cannot cover its upright artwork.
            q=below(q,depth-rise);
            if(q.length<6)continue;
            pieces.add(q);count+=(q.length/2-2)*6;
        }
        if(count==0)return EMPTY;
        float[] triangles=new float[count];int at=0;
        for(float[] q:pieces)for(int i=2;i<q.length-2;i+=2) {
            triangles[at++]=q[0];triangles[at++]=q[1];triangles[at++]=q[i];triangles[at++]=q[i+1];triangles[at++]=q[i+2];triangles[at++]=q[i+3];
        }
        return triangles;
    }
    private static void subtract(ArrayList<float[]> out,float l,float t,float r,float b,float[][] water,int from) {
        if(r<=l || b<=t)return;
        for(int i=from;i<water.length;i++) {
            float[] w=water[i];float ll=Math.max(l,w[0]),tt=Math.max(t,w[1]),rr=Math.min(r,w[2]),bb=Math.min(b,w[3]);
            if(rr<=ll || bb<=tt)continue;
            subtract(out,l,t,r,tt,water,i+1);subtract(out,l,bb,r,b,water,i+1);
            subtract(out,l,tt,ll,bb,water,i+1);subtract(out,rr,tt,r,bb,water,i+1);return;
        }
        out.add(new float[]{l,t,r,b});
    }
    private static float[] below(float[] q,float limit) {
        if(limit==Float.NEGATIVE_INFINITY)return q;
        float[] result=new float[q.length+4];int n=0;
        float ax=q[q.length-2],ay=q[q.length-1];boolean inside=ay>=limit;
        for(int i=0;i<q.length;i+=2) {
            float bx=q[i],by=q[i+1];boolean next=by>=limit;
            if(next!=inside){float t=(limit-ay)/(by-ay);result[n++]=ax+(bx-ax)*t;result[n++]=limit;}
            if(next){result[n++]=bx;result[n++]=by;}
            ax=bx;ay=by;inside=next;
        }
        float[] trimmed=new float[n];System.arraycopy(result,0,trimmed,0,n);return trimmed;
    }
    public static boolean contains(float[] triangles,float x,float y) {
        for(int i=0;i<triangles.length;i+=6) {
            float a=cross(triangles[i],triangles[i+1],triangles[i+2],triangles[i+3],x,y);
            float b=cross(triangles[i+2],triangles[i+3],triangles[i+4],triangles[i+5],x,y);
            float c=cross(triangles[i+4],triangles[i+5],triangles[i],triangles[i+1],x,y);
            float area=cross(triangles[i],triangles[i+1],triangles[i+2],triangles[i+3],triangles[i+4],triangles[i+5]);
            if(Math.abs(area)>.00001f && ((a>=0 && b>=0 && c>=0) || (a<=0 && b<=0 && c<=0)))return true;
        }
        return false;
    }
    private static float cross(float x,float y,float xx,float yy,float px,float py){return (xx-x)*(py-y)-(yy-y)*(px-x);}
}
