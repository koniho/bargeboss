package com.bargeboss.game;
import java.util.*;
/** Changing counters retain words/digits while preserving exact typographic layout. */
public final class ChangingNumberTextTest {
    private static final class Capture implements Painter {
        final ArrayList<float[]> polygons=new ArrayList<float[]>();final HashSet<Integer> keys=new HashSet<Integer>();
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int b,float x,float y,float w,float h,float r,float blend,float alpha){}
        public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}
        public void poly(float[] xy,int c){polygons.add(xy.clone());}
        public void layer(StaticLayer layer,float x,float y,float s,float a){keys.add(layer.key());layer.draw(this,x,y,s,a);}
    }
    public static void run(){
        Capture digits=new Capture();int checks=0;
        for(int value=0;value<=100;value++){
            String text="BUOY 2 / 2 - "+value+"% / CROSS THROUGH THE MIDDLE";
            Capture whole=new Capture();digits.polygons.clear();float size=Math.min(11,396/Lettering.width(text,1));
            Lettering.draw(whole,text,18,89,size,0xffabcdee);Lettering.changingNumbers(digits,text,18,89,size,0xffabcdee);
            if(whole.polygons.size()!=digits.polygons.size())throw new AssertionError("counter lost glyph faces");
            for(int i=0;i<whole.polygons.size();i++){
                float[] a=whole.polygons.get(i),b=digits.polygons.get(i);
                for(int j=0;j<a.length;j++){if(Math.abs(a[j]-b[j])>.001f)throw new AssertionError("counter glyph moved");checks++;}
            }
        }
        if(digits.keys.size()!=14)throw new AssertionError("counter cache is unbounded: "+digits.keys.size());
        System.out.println(checks+" counter glyph coordinates preserved with14 reusable word/digit layers");
    }
    public static void main(String[] args){run();}
}
