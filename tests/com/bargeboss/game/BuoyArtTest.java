package com.bargeboss.game;

/** Buoy water-plane and effect registration contracts shared by every host. */
public final class BuoyArtTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static boolean near(float a,float b){return Math.abs(a-b)<.0001f;}
    public static void run(){
        Game root=new Game();root.startTutorial();Game g=root.activeGame();
        float wx=Tutorial.DRIVE_X[0],wy=Tutorial.DRIVE_Y[0];
        g.layout=Layout.focused(960,600,wx,wy,2,480,350);
        float minRoll=10,maxRoll=-10,minY=10000,maxY=-10000;
        for(int frame=0;frame<BuoyArt.COUNT;frame++){
            check(SpriteAtlas.file(SpriteAtlas.BUOY,frame).equals(BuoyArt.file(frame)),"variant selects its own original PNG");
            check(SpriteAtlas.sourceLeft(SpriteAtlas.BUOY,frame)==0 && SpriteAtlas.sourceRight(SpriteAtlas.BUOY,frame)==1 && SpriteAtlas.sourceTop(SpriteAtlas.BUOY,frame)==0 && SpriteAtlas.sourceBottom(SpriteAtlas.BUOY,frame)==1,"full source without unintended atlas crop");
            check(BuoyArt.smokeCount(frame)==0 && BuoyArt.tireCount(frame)==0,"nonexistent smoke/tire anchors explicitly omitted");
            for(int tick=0;tick<240;tick+=3){
                g.visualTicks=tick;BuoyArt.Pose a=BuoyArt.pose(g,wx,wy,48),b=BuoyArt.pose(g,wx,wy,48);
                check(a.x==b.x && a.y==b.y && a.roll==b.roll && a.waterY==b.waterY,"repeat rendering has deterministic pose");
                check(Math.abs(a.roll)<=BuoyArt.MAX_ROLL && Float.isFinite(a.waterY),"bounded wave roll and surface height");
                check(near(a.pointX(frame,BuoyArt.pivotX(frame),BuoyArt.pivotY(frame)),a.x) && near(a.pointY(frame,BuoyArt.pivotX(frame),BuoyArt.pivotY(frame)),a.y),"waterline pivot never orbits during roll");
                float dx=a.pointX(frame,BuoyArt.lampX(frame),BuoyArt.lampY(frame))-a.x,dy=a.pointY(frame,BuoyArt.lampX(frame),BuoyArt.lampY(frame))-a.y;
                float expected=Physics.length(BuoyArt.lampX(frame)-BuoyArt.pivotX(frame),BuoyArt.lampY(frame)-BuoyArt.pivotY(frame))*a.size;
                check(Math.abs(Physics.length(dx,dy)-expected)<.001f,"lamp rigidly follows rotated sprite");
                Capture capture=new Capture();BuoyArt.hull(capture,frame,a);
                check(capture.push==1 && capture.pop==1 && capture.frame==frame,"sprite waterplane clipping always balances");
                check(capture.top==a.waterY && capture.secondTop==a.waterY,"submergence is horizontal despite rolled sprite");
                check(near(capture.ax,a.size*a.cos) && near(capture.ay,a.size*a.sin),"sprite uses same rigid transform as lamp");
                minRoll=Math.min(minRoll,a.roll);maxRoll=Math.max(maxRoll,a.roll);minY=Math.min(minY,a.y);maxY=Math.max(maxY,a.y);
            }
        }
        check(maxRoll-minRoll>.02f && maxY-minY>1,"ambient waves visibly sway and bob every buoy");
        // Emit real wake packets, rather than injecting anonymous packets ignored by height(null).
        g.tug.x=wx;g.tug.y=wy;g.tug.vx=45;g.tug.vy=0;g.tug.wakeTrailTicks=7;g.water.step(g);
        check(g.water.emitted>0 && g.water.energy(wx,wy,null)>0,"nearby watercraft wake reaches marker wave field");
        int tick=g.ticks,emitted=g.water.emitted;BuoyArt.Pose wake=BuoyArt.pose(g,wx,wy,48);
        Game calm=new Game();calm.layout=g.layout;calm.visualTicks=g.visualTicks;BuoyArt.Pose idle=BuoyArt.pose(calm,wx,wy,48);
        check(wake.y!=idle.y && wake.roll!=idle.roll,"nearby wake changes both heave and roll");
        Capture c=new Capture();BuoyArt.draw(g,c,wx,wy,48,BuoyArt.YELLOW,NavigationLights.YELLOW,1,1);BuoyArt.shadows(g,c,wx,wy,48,BuoyArt.YELLOW);
        check(c.push==c.pop && g.ticks==tick && g.water.emitted==emitted,"all depth/surface clips balance without gameplay mutation");
        check(!ShadowArt.atlas(SpriteAtlas.FOAM) && !ShadowArt.atlas(SpriteAtlas.BUOY) && ShadowArt.source(ShadowArt.BUOY)==SpriteAtlas.BUOY,"new shadows skip foam and colored source IDs");
        System.out.println(checks+" buoy sprite, waterline, wave response and anchor assertions passed");
    }
    private static final class Capture implements Painter {
        int push,pop,frame;float top,secondTop,ax,ay;
        public void pushClipOutside(float[] t){push++;if(t.length>=12){top=t[1];secondTop=t[3];}}
        public void popClip(){pop++;}
        public void rect(float x,float y,float w,float h,int c){}
        public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}
        public void poly(float[] xy,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
        public void spriteAffine(int a,int f,float x,float y,float ax,float ay,float bx,float by,float alpha){frame=f;this.ax=ax;this.ay=ay;}
    }
    public static void main(String[] args){run();}
}
