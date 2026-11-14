package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Pixel comparison fixture for terrain culling, independent of factory/effect edits. */
public final class TerrainRenderReview {
    public static void main(String[] args) throws Exception {
        File directory=new File(args[0]);directory.mkdirs();
        for(int width:new int[]{960,1333,1920}) for(String scene:ScenePerformanceBenchmark.SCENES) {
            Game g=ScenePerformanceBenchmark.fixture(width,scene);
            BufferedImage image=new BufferedImage(width,600,BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics=image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Painter p=new DesktopPainter(graphics);p.rect(0,0,width,600,HarborAtmosphere.WATER_BASE);
            TrainingGroundView.walls(g,p);TrainingGroundView.ground(g,p);IndustrialRoadView.draw(g,p);
            TrainingGroundView.caps(g,p);TrainingGroundView.foreground(g,p);
            graphics.dispose();ImageIO.write(image,"png",new File(directory,width+"-"+scene+".png"));
            if(args.length>1) {
                BufferedImage before=ImageIO.read(new File(args[1],width+"-"+scene+".png"));
                int differences=0;
                for(int y=0;y<600;y++) for(int x=0;x<width;x++) if(before.getRGB(x,y)!=image.getRGB(x,y)) differences++;
                System.out.println(width+" "+scene+": "+differences+" changed pixels");
                if(differences!=0) throw new AssertionError("Terrain culling changed visible pixels");
            }
        }
    }
}
