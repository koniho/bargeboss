package com.bargeboss.game;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Existing cargo families on the same current hull, camera and lighting. */
public final class CargoStyleReview {
    public static void main(String[] args)throws Exception {
        BufferedImage image=new BufferedImage(1280,780,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Painter p=new DesktopPainter(graphics);p.rect(0,0,1280,780,0xff061d27);
        for(int row=0;row<3;row++)for(int col=0;col<4;col++){
            Game g=new Game();g.reset();g.visualTicks=18;
            float heading=col*(float)Math.PI/4;
            Body b=new Body(1,row,300,500,heading);
            RiverCamera origin=new RiverCamera(0,0,3),camera=new RiverCamera(col*320+160-origin.screenX(300,500),row*260+170-origin.screenY(300,500),3);
            VesselView.draw(g,new RiverPainter(p,camera),b,false,false);
            Lettering.center(p,new String[]{"STEEL","CONTAINERS","COAL"}[row]+" / "+col*45,col*320+160,row*260+28,11,VisualStyle.CREAM);
        }
        graphics.dispose();ImageIO.write(image,"png",new File("out/cargo-style-current.png"));
    }
}
