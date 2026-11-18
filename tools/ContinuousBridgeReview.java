package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Compare live part-local BSPs with exact rebuilt world BSPs, plus 60 Hz cycle export. */
public final class ContinuousBridgeReview {
    private static BufferedImage draw(Game g,LandmarkBridge b,float value,boolean live){
        BufferedImage im=new BufferedImage(800,600,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=im.createGraphics();
        // No AA for the order comparison: coverage along newly split edges can differ by a pixel.
        Painter p=new DesktopPainter(graphics);p.rect(0,0,800,600,0xff182734);
        b.moving.draw(g,p,live?b.pose(value):b.moving.pose(value));graphics.dispose();return im;
    }
    public static void main(String[] args)throws Exception{
        File dir=new File("out/continuous-bridges");dir.mkdirs();Game g=new Game();int checks=0,worst=0;
        for(int region=0;region<2;region++){
            if(region==1){g.challengeId=Challenges.INDIANA;g.reset();}
            for(LandmarkBridge b:LocationBridges.forGame(g))if(b.movable()){
                g.layout=Layout.focused(800,600,b.x,b.y,1.35f,400,420);
                for(float amount:new float[]{0,.001f,.017f,.077f,.123f,.249f,.367f,.501f,.713f,.943f,1}){
                    BufferedImage live=draw(g,b,amount,true),exact=draw(g,b,amount,false);int covered=0,different=0;
                    for(int y=0;y<600;y++)for(int x=0;x<800;x++){
                        int a=live.getRGB(x,y),c=exact.getRGB(x,y);if(a!=0xff182734 || c!=0xff182734)covered++;
                        if(Math.abs((a>>16&255)-(c>>16&255))>12 || Math.abs((a>>8&255)-(c>>8&255))>12 || Math.abs((a&255)-(c&255))>12)different++;
                    }
                    // Different BSP cuts produce single-pixel raster seams; reject substantial wrong-order areas.
                    int permille=1000*different/Math.max(1,covered);worst=Math.max(worst,permille);checks++;
                    if(permille>10){ImageIO.write(live,"png",new File(dir,"failed-live-"+b.id+".png"));ImageIO.write(exact,"png",new File(dir,"failed-exact-"+b.id+".png"));throw new AssertionError(b.id+" at "+amount+": mismatch="+different+" / "+covered);}
                }
            }
        }
        System.out.println(checks+" live/exact world-BSP pixel comparisons; worst differing coverage "+worst+"/1000 (edge seams included)");
        // Show bascule, translating lift and rolling rail mechanisms at production headings.
        LandmarkBridge[] examples={LocationBridges.calumet()[7],LocationBridges.calumet()[0],LocationBridges.indiana()[1]};
        for(int tick=0;tick<=300;tick++){
            BufferedImage frame=new BufferedImage(1500,600,BufferedImage.TYPE_INT_RGB);Graphics2D all=frame.createGraphics();
            for(int i=0;i<examples.length;i++){
                LandmarkBridge b=examples[i];g.layout=Layout.focused(500,600,b.x,b.y,1.25f,250,410);g.visualTicks=tick;
                BufferedImage panel=new BufferedImage(500,600,BufferedImage.TYPE_INT_RGB);Graphics2D pg=panel.createGraphics();pg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Painter p=new DesktopPainter(pg);p.rect(0,0,500,600,0xff182734);b.draw(g,p,tick/300f,tick==300);Lettering.center(p,b.name,250,30,10,VisualStyle.CREAM);pg.dispose();all.drawImage(panel,i*500,0,null);
            }
            all.dispose();ImageIO.write(frame,"png",new File(dir,String.format("motion-%03d.png",tick)));
        }
        System.out.println("301 exact 60 Hz opening frames exported");
    }
}
