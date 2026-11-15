package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual raster guard for the disjoint-polygon primitive, including AA and alpha ordering. */
public final class WaterBatchReview {
    static BufferedImage render(Game g,boolean expand) {
        BufferedImage image=new BufferedImage((int)g.layout.width,(int)g.layout.height,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        final Painter nativePainter=new DesktopPainter(graphics);
        Painter p=expand?new Expanded(nativePainter):nativePainter;
        WaveView.draw(g,p);graphics.dispose();return image;
    }
    static final class Expanded implements Painter {
        final Painter p;Expanded(Painter p){this.p=p;}
        public boolean prefersPolygonBatches(){return false;}
        public void poly(float[] xy,int c){p.poly(xy,c);}
        public void rect(float x,float y,float w,float h,int c){p.rect(x,y,w,h,c);}
        public void circle(float x,float y,float r,int c){p.circle(x,y,r,c);}
        public void line(float x,float y,float xx,float yy,float w,int c){p.line(x,y,xx,yy,w,c);}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){p.sprite(a,f,x,y,w,h,r,alpha);}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){p.spriteBlend(a,f,ff,x,y,w,h,r,mix,alpha);}
    }
    public static void main(String[] args)throws Exception {
        int pixels=0;new File("out/water-batch-review").mkdirs();
        for(int width:new int[]{960,1333,1920})for(String scene:ScenePerformanceBenchmark.SCENES){
            Game g=ScenePerformanceBenchmark.fixture(width,scene);int tick=g.visualTicks;
            for(int offset:new int[]{0,29,107}){
                g.visualTicks=tick+offset;BufferedImage a=render(g,true),b=render(g,false);
                for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++){
                    if(a.getRGB(x,y)!=b.getRGB(x,y))throw new AssertionError("Batch changed AA/compositing at "+scene+" "+width+" "+x+","+y);
                    pixels++;
                }
                if(offset==0)ImageIO.write(b,"png",new File("out/water-batch-review/"+width+"-"+scene+".png"));
            }
        }
        System.out.println(pixels+" water batch AA/compositing pixels are identical to individual polygon fills");
    }
}
