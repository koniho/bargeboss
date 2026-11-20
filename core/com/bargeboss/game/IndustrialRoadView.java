package com.bargeboss.game;

/** Shared 64-unit road tiles separating the tutorial's packed industrial blocks. */
public final class IndustrialRoadView {
    static final int MIN_COL=TrainingScenery.MIN_COL,MAX_COL=TrainingScenery.MAX_COL,MIN_ROW=TrainingScenery.MIN_ROW,MAX_ROW=TrainingScenery.MAX_ROW;
    private static final byte[] ROADS=makeRoads();
    private static final RoadTile[] TILES=makeTiles();
    private IndustrialRoadView() {}
    static boolean isRoadCell(int col,int row) {
        return roadClear(col,row);
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
        return roadMask(col,row)!=0;
    }
    private static int roadMask(int col,int row) {
        return col<MIN_COL || col>=MAX_COL || row<MIN_ROW || row>=MAX_ROW?0:
                ROADS[(row-MIN_ROW)*(MAX_COL-MIN_COL)+col-MIN_COL];
    }
    private static byte[] makeRoads() {
        int width=MAX_COL-MIN_COL,height=MAX_ROW-MIN_ROW;
        byte[] roads=new byte[width*height];
        boolean[] dry=new boolean[roads.length];
        for(int row=MIN_ROW;row<MAX_ROW;row++)for(int col=MIN_COL;col<MAX_COL;col++){
            int at=(row-MIN_ROW)*width+col-MIN_COL;
            dry[at]=TrainingScenery.nearWater(col*64,row*64,64,64) && clearance(col*64,row*64,64,64,48);
            if(dry[at] && (Math.floorMod(col,5)==4 || Math.floorMod(row,5)==4))roads[at]=1;
        }
        // Join the clipped avenues with shortest dry service-road links. Multi-source
        // flood fill starts on the connected network; no water crossing or filler land.
        int root=0;while(root<roads.length && roads[root]==0)root++;
        boolean[] connected=new boolean[roads.length];
        int[] queue=new int[roads.length],parent=new int[roads.length];
        if(root<roads.length)connected[root]=true;
        while(root<roads.length){
            java.util.Arrays.fill(parent,-1);int head=0,tail=0;
            for(int i=0;i<roads.length;i++)if(connected[i]){queue[tail++]=i;parent[i]=i;}
            int target=-1;
            while(head<tail && target<0){
                int at=queue[head++];
                if(roads[at]!=0 && !connected[at]){target=at;break;}
                for(int dir=0;dir<4;dir++){
                    int next=dir==0?at-1:dir==1?at+1:dir==2?at-width:at+width;
                    if(next<0 || next>=roads.length || (dir<2 && next/width!=at/width) || !dry[next] || parent[next]>=0)continue;
                    parent[next]=at;queue[tail++]=next;
                }
            }
            if(target<0)break;
            while(!connected[target]){connected[target]=true;roads[target]=1;target=parent[target];}
        }
        for(int i=0;i<roads.length;i++)if(!connected[i])roads[i]=0;
        for(int row=0;row<height;row++) for(int col=0;col<width;col++) {
            int at=row*width+col;if(roads[at]==0) continue;
            if(col>0 && roads[at-1]!=0) roads[at]|=2;
            if(col+1<width && roads[at+1]!=0) roads[at]|=4;
            if(row>0 && roads[at-width]!=0) roads[at]|=8;
            if(row+1<height && roads[at+width]!=0) roads[at]|=16;
        }
        return roads;
    }
    /** A complete parked footprint must stay on the connected paved surface. */
    static boolean roadFootprint(float x,float y,float halfLength,float halfWidth){
        int l=(int)StrictMath.floor((x-halfLength)/64),r=(int)StrictMath.floor((x+halfLength)/64);
        int t=(int)StrictMath.floor((y-halfWidth)/64),b=(int)StrictMath.floor((y+halfWidth)/64);
        for(int row=t;row<=b;row++)for(int col=l;col<=r;col++)if(!roadClear(col,row))return false;
        return true;
    }
    private static final class RoadTile {
        final float[] face,patch,crack;final int shade;
        RoadTile(int col,int row){
            float x=col*64,y=row*64;int seed=Math.floorMod(col*37+row*71,19);
            face=new float[]{x,y,x+64,y,x+64,y+64,x,y+64};
            float px=x+7+seed%9,py=y+8+seed%13;
            patch=new float[]{px,py,px+22,py-2,px+26,py+13,px+3,py+16};
            crack=new float[]{x+7,y+47-seed%7,x+24,y+43-seed%5,x+32,y+49,x+49,y+42+seed%9};
            shade=seed%2==0?0xff343532:0xff393933;
        }
        void draw(Painter p){
            p.surfacePolygon(face,face,SurfaceMaterial.CONCRETE,shade,.12f);
            p.surfacePolygon(patch,patch,SurfaceMaterial.CONCRETE,0xff252a28,.24f);
            for(int i=0;i<crack.length-2;i+=2)p.line(crack[i],crack[i+1],crack[i+2],crack[i+3],.65f,0xff171e20);
        }
    }
    private static RoadTile[] makeTiles(){
        RoadTile[] tiles=new RoadTile[ROADS.length];
        for(int row=MIN_ROW;row<MAX_ROW;row++)for(int col=MIN_COL;col<MAX_COL;col++)if(roadClear(col,row))tiles[(row-MIN_ROW)*(MAX_COL-MIN_COL)+col-MIN_COL]=new RoadTile(col,row);
        return tiles;
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
            int road=roadMask(col,row);
            if(road==0 || !TrainingGroundView.tileVisible(g,col,row,false)) continue;
            float x=col*64,y=row*64;
            TILES[(row-MIN_ROW)*(MAX_COL-MIN_COL)+col-MIN_COL].draw(world);
            // Flush concrete shoulders and faded lane paint stay below factories.
            boolean left=(road&2)!=0,right=(road&4)!=0;
            boolean up=(road&8)!=0,down=(road&16)!=0;
            if(!left) edge(world,x,y,x,y+64);
            if(!right) edge(world,x+64,y,x+64,y+64);
            if(!up) edge(world,x,y,x+64,y);
            if(!down) edge(world,x,y+64,x+64,y+64);
            if(left && right && !(up && down)) {
                world.line(x+7,y+32,x+23,y+32,1.2f,0x886b6f61);
                world.line(x+39,y+32,x+55,y+32,1.2f,0x886b6f61);
            } else if(up && down && !(left && right)) {
                world.line(x+32,y+7,x+32,y+23,1.2f,0x886b6f61);
                world.line(x+32,y+39,x+32,y+55,1.2f,0x886b6f61);
            }
        }
    }
    private static void edge(Painter p,float x,float y,float xx,float yy) {
        p.line(x,y,xx,yy,3,0xff3b484d);
        p.line(x,y,xx,yy,.8f,0xff637077);
    }
}
