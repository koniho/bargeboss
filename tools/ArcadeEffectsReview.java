package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Contact sheet of real shared cue/icon primitives at successive simulation phases. */
public final class ArcadeEffectsReview {
    public static void main(String[] args)throws Exception{
        BufferedImage im=new BufferedImage(960,540,BufferedImage.TYPE_INT_RGB);
        Graphics2D g2=im.createGraphics();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Painter p=new DesktopPainter(g2);p.rect(0,0,960,540,0xff091923);
        Lettering.heading(p,"ARCADE FEEDBACK",32,24,26,PilotGlass.TEXT);
        Game g=new Game();
        for(int i=0;i<4;i++){
            float x=130+i*230;
            g.visualTicks=i*22;
            SoftCue.target(g,p,x,150,45,1,i==3?SoftCue.RED:SoftCue.CYAN,2);
            SoftCue.burst(p,x,310,25,i*.22f,SoftCue.YELLOW);
            SoftCue.ring(p,x,310,28+i*12,1,SoftCue.alpha(SoftCue.CYAN,1-i*.22f),2);
            ArcadeIcons.draw(p,i,x,450,1.8f,PilotGlass.AMBER);
        }
        Lettering.draw(p,"OBJECTIVE / BREATHING CHEVRONS",32,212,14,PilotGlass.TEXT);
        Lettering.draw(p,"SUCCESS / OUTWARD SNAP",32,367,14,PilotGlass.TEXT);
        Lettering.draw(p,"FIREWORKS                 WATER CANNON                 RELEASE LINK                  RADIO",32,500,14,PilotGlass.TEXT);
        g2.dispose();File dir=new File("out/ui-arcade-v4");dir.mkdirs();ImageIO.write(im,"png",new File(dir,"feedback.png"));
    }
}
