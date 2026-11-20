package com.bargeboss.game;

import java.util.ArrayList;

/** Broad crest identity must survive zoom; native ripple triangulation is separate. */
public final class SeaWaveZoomTest {
    private static final class Trace implements Painter {
        int total;final RiverCamera camera;final ArrayList<float[]> patches=new ArrayList<float[]>();
        Trace(RiverCamera camera){this.camera=camera;}
        public void waterFoam(float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha){
            if(kind==5)total++;
            if(kind==5 && x>40 && x<920 && y>40 && y<560)
                patches.add(new float[]{camera.worldX(x,y),camera.worldY(x,y),alpha});
        }
        public void poly(float[] p,int c){} public void rect(float x,float y,float w,float h,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){} public void circle(float x,float y,float r,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
    }
    private static Trace draw(Game g,float scale){
        g.layout=Layout.focused(960,600,MontroseHarbor.mapX(2200),MontroseHarbor.mapY(1600),scale,480,300);
        Trace t=new Trace(g.layout.camera);SeaWaves.draw(g,new RiverPainter(t,g.layout.camera));return t;
    }
    public static void run(){
        Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();g.settings.adjustWaves(8);g.visualTicks=120;int checked=0;
        for(int bearing=0;bearing<24;bearing++){g.settings.turnWaves(1);
            for(float scale:new float[]{.11f,.2f,.4f,.65f,.99f,1f,1.01f,1.5f,2.5f}){
                Trace a=draw(g,scale),b=draw(g,scale*1.0001f);
                if(a.patches.isEmpty() || b.patches.isEmpty())throw new AssertionError("No broad waves at zoom "+scale);
                if(a.total>1024 || b.total>1024)throw new AssertionError("Unbounded crest submissions");
                for(float[] patch:a.patches){
                    if(patch[2]<.001f)continue;
                    float sx=b.camera.screenX(patch[0],patch[1]),sy=b.camera.screenY(patch[0],patch[1]);
                    if(sx<42 || sx>918 || sy<42 || sy>558)continue;
                    float[] match=null;
                    for(float[] candidate:b.patches)if(Math.abs(candidate[0]-patch[0])<.02f && Math.abs(candidate[1]-patch[1])<.02f){match=candidate;break;}
                    if(match==null)throw new AssertionError("Zoom replaced a visible broad crest at scale "+scale);
                    if(Math.abs(match[2]-patch[2])>.002f)throw new AssertionError("Broad crest opacity jumped");
                    checked++;
                }
            }
        }
        System.out.println(checked+" broad crest world anchors and continuous zoom opacity checks passed");
    }
    public static void main(String[] args){run();}
}
