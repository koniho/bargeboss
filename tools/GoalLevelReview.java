package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual shared title, objective HUD, lock deadlines and goal-result review. */
public final class GoalLevelReview {
    private static Game fixture(int id){
        Game g=new Game();g.configureCanvas(1333,600);g.challengeId=id;g.reset();
        for(int i=0;i<240;i++)g.step();
        return g;
    }
    private static void capture(Game g,String name)throws Exception{
        BufferedImage image=new BufferedImage((int)g.layout.width,(int)g.layout.height,BufferedImage.TYPE_INT_RGB);
        Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(g,new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File("out/"+name+".png"));
    }
    public static void main(String[] args)throws Exception{
        new File("out").mkdirs();
        Game g=fixture(0);g.mode=Game.TITLE;capture(g,"goal-level-title");
        g.restoreProgress("BB1|0|3,7000|3,8000|3,9000");g.mode=Game.CHALLENGES;capture(g,"goal-level-selection");
        for(int id=0;id<2;id++){
            g=fixture(id);g.visualTicks=15;capture(g,"goal-level-"+(id+1)+"-start");
            g.delivered=1;capture(g,"goal-level-"+(id+1)+"-progress");
        }
        g=fixture(2);g.tug.x=300;g.tug.y=440;
        g.layout=Layout.focused(1333,600,300,200,1.2f,660,340);
        g.lock.step(g);
        g.lock.phase=Lock.ENTRY;g.lock.ticks=Lock.ENTRY_TICKS-12*60;capture(g,"goal-lock-entry");
        g.tug.y=270;int slot=0;
        for(Body b:g.cargo)if(b!=null && b.active){b.x=260+slot*90;b.y=160;b.angle=-(float)Math.PI/2;slot++;}
        g.lock.phase=Lock.FILLING;g.lock.ticks=Lock.CYCLE_TICKS/2;capture(g,"goal-lock-filling");
        g.tug.y=-100;slot=0;
        for(Body b:g.cargo)if(b!=null && b.active){b.y=slot++==0?-90:85;}
        g.lock.phase=Lock.EXIT;g.lock.ticks=Lock.EXIT_TICKS-8*60;capture(g,"goal-lock-exit");
        for(Body b:g.cargo)if(b!=null && b.active)b.y=-120;
        g.lock.phase=Lock.COMPLETE;g.delivered=g.quota();g.earnedStars=3;g.mode=Game.FINISH;capture(g,"goal-lock-complete");
        for(boolean exit:new boolean[]{false,true}) {
            g=fixture(2);g.tug.x=300;g.tug.y=440;g.lock.step(g);
            g.lock.phase=exit?Lock.EXIT:Lock.ENTRY;
            g.lock.ticks=(exit?Lock.EXIT_TICKS:Lock.ENTRY_TICKS)-1;
            g.step();
            if(!g.lock.failed() || g.mode!=Game.FINISH || g.earnedStars!=0)
                throw new AssertionError("A missed lock deadline must show failure without rewarding completion");
            capture(g,exit?"goal-lock-exit-failed":"goal-lock-entry-failed");
        }
        System.out.println("Goal-based title, selection, delivery progression and lock phases exported to out/");
    }
}
