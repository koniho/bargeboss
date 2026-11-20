package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Actual shared-renderer radio opening and responsive four-choice layout. */
public final class DialogueReview {
    static void capture(Game root,String name)throws Exception{
        Game g=root.activeGame();BufferedImage image=new BufferedImage((int)g.layout.width,(int)g.layout.height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        root.render(new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File("out/"+name+".png"));
    }
    public static void main(String[] args)throws Exception{
        Game root=new Game();root.configureCanvas(1333,600);root.startTutorial();Game g=root.activeGame();
        for(int i=0;i<100;i++)root.step();capture(root,"dialogue-approach");
        for(int i=100;i<280;i++)root.step();capture(root,"dialogue-question");
        Layout.Box b=g.dialogue.choiceBox(g,0);root.pointerDown(1,b.centerX(),b.centerY());root.pointerUp(1,b.centerX(),b.centerY());
        for(int i=0;i<120;i++)root.step();capture(root,"dialogue-acknowledgement");
        for(int i=0;i<90;i++)root.step();capture(root,"dialogue-driving-handoff");
        root.configureCanvas(960,600);g.dialogue.show(g,"HARBOR CONTROL","CHOOSE YOUR NEXT ASSIGNMENT","ALL CREWS ARE STANDING BY.","TOW TO THE NORTH BERTH.","SHIFT THE COAL BARGE.","WAIT FOR THE BRIDGE.","REQUEST MORE INSTRUCTIONS.");
        for(int i=0;i<20;i++)root.step();capture(root,"dialogue-four-choices");
    }
}
