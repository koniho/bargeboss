package com.bargeboss.game;

import java.util.ArrayList;

/** Small electric yard haulers shuttle along verified dry road runs, outside gameplay water. */
public final class YardActivity {
    private static final float[][] ROUTES=routes();
    private YardActivity() {}
    private static float[][] routes() {
        ArrayList<float[]> runs=new ArrayList<float[]>();
        for(int axis=0;axis<2;axis++){
            int low=axis==0?IndustrialRoadView.MIN_ROW:IndustrialRoadView.MIN_COL;
            int high=axis==0?IndustrialRoadView.MAX_ROW:IndustrialRoadView.MAX_COL;
            int first=axis==0?IndustrialRoadView.MIN_COL:IndustrialRoadView.MIN_ROW;
            int last=axis==0?IndustrialRoadView.MAX_COL:IndustrialRoadView.MAX_ROW;
            for(int lane=low;lane<high;lane++){
                int start=first;
                while(start<last){
                    while(start<last && !laneCell(start,lane,axis))start++;
                    int end=start;while(end<last && laneCell(end,lane,axis))end++;
                    for(int block=start;block+2<=end;block+=5){
                        int limit=Math.min(end,block+5);
                        runs.add(new float[]{block*64+44,limit*64-44,lane*64+32,axis});
                    }
                    start=end+1;
                }
            }
        }
        return runs.toArray(new float[runs.size()][]);
    }
    private static boolean laneCell(int along,int lane,int axis){
        int col=axis==0?along:lane,row=axis==0?lane:along;
        // Cross traffic has its own non-overlapping lane sections at junctions.
        if(axis==1 && (IndustrialRoadView.roadClear(col-1,row) || IndustrialRoadView.roadClear(col+1,row)))return false;
        return routeCell(col,row);
    }
    /** Remove occupied cells before assembling runs, so vehicles stop and reverse
     * continuously on their remaining lane instead of disappearing near parked trucks. */
    private static boolean routeCell(int col,int row){
        if(!IndustrialRoadView.roadClear(col,row))return false;
        float l=col*64,t=row*64,r=l+64,b=t+64;
        for(int i=0;i<CalumetArt.count();i++){
            float[] v=CalumetArt.item(i);if((int)v[0]!=SpriteAtlas.SHORE_TRUCKS)continue;
            // Parked access apron plus the moving hauler's full half-extents.
            if(l<v[2]+99 && r>v[2]-99 && t<v[3]+54 && b>v[3]-54)return false;
        }
        return true;
    }
    static int count(){return ROUTES.length;}
    static void position(int route,int ticks,float[] out) {
        float[] r=ROUTES[route];int travel=(int)((r[1]-r[0])/18*60),cycle=2*travel+180;
        int phase=Math.floorMod(ticks+route*431,cycle);
        float along=phase<travel?phase/(float)travel:phase<travel+90?1:phase<2*travel+90?1-(phase-travel-90)/(float)travel:0;
        out[0]=r[3]==0?r[0]+(r[1]-r[0])*along:r[2];out[1]=r[3]==0?r[2]:r[0]+(r[1]-r[0])*along;out[2]=phase<travel || phase>=travel+90 && phase<2*travel+90?1:0;
    }
    private static float[] face(RiverCamera c,float[] xyz) {
        float[] xy=new float[xyz.length/3*2];
        for(int i=0,j=0;i<xyz.length;i+=3,j+=2){xy[j]=c.screenX(xyz[i],xyz[i+1]);xy[j+1]=c.screenY(xyz[i],xyz[i+1])-(GroundArt.LAND_RISE+xyz[i+2])*c.scale;}
        return xy;
    }
    static void box(Game g,Painter p,RiverCamera c,float x,float y,float w,float d,float z,float h,int top,int side) {
        float l=x-w/2,r=x+w/2,t=y-d/2,b=y+d/2;
        p.poly(face(c,new float[]{l,t,z,l,b,z,l,b,z+h,l,t,z+h}),SceneryLighting.face(g,0xff26313a,-1,l,y,(GroundArt.LAND_RISE+z+h/2)/ArticulatedModel.Z_SCALE,-1,0,0));
        p.poly(face(c,new float[]{l,b,z,r,b,z,r,b,z+h,l,b,z+h}),SceneryLighting.face(g,side,-1,x,b,(GroundArt.LAND_RISE+z+h/2)/ArticulatedModel.Z_SCALE,0,1,0));
        p.poly(face(c,new float[]{l,t,z+h,r,t,z+h,r,b,z+h,l,b,z+h}),SceneryLighting.face(g,top,-1,x,y,(GroundArt.LAND_RISE+z+h)/ArticulatedModel.Z_SCALE,0,0,1));
    }
    static int[] visible(Game g) {
        int[] found=new int[8];int count=0;float[] at=new float[3];RiverCamera c=g.layout.camera;float s=c.scale;
        for(int route=0;route<ROUTES.length && count<found.length;route++) {
            position(route,g.visualTicks,at);float x=c.screenX(at[0],at[1]),y=c.screenY(at[0],at[1])-GroundArt.LAND_RISE*s;
            if(x< -40*s || x>g.layout.width+40*s || y< -40*s || y>g.layout.height+40*s)continue;
            int j=count-1;float d=depth(g,route);
            while(j>=0 && depth(g,found[j])>d){found[j+1]=found[j];j--;}
            found[j+1]=route;count++;
        }
        return java.util.Arrays.copyOf(found,count);
    }
    static float depth(Game g,int route) {float[] at=new float[3];position(route,g.visualTicks,at);return g.layout.camera.screenY(at[0],at[1]);}
    static void lights(Game g,SceneryLighting out){
        float[] at=new float[3];
        for(int route=0;route<ROUTES.length;route++){
            position(route,g.visualTicks,at);boolean vertical=ROUTES[route][3]!=0;
            float x=at[0],y=at[1],v=at[2]*.2f*(float)StrictMath.sin(g.visualTicks*.8+route);
            float blink=.5f+.5f*(float)StrictMath.sin(g.visualTicks*.13+route*2.1);
            out.add(x+(vertical?0:12),y+(vertical?12:0),(GroundArt.LAND_RISE+18+v)/ArticulatedModel.Z_SCALE,55,.12f+.22f*blink,0xffffaf45);
            out.add(x+(vertical?0:17),y+(vertical?17:0),(GroundArt.LAND_RISE+7)/ArticulatedModel.Z_SCALE,55,.2f,0xffffd39a);
        }
    }
    public static void draw(Game g,Painter canvas) {for(int route:visible(g))drawVehicle(g,canvas,route);}
    static void drawVehicle(Game g,Painter canvas,int route) {
        RiverCamera c=g.layout.camera;float s=c.scale;float[] at=new float[3];position(route,g.visualTicks,at);float x=at[0],y=at[1];
            boolean vertical=ROUTES[route][3]!=0;
            float vibration=at[2]*.2f*(float)StrictMath.sin(g.visualTicks*.8+route);
            for(int end=-1;end<=1;end+=2)for(int side=-1;side<=1;side+=2)
                box(g,canvas,c,x+(vertical?side*6:end*12),y+(vertical?end*12:side*6),vertical?3:5,vertical?5:3,0,4,0xff162028,0xff10181e);
            box(g,canvas,c,x,y,vertical?12:34,vertical?34:12,3+vibration,4,0xff987340,0xff5d4833);
            box(g,canvas,c,x-(vertical?0:7),y-(vertical?7:0),vertical?10:14,vertical?14:10,7+vibration,7,0xff6f7d80,0xff3f4b54);
            box(g,canvas,c,x+(vertical?0:11),y+(vertical?11:0),vertical?11:10,vertical?10:11,7+vibration,9,0xffb18b4e,0xff6c5337);
            box(g,canvas,c,x+(vertical?0:11),y+(vertical?11:0),vertical?11.1f:8,vertical?8:11.1f,10+vibration,4,0xff496779,0xff314c60);
            float lampX=c.screenX(x+(vertical?0:12),y+(vertical?12:0)),lampY=c.screenY(x+(vertical?0:12),y+(vertical?12:0))-(GroundArt.LAND_RISE+18+vibration)*s;
            float blink=.5f+.5f*(float)StrictMath.sin(g.visualTicks*.13+route*2.1);
            SceneryEffects.compactGlow(canvas,lampX,lampY,1.6f*s,0xffffaf45,.35f+.65f*blink,g.visualTicks,route);
            canvas.circle(lampX,lampY,1.3f*s,0xffffcc73);
            float hx=c.screenX(x+(vertical?0:17),y+(vertical?17:0)),hy=c.screenY(x+(vertical?0:17),y+(vertical?17:0))-(GroundArt.LAND_RISE+7)*s;
            SceneryEffects.compactGlow(canvas,hx,hy,1.05f*s,0xffffd39a,.8f,g.visualTicks,route*.7f);
    }
}
