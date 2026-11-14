package com.bargeboss.game;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Fixed-pose overlap fixture: transition in, hold, clear, transition back. */
public final class BuildingFadeReview {
    public static void main(String[] args)throws Exception {
        Game root=new Game();root.configureCanvas(1333,600);root.startTutorial();Game g=root.activeGame();
        g.tug.x=-1610;g.tug.y=TrainingChannel.BASIN_BOTTOM-16;g.tug.angle=0;
        g.tug.floatingInitialized=true;g.tug.heave=0;
        g.layout=Layout.focused(1333,600,g.tug.x,g.tug.y,1.9f,666,360);
        File dir=new File("out/animation-frames/building-fade");dir.mkdirs();
        for(int tick=0;tick<120;tick++) {
            if(tick==60){g.tug.x-=400;g.tug.y-=300;}
            g.visualTicks=tick;g.buildingFades.step(g);
            if(tick%5!=0)continue;
            BufferedImage image=new BufferedImage(1333,600,BufferedImage.TYPE_INT_RGB);Graphics2D canvas=image.createGraphics();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Painter p=new DesktopPainter(canvas);p.beginFrame();TrainingChannelView.draw(g,new SceneryOcclusion(p));
            Lettering.center(p,tick<60?"BUILDING FADES / SHORE STAYS SOLID":"BOAT CLEAR / BUILDING RETURNS",666,24,15,VisualStyle.CREAM);
            canvas.dispose();ImageIO.write(image,"png",new File(dir,String.format("%04d.png",tick/5)));
        }
        long start=System.nanoTime();for(int i=0;i<1200;i++)g.buildingFades.step(g);
        System.out.println("Building fade fixed-step scan: "+((System.nanoTime()-start)/1200000.0)+" microseconds/step (local JVM sample)");
    }
}
