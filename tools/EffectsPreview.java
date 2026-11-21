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
            VesselSmoke smoke=new VesselSmoke();
            for(int tick=0;tick<120;tick++){smoke.drift();smoke.emit(g,tug);}
            VesselView.draw(g,new RiverPainter(p,camera),tug,true);
            smoke.draw(p,camera,1600,800);
            Lettering.center(p,""+Math.round(i*360/32f),x,(i/8)*200+10,12,VisualStyle.GOLD);
        }
        graphics.dispose();ImageIO.write(sheet,"png",new File("out/smoke-headings.png"));
        g=new Game();g.reset();for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}
        ReplayInput.throttle(g,0,-90);
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
        smokeMotion();
        System.out.println("32 chimney headings, independent smoke and persistent wake animations exported to out/");
    }
    private static void smokeMotion() throws Exception {
        Game g=new Game();g.reset();Body tug=g.tug;
        tug.x=240;tug.y=380;tug.angle=0;tug.engineLoad=1;tug.floatingInitialized=true;
        RiverCamera origin=new RiverCamera(0,0,2.3f);
        RiverCamera camera=new RiverCamera(130-origin.screenX(tug.x,tug.y),335-origin.screenY(tug.x,tug.y),2.3f);
        File directory=new File("out/animation-frames/smoke");directory.mkdirs();
        for(int tick=0;tick<660;tick++) {
            if(tick<300){tug.angle=(float)(Math.PI/4+Math.max(0,tick-150)*Math.PI/300);tug.vx=38*(float)StrictMath.cos(tug.angle);tug.vy=38*(float)StrictMath.sin(tug.angle);}
            else {tug.engineLoad=0;tug.vx=tug.vy=0;}
            tug.x+=tug.vx/60;tug.y+=tug.vy/60;g.visualTicks=tick;
            g.smoke.drift();g.smoke.emit(g,tug);
            if(tick%5!=0)continue;
            BufferedImage frame=new BufferedImage(800,450,BufferedImage.TYPE_INT_RGB);
            Graphics2D canvas=frame.createGraphics();canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Painter p=new DesktopPainter(canvas);p.beginFrame();p.rect(0,0,800,450,0xff244955);
            for(int row=0;row<14;row++)for(int col=0;col<16;col++)p.line(col*58,row*35,col*58+19,row*35-2,1,0x22428a91);
            VesselView.draw(g,new RiverPainter(p,camera),tug,true,false);g.smoke.draw(p,camera,800,450);
            Lettering.center(p,tick<150?"UNDER LOAD":tick<300?"TURNING - TRAIL STAYS BEHIND":"IDLE - OLD SMOKE KEEPS DRIFTING",400,24,12,VisualStyle.CREAM);
            canvas.dispose();ImageIO.write(frame,"png",new File(directory,String.format("%04d.png",tick/5)));
        }
    }

}
