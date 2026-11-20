package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Shared renderer captures of current location entry, objective HUD and staged result states. */
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
        // Location entries replaced the old independently selectable lock/order stages.
        for(int id:new int[]{Challenges.TWO_BANKS,Challenges.MONTROSE,Challenges.INDIANA}){
            g=fixture(id);g.visualTicks=15;capture(g,"goal-location-"+id+"-start");
            Body load=g.cargo[0];
            if(load!=null){load.deliveryRecorded=true;g.delivered=1;capture(g,"goal-location-"+id+"-progress");}
        }
        for(int job=0;job<4;job++){
            g=fixture(Challenges.TWO_BANKS);g.voyage.job=job;
            for(int i=0;i<g.cargo.length;i++)g.cargo[i]=null;
            g.voyage.setup(g);
            float x=job==2?g.voyage.gateX():g.voyage.dockX(g.voyage.destination(2));
            float y=job==2?g.voyage.gateY():g.voyage.dockY(g.voyage.destination(2));
            g.layout=Layout.focused(1333,600,x,y,Game.TRANSIT_ZOOM,666,320);
            capture(g,"goal-calumet-job-"+job);
        }
        // Explicit result-screen fixture. Rule-driven handoffs are checked in LocationVoyageTest.
        g=fixture(Challenges.TWO_BANKS);g.voyage.job=3;g.voyage.complete=true;
        g.earnedStars=3;g.mode=Game.FINISH;capture(g,"goal-calumet-complete");
        System.out.println("Current location entries, contiguous river jobs and location results exported to out/");
    }
}
