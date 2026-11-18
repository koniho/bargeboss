package com.bargeboss.game;
public final class RenderExclusionsTest {
    private static final class Count implements Painter {
        int sprites,smoke,glows;
        public void smokePuff(float x,float y,float r,float phase,int color){smoke++;}
        public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){glows++;}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){sprites++;}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
        public void rect(float x,float y,float w,float h,int color){}
        public void circle(float x,float y,float r,int color){}
        public void poly(float[] xy,int color){}
        public void line(float x,float y,float xx,float yy,float w,int color){}
    }
    private static Count draw(Game g,int mask){g.renderExclusions=mask;Count p=new Count();FactoryArt.draw(g,p,0,0,0);return p;}
    private static void check(boolean value){if(!value)throw new AssertionError("Render exclusion isolation failed");}
    public static void run(){
        Game g=new Game();g.reset();check(g.renderExclusions==0);
        g.layout=Layout.renderTile(960,600,new RiverCamera(480+RiverCamera.Q*288,450-RiverCamera.Q*RiverCamera.DEPTH*312,1));
        Count baseline=draw(g,0);check(baseline.sprites==1&&baseline.smoke>0&&baseline.glows>0);
        Count art=draw(g,1);check(art.sprites==0&&art.smoke==baseline.smoke&&art.glows==baseline.glows);
        Count smoke=draw(g,2);check(smoke.smoke==0&&smoke.sprites==1&&smoke.glows==baseline.glows);
        Count light=draw(g,4);check(light.glows==0&&light.sprites==1&&light.smoke==baseline.smoke);
        Count all=draw(g,7);check(all.sprites==0&&all.smoke==0&&all.glows==0);
        Count restore=draw(g,0);check(restore.sprites==baseline.sprites&&restore.smoke==baseline.smoke&&restore.glows==baseline.glows);
        System.out.println("Factory artwork/smoke/light exclusions isolated and restored");
    }
}
