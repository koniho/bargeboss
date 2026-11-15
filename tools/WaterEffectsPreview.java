package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual shared water, wakes and scenery clipping in introductory and training scenes. */
public final class WaterEffectsPreview {
    private static final File OUTPUT=new File("out/water-effects");
    private static void save(Game g,String name) throws Exception {
        BufferedImage image=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);
        Graphics2D draw=image.createGraphics();draw.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(g,new DesktopPainter(draw));draw.dispose();ImageIO.write(image,"png",new File(OUTPUT,name+".png"));
    }
    public static void main(String[] args) throws Exception {
        OUTPUT.mkdirs();Game g=new Game();g.reset();
        for(int i=0;i<150;i++)g.step();save(g,"harbor");
        ReplayInput.down(g,g.tug.x,g.tug.y);ReplayInput.move(g,g.downX,g.downY-100);
        for(int i=0;i<70;i++)g.step();save(g,"underway");ReplayInput.up(g,g.pointerX,g.pointerY);
        File frames=new File(OUTPUT,"frames");frames.mkdirs();
        for(int i=0;i<120;i++) {
            g.step();if(i%10!=0)continue;
            save(g,"frames/"+String.format("%03d",i/10));
        }
        g=new Game();g.challengeId=1;g.reset();g.visualTicks=210;
        g.tug.x=366;g.tug.y=286;g.tug.vx=32;g.tug.vy=18;
        g.water.x[0]=370;g.water.y[0]=280;g.water.age[0]=75;g.water.strength[0]=1;
        save(g,"pier-wake");
        g=new Game();g.startTutorial();g=g.activeGame();g.tutorial.lesson=Tutorial.CHANNEL;g.tutorial.setup(g);g.fitTutorialCamera();
        g.visualTicks=180;save(g,"tutorial-entry");
        g.tug.x=290;g.tug.y=440;g.tug.vx=5;g.tug.vy=-36;g.tug.angle=-(float)Math.PI/2;
        g.water.x[0]=270;g.water.y[0]=485;g.water.age[0]=90;g.water.strength[0]=1;g.fitTutorialCamera();
        save(g,"tutorial-turn");
        System.out.println("Shared water/wake/scenery captures: "+OUTPUT);
    }
}
