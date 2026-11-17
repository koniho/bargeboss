package com.bargeboss.game;

/** Crest batches must preserve scope, projection, clipping and exception cleanup. */
public final class FoamBatchTest {
    private static final class Capture implements Painter {
        int begin,end,clips,patches;boolean inside,fail;
        public void beginWaterFoamBatch(){if(inside)throw new AssertionError("nested batch");inside=true;begin++;}
        public void endWaterFoamBatch(){if(!inside)throw new AssertionError("unbalanced batch");inside=false;end++;}
        public void pushClipOutside(float[] triangles){if(inside)throw new AssertionError("clip changed inside crest pass");clips++;}
        public void popClip(){if(inside)throw new AssertionError("batch must flush before clip restore");clips--;}
        public void waterFoam(float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha){
            if(kind!=5 || !inside || !Float.isFinite(phase) || alpha<=0)throw new AssertionError("invalid crest batch");
            patches++;if(fail)throw new IllegalStateException("injected draw failure");
        }
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
        public void rect(float x,float y,float w,float h,int c){}
        public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}
        public void poly(float[] xy,int c){}
    }
    public static void run(){
        Game g=new Game();g.challengeId=Challenges.INDIANA;g.reset();
        g.water.age[0]=45;g.water.strength[0]=.8f;g.water.x[0]=600;g.water.y[0]=-500;
        for(int pass=0;pass<2;pass++){
            Capture c=new Capture();c.fail=pass==1;
            Painter wrapped=new OpacityPainter(new ViewportPainter(c,1320,600),.8f);
            try{g.water.draw(g,new RiverPainter(wrapped,g.layout.camera));if(c.fail)throw new AssertionError("failure fixture missed");}
            catch(IllegalStateException expected){if(!c.fail)throw expected;}
            if(c.begin!=1 || c.end!=1 || c.clips!=0 || c.inside || c.patches==0)throw new AssertionError("crest scope cleanup");
        }
        System.out.println("Foam batch projection, clip ordering and exception cleanup pass");
    }
}
