package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Actual shared-renderer tap acknowledgement review. */
public final class MenuFeedbackReview {
    private static void save(Game g,String name)throws Exception{
        BufferedImage im=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=im.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.render(new DesktopPainter(graphics));graphics.dispose();
        ImageIO.write(im,"png",new File("out/menu-feedback/"+name+".png"));
    }
    public static void main(String[] args)throws Exception{
        new File("out/menu-feedback").mkdirs();Game g=new Game();
        Layout.Box b=g.controlBox(Layout.CHALLENGES);save(g,"title-before");
        g.pointerDown(1,b.centerX(),b.centerY());g.pointerUp(1,b.centerX(),b.centerY());save(g,"title-pulse");
        for(int i=0;i<8;i++)g.advance(1.0/60);save(g,"locations-after");
        g.openSettings();b=g.controlBox(Layout.SOUND_MUTE);g.pointerDown(2,b.centerX(),b.centerY());save(g,"settings-pulse");
        g.reset();g.dialogue.show(g,"HARBOR CONTROL","READY TO GO?","SELECT YOUR REPLY.","READY, CAPTAIN.","GIVE ME A MOMENT.");
        for(int i=0;i<100;i++)g.dialogue.step();b=g.dialogue.choiceBox(g,1);
        g.pointerDown(3,b.centerX(),b.centerY());g.pointerUp(3,b.centerX(),b.centerY());save(g,"dialogue-pulse");
    }
}
