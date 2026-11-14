package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Real tutorial approach and ready-to-tie poses, including an animated pulse. */
public final class VesselGlowReview {
    private static BufferedImage capture(Game g,int tick) {
        g.visualTicks=tick;
        BufferedImage im=new BufferedImage(1333,600,BufferedImage.TYPE_INT_RGB);Graphics2D p=im.createGraphics();
        p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(g,new DesktopPainter(p));p.dispose();return im;
    }
    public static void main(String[] args)throws Exception {
        File dir=new File("out/vessel-glow");dir.mkdirs();
        Game root=new Game();root.configureCanvas(1333,600);root.startTutorial();Game g=root.activeGame();
        for(int t=0;t<16000 && g.tutorial.lesson<Tutorial.HOLD;t++){ReplayInput.tutorial(root);root.step();}
        if(g.tutorial.lesson!=Tutorial.HOLD)throw new AssertionError("Approach scene not reached");
        for(int i=0;i<40;i++)capture(g,0);
        BufferedImage sheet=new BufferedImage(2666,1200,BufferedImage.TYPE_INT_RGB);Graphics2D canvas=sheet.createGraphics();
        canvas.drawImage(capture(g,0),0,0,null);canvas.drawImage(capture(g,45),1333,0,null);
        ImageIO.write(capture(g,45),"png",new File(dir,"BargeBoss-tutorial-hull-glow-v1.png"));
        for(int i=0;i<30;i++)ImageIO.write(capture(g,i*3),"png",new File(dir,String.format("pulse-%02d.png",i)));
        g.tutorial.lesson=Tutorial.TIE;g.tutorial.setup(g);
        if(!g.canTie(g.cargo[0]))throw new AssertionError("Tie scene not ready");
        for(int i=0;i<40;i++)capture(g,0);
        canvas.drawImage(capture(g,0),0,600,null);canvas.drawImage(capture(g,45),1333,600,null);canvas.dispose();
        ImageIO.write(capture(g,45),"png",new File(dir,"BargeBoss-ready-hull-glow-v1.png"));
        ImageIO.write(sheet,"png",new File(dir,"BargeBoss-hull-glow-low-high-v1.png"));
        System.out.println("Actual tutorial target / mooring pulse captures exported to out/vessel-glow");
    }
}
