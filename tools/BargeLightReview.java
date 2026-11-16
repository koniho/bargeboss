package com.bargeboss.game;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Actual renderer: eased push pulse and either-end towing/pushing anchor review. */
public final class BargeLightReview {
    private static void broadside()throws Exception {
        BufferedImage image=new BufferedImage(1080,520,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Painter p=new DesktopPainter(graphics);p.rect(0,0,1080,520,0xff061d27);
        for(int row=0;row<2;row++)for(int col=0;col<3;col++){
            Game g=new Game();g.reset();g.visualTicks=18;
            float heading=(float)(Math.PI/4+row*Math.PI+(col-1)*Math.PI/16);
            Body b=new Body(1,2,300,500,heading);
            RiverCamera origin=new RiverCamera(0,0,3.3f),camera=new RiverCamera(col*360+180-origin.screenX(300,500),row*260+150-origin.screenY(300,500),3.3f);
            VesselView.draw(g,new RiverPainter(p,camera),b,false,false);
            Lettering.center(p,(row==0?"RIGHT":"LEFT")+" / "+(col==1?"EXACT":"ADJACENT POSE"),col*360+180,row*260+28,11,VisualStyle.CREAM);
        }
        graphics.dispose();ImageIO.write(image,"png",new File("out/barge-stern-broadside.png"));
    }
    public static void main(String[] args)throws Exception {
        if(args.length>0 && args[0].equals("--broadside")){broadside();return;}
        BufferedImage image=new BufferedImage(1200,840,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Painter p=new DesktopPainter(graphics);p.rect(0,0,1200,840,0xff061d27);
        int[] ticks={0,7,18,34,48};
        for(int row=0;row<3;row++)for(int col=0;col<5;col++){
            Game g=new Game();g.reset();g.visualTicks=ticks[col];
            float heading=(float)(Math.PI*.75),angle=heading+(row==1?(float)Math.PI:0);
            Body b=new Body(1,2,300,500,angle);g.cargo[0]=b;
            g.tug.angle=heading;g.tug.x=b.x-65*(float)Math.cos(heading);g.tug.y=b.y-65*(float)Math.sin(heading);g.pushing=true;
            if(row==2){g.pushing=false;g.tug.x=b.x+70*(float)Math.cos(heading);g.tug.y=b.y+70*(float)Math.sin(heading);g.tow.attach(g,b);}
            RiverCamera origin=new RiverCamera(0,0,2.6f),camera=new RiverCamera(col*240+120-origin.screenX(300,500),row*280+165-origin.screenY(300,500),2.6f);
            VesselNavigationLights.reflections(g,new RiverPainter(p,camera),b,false);
            VesselView.draw(g,new RiverPainter(p,camera),b,false,false);
            Lettering.center(p,row==0?"PUSH BOW FIRST":row==1?"PUSH OTHER END":"TOW ASTERN",col*240+120,row*280+22,10,VisualStyle.CREAM);
            Lettering.center(p,"TICK "+ticks[col],col*240+120,row*280+255,10,VisualStyle.CREAM);
        }
        graphics.dispose();ImageIO.write(image,"png",new File("out/barge-light-pulse.png"));
    }
}
