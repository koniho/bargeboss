package com.bargeboss.game;

/** Solid land coverage is a surface test, independent of bow heading and travel direction. */
public final class ShoreOcclusionTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void run() {
        Game root=new Game();root.startTutorial();Game g=root.activeGame();
        for(int bank=0;bank<4;bank++)for(float zoom:new float[]{.8f,2.2f}) {
            Body b=g.tug;b.x=bank==0?-1610:bank==1?TrainingChannel.BASIN_LEFT+16:bank==2?TrainingChannel.MIN-16:-1610;
            b.y=bank==0?TrainingChannel.BASIN_BOTTOM-16:bank==3?TrainingChannel.BASIN_TOP+16:600;
            g.layout=Layout.focused(960,600,b.x,b.y,zoom,480,340);RiverCamera c=g.layout.camera;
            float[] clip=ShoreOcclusion.vessel(g,c,b,false),surface=ShoreOcclusion.vessel(g,c,b,true);
            float rise=GroundArt.LAND_RISE*c.scale,depth=c.screenY(b.x,b.y);
            for(int dy=-100;dy<=100;dy+=7)for(int dx=-100;dx<=100;dx+=7) {
                float wx=b.x+dx+.31f,wy=b.y+dy+.47f,sx=c.screenX(wx,wy),sy=c.screenY(wx,wy)-rise;
                boolean land=!TrainingChannel.contains(wx,wy),front=sy+rise>=depth;
                check(ShoreOcclusion.contains(clip,sx,sy)==(land && front),"entire foreground land surface occludes; water and background land do not");
                check(ShoreOcclusion.contains(surface,sx,sy)==land,"water-plane shadows/reflections cannot spill onto either bank");
            }
            for(int heading=0;heading<32;heading++) {
                b.angle=heading*(float)Math.PI/16;b.vx=heading%2==0?90:-90;b.vy=-b.vx;b.heave=heading%3-1;
                float[] turned=ShoreOcclusion.vessel(g,c,b,false);
                check(java.util.Arrays.equals(clip,turned),"turning, reversing and heave do not move the solid shore");
            }
        }
        SceneryOcclusion scene=new SceneryOcclusion(new Parity());
        float[] cover={0,0,600,0,600,600,0,0,600,600,0,600};
        scene.pushClipOutside(cover);scene.sprite(SpriteAtlas.TUG,0,300,300,100,100,0,1);scene.popClip();
        check(SceneryOcclusion.polygonOpacity(scene,new float[]{100,100,500,100,500,500,100,500})==1,"hull pixels hidden by solid ground cannot cause a building to fade");
        scene.sprite(SpriteAtlas.TUG,0,300,300,100,100,0,1);
        check(SceneryOcclusion.polygonOpacity(scene,new float[]{100,100,500,100,500,500,100,500})<.3f,"restored clip allows a visible vessel to trigger building fading");
        System.out.println(checks+" foreground shore surface, direction and fade-mask assertions passed");
    }
}
