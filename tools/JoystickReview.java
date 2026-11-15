package com.bargeboss.game;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Review the real shared HUD at narrow and wide safe-area sizes. */
public final class JoystickReview {
    private static void capture(Game g,String name)throws Exception {
        BufferedImage image=new BufferedImage((int)g.layout.width,(int)g.layout.height,BufferedImage.TYPE_INT_RGB);
        Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(g,new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File("out/"+name+".png"));
    }
    public static void main(String[] args)throws Exception {
        for(int width:new int[]{960,1333}) {
            Game g=new Game();g.configureCanvas(width,600);g.reset();
            float cx=Joystick.centerX(g),cy=Joystick.centerY(g);g.pointerDown(0,cx,cy);g.pointerMove(0,cx+25,cy-25);for(int i=0;i<35;i++)g.step();
            capture(g,"joystick-left-"+width);g.cancelInputs();g.settings.setJoystickSide(GameSettings.RIGHT);
            capture(g,"joystick-right-"+width);ReplayInput.tap(g,Layout.SETTINGS);capture(g,"joystick-settings-"+width);
        }
        System.out.println("Joystick and settings frames exported to out/");
    }
}
