package com.bargeboss.game;

import java.util.ArrayList;

/** Small electric yard haulers shuttle along verified dry road runs, outside gameplay water. */
public final class YardActivity {
    private static final float[][] ROUTES=routes();
    private YardActivity() {}
    private static float[][] routes() {
        ArrayList<float[]> runs=new ArrayList<float[]>();
        for(int row=IndustrialRoadView.MIN_ROW;row<IndustrialRoadView.MAX_ROW;row++) {
            if(Math.floorMod(row,5)!=4)continue;
            int start=IndustrialRoadView.MIN_COL;
            while(start<IndustrialRoadView.MAX_COL) {
                while(start<IndustrialRoadView.MAX_COL && !routeCell(start,row))start++;
                int end=start;while(end<IndustrialRoadView.MAX_COL && routeCell(end,row))end++;
                for(int block=start;block+3<=end;block+=6) {
                    int limit=Math.min(end,block+6);
                    runs.add(new float[]{block*64+44,limit*64-44,row*64+32});
                }
                start=end+1;
            }
        }
        return runs.toArray(new float[runs.size()][]);
    }
    /** Remove occupied cells before assembling runs, so vehicles stop and reverse
     * continuously on their remaining lane instead of disappearing near parked trucks. */
    private static boolean routeCell(int col,int row){
        if(!IndustrialRoadView.roadClear(col,row) || IndustrialRoadView.clearance(col*64,row*64,64,64,190))return false;
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
        out[0]=r[0]+(r[1]-r[0])*along;out[1]=r[2];out[2]=phase<travel || phase>=travel+90 && phase<2*travel+90?1:0;
    }
    private static float[] face(RiverCamera c,float[] xyz) {
        float[] xy=new float[xyz.length/3*2];
        for(int i=0,j=0;i<xyz.length;i+=3,j+=2){xy[j]=c.screenX(xyz[i],xyz[i+1]);xy[j+1]=c.screenY(xyz[i],xyz[i+1])-(GroundArt.LAND_RISE+xyz[i+2])*c.scale;}
        return xy;
    }
    private static void box(Painter p,RiverCamera c,float x,float y,float w,float d,float z,float h,int top,int side) {
        float l=x-w/2,r=x+w/2,t=y-d/2,b=y+d/2;
        p.poly(face(c,new float[]{l,t,z,l,b,z,l,b,z+h,l,t,z+h}),0xff26313a);
        p.poly(face(c,new float[]{l,b,z,r,b,z,r,b,z+h,l,b,z+h}),side);
        p.poly(face(c,new float[]{l,t,z+h,r,t,z+h,r,b,z+h,l,b,z+h}),top);
    }
    static int[] visible(Game g) {
        int[] found=new int[6];int count=0;float[] at=new float[3];RiverCamera c=g.layout.camera;float s=c.scale;
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
    public static void draw(Game g,Painter canvas) {for(int route:visible(g))drawVehicle(g,canvas,route);}
    static void drawVehicle(Game g,Painter canvas,int route) {
        RiverCamera c=g.layout.camera;float s=c.scale;float[] at=new float[3];position(route,g.visualTicks,at);float x=at[0],y=at[1];
            float vibration=at[2]*.2f*(float)StrictMath.sin(g.visualTicks*.8+route);
            for(int end=-1;end<=1;end+=2)for(int side=-1;side<=1;side+=2)
                box(canvas,c,x+end*12,y+side*6,5,3,0,4,0xff162028,0xff10181e);
            box(canvas,c,x,y,34,12,3+vibration,4,0xff987340,0xff5d4833);
            box(canvas,c,x-7,y,14,10,7+vibration,7,0xff6f7d80,0xff3f4b54);
            box(canvas,c,x+11,y,10,11,7+vibration,9,0xffb18b4e,0xff6c5337);
            box(canvas,c,x+11,y,8,11.1f,10+vibration,4,0xff496779,0xff314c60);
            float lampX=c.screenX(x+12,y),lampY=c.screenY(x+12,y)-(GroundArt.LAND_RISE+18+vibration)*s;
            float blink=.5f+.5f*(float)StrictMath.sin(g.visualTicks*.13+route*2.1);
            SceneryEffects.compactGlow(canvas,lampX,lampY,1.6f*s,0xffffaf45,.35f+.65f*blink,g.visualTicks,route);
            canvas.circle(lampX,lampY,1.3f*s,0xffffcc73);
            float hx=c.screenX(x+17,y),hy=c.screenY(x+17,y)-(GroundArt.LAND_RISE+7)*s;
            SceneryEffects.compactGlow(canvas,hx,hy,1.05f*s,0xffffd39a,.8f,g.visualTicks,route*.7f);
    }
}
