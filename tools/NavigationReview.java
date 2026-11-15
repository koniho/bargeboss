package com.bargeboss.game;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Native-composition review of the shared lit waterway markers. */
public final class NavigationReview {
    private static void capture(Game g,String name)throws Exception{
        BufferedImage image=new BufferedImage((int)g.layout.width,(int)g.layout.height,BufferedImage.TYPE_INT_RGB);
        Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(g,new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File("out/"+name+".png"));
    }
    public static void main(String[] args)throws Exception{
        Game root=new Game();root.configureCanvas(1333,600);root.startTutorial();Game g=root.activeGame();
        g.layout=Layout.focused(1333,600,Tutorial.DRIVE_X[0],Tutorial.DRIVE_Y[0],1.65f,700,370);g.visualTicks=15;
        capture(g,"navigation-practice-buoys");
        g.tutorial.lesson=Tutorial.CHANNEL;g.tug.x=TrainingChannel.MIN-80;g.tug.y=500;
        g.layout=Layout.focused(1333,600,TrainingChannel.MIN,500,1.7f,650,360);capture(g,"navigation-canal-entrance");
        g.tutorial.lesson=Tutorial.TRAIN;g.tug.x=300;g.tug.y=-140;
        g.layout=Layout.focused(1333,600,300,-200,1.7f,650,360);g.visualTicks=15-3*19+120;capture(g,"navigation-narrow-entrance");
        g=new Game();g.configureCanvas(1333,600);g.reset();g.visualTicks=15;
        g.layout=Layout.focused(1333,600,300,Physics.BOTTOM-45,1.3f,650,360);capture(g,"navigation-harbor-entrance");
        System.out.println("Lit practice buoys and red-right-returning entrance frames exported to out/");
    }
}
