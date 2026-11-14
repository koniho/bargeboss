package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Same current camera/geometry: original terrain composition versus warmed native layers. */
public final class TerrainLayerReview {
    private static void draw(Game g,Painter p,boolean cached) {
        p.beginFrame();p.rect(0,0,g.layout.width,g.layout.height,HarborAtmosphere.WATER_BASE);
        if(cached){StaticTerrain.draw(g,p,false);StaticTerrain.draw(g,p,true);}
        else {TrainingGroundView.walls(g,p);TrainingGroundView.ground(g,p);IndustrialRoadView.draw(g,p);TrainingGroundView.caps(g,p);TrainingGroundView.foreground(g,p);}
    }
    public static void main(String[] args)throws Exception {
        File directory=new File("out/terrain-layer-review");directory.mkdirs();
        System.out.println("width,scene,changedPercent,largeRGBPercent,meanChannelError");
        for(int width:new int[]{960,1333,1920})for(String scene:ScenePerformanceBenchmark.SCENES) {
            Game game=ScenePerformanceBenchmark.fixture(width,scene);BufferedImage[] versions=new BufferedImage[2];
            for(int i=0;i<2;i++) {
                versions[i]=new BufferedImage(width,600,BufferedImage.TYPE_INT_RGB);Graphics2D g=versions[i].createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Painter painter=new DesktopPainter(g);
                if(i==1)for(int warm=0;warm<80;warm++)draw(game,painter,true);
                draw(game,painter,i==1);g.dispose();
                ImageIO.write(versions[i],"png",new File(directory,width+"-"+scene+(i==0?"-direct":"-cached")+".png"));
            }
            long error=0;int changed=0,large=0;
            BufferedImage diff=new BufferedImage(width,600,BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<600;y++)for(int x=0;x<width;x++) {
                int a=versions[0].getRGB(x,y),b=versions[1].getRGB(x,y),difference=0;
                for(int shift=0;shift<=16;shift+=8)difference+=Math.abs((a>>shift&255)-(b>>shift&255));
                error+=difference;if(a!=b)changed++;if(difference>60)large++;
                int v=Math.min(255,difference*3);diff.setRGB(x,y,(v<<16)|(v<<8)|v);
            }
            System.out.printf(java.util.Locale.ROOT,"%d,%s,%.3f,%.3f,%.5f%n",width,scene,changed*100.0/(width*600),large*100.0/(width*600),error/(width*600.0*3));
            if(error/(width*600.0*3)>1 || large*100.0/(width*600)>.5)
                throw new AssertionError("Cached terrain changed more than the reviewed antialias/resampling tolerance: "+width+" "+scene);
            BufferedImage contact=new BufferedImage(width,1800,BufferedImage.TYPE_INT_RGB);Graphics2D g=contact.createGraphics();
            g.drawImage(versions[0],0,0,null);g.drawImage(versions[1],0,600,null);g.drawImage(diff,0,1200,null);g.dispose();
            ImageIO.write(contact,"png",new File(directory,width+"-"+scene+"-comparison.png"));
        }
    }
}
