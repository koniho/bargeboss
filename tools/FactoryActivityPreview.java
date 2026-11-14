package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual shared renderer with a fixed camera/pose and a controlled visual-clock sweep. */
public final class FactoryActivityPreview {
    public static void main(String[] args)throws Exception {
        boolean study=args.length>0 && args[0].equals("study");
        File dir=new File("out/factory-activity/"+(study?"study":"scene"));dir.mkdirs();
        Game g=ScenePerformanceBenchmark.fixture(1333,"canal-middle");int base=g.visualTicks;
        int width=study?1200:1333,height=study?720:600;
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        DesktopPainter painter=new DesktopPainter(graphics);
        if(!study)for(int warm=0;warm<60;warm++)Renderer.draw(g,painter);
        for(int frame=0;frame<144;frame++) {
            g.visualTicks=study?frame*5:base+frame*5;
            if(study) {
                painter.beginFrame();painter.rect(0,0,width,height,0xff0d1d27);
                int[] assets={0,2,5,12};
                for(int slot=0;slot<assets.length;slot++) {
                    int asset=assets[slot];float scale=1.35f,px=slot%2*600+300,py=slot/2*360+285;
                    RiverCamera camera=new RiverCamera(px-RiverCamera.Q*320*scale,py,scale,0,0,1.5f);
                    g.layout=Layout.renderTile(width,height,camera);g.tutorial=null;
                    FactoryArt.draw(g,painter,asset,0,0);
                    Lettering.center(painter,FactoryArt.id(asset).replace('-',' ').toUpperCase(java.util.Locale.ROOT),px,slot/2*360+335,13,0xffdfd4b3);
                }
            } else Renderer.draw(g,painter);
            ImageIO.write(image,"png",new File(dir,String.format(java.util.Locale.ROOT,"%04d.png",frame)));
        }
        graphics.dispose();System.out.println("Wrote144 frames at12fps,5 visual ticks/frame; fixed scene pose, live shared effects: "+dir);
    }
}
