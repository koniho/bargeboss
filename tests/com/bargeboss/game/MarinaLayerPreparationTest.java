package com.bargeboss.game;

import java.util.HashMap;

/** Host cache-key contract: hints carry display scale, never an already quantized density. */
public final class MarinaLayerPreparationTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static final class Capture implements Painter {
        final boolean prepare;final HashMap<Integer,Integer> densities=new HashMap<Integer,Integer>();
        long hash=1,terrainBytes;int preparedDraws,terrainDraws,lightDraws;
        Capture(boolean prepare){this.prepare=prepare;}
        private void add(int n){hash=hash*31+n;}
        private void v(float n){add(Float.floatToIntBits(n));}
        public boolean preparesLayers(){return prepare;}
        public void prepareLayer(StaticLayer layer,float scale){
            // Mirror the actual host's one quantization; this is the key eventually requested.
            int bit=StaticLayer.rasterScale(scale)==2?1:2;Integer previous=densities.get(layer.key());
            densities.put(layer.key(),(previous==null?0:previous)|bit);
        }
        public void layer(StaticLayer layer,float x,float y,float scale,float alpha){
            if(layer.backgroundSafe()){
                if(prepare){Integer requested=densities.get(layer.key());int bit=StaticLayer.rasterScale(scale)==2?1:2;
                    check(requested!=null && (requested&bit)!=0,"Actual display density prepared before layer draw: "+layer.key()+" scale "+scale);preparedDraws++;}
                if(layer.key()>=819000 && layer.key()<821000){
                    terrainDraws++;float density=StaticLayer.rasterScale(scale);
                    terrainBytes+=(long)(layer.width()*layer.height()*density*density*4);
                    check(layer.width()>0 && layer.height()>0 && layer.width()<=400 && layer.height()<=310,"Only nonempty bounded terrain chunks submitted");
                }else lightDraws++;
            }
            add(1);add(layer.key());v(x);v(y);v(scale);v(alpha);
        }
        public void pushClipOutside(float[] q){add(2);for(float f:q)v(f);}public void popClip(){add(3);}
        public void poly(float[] q,int c){add(4);add(c);for(float f:q)v(f);}
        public void line(float x,float y,float xx,float yy,float w,int c){add(5);v(x);v(y);v(xx);v(yy);v(w);add(c);}
        public void rect(float x,float y,float w,float h,int c){add(6);v(x);v(y);v(w);v(h);add(c);}
        public void circle(float x,float y,float r,int c){add(7);v(x);v(y);v(r);add(c);}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){add(8);add(a);add(f);v(x);v(y);v(w);v(h);v(r);v(alpha);}
        public void spriteBlend(int a,int f,int b,float x,float y,float w,float h,float r,float mix,float alpha){add(9);add(b);v(mix);sprite(a,f,x,y,w,h,r,alpha);}
        public void spriteAffine(int a,int f,float x,float y,float ax,float ay,float bx,float by,float alpha){add(10);add(a);add(f);v(x);v(y);v(ax);v(ay);v(bx);v(by);v(alpha);}
    }
    public static void run(){
        Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();
        float base=new Layout(1333,600).camera.scale;
        for(float scale:new float[]{1,1.599f,1.601f,2.2f,2.46f,2.8f})for(int tick:new int[]{0,87,340}){
            g.layout=Layout.focused(1333,600,MontroseHarbor.SLIP_X,MontroseHarbor.SLIP_Y,scale/base,666,320);g.visualTicks=tick;
            int ticks=g.ticks;float x=g.tug.x,y=g.tug.y;
            Capture hints=new Capture(true),plain=new Capture(false);MontroseView.draw(g,hints);MontroseView.draw(g,plain);
            check(hints.preparedDraws>0 && hints.terrainDraws>0 && hints.lightDraws>0,"Ground/pool/halo families exercised");
            check(hints.terrainBytes<StaticLayer.cacheBudgetBytes(),"Local docking terrain working set fits the native layer budget");
            check(hints.hash==plain.hash,"Preparation hints cannot alter visible drawing commands");
            check(ticks==g.ticks && g.visualTicks==tick && x==g.tug.x && y==g.tug.y,"Preparation/rendering never mutates gameplay");
        }
        System.out.println(checks+" marina layer preparation density, threshold, halo breathing and draw-purity assertions passed");
    }
    public static void main(String[] args){run();}
}
