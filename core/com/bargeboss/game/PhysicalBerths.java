package com.bargeboss.game;

/** Quay footprints for the one-slip exchange and the single-file dead-end harbor.
 * The same rectangles drive collision, water clipping, shore cutouts and drawing. */
public final class PhysicalBerths {
    public static final int SHARED=1,DEEP=3;
    public static final float LEFT=170,RIGHT=228,CENTER=199,MOUTH=570,APPROACH=660;
    public static final float HALF_WIDTH=29,HALF_LENGTH=53;
    // Each rectangle is minX,minY,maxX,maxY. The slip has one southern opening.
    private static final float[][] SHARED_SOLIDS={{170,350,300,390},{228,390,300,570}};
    private static final float[][] DEEP_SOLIDS={{170,100,430,148},{228,148,430,570}};
    private static final float[][] NONE=new float[0][];
    private PhysicalBerths(){}
    public static boolean active(int id){int kind=Challenges.kind(id);return kind==SHARED || kind==DEEP;}
    public static float[][] solids(int id){int kind=Challenges.kind(id);return kind==SHARED?SHARED_SOLIDS:kind==DEEP?DEEP_SOLIDS:NONE;}
    public static float top(int id){return Challenges.kind(id)==DEEP?148:Physics.TOP;}
    public static float dockX(int id,int berth){return CENTER;}
    public static float dockY(int id,int berth){return Challenges.kind(id)==SHARED?480:berth==2?220:berth==0?350:480;}
    public static float halfWidth(int id){return HALF_WIDTH;}
    public static float halfLength(int id){return HALF_LENGTH;}
    public static boolean contains(int id,float x,float y){
        if(x<Physics.LEFT || x>Physics.RIGHT || y<top(id) || y>Physics.BOTTOM)return false;
        for(float[] r:solids(id))if(x>=r[0] && x<=r[2] && y>=r[1] && y<=r[3])return false;
        return true;
    }
    public static boolean clear(int id,float x,float y,float xx,float yy,float pad){
        return HarborGeometry.clear(x,y,xx,yy,pad,solids(id));
    }
    /** Alternate banks and quay constraints so a corner cannot push a hull through the opposite bank. */
    public static float collide(int id,Body b){return collide(id,b,null);}
    public static float collide(int id,Body b,CollisionSplashes splashes){
        float impact=0;
        for(int i=0;i<4;i++){
            impact=Math.max(impact,Physics.banks(b,top(id),Physics.BOTTOM,false,splashes));
            impact=Math.max(impact,HarborGeometry.collide(b,solids(id),splashes));
        }
        return impact;
    }
    /** Full hull checks are stricter than the wide harbor's center-based lasso assistance. */
    public static boolean fits(int id,Body b){
        float x=dockX(id,b.berth),y=dockY(id,b.berth);
        for(int i=0;i<3;i++)if(Math.abs(b.discX(i)-x)+b.radius>HALF_WIDTH
                || Math.abs(b.discY(i)-y)+b.radius>HALF_LENGTH)return false;
        return true;
    }
    /** Approach along the channel axis before entering; a circumscribed-radius route cannot fit
     * this intentionally single-file channel even though an aligned hull fits with clearance. */
    public static void waypoint(int id,float x,float y,float tx,float ty,float pad,float[] out){
        float head=Challenges.kind(id)==SHARED?390:top(id);
        boolean sourceInside=x<RIGHT && y>=head && y<MOUTH,targetInside=tx<RIGHT && ty>=head && ty<MOUTH;
        if(targetInside && !sourceInside){
            if(Math.abs(x-CENTER)>7 || y<MOUTH+50){
                HarborGeometry.waypoint(x,y,CENTER,APPROACH,Math.min(pad,21),top(id),Physics.BOTTOM,out,solids(id));return;
            }
            out[0]=CENTER;out[1]=ty;return;
        }
        if(sourceInside && !targetInside){out[0]=CENTER;out[1]=APPROACH;return;}
        if(sourceInside && targetInside){out[0]=CENTER;out[1]=ty;return;}
        HarborGeometry.waypoint(x,y,tx,ty,pad,top(id),Physics.BOTTOM,out,solids(id));
    }
}
