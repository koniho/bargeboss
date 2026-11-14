package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual painted hull contact at idle and underway, with heading and heave changes. */
public final class WaterlineReview {
    public static void main(String[] args)throws Exception {
        File dir=new File("out/waterline");dir.mkdirs();Game g=new Game();g.mode=Game.TITLE;
        for(int frame=0;frame<36;frame++) {
            g.visualTicks=frame*3;
            BufferedImage image=new BufferedImage(1000,560,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(0x0b2732));graphics.fillRect(0,0,1000,560);
            for(int kind=0;kind<2;kind++)for(int view=0;view<4;view++) {
                float angle=view*(float)Math.PI/2+frame*.006f;
                Body b=new Body(kind==0?0:1,kind==0?-1:0,300,400,angle);
                if(view%2==1){b.vx=45;b.vy=10;}
                b.floatingInitialized=true;b.heave=(float)StrictMath.sin(frame*.15)*1.1f;b.heaveVelocity=(float)StrictMath.cos(frame*.15)*3.3f;
                RiverCamera base=new RiverCamera(0,0,1.65f);
                RiverCamera camera=new RiverCamera(125+view*250-base.screenX(b.x,b.y),190+kind*260-base.screenY(b.x,b.y),1.65f);
                g.layout=Layout.renderTile(1000,560,camera);
                VesselView.draw(g,new RiverPainter(new DesktopPainter(graphics),camera),b,kind==0,false);
                graphics.setColor(new Color(0xa8c9c8));graphics.drawString((kind==0?"TUG":"BARGE")+(view%2==0?" / IDLE":" / MOVING"),view*250+65,kind*260+240);
            }
            graphics.dispose();ImageIO.write(image,"png",new File(dir,String.format("%03d.png",frame)));
        }
        System.out.println("Animated waterline review exported to out/waterline");
    }
}
