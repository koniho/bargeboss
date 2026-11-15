package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Production RGBA, waterline pixels and registered rolling lantern review. */
public final class BuoySpriteReview {
    public static void main(String[] args)throws Exception{
        if(args.length>0 && args[0].equals("motion")){motion();return;}
        int assertions=0;
        BufferedImage board=new BufferedImage(1200,700,BufferedImage.TYPE_INT_RGB);Graphics2D p=board.createGraphics();
        p.setColor(new Color(0x10262c));p.fillRect(0,0,1200,700);p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        DesktopPainter painter=new DesktopPainter(p);
        for(int frame=0;frame<3;frame++){
            BufferedImage src=ImageIO.read(new File("assets/sprites/"+BuoyArt.file(frame)));
            if(src.getWidth()!=1254 || src.getHeight()!=1254)throw new AssertionError("Source registration dimensions");
            if((src.getRGB(Math.round(BuoyArt.lampX(frame)*1254),Math.round(BuoyArt.lampY(frame)*1254))>>>24)<240)throw new AssertionError("Lantern not registered on source art");
            assertions+=2;
            for(int pose=0;pose<3;pose++){
                float roll=(pose-1)*BuoyArt.MAX_ROLL;BuoyArt.Pose b=new BuoyArt.Pose(80+frame*400+pose*120,300,180,roll,296,pose*1.7f);
                BufferedImage clipped=new BufferedImage(400,400,BufferedImage.TYPE_INT_ARGB);Graphics2D c=clipped.createGraphics();
                BuoyArt.Pose local=new BuoyArt.Pose(200,300,260,roll,294+pose,pose*1.7f);
                BuoyArt.hull(new DesktopPainter(c),frame,local);c.dispose();int above=0;
                for(int y=0;y<400;y++)for(int x=0;x<400;x++){
                    int a=clipped.getRGB(x,y)>>>24;
                    if(y>BuoyArt.surfaceY(local,x)+2 && a!=0)throw new AssertionError("Submerged source pixels leak below curved water at frame "+frame+" roll "+roll);
                    if(a>128)above++;
                }
                if(above<2000)throw new AssertionError("Hull unexpectedly clipped away");assertions++;
                BuoyArt.hull(painter,frame,b);BuoyArt.contact(painter,b);float lx=b.pointX(frame,BuoyArt.lampX(frame),BuoyArt.lampY(frame)),ly=b.pointY(frame,BuoyArt.lampX(frame),BuoyArt.lampY(frame));
                int color=frame==0?NavigationLights.RED:frame==1?NavigationLights.GREEN:NavigationLights.YELLOW;
                SceneryEffects.glow(painter,lx,ly,24,color,1,18,frame);painter.circle(lx,ly,3,0xffffefcc);
            }
            p.setColor(Color.WHITE);p.drawString(BuoyArt.file(frame)+"  -6 / 0 / +6 degrees",frame*400+12,345);
            BuoyArt.Pose large=new BuoyArt.Pose(frame*400+200,650,290,0,644);BuoyArt.hull(painter,frame,large);BuoyArt.contact(painter,large);
        }
        p.dispose();new File("out").mkdirs();ImageIO.write(board,"png",new File("out/buoy-sprite-review.png"));
        System.out.println(assertions+" buoy source/lantern/waterline pixel checks; out/buoy-sprite-review.png");
    }
    /** Six real-time seconds of shared buoy/water drawing, enlarged to inspect submergence. */
    private static void motion()throws Exception{
        File dir=new File("out/buoy-waterline-motion");dir.mkdirs();Game g=new Game();g.reset();
        g.layout=Layout.focused(960,600,300,550,3.8f,480,380);
        for(int tick=0;tick<360;tick++){
            g.visualTicks=tick;
            // Pass one real wake source behind the review floats, then let released packets drift.
            g.tug.x=220+tick*.4f;g.tug.y=550;g.tug.vx=tick<240?24:0;g.tug.vy=0;g.water.step(g);
            if(tick%5!=0)continue;
            BufferedImage frame=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);Graphics2D canvas=frame.createGraphics();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            canvas.setColor(new Color(0x0b2029));canvas.fillRect(0,0,960,600);DesktopPainter p=new DesktopPainter(canvas);
            g.water.draw(g,new RiverPainter(p,g.layout.camera));
            for(int i=0;i<3;i++){
                float wx=240+i*60,wy=490+i*60;int color=i==0?NavigationLights.RED:i==1?NavigationLights.GREEN:NavigationLights.YELLOW;
                BuoyArt.shadows(g,p,wx,wy,48,i);BuoyArt.draw(g,p,wx,wy,48,i,color,.6f,0);
            }
            canvas.setColor(new Color(0xc1dedc));canvas.drawString("SHARED BUOY WATERLINE / wave-driven roll + delayed heave + curved submergence",35,548);
            canvas.drawString("Soft foam and outward contact crests remain separate from the painted sprites.",35,570);
            canvas.dispose();ImageIO.write(frame,"png",new File(dir,String.format("%03d.png",tick/5)));
        }
        System.out.println("72 real-time 12 fps buoy waterline frames exported to out/buoy-waterline-motion/");
    }
}
