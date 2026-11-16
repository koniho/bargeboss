package com.bargeboss.game;

/** Quay footprints for the one-slip exchange and the single-file dead-end harbor.
 * The same rectangles drive collision, water clipping, shore cutouts and drawing. */
public final class PhysicalBerths {
    public static final int SHARED=1,DEEP=3;
    public static final float LEFT=570,RIGHT=628,CENTER=599,MOUTH=570,APPROACH=660;
    public static final float HALF_WIDTH=29,HALF_LENGTH=53;
    // Each rectangle is minX,minY,maxX,maxY. The slip has one southern opening.
    private static final float[][] SOLIDS={{430,148,570,570},{570,100,628,148},{628,100,900,832}};
    private PhysicalBerths(){}
    public static boolean active(int id){int kind=Challenges.kind(id);return kind==SHARED || kind==DEEP;}
    public static float[][] solids(int id){return SOLIDS;}
    public static float top(int id){return 148;}
    public static float dockX(int id,int berth){return CENTER;}
    public static float dockY(int id,int berth){return Challenges.kind(id)==SHARED?480:berth==2?220:berth==0?350:480;}
    public static float halfWidth(int id){return HALF_WIDTH;}
    public static float halfLength(int id){return HALF_LENGTH;}
    public static boolean contains(int id,float x,float y){
        return CalumetMap.contains(x,y);
    }
    public static boolean clear(int id,float x,float y,float xx,float yy,float pad){
        return CalumetMap.clear(x,y,xx,yy,pad);
    }
    /** Alternate banks and quay constraints so a corner cannot push a hull through the opposite bank. */
    public static float collide(int id,Body b){return collide(id,b,null);}
    public static float collide(int id,Body b,CollisionSplashes splashes){
        return CalumetMap.collide(b,splashes);
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
        boolean sourceInside=x>LEFT && x<RIGHT && y>=148 && y<MOUTH,targetInside=tx>LEFT && tx<RIGHT && ty>=148 && ty<MOUTH;
        if(targetInside && !sourceInside){
            // The eastern slip is separated from the main river by a solid quay.
            // Reach the broad southern basin before traversing to its channel axis.
            if(x<LEFT && !CalumetMap.clear(x,y,CENTER,APPROACH,Math.min(pad,20))){out[0]=300;out[1]=APPROACH;return;}
            out[0]=CENTER;out[1]=y>=MOUTH+50 && Math.abs(x-CENTER)<7?ty:APPROACH;return;
        }
        if(sourceInside && !targetInside){out[0]=CENTER;out[1]=APPROACH;return;}
        if(!CalumetMap.clear(x,y,tx,ty,Math.min(pad,20)) && (x>Physics.RIGHT || tx>Physics.RIGHT)){out[0]=x<Physics.RIGHT?300:CENTER;out[1]=APPROACH;return;}
        out[0]=tx;out[1]=ty;
    }
}
