package com.bargeboss.game;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Opening buoy exercise captured with real screen-pointer controls. */
public final class BuoyReview {
    static void capture(Game root,String name)throws Exception {
        Game g=root.activeGame();BufferedImage image=new BufferedImage((int)g.layout.width,(int)g.layout.height,BufferedImage.TYPE_INT_RGB);
        Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(root,new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File("out/"+name+".png"));
    }
    public static void main(String[] args)throws Exception {
        Game root=new Game();root.configureCanvas(1333,600);root.startTutorial();Game g=root.activeGame();
        capture(root,"buoy-opening");boolean[] shown=new boolean[2];
        for(int tick=0;tick<5000 && g.tutorial.lesson<Tutorial.REVERSE;tick++){
            ReplayInput.tutorial(root);root.step();int buoy=g.tutorial.driveMarker;
            if(g.tutorial.lesson==Tutorial.FIGURE_EIGHT && buoy<2 && g.tutorial.orbitProgress()>.4f && !shown[buoy]){capture(root,"buoy-orbit-"+(buoy+1));shown[buoy]=true;}
        }
        if(g.tutorial.lesson!=Tutorial.REVERSE || !shown[0] || !shown[1])throw new AssertionError("Buoy review did not circle both markers");
        capture(root,"buoy-laps-complete");System.out.println("Two lit buoy orbits exported to out/");
    }
}
