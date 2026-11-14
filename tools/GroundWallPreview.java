package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Terrain-only review while independent factory artwork is being generated. */
public final class GroundWallPreview {
    public static void main(String[] args) throws Exception {
        new File("out/ground-review").mkdirs();
        float[] focusX={-1000,130,300},focusY={390,500,650};
        String[] names={"corrugated-corner","open-harbor-mouth","raised-berth"};
        for(int i=0;i<names.length;i++) {
            Game root=new Game();root.startTutorial();Game g=root.activeGame();
            g.tutorial.lesson=Tutorial.CHANNEL;g.tug.x=focusX[i];g.tug.y=focusY[i];g.tug.angle=0;
            if(i==0)g.tug.y+=35;if(i==2)g.tug.x=205;
            g.layout=Layout.focused(960,600,focusX[i],focusY[i],1,480,330);g.visualTicks=80;
            BufferedImage image=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();
            Painter p=new DesktopPainter(graphics),world=new RiverPainter(p,g.layout.camera);
            p.rect(0,0,960,600,HarborAtmosphere.WATER_BASE);WaveView.surface(g,p);ArcadeView.water(g,world);
            TrainingGroundView.walls(g,p);TrainingGroundView.ground(g,p);TrainingGroundView.caps(g,p);
            VesselView.draw(g,world,g.tug,true);
            TrainingGroundView.foreground(g,p);
            Lettering.draw(p,"RAISED QUAY / CORRUGATED STEEL",20,20,16,VisualStyle.CREAM);
            Lettering.draw(p,"GEOMETRY REVIEW / FACTORIES NOT SHOWN",20,45,10,VisualStyle.GOLD);
            graphics.dispose();ImageIO.write(image,"png",new File("out/ground-review/"+names[i]+".png"));
        }
    }
}
