package com.bargeboss.game;

/** Retained static shore geometry must agree with direct masks after camera changes. */
public final class WaterClipTest {
    public static void run(){
        Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();
        WaterClip cache=new WaterClip();int checks=0;
        float x=MontroseHarbor.mapX(350),y=MontroseHarbor.mapY(1250);
        for(boolean union:new boolean[]{false,true})for(float zoom:new float[]{.65f,1.5f,2.5f}){
            RiverCamera c=Layout.focused(960,600,x+zoom*11,y-zoom*9,zoom,480,300).camera;
            float l=x-140,t=y-140,r=x+140,b=y+140;
            float[] saved=cache.get(g,c,l,t,r,b,union);
            float[] direct=ShoreOcclusion.region(g,c,l-96,t-96,r+96,b+96,Float.NEGATIVE_INFINITY,union);
            for(int sy=80;sy<520;sy+=13)for(int sx=80;sx<880;sx+=13){
                if(ShoreOcclusion.contains(saved,sx+.37f,sy+.23f)!=ShoreOcclusion.contains(direct,sx+.37f,sy+.23f))throw new AssertionError("retained shoreline changes coverage after camera transform");checks++;
            }
            if(cache.get(g,c,l,t,r,b,union)!=saved)throw new AssertionError("stationary water mask should reuse its projection");
        }
        System.out.println(checks+" retained water-mask coverage assertions passed");
    }
}
