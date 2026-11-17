package com.bargeboss.game;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
/** A translucent waterline must not accumulate opacity at segment joins. */
public final class WaterlineOpacityPixelTest {
    public static void main(String[] args) {
        BufferedImage image=new BufferedImage(320,80,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        float[] edge=new float[50];for(int i=0;i<25;i++){edge[i*2]=20+i*10;edge[i*2+1]=40;}
        VesselWaterline.contact(new DesktopPainter(g),edge,4);g.dispose();
        for(int x=22;x<258;x++) {
            int alpha=image.getRGB(x,40)>>>24;
            if(alpha!=72)throw new AssertionError("Waterline alpha differs at x="+x+": "+alpha);
        }
        System.out.println("236 single-composite waterline opacity probes pass, including every interior join");
    }
}
