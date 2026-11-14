package com.bargeboss.game;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Shared-renderer review at the screenshot's wide-phone aspect ratio. */
public final class SoftCueReview {
    public static void main(String[] args)throws Exception {
        Game root=new Game();root.configureCanvas(1333,600);root.startTutorial();Game g=root.activeGame();
        for(int lesson:new int[]{Tutorial.MOVE,Tutorial.HOLD,Tutorial.TIE}) {
            g.tutorial.lesson=lesson;g.tutorial.setup(g);g.visualTicks=35;
            if(lesson==Tutorial.TIE) {
                Body b=g.cargo[0];ReplayInput.down(root,b.x,b.y);ReplayInput.move(root,g.bollardX(b.berth),g.bollardY(b.berth));
            }
            BufferedImage frame=new BufferedImage(1333,600,BufferedImage.TYPE_INT_RGB);Graphics2D c=frame.createGraphics();
            c.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.render(new DesktopPainter(c));c.dispose();ImageIO.write(frame,"png",new File("out/soft-guidance-"+lesson+".png"));
        }
    }
}
