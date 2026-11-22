package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Actual shared arrival panel before and after the first radio introduction. */
public final class ArrivalPortraitReview {
    public static void main(String[] args)throws Exception{
        File dir=new File("docs/art-studies/instructor-arrival-v1");dir.mkdirs();
        Game root=new Game();root.configureCanvas(1333,600);root.startTutorial();
        for(int tick=1;tick<=270;tick++){
            root.step();if(tick!=60 && tick!=270)continue;
            BufferedImage im=new BufferedImage(1333,600,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Renderer.draw(root,new DesktopPainter(g));g.dispose();
            ImageIO.write(im,"png",new File(dir,tick==60?"approach.png":"introduction.png"));
        }
        TutorialEntranceTest.run();
    }
}
