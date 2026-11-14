package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Compare real antialiased raster output with and without submission culling. */
public final class ViewportPixelTest {
    private static final int W=173,H=127;
    private static BufferedImage draw(boolean cull) {
        BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D graphics=image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Painter p=new DesktopPainter(graphics);if(cull)p=new ViewportPainter(p,W,H);
        p.rect(0,0,W,H,0xff18242d);
        for(int i=0;i<100;i++) {
            float x=(i*71%347)-87,y=(i*53%253)-64;
            int color=((i%5==0?0:80)<<24)|0xc5be7b;
            p.circle(x,y,2+i%17,color);p.line(x-7,y-9,x+11,y+13,1+i%8,color);
            p.rect(x-4,y-3,8,6,color);p.poly(new float[]{x-11,y+7,x+8,y-12,x+18,y+4},color);
        }
        for(int i=0;i<32;i++) {
            float x=(i*41%300)-63,y=(i*29%220)-47,r=(float)(i*Math.PI/16);
            p.sprite(SpriteAtlas.TUG,i,x,y,49,49,r,.7f);
            p.spriteBlend(SpriteAtlas.BARGE,i,(i+1)%32,x+7,y+3,54,54,-r,.4f,.6f);
        }
        // Thick strokes / circles with centers outside the view still reach its edge.
        p.line(-9,-10,-9,H+10,22,0x80ff7766);p.circle(W+6,H/2f,10,0x8899ffdd);
        graphics.dispose();return image;
    }
    public static void main(String[] args) {
        BufferedImage raw=draw(false),culled=draw(true);int changed=0;
        for(int y=0;y<H;y++)for(int x=0;x<W;x++)if(raw.getRGB(x,y)!=culled.getRGB(x,y))changed++;
        if(changed!=0)throw new AssertionError("Viewport rejection changed "+changed+" visible pixels");
        System.out.println("Viewport culling: "+W*H+" pixels identical, including rotated/blended registered sprites and antialiased edges");
    }
}
