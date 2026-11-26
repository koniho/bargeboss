package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Real shared-renderer captures of the captain picker, title shortcut and HUD portrait states. */
public final class PlayerPortraitReview {
    private static void capture(Game g,int w,int h,String name) throws Exception {
        BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(g,new DesktopPainter(p));p.dispose();
        ImageIO.write(image,"png",new File("out/captain-"+name+".png"));
    }
    public static void main(String[] args) throws Exception {
        new File("out").mkdirs();
        for(int[] size:new int[][]{{960,600},{1243,600}}){
            int w=size[0],h=size[1];String s="-"+w;
            Game g=new Game();g.configureCanvas(w,h);capture(g,w,h,"title-new"+s);
            ReplayInput.tap(g,Layout.START);g.captainChoice=1;capture(g,w,h,"picker"+s);
            ReplayInput.tap(g,Layout.CAPTAIN_CONFIRM);
            Game t=g.activeGame();for(int i=0;i<200;i++)t.step();capture(g,w,h,"tutorial"+s);
            g=new Game();g.configureCanvas(w,h);g.settings.setCaptain(2);g.mode=Game.TITLE;capture(g,w,h,"title-chosen"+s);
            for(int c=0;c<PlayerPortrait.COUNT;c++){
                g=new Game();g.configureCanvas(w,h);g.settings.setCaptain(c);g.reset();
                for(int i=0;i<90;i++)g.step();
                g.visualTicks=40;capture(g,w,h,"hud-idle-"+c+s);
                g.visualTicks=0;capture(g,w,h,"hud-blink-"+c+s);
                g.portrait.ackLine=0;g.portrait.ackTicks=40;g.visualTicks=5;capture(g,w,h,"hud-talk-"+c+s);
                g.portrait.ackTicks=0;PlayerPortrait.wince(g);g.portrait.winceTicks=30;capture(g,w,h,"hud-wince-"+c+s);
                g.portrait.winceTicks=0;g.portrait.mad();g.portrait.madTicks=44;capture(g,w,h,"hud-mad-"+c+s);
            }
        }
    }
}
