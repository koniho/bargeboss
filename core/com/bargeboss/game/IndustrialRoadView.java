package com.bargeboss.game;

/** Shared 64-unit road tiles separating the tutorial's packed industrial blocks. */
public final class IndustrialRoadView {
    static final int MIN_COL=-32,MAX_COL=24,MIN_ROW=-12,MAX_ROW=28;
    private IndustrialRoadView() {}
    static boolean isRoadCell(int col,int row) {
        return Math.floorMod(col,5)==4 || Math.floorMod(row,5)==4;
    }
    static boolean clearance(float x,float y,float width,float height,float distance) {
        for(float[] water:TrainingChannel.WATER_RECTANGLES) {
            float dx=Math.max(0,Math.max(water[0]-x-width,x-water[2]));
            float dy=Math.max(0,Math.max(water[1]-y-height,y-water[3]));
            if(dx*dx+dy*dy<distance*distance) return false;
        }
        return true;
    }
    static boolean roadClear(int col,int row) {
        return col>=MIN_COL && col<MAX_COL && row>=MIN_ROW && row<MAX_ROW
                && isRoadCell(col,row) && clearance(col*64,row*64,64,64,48);
    }
    public static void draw(Game g,Painter canvas) {
        RiverCamera c=g.layout.camera;
        Painter world=new RiverPainter(GroundArt.raisedCanvas(g,canvas),c);
        float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        for(int corner=0;corner<4;corner++) {
            float sx=corner%2*g.layout.width,sy=corner/2*g.layout.height+GroundArt.LAND_RISE*c.scale;
            float x=c.worldX(sx,sy),y=c.worldY(sx,sy);
            minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
        }
        int firstX=Math.max(MIN_COL,(int)StrictMath.floor(minX/64)-1),lastX=Math.min(MAX_COL,(int)StrictMath.ceil(maxX/64)+1);
        int firstY=Math.max(MIN_ROW,(int)StrictMath.floor(minY/64)-1),lastY=Math.min(MAX_ROW,(int)StrictMath.ceil(maxY/64)+1);
        for(int row=firstY;row<lastY;row++) for(int col=firstX;col<lastX;col++) {
            if(!roadClear(col,row)) continue;
            float x=col*64,y=row*64;
            world.rect(x,y,64,64,0xff303b3e);
            // Flush concrete shoulders and faded lane paint stay below factories.
            boolean left=roadClear(col-1,row),right=roadClear(col+1,row);
            boolean up=roadClear(col,row-1),down=roadClear(col,row+1);
            if(!left) edge(world,x,y,x,y+64);
            if(!right) edge(world,x+64,y,x+64,y+64);
            if(!up) edge(world,x,y,x+64,y);
            if(!down) edge(world,x,y+64,x+64,y+64);
            if(left && right && !(up && down)) {
                world.line(x+7,y+32,x+23,y+32,1.2f,0x887d8070);
                world.line(x+39,y+32,x+55,y+32,1.2f,0x887d8070);
            } else if(up && down && !(left && right)) {
                world.line(x+32,y+7,x+32,y+23,1.2f,0x887d8070);
                world.line(x+32,y+39,x+32,y+55,1.2f,0x887d8070);
            }
        }
    }
    private static void edge(Painter p,float x,float y,float xx,float yy) {
        p.line(x,y,xx,yy,3,0xff62675f);
        p.line(x,y,xx,yy,.8f,0xff868779);
    }
}
