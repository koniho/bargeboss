package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
public final class CoachingReview {
    public static void main(String[] args)throws Exception{
        File dir=new File("docs/art-studies/coaching-impact-v1");dir.mkdirs();
        for(boolean retry:new boolean[]{false,true}){
            Game root=new Game();ProfileScenario.prepare(root,"instructor-teach",1333,600);Game g=root.activeGame();
            g.tutorial.instructorRadio.teach=false;
            g.tutorial.navigation.retry=retry;g.tutorial.feedback.sample(g);g.tutorial.navigation.retry=false;
            g.tutorial.feedback.completed(g,Tutorial.FIGURE_EIGHT,Tutorial.REVERSE);
            for(int i=0;i<90;i++)root.step();
            BufferedImage im=new BufferedImage(1333,600,BufferedImage.TYPE_INT_RGB);Graphics2D c=im.createGraphics();
            c.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Renderer.draw(root,new DesktopPainter(c));c.dispose();ImageIO.write(im,"png",new File(dir,retry?"encouragement.png":"praise.png"));
        }
    }
}
