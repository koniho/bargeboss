package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Inspect the actual shared responsive composition at phone and tablet sizes. */
public final class ScreenSizesPreview {
    private static void capture(Game g,int width,int height,String name) throws Exception {
        Viewport viewport=new Viewport(width,height);
        g.configureCanvas(viewport.logicalWidth(),viewport.logicalHeight());
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D p=image.createGraphics();
        p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        p.scale(viewport.scale,viewport.scale); Renderer.draw(g,new DesktopPainter(p)); p.dispose();
        ImageIO.write(image,"png",new File("out/"+name+".png"));
    }
    public static void main(String[] args) throws Exception {
        new File("out").mkdirs(); Game g=new Game();
        capture(g,1600,720,"wide-title");
        g.mode=Game.CHALLENGES; capture(g,1600,720,"wide-challenges-locked"); g.reset();
        capture(g,1600,720,"wide-stage");
        for(int i=0;i<165;i++) g.step(); capture(g,1600,720,"wide-feeder-dropoff");
        g.background(); capture(g,1600,720,"wide-pause");
        g.mode=Game.HELP; capture(g,1600,720,"wide-help");
        g.startTutorial(); capture(g,1600,720,"wide-tutorial");
        g=new Game();g.reset(); capture(g,1280,720,"standard-stage");
        capture(g,1024,768,"tablet-stage");
        g=new Game(); g.restoreProgress("BB1|2|3,7000|3,7500|3,8800"); g.mode=Game.CHALLENGES;
        capture(g,1600,720,"wide-challenges-captain");
        g=new Game(); g.reset(); for(Feeder f:g.feeders) { f.phase=Feeder.IDLE; f.load=null; }
        g.tug.x=300;g.tug.y=450; capture(g,1600,720,"scenery-clear");
        g.tug.x=350;g.tug.y=280; capture(g,1600,720,"scenery-faded");
        System.out.println("Responsive phone and tablet captures exported to out/");
    }
}
