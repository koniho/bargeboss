package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Every authored heading, all towable hull families and both attachment ends. */
public final class RopeReview {
    public static void main(String[] args)throws Exception {
        new File("out/rope-review").mkdirs();
        for(int kind=0;kind<4;kind++) {
            BufferedImage board=new BufferedImage(1600,1600,BufferedImage.TYPE_INT_RGB);Graphics2D panel=board.createGraphics();
            for(int heading=0;heading<32;heading++) {
                Game g=new Game();g.reset();for(int i=0;i<g.cargo.length;i++)g.cargo[i]=null;
                for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}
                float angle=heading*(float)Math.PI/16;
                Body b=new Body(1,kind==3?0:kind,300,500,angle,kind==3);g.cargo[0]=b;
                float c=(float)StrictMath.cos(angle),s=(float)StrictMath.sin(angle);
                g.tug.x=300+c*125;g.tug.y=500+s*125;g.tug.angle=angle;
                g.tow.attach(g,b);g.tow.length=80;
                g.layout=Layout.focused(400,200,300+c*50,500+s*50,1.35f,200,130);
                BufferedImage frame=new BufferedImage(400,200,BufferedImage.TYPE_INT_RGB);Graphics2D p=frame.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Renderer.draw(g,new DesktopPainter(p));p.setColor(Color.WHITE);p.drawString("heading "+heading+" / "+(kind==3?"yacht":"cargo "+kind),8,190);p.dispose();
                panel.drawImage(frame,heading%4*400,heading/4*200,null);
            }
            panel.dispose();ImageIO.write(board,"png",new File("out/rope-review/family-"+kind+".png"));
        }
    }
}
