package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Same-state whole-scene image comparison for rendering optimizations. */
public final class SceneRenderReview {
    public static void main(String[] args) throws Exception {
        File directory=new File(args[0]);directory.mkdirs();
        boolean compare=args.length>1 && !args[1].equals("warm");
        boolean warm=args.length>1 && args[args.length-1].equals("warm");
        for(int width:new int[]{960,1333,1920}) {
            BufferedImage comparison=compare?new BufferedImage(width*2,ScenePerformanceBenchmark.SCENES.length*600,BufferedImage.TYPE_INT_RGB):null;
            Graphics2D sheet=comparison==null?null:comparison.createGraphics();int row=0;
            for(String scene:ScenePerformanceBenchmark.SCENES) {
                Game g=ScenePerformanceBenchmark.fixture(width,scene);
                BufferedImage image=new BufferedImage(width,600,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                if(warm)for(int frame=0;frame<80;frame++)Renderer.draw(g,new DesktopPainter(graphics));
                Renderer.draw(g,new DesktopPainter(graphics));graphics.dispose();
                ImageIO.write(image,"png",new File(directory,width+"-"+scene+".png"));
                if(compare) {
                    BufferedImage before=ImageIO.read(new File(args[1],width+"-"+scene+".png"));
                    long error=0;int changed=0,large=0;
                    for(int y=0;y<600;y++)for(int x=0;x<width;x++) {
                        int a=before.getRGB(x,y),b=image.getRGB(x,y),difference=0;
                        for(int shift=0;shift<=16;shift+=8)difference+=Math.abs((a>>shift&255)-(b>>shift&255));
                        error+=difference;if(a!=b)changed++;if(difference>60)large++;
                    }
                    System.out.printf(java.util.Locale.ROOT,"%d %s: changed=%.2f%% largeRGBChange=%.2f%% meanChannelError=%.3f/255; tick=%d visualTick=%d tug=%.3f,%.3f%n",
                            width,scene,changed*100.0/(width*600),large*100.0/(width*600),error/(width*600.0*3),g.ticks,g.visualTicks,g.tug.x,g.tug.y);
                    sheet.drawImage(before,0,row*600,null);sheet.drawImage(image,width,row*600,null);
                }
                row++;
            }
            if(sheet!=null){sheet.dispose();ImageIO.write(comparison,"png",new File(directory,"BargeBoss-scene-performance-before-after-"+width+"-v1.png"));}
        }
    }
}
