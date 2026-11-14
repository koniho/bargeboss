package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual shared smoke on all headings, plus outward wake left behind a stopped tug. */
public final class EffectsPreview {
    public static void main(String[] args) throws Exception {
        BufferedImage sheet=new BufferedImage(1600,800,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=sheet.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Painter p=new DesktopPainter(graphics);p.rect(0,0,1600,800,0xff205d6a);
        Game g=new Game();g.visualTicks=43;
        for(int i=0;i<32;i++) {
            float x=100+(i%8)*200,y=165+(i/8)*200;
            RiverCamera origin=new RiverCamera(0,0,2.1f);
            RiverCamera camera=new RiverCamera(x-origin.screenX(300,500),y-origin.screenY(300,500),2.1f);
            Body tug=new Body(0,-1,300,500,(float)(i*Math.PI/16));tug.floatingInitialized=true;tug.engineLoad=1;
            VesselView.draw(g,new RiverPainter(p,camera),tug,true);
            Lettering.center(p,""+Math.round(i*360/32f),x,(i/8)*200+10,12,VisualStyle.GOLD);
        }
        graphics.dispose();ImageIO.write(sheet,"png",new File("out/smoke-headings.png"));
        g=new Game();g.reset();for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}
        ReplayInput.down(g,g.tug.x,g.tug.y);ReplayInput.move(g,g.downX,g.downY-90);
        File directory=new File("out/animation-frames/wake");directory.mkdirs();
        for(int i=0;i<420;i++) {
            if(i==100) ReplayInput.up(g,g.pointerX,g.pointerY);
            g.step();
            if(i%5!=0) continue;
            BufferedImage frame=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);Graphics2D canvas=frame.createGraphics();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.render(new DesktopPainter(canvas));canvas.dispose();
            ImageIO.write(frame,"png",new File(directory,String.format("%04d.png",i/5)));
        }
        System.out.println("32 chimney headings and persistent wake animation exported to out/");
    }
}
