package com.bargeboss.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual shared chimney/glow rendering and read-only named source-anchor inspection. */
public final class SceneryEffectsPreview {
    public static void main(String[] args) throws Exception {
        File dir=new File("out/scenery-lighting");dir.mkdirs();
        BufferedImage source=ImageIO.read(new File("assets/sprites/chimneys-v1.png"));
        BufferedImage anchors=new BufferedImage(1774,960,BufferedImage.TYPE_INT_RGB);
        Graphics2D ag=anchors.createGraphics();ag.setColor(new Color(0x173c45));ag.fillRect(0,0,1774,960);ag.drawImage(source,0,0,null);
        ag.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,18));
        for(int f=0;f<2;f++) {
            point(ag,f*887+ChimneyArt.pivotX(f)*887,ChimneyArt.pivotY(f)*887,new Color(0x71f3ce));
            point(ag,f*887+ChimneyArt.outletX(f)*887,ChimneyArt.outletY(f)*887,new Color(0x9bd8ff));
            point(ag,f*887+ChimneyArt.lightX(f)*887,ChimneyArt.lightY(f)*887,new Color(0xffcb63));
            ag.setColor(Color.WHITE);ag.drawString(ChimneyArt.outletId(f)+" / "+ChimneyArt.lightId(f),f*887+70,928);
        }
        ag.dispose();ImageIO.write(anchors,"png",new File(dir,"chimney-anchors-v1.png"));
        BufferedImage phases=new BufferedImage(1440,900,BufferedImage.TYPE_INT_RGB);
        Graphics2D pg=phases.createGraphics();
        for(int i=0;i<3;i++) {
            Game g=new Game();g.reset();g.visualTicks=90+i*90;
            BufferedImage image=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);Graphics2D draw=image.createGraphics();
            draw.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Renderer.draw(g,new DesktopPainter(draw));draw.dispose();
            ImageIO.write(image,"png",new File(dir,"scene-"+i+".png"));
            pg.drawImage(image,(i%2)*720,(i/2)*450,720,450,null);
        }
        pg.setColor(new Color(0x142b35));pg.fillRect(720,450,720,450);pg.setColor(new Color(0xffd487));pg.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,23));
        pg.drawString("Organic glow + attached industrial smoke",754,523);
        pg.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,19));pg.drawString("Same scene at ticks90,180,270. Shared Java renderer.",754,567);
        pg.drawString("Source pixels unchanged; smoke/glow are independent.",754,600);pg.dispose();
        ImageIO.write(phases,"png",new File(dir,"scenery-effects-v1.png"));
    }
    private static void point(Graphics2D g,float x,float y,Color color) {g.setColor(Color.BLACK);g.fillOval((int)x-8,(int)y-8,16,16);g.setColor(color);g.drawOval((int)x-8,(int)y-8,16,16);g.drawLine((int)x-12,(int)y,(int)x+12,(int)y);g.drawLine((int)x,(int)y-12,(int)x,(int)y+12);}
}
