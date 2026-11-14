package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual shared-renderer frames for the edge-launched lasso and damage pulses. */
public final class FeedbackPreview {
    private static BufferedImage capture(Game g,String name) throws Exception {
        BufferedImage image=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics();g.render(new DesktopPainter(graphics));graphics.dispose();
        ImageIO.write(image,"png",new File("out/feedback/"+name+".png"));return image;
    }
    public static void main(String[] args) throws Exception {
        new File("out/feedback").mkdirs();BufferedImage[] frames=new BufferedImage[6];
        Game g=new Game();ReplayInput.controlFixture(g);Body b=g.cargo[0];b.x=Game.DOCK_X[2];b.y=g.dockY(2);
        ReplayInput.down(g,b.x,b.y);ReplayInput.move(g,g.bollardX(2),g.bollardY(2));
        frames[0]=capture(g,"lasso-armed");ReplayInput.up(g,g.bollardX(2),g.bollardY(2));
        for(int i=0;i<6;i++)g.step();frames[1]=capture(g,"lasso-arc-early");
        for(int i=0;i<6;i++)g.step();frames[2]=capture(g,"lasso-arc-late");
        g=new Game();ReplayInput.controlFixture(g);b=g.cargo[0];b.x=Game.DOCK_X[2];b.y=g.dockY(2);
        ReplayInput.down(g,b.x,b.y);ReplayInput.move(g,g.bollardX(2),g.bollardY(2)-90);
        ReplayInput.up(g,g.bollardX(2),g.bollardY(2)-90);
        for(int i=0;i<Game.ROPE_FLIGHT_TICKS+7;i++)g.step();frames[3]=capture(g,"lasso-recoil");
        g=new Game();g.reset();g.tug.x=Physics.LEFT+g.tug.half+g.tug.radius+1;g.tug.y=450;g.tug.angle=0;g.tug.vx=-180;
        for(int i=0;i<10 && g.hull==100;i++)g.step();frames[4]=capture(g,"damage-flash");
        for(int i=0;i<12;i++)g.step();frames[5]=capture(g,"damage-between-pulses");
        BufferedImage sheet=new BufferedImage(1920,1800,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=sheet.createGraphics();
        for(int i=0;i<frames.length;i++)graphics.drawImage(frames[i],i%2*960,i/2*600,null);
        graphics.dispose();ImageIO.write(sheet,"png",new File("out/feedback/BargeBoss-lasso-and-damage-v1.png"));
        System.out.println("Edge lasso aim/flight/recoil and real hull-damage frames exported.");
    }
}
