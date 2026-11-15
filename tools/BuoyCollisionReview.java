package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Real shared simulation: drive into a float, back away, then watch the mooring settle. */
public final class BuoyCollisionReview {
    public static void main(String[] args)throws Exception{
        Game root=new Game();root.startTutorial();Game g=root.activeGame();
        int marker=NavigationLights.count(g)-2;Buoys.Buoy b=g.buoys.get(g,marker);
        g.tug.x=b.anchorX-72;g.tug.y=b.anchorY;g.tug.angle=0;
        g.down(g.tug.x,g.tug.y);g.move(g.downX+110,g.downY);
        File dir=new File("out/buoy-collision-motion");dir.mkdirs();
        float displacement=0,roll=0;BufferedImage board=new BufferedImage(1440,600,BufferedImage.TYPE_INT_RGB);
        Graphics2D panels=board.createGraphics();int panel=0;
        for(int tick=0;tick<360;tick++){
            if(tick==48){g.cancel();g.down(g.tug.x,g.tug.y);g.move(g.downX-110,g.downY);}
            if(tick==100)g.cancel();
            g.step();displacement=Math.max(displacement,Physics.length(b.x-b.anchorX,b.y-b.anchorY));roll=Math.max(roll,Math.abs(b.roll));
            if(tick%6!=0)continue;
            g.layout=Layout.focused(960,600,b.anchorX-25,b.anchorY,2.7f,480,360);
            BufferedImage frame=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);Graphics2D canvas=frame.createGraphics();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Renderer.draw(g,new DesktopPainter(canvas));
            canvas.setColor(new Color(0x0b2029));canvas.fillRect(24,535,675,46);
            canvas.setColor(new Color(0xd7e9df));canvas.drawString((tick<48?"PUSH INTO FLOAT":tick<100?"BACK AWAY":"TETHER SETTLES")+" / displacement "+String.format(java.util.Locale.ROOT,"%.1f",Physics.length(b.x-b.anchorX,b.y-b.anchorY))+" world units",40,563);
            canvas.dispose();ImageIO.write(frame,"png",new File(dir,String.format(java.util.Locale.ROOT,"%03d.png",tick/6)));
            if(tick==0 || tick==24 || tick==48 || tick==84 || tick==150 || tick==354){
                panels.drawImage(frame,panel%3*480,panel/3*300,480,300,null);panel++;
            }
        }
        panels.dispose();ImageIO.write(board,"png",new File("out/buoy-collision-review.png"));
        if(displacement<5 || roll<.01f)throw new AssertionError("Review must visibly bump and rock a buoy: "+displacement+", "+roll);
        System.out.println("Buoy bump/return review: maximum displacement "+displacement+", roll "+roll+"; 60 frames exported");
    }
}
