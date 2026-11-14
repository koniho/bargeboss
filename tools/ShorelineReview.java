package com.bargeboss.game;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Reproduce hull/shore overlap on both foreground basin edges. */
public final class ShorelineReview {
    public static void main(String[] args)throws Exception {
        for(int frameIndex=0;frameIndex<4;frameIndex++) {
            int bank=frameIndex%2;
            Game root=new Game();root.configureCanvas(1333,600);root.startTutorial();Game g=root.activeGame();
            g.tug.x=bank==0?-1610:TrainingChannel.BASIN_LEFT+16;
            g.tug.y=bank==0?TrainingChannel.BASIN_BOTTOM-16:600;
            g.tug.angle=bank==0?0:(float)Math.PI/2;
            g.cargo[0]=new Body(1,0,g.tug.x-(bank==0?85:-3),g.tug.y-(bank==0?3:85),g.tug.angle);
            if(frameIndex>=2){g.tow.attach(g,g.cargo[0]);g.tug.angle+=(float)Math.PI;g.tow.catchTicks=0;}
            g.layout=Layout.focused(1333,600,g.tug.x,g.tug.y,1.7f,666,360);
            BufferedImage frame=new BufferedImage(1333,600,BufferedImage.TYPE_INT_RGB);Graphics2D c=frame.createGraphics();
            c.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.render(new DesktopPainter(c));c.dispose();ImageIO.write(frame,"png",new File("out/shoreline-hulls-"+frameIndex+".png"));
        }
    }
}
