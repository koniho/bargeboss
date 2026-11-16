package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Geometry motion strip: the same detached packet at five ages, drawn through the real water pass. */
public final class WakeMotionReview {
    public static void main(String[] args)throws Exception {
        File dir=new File("out/wake-motion");dir.mkdirs();
        BufferedImage strip=new BufferedImage(1500,400,BufferedImage.TYPE_INT_RGB);Graphics2D sheet=strip.createGraphics();
        Game g=new Game();g.reset();g.layout=Layout.focused(300,400,300,500,1.25f,150,180);
        g.water=new WaterSurface();g.water.x[0]=300;g.water.y[0]=500;g.water.heading[0]=0;g.water.strength[0]=1;
        int[] ages={8,38,68,98,128};
        for(int i=0;i<ages.length;i++) {
            g.water.age[0]=ages[i];BufferedImage frame=new BufferedImage(300,400,BufferedImage.TYPE_INT_RGB);Graphics2D canvas=frame.createGraphics();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Painter p=new DesktopPainter(canvas);p.rect(0,0,300,400,0xff102b34);RiverPainter world=new RiverPainter(p,g.layout.camera);
            world.line(280,500,320,500,.6f,0xff3f626d);world.circle(300,500,2,0xffe8bd68);
            g.water.draw(g,world);canvas.setColor(Color.WHITE);canvas.drawString("Age "+ages[i]+" ticks",12,380);canvas.dispose();
            sheet.drawImage(frame,i*300,0,null);ImageIO.write(frame,"png",new File(dir,"age-"+ages[i]+".png"));
        }
        sheet.dispose();ImageIO.write(strip,"png",new File(dir,"propagation.png"));
    }
}
