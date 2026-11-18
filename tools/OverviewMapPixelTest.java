package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
/** A completed contour is faded once: joins cannot become more opaque than strokes. */
public final class OverviewMapPixelTest {
    public static void main(String[] args){
        int ink=0;
        for(int stage=0;stage<3;stage++){
            OverviewMap.Chart chart=new OverviewMap.Chart(stage);
            BufferedImage image=new BufferedImage(368,328,BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            DesktopPainter p=new DesktopPainter(g);p.beginFrame();p.layer(chart,0,0,2,.53f);g.dispose();
            int maximum=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
                int alpha=image.getRGB(x,y)>>>24;maximum=Math.max(maximum,alpha);if(alpha>0)ink++;
                if(alpha>136)throw new AssertionError("overlapping chart joints accumulated opacity: "+alpha);
            }
            if(maximum<130)throw new AssertionError("chart did not render");
        }
        System.out.println(ink+" outline pixels checked: joints never exceed group opacity");
    }
}
