package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Production RGBA, waterline pixels and registered rolling lantern review. */
public final class BuoySpriteReview {
    public static void main(String[] args)throws Exception{
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
                float roll=(pose-1)*BuoyArt.MAX_ROLL;BuoyArt.Pose b=new BuoyArt.Pose(100+frame*400+pose*130,300,180,roll,306);
                BufferedImage clipped=new BufferedImage(400,400,BufferedImage.TYPE_INT_ARGB);Graphics2D c=clipped.createGraphics();
                BuoyArt.Pose local=new BuoyArt.Pose(200,300,260,roll,307+pose);
                BuoyArt.hull(new DesktopPainter(c),frame,local);c.dispose();int above=0;
                for(int y=0;y<400;y++)for(int x=0;x<400;x++){
                    int a=clipped.getRGB(x,y)>>>24;
                    if(y>local.waterY+1 && a!=0)throw new AssertionError("Submerged source pixels leak below water at frame "+frame+" roll "+roll);
                    if(a>128)above++;
                }
                if(above<2000)throw new AssertionError("Hull unexpectedly clipped away");assertions++;
                BuoyArt.hull(painter,frame,b);float lx=b.pointX(frame,BuoyArt.lampX(frame),BuoyArt.lampY(frame)),ly=b.pointY(frame,BuoyArt.lampX(frame),BuoyArt.lampY(frame));
                int color=frame==0?NavigationLights.RED:frame==1?NavigationLights.GREEN:NavigationLights.YELLOW;
                SceneryEffects.glow(painter,lx,ly,24,color,1,18,frame);painter.circle(lx,ly,3,0xffffefcc);
            }
            p.setColor(Color.WHITE);p.drawString(BuoyArt.file(frame)+"  -6 / 0 / +6 degrees",frame*400+12,345);
            BuoyArt.Pose large=new BuoyArt.Pose(frame*400+200,650,290,0,658);BuoyArt.hull(painter,frame,large);
        }
        p.dispose();new File("out").mkdirs();ImageIO.write(board,"png",new File("out/buoy-sprite-review.png"));
        System.out.println(assertions+" buoy source/lantern/waterline pixel checks; out/buoy-sprite-review.png");
    }
}
