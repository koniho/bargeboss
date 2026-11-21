package com.bargeboss.game;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Existing cargo families on the same current hull, camera and lighting. */
public final class CargoStyleReview {
    public static void main(String[] args)throws Exception {
        boolean all=args.length>0 && args[0].equals("--all"),unload=args.length>0 && args[0].equals("--unload");
        int columns=all?8:4,poses=all?64:4,cw=all?240:320,ch=all?180:260,rows=all?24:3;
        BufferedImage image=new BufferedImage(columns*cw,rows*ch,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Painter p=new DesktopPainter(graphics);p.rect(0,0,image.getWidth(),image.getHeight(),0xff061d27);
        for(int kind=0;kind<3;kind++)for(int pose=0;pose<poses;pose++){
            int row=kind*(all?8:1)+pose/columns,col=pose%columns;
            Game g=new Game();g.reset();g.mode=Game.TITLE;g.visualTicks=18;
            float heading=unload?(float)Math.PI/4:pose*(float)Math.PI/(all?32:4);
            Body b=new Body(1,kind,300,500,heading);if(unload)b.unloadingTicks=pose*60;
            RiverCamera origin=new RiverCamera(0,0,all?2.2f:3),camera=new RiverCamera(col*cw+cw/2-origin.screenX(300,500),row*ch+ch*.65f-origin.screenY(300,500),all?2.2f:3);
            VesselView.draw(g,new RiverPainter(p,camera),b,false,false);
            Lettering.center(p,new String[]{"STEEL","CONTAINERS","COAL"}[kind]+" / "+(unload?pose*60:Math.round(heading*180/(float)Math.PI)),col*cw+cw/2,row*ch+22,10,VisualStyle.CREAM);
        }
        graphics.dispose();ImageIO.write(image,"png",new File("out/cargo-style-"+(all?"all-poses":unload?"unloading":"current")+".png"));
    }
}
