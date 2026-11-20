package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual shared UI for catalog growth and persistent unloaded berth occupancy. */
public final class BerthSwapReview {
    private static void capture(Game g,String name)throws Exception {
        BufferedImage image=new BufferedImage((int)g.layout.width,(int)g.layout.height,BufferedImage.TYPE_INT_RGB);
        Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(g,new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File("out/"+name+".png"));
    }
    public static void main(String[] args)throws Exception {
        new File("out").mkdirs();
        for(int width:new int[]{960,1333}){
            Game g=new Game();g.configureCanvas(width,600);g.mode=Game.CHALLENGES;g.challengePage=1;
            capture(g,"catalog-locked-page-2-"+width);
            g.restoreProgress("BB1|2|3,1000|3,2000|3,3000");g.selectChallenge(3);g.mode=Game.CHALLENGES;
            capture(g,"catalog-page-2-"+width);
            g=new Game();g.configureCanvas(width,600);g.resetPracticeYard();
            for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}
            Body empty=new Body(1,2,g.dockX(2),g.dockY(2),(float)Math.PI/2);g.cargo[0]=empty;
            Mooring.attach(empty);empty.mooringTicks=Mooring.ALIGN_TICKS;empty.unloadingTicks=Game.UNLOAD_TICKS-1;
            g.tug.x=330;g.tug.y=g.dockY(2)+130;g.step();
            if(!empty.deliveryRecorded || !empty.active || !empty.lineAttached)throw new AssertionError("Empty barge must stay secured after unloading");
            for(int t=0;t<180;t++)g.step();
            g.cargo[1]=new Body(2,0,350,650,-(float)Math.PI/2);
            g.layout=Layout.focused(width,600,270,g.dockY(2)+50,1.65f,width*.5f,330);
            capture(g,"shared-slip-empty-"+width);
            g=new Game();g.configureCanvas(width,600);g.challengeId=3;g.reset();
            g.layout=Layout.focused(width,600,280,380,1.15f,width*.5f,310);
            capture(g,"deep-channel-berths-"+width);
        }
        System.out.println("Catalog second-page and persistent empty-slip views exported at 960/1333 widths");
    }
}
