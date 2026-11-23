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
    private static boolean navigable(int stage,float x,float y){
        return stage==0?TrainingChannel.contains(x,y):stage==1?MontroseHarbor.contains(x,y):
                stage==2?IndianaHarbor.contains(x,y):CalumetRiver.contains(x,y);
    }
    private static boolean nearContour(float[][] paths,float x,float y){
        for(float[] q:paths)for(int i=0;i<q.length;i+=2){int j=(i+2)%q.length;
            float dx=q[j]-q[i],dy=q[j+1]-q[i+1],length=dx*dx+dy*dy;
            float t=length==0?0:Math.max(0,Math.min(1,((x-q[i])*dx+(y-q[i+1])*dy)/length));
            float ex=x-q[i]-t*dx,ey=y-q[i+1]-t*dy;if(ex*ex+ey*ey<.75f*.75f)return true;
        }return false;
    }
    private static void invertedFill(){
        int checked=0;
        for(int stage=0;stage<4;stage++){
            long start=System.nanoTime();
            OverviewMap.Chart chart=new OverviewMap.Chart(stage);
            long buildNanos=System.nanoTime()-start;
            BufferedImage image=new BufferedImage(736,736,BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics=image.createGraphics();graphics.scale(4,4);
            try{chart.paint(new DesktopPainter(graphics));}finally{graphics.dispose();}
            int wet=0,dry=0;
            float pad=1.5f/chart.scale;
            for(int py=12;py<chart.y(stage==1?chart.maxY:chart.minX)*4-12;py+=7)
                for(int px=12;px<chart.x(stage==1?chart.maxX:chart.maxY)*4-12;px+=7){
                    float x=chart.maxX-((py+.5f)/4-OverviewMap.EDGE)/chart.scale;
                    float y=chart.minY+((px+.5f)/4-OverviewMap.EDGE)/chart.scale;
                    if(stage==1){x=chart.minX+((px+.5f)/4-OverviewMap.EDGE)/chart.scale;y=chart.minY+((py+.5f)/4-OverviewMap.EDGE)/chart.scale;}
                    boolean water=navigable(stage,x,y),edge=false;
                    for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)
                        if(navigable(stage,x+dx*pad,y+dy*pad)!=water)edge=true;
                    if(edge)continue;
                    int alpha=image.getRGB(px,py)>>>24;
                    if(alpha>113)continue; // Opaque chart contours are independent of the fill.
                    // Sparse world probes can miss a narrow dock tip. Exclude
                    // raster-edge samples next to the registered chart contours.
                    if(water && alpha!=48 && (nearContour(chart.docks,(px+.5f)/4,(py+.5f)/4)
                            || nearContour(chart.water,(px+.5f)/4,(py+.5f)/4)))continue;
                    if(water){wet++;if(alpha!=48)throw new AssertionError("navigable water backing incorrect stage="+stage+" alpha="+alpha+" world="+x+","+y);}
                    else {dry++;if(alpha<112)throw new AssertionError("land/dock fill missing stage="+stage+" world="+x+","+y);}
                }
            if(wet==0 || dry==0)throw new AssertionError("inverted fill needs both water and land samples");
            checked+=wet+dry;
            System.out.println("chart "+stage+": "+chart.navigableHoles.length*4+" clip bytes, build "+buildNanos/1000000+" ms; water="+wet+" land="+dry);
        }
        System.out.println(checked+" interior pixels checked: faint dark water, shaded land/docks");
    }
    public static void main(String[] args) {
        invertedFill();
        groupOpacity();
        int pixels=0;
        for(int stage=0;stage<4;stage++)for(int side:new int[]{GameSettings.LEFT,GameSettings.RIGHT})for(float level:new float[]{0,1f/3,1}) {
            Game g=new Game();
            if(stage==0)ReplayInput.controlFixture(g);
            else {g.challengeId=stage==1?Challenges.MONTROSE:stage==2?Challenges.INDIANA:0;g.reset();g.montroseArrival.clear();g.dialogue.close(g);}
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
            float playerX=OverviewMap.mapX(g,g.tug.x,g.tug.y),playerY=OverviewMap.mapY(g,g.tug.x,g.tug.y);
            if(playerX>=1 && playerY>=1 && playerX<map.w-1 && playerY<map.h-1) {
                int player=image.getRGB(Math.round(map.x+playerX),Math.round(map.y+playerY));
                if((player&0xffffff)!=(OverviewMap.PLAYER&0xffffff))throw new AssertionError("player pixel must match bounded following projection");
            }
            // At full fit, top/left edges are the real boundary. Cropped edges
            // retain the faint viewport frame when they are not world limits.
            int top=image.getRGB((int)map.x+2,(int)map.y)>>>24;
            int left=image.getRGB((int)map.x,(int)map.y+2)>>>24;
            if(level==0 && (top<70 || left<70))throw new AssertionError("full-area fit must align top-left play boundary with viewport");
            OverviewMap.Chart chart=OverviewMap.chart(g);
            float right=OverviewMap.mapX(g,stage==1?chart.maxX:chart.maxY),bottom=OverviewMap.mapY(g,stage==1?chart.maxY:chart.minX);
            if(level==0 && right<map.w-8) {
                int edge=image.getRGB((int)(map.x+map.w-1),(int)map.y+2)>>>24;
                if(edge==0 || edge>=70)throw new AssertionError("unused viewport edge must remain faint");
            } else if(level==0 && bottom<map.h-8) {
                int edge=image.getRGB((int)map.x+2,(int)(map.y+map.h-1))>>>24;
                if(edge==0 || edge>=70)throw new AssertionError("unused viewport edge must remain faint");
            }
            int thumb=image.getRGB(Math.round(slider.x+24+(slider.w-48)*level),Math.round(slider.centerY()));
            if(g.overview.active || (thumb>>>24)<200)throw new AssertionError("idle slider must retain its relative indicator");
        }
        System.out.println(pixels+" overview pixels clipped; bounded player following and released zoom indicator visible on both HUD sides");
    }
}
