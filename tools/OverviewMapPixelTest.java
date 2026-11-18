package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Actual cached-layer clipping and idle slider pixels, on both HUD sides. */
public final class OverviewMapPixelTest {
    private static void groupOpacity(){
        int ink=0;
        for(int stage=0;stage<4;stage++){
            OverviewMap.Chart chart=new OverviewMap.Chart(stage);
            BufferedImage image=new BufferedImage((int)chart.width()*2,(int)chart.height()*2,BufferedImage.TYPE_INT_ARGB_PRE);
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
    public static void main(String[] args) {
        groupOpacity();
        int pixels=0;
        for(int stage=0;stage<4;stage++)for(int side:new int[]{GameSettings.LEFT,GameSettings.RIGHT})for(float level:new float[]{0,1f/3,1}) {
            Game g=new Game();
            if(stage==0)ReplayInput.controlFixture(g);
            else {g.challengeId=stage==1?Challenges.MONTROSE:stage==2?Challenges.INDIANA:0;g.reset();}
            g.settings.setJoystickSide(side);
            Layout.Box slider=OverviewMap.sliderBounds(g),map=OverviewMap.bounds(g);
            ReplayInput.tap(g,slider.x+24+(slider.w-48)*level,slider.centerY());
            BufferedImage image=new BufferedImage(960,600,BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics=image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            try {OverviewMap.draw(g,new DesktopPainter(graphics));} finally {graphics.dispose();}
            for(int y=0;y<600;y++)for(int x=0;x<960;x++)if((image.getRGB(x,y)>>>24)>0) {
                pixels++;
                if(!map.contains(x+.5f,y+.5f) && !slider.contains(x+.5f,y+.5f))
                    throw new AssertionError("overview escaped clip at "+x+","+y+" stage="+stage+" zoom="+level);
            }
            int player=image.getRGB(Math.round(map.centerX()),Math.round(map.centerY()));
            if((player&0xffffff)!=(OverviewMap.PLAYER&0xffffff))throw new AssertionError("player pixel not centered");
            if(level==0) {
                int edge=image.getRGB(Math.round(map.centerX()),(int)map.y)>>>24;
                if(edge==0 || edge>=70)throw new AssertionError("viewport border must be visible but fainter than play boundary");
            }
            int thumb=image.getRGB(Math.round(slider.x+24+(slider.w-48)*level),Math.round(slider.centerY()));
            if(g.overview.active || (thumb>>>24)<200)throw new AssertionError("idle slider must retain its relative indicator");
        }
        System.out.println(pixels+" overview pixels clipped; player centered and released zoom indicator visible on both HUD sides");
    }
}
