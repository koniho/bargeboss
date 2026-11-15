package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual hull/leaf contacts at closed, swinging and folded door poses. */
public final class LockDoorReview {
    public static void main(String[] args)throws Exception{
        BufferedImage board=new BufferedImage(1920,800,BufferedImage.TYPE_INT_RGB);Graphics2D panels=board.createGraphics();
        for(int gate=0;gate<2;gate++)for(int sample=0;sample<3;sample++){
            Game g=new Game();g.challengeId=2;g.reset();for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}
            g.lock.phase=gate==0?Lock.OPENING:Lock.ENTRY;g.lock.ticks=sample*150;
            float open=gate==0?g.lock.exitOpening():g.lock.entryOpening();
            g.cargo[0]=new Body(10,0,0,0,0);
            for(int side=0;side<2;side++){
                Body b=side==0?g.tug:g.cargo[0];float a=LockDoors.angle(gate,side,open);
                float ux=(float)StrictMath.cos(a),uy=(float)StrictMath.sin(a),nx=-uy,ny=ux;
                if(side==0?nx<0:nx>0){nx=-nx;ny=-ny;}
                b.angle=(float)StrictMath.atan2(-ny,-nx);
                b.x=LockDoors.hingeX(side)+ux*LockDoors.LENGTH*.55f+nx*(b.half+b.radius+Lock.GATE_HALF-2);
                b.y=LockDoors.hingeY(gate)+uy*LockDoors.LENGTH*.55f+ny*(b.half+b.radius+Lock.GATE_HALF-2);
                b.vx=-nx*50;b.vy=-ny*50;
                if(g.lock.collide(b,g.splashes)<=0)throw new AssertionError("Review needs actual door contact");
            }
            for(int tick=0;tick<12;tick++){g.visualTicks++;g.splashes.step(g);}
            g.layout=Layout.focused(960,600,300,LockDoors.hingeY(gate)+(gate==0?-40:40),1.7f,480,350);
            BufferedImage frame=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);Graphics2D c=frame.createGraphics();
            c.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Renderer.draw(g,new DesktopPainter(c));
            c.setColor(new Color(0x10262c));c.fillRect(20,540,700,40);c.setColor(Color.WHITE);
            c.drawString((gate==0?"EXIT":"ENTRY")+" DOOR / "+new String[]{"CLOSED","SWINGING","FOLDED"}[sample]+" / TUG AND BARGE CONTACT",35,565);c.dispose();
            panels.drawImage(frame,sample*640,gate*400,640,400,null);
        }
        panels.dispose();new File("out").mkdirs();ImageIO.write(board,"png",new File("out/lock-door-collision-review.png"));
        System.out.println("Six rigid lock-door contact poses exported to out/lock-door-collision-review.png");
    }
}
