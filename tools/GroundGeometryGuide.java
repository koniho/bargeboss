package com.bargeboss.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Exact ground-plane silhouette only; never edits generated source pixels. */
public final class GroundGeometryGuide {
    public static void main(String[] args) throws Exception {
        BufferedImage image=new BufferedImage(1024,1024,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        int[] x={128,512,896,512},y={512,290,512,734};
        g.setColor(new Color(0xff535a59,true));g.fillPolygon(x,y,4);
        g.dispose();ImageIO.write(image,"png",new File("docs/art-studies/ground-v1/interior-guide.png"));
    }
}
