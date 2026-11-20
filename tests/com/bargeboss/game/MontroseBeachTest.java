package com.bargeboss.game;

/** New navigable coast shares solid ground, mask geometry and the chart frame. */
public final class MontroseBeachTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static boolean wet(float x,float y){return MontroseHarbor.contains(MontroseHarbor.mapX(x),MontroseHarbor.mapY(y));}
    public static void run(){
        for(float[] a:new float[][]{{900,-1050},{1600,-800},{2000,-900},{2400,-1400},{2200,-1700},{2500,100},{453,1400}})check(wet(a[0],a[1]),"Beach/lake route water "+a[0]+","+a[1]);
        for(float[] a:new float[][]{{1600,-530},{1080,-545},{620,-965},{1280,-465},{MontroseBeach.BEACON_X,MontroseBeach.BEACON_Y},{2170,-900}})check(!wet(a[0],a[1]),"Sand, pavilion and breakwater are solid "+a[0]+","+a[1]);
        float[][] route={{453,1450},{900,1480},{1800,1450},{2500,1000},{2550,-1600},{2200,-1800},{1700,-1300},{1600,-850}};
        for(int i=1;i<route.length;i++)check(MontroseHarbor.clear(MontroseHarbor.mapX(route[i-1][0]),MontroseHarbor.mapY(route[i-1][1]),MontroseHarbor.mapX(route[i][0]),MontroseHarbor.mapY(route[i][1])),"Continuous inlet to beacon to beach route "+i);
        check(!MontroseHarbor.clear(MontroseHarbor.mapX(2050),MontroseHarbor.mapY(-1100),MontroseHarbor.mapX(2350),MontroseHarbor.mapY(-1100)),"Breakwater blocks crossing");
        for(float[] a:new float[][]{{1000,600},{650,500},{1450,420}})check(SeaWaves.exposure(MontroseHarbor.mapX(a[0]),MontroseHarbor.mapY(a[1]))==0,"Marina remains sheltered");
        check(SeaWaves.exposure(MontroseHarbor.mapX(2400),MontroseHarbor.mapY(-1400))>.99f,"Exposed beacon lake retains swell");
        check(SeaWaves.exposure(MontroseHarbor.mapX(1600),MontroseHarbor.mapY(-850))>.4f,"Beach water does not inherit marina shelter");
        Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();
        float x=MontroseHarbor.mapX(MontroseBeach.BEACON_X),y=MontroseHarbor.mapY(MontroseBeach.BEACON_Y);
        check(PolygonWater.contains(MontroseHarbor.PLAY_BOUNDS,x,y),"Beacon inside play bounds");
        check(OverviewMap.mapX(g,x,y)>OverviewMap.mapX(g,MontroseHarbor.SLIP_X,MontroseHarbor.SLIP_Y),"Beacon east/right of marina on overview");
        check(OverviewMap.mapY(g,x,y)<OverviewMap.mapY(g,MontroseHarbor.SLIP_X,MontroseHarbor.SLIP_Y),"Beacon north/up on overview");
        g.layout=Layout.focused(960,600,x,y,1,480,300);RiverCamera c=g.layout.camera;
        float[] mask=MontroseHarbor.shore(c,x-60,y-60,x+60,y+60,Float.NEGATIVE_INFINITY);
        check(ShoreOcclusion.contains(mask,c.screenX(x,y),c.screenY(x,y)-MontroseHarbor.LAND_RISE*c.scale),"Beacon concrete masks water and hull pixels");
        // Every new coast vertex must be included in the triangulated land/water area.
        double sum=0;for(float[] tri:MontroseHarbor.WATER_TRIANGLES)sum+=Math.abs(PolygonWater.area(tri));
        check(Math.abs(sum-Math.abs(PolygonWater.area(MontroseHarbor.WATER_POLYGON)))<128,"Expanded coast triangulates without lost/overlapping area");
        System.out.println(checks+" Montrose beach route, breakwater, shoreline mask, chart and exposure checks passed");
    }
    public static void main(String[] args){run();}
}
