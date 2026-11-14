package com.bargeboss.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Read-only inspection of source pixels and the shared warehouse emitter registration. */
public final class SceneryAnchorPreview {
    public static void main(String[] args) throws Exception {
        BufferedImage source=ImageIO.read(new File("assets/sprites/static-harbor-v1.png"));
        BufferedImage image=new BufferedImage(1100,760,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setColor(new Color(24,48,58));g.fillRect(0,0,1100,760);
        g.drawImage(source.getSubimage(1448,0,724,724),0,0,null);
        g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));g.setColor(new Color(103,231,215));
        int[] xs={49,370,667,346},ys={524,689,512,347};g.drawPolygon(xs,ys,4);
        int px=Math.round(StaticHarborArt.pivotX(2)*724),py=Math.round(StaticHarborArt.pivotY(2)*724);
        g.drawLine(px-12,py,px+12,py);g.drawLine(px,py-12,px,py+12);
        g.drawString("Ground pivot / inferred footprint",730,40);
        g.setColor(new Color(255,207,90));g.drawString("Pixel-inspected window anchors",730,80);
        for(int i=0;i<StaticHarborArt.lightCount(2);i++) {
            int x=Math.round(StaticHarborArt.lightX(i)*724),y=Math.round(StaticHarborArt.lightY(i)*724);
            g.setColor(Color.BLACK);g.fillOval(x-8,y-8,16,16);g.setColor(new Color(255,207,90));g.drawOval(x-8,y-8,16,16);
            g.drawString(""+(i+1),x-4,y+5);g.drawString((i+1)+"  "+StaticHarborArt.lightId(i),730,120+i*30);
        }
        g.setColor(Color.WHITE);g.drawString("Smoke outlets: none",730,380);
        g.drawString("Roof pipe is a closed return bend.",730,410);
        g.drawString("Local XYZ is approximate;",730,470);g.drawString("runtime uses painted pane centers.",730,500);
        g.dispose();ImageIO.write(image,"png",new File("out/scenery-warehouse-anchors.png"));
    }
}
