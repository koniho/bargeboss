package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Full shared-renderer captures of rescue lamp state, including hull occlusion. */
public final class CruiserLightPreview {
    private static void capture(Game g,String name)throws Exception{BufferedImage image=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Renderer.draw(g,new DesktopPainter(p));p.dispose();new File("out").mkdirs();ImageIO.write(image,"png",new File("out/"+name+".png"));}
    public static void main(String[] args)throws Exception{
        Game g=new Game();g.selectChallenge(Challenges.RAFT_RESCUE);g.reset();Body b=g.cargo[0];
        g.layout=Layout.focused(960,600,b.x+11,b.y,4.5f,480,340);capture(g,"cruiser-lights-anchored");
        g.rescue.rafted=false;if(!g.tow.attach(g,b,g.tug))throw new AssertionError("Cannot attach preview tow");
        for(int i=0;i<8;i++){b.angle=i*(float)StrictMath.PI/4;capture(g,"cruiser-lights-towed-"+i);}
        g.tow.release(g);capture(g,"cruiser-lights-detached");b.deliveryRecorded=true;capture(g,"cruiser-lights-delivered");
    }
}
