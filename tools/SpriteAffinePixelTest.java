package com.bargeboss.game;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Map;

/** Math-based probes for source-cell affine axes, pivots, reflection and soft-mask blending. */
public final class SpriteAffinePixelTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static BufferedImage pattern(int frame) {
        BufferedImage source=new BufferedImage(40,32,BufferedImage.TYPE_INT_ARGB_PRE);
        BufferedImage image=source.getSubimage(4,4,32,24);
        int px=Math.round(SpriteAtlas.pivotX(SpriteAtlas.TUG,frame)*32),py=Math.round(SpriteAtlas.pivotY(SpriteAtlas.TUG,frame)*24);
        for(int y=0;y<24;y++)for(int x=0;x<32;x++) {
            int c=y<12?(x<16?0xffff0000:0xff00ff00):(x<16?0xff0000ff:0xffffff00);
            if(Math.abs(x-px)<=2 && Math.abs(y-py)<=2)c=0xffffffff;
            image.setRGB(x,y,c);
        }
        return image;
    }
    private static BufferedImage draw(int frame,float[] axes,float alpha,boolean blue) {
        BufferedImage output=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB_PRE);Graphics2D g=output.createGraphics();
        Painter p=new DesktopPainter(g);if(blue)p.rect(0,0,256,256,0xff0000ff);
        p.spriteAffine(SpriteAtlas.TUG,frame,128,128,axes[0],axes[1],axes[2],axes[3],alpha);g.dispose();return output;
    }
    private static void geometry(BufferedImage[] frames) {
        float[][] transforms={{112,0,0,96},{100,28,42,86},{104,14,28,-74},{-90,-12,34,82},{0,110,-90,0}};
        float[][] probes={{.1f,.1f},{.9f,.1f},{.1f,.9f},{.9f,.9f}};
        int[] colors={0xffff0000,0xff00ff00,0xff0000ff,0xffffff00};
        for(int frame:new int[]{0,1,7,16,31}) {
            frames[frame]=pattern(frame);float px=SpriteAtlas.pivotX(SpriteAtlas.TUG,frame),py=SpriteAtlas.pivotY(SpriteAtlas.TUG,frame);
            for(float[] a:transforms) {
                BufferedImage image=draw(frame,a,1,false);
                check(image.getRGB(128,128)==0xffffffff,"normalized source pivot remains fixed under shear and reflection");
                for(int i=0;i<probes.length;i++) {
                    float u=probes[i][0]-px,v=probes[i][1]-py;
                    int x=Math.round(128+a[0]*u+a[2]*v),y=Math.round(128+a[1]*u+a[3]*v);
                    check(image.getRGB(x,y)==colors[i],"source corner colors follow the supplied full-cell axes without double scaling");
                }
                float left=Float.MAX_VALUE,top=left,right=-left,bottom=-left;
                for(int corner=0;corner<4;corner++) {
                    float u=corner%2-px,v=corner/2-py,x=128+a[0]*u+a[2]*v,y=128+a[1]*u+a[3]*v;
                    left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);
                }
                int actualLeft=256,actualTop=256,actualRight=-1,actualBottom=-1;
                for(int y=0;y<256;y++)for(int x=0;x<256;x++)if((image.getRGB(x,y)>>>24)>0) {
                    actualLeft=Math.min(actualLeft,x);actualRight=Math.max(actualRight,x);
                    actualTop=Math.min(actualTop,y);actualBottom=Math.max(actualBottom,y);
                }
                check(Math.abs(actualLeft-left)<=2 && Math.abs(actualTop-top)<=2
                        && Math.abs(actualRight+1-right)<=2 && Math.abs(actualBottom+1-bottom)<=2,
                        "painted source extent matches all four affine-transformed corners");
            }
        }
    }
    private static void alphaAndFiltering(BufferedImage[] frames) {
        BufferedImage red=new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB_PRE);
        for(int y=0;y<8;y++)for(int x=0;x<8;x++)red.setRGB(x,y,0x80ff0000);
        frames[0]=red;float[] axes={80,14,20,-70};
        check(Math.abs((draw(0,axes,.5f,false).getRGB(128,128)>>>24)-64)<=1,"source and global alpha multiply exactly once");
        int over=draw(0,axes,.5f,true).getRGB(128,128);
        check((over>>>24)==255 && Math.abs(((over>>16)&255)-64)<=1 && Math.abs((over&255)-191)<=1,
                "affine masks use source-over compositing on existing scene pixels");
        BufferedImage ramp=new BufferedImage(2,2,BufferedImage.TYPE_INT_ARGB_PRE);
        ramp.setRGB(1,0,0xff000000);ramp.setRGB(1,1,0xff000000);frames[0]=ramp;
        BufferedImage filtered=draw(0,new float[]{8,0,0,8},1,false);
        float u=SpriteAtlas.pivotX(SpriteAtlas.TUG,0)+(127.5f-128)/8;
        int expected=Math.round(Math.max(0,Math.min(1,u*2-.5f))*255),actual=filtered.getRGB(127,128)>>>24;
        check(expected>20 && expected<230 && Math.abs(actual-expected)<=2,"bilinear interpolation keeps soft alpha ramps between source samples");
        check(draw(0,axes,0,false).getRGB(128,128)==0,"zero opacity contributes no pixels");
        check(draw(0,new float[]{80,0,160,0},1,false).getRGB(128,128)==0,"collapsed projection contributes no pixels");
        BufferedImage output=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB_PRE);Graphics2D g=output.createGraphics();
        g.translate(3,7);g.scale(1.2,1.3);g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        AffineTransform before=g.getTransform();new DesktopPainter(g).spriteAffine(0,0,20,20,12,4,3,9,.4f);
        check(g.getTransform().equals(before) && g.getComposite().equals(AlphaComposite.Src)
                && g.getRenderingHint(RenderingHints.KEY_INTERPOLATION)==RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
                "affine drawing restores the host transform composite and filtering state");g.dispose();
    }
    @SuppressWarnings("unchecked") public static void main(String[] args) throws Exception {
        Field field=DesktopPainter.class.getDeclaredField("ATLASES");field.setAccessible(true);
        Map<Integer,BufferedImage[]> cache=(Map<Integer,BufferedImage[]>)field.get(null);
        BufferedImage[] previous=cache.get(SpriteAtlas.TUG),frames=new BufferedImage[SpriteAtlas.count(SpriteAtlas.TUG)];
        try {cache.put(SpriteAtlas.TUG,frames);geometry(frames);alphaAndFiltering(frames);}
        finally {if(previous==null)cache.remove(SpriteAtlas.TUG);else cache.put(SpriteAtlas.TUG,previous);}
        System.out.println(checks+" affine sprite corner, pivot, reflection and soft alpha assertions passed");
    }
}
