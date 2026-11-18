package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Actual title frames and a fixed-wall crest sequence, for depth and foam review. */
public final class TitleRockWaveReview {
    public static void main(String[] args)throws Exception{
        File dir=new File("out/title-rock-waves");dir.mkdirs();
        for(int tick:new int[]{0,30,60,90,120,180,239}){
            Game game=new Game();game.configureCanvas(1243,600);game.challengeId=1;game.visualTicks=tick;
            BufferedImage image=new BufferedImage(1243,600,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Renderer.draw(game,new DesktopPainter(g));g.dispose();ImageIO.write(image,"png",new File(dir,"title-"+tick+".png"));
        }
        BufferedImage strip=new BufferedImage(960,110*6,BufferedImage.TYPE_INT_RGB);Graphics2D g=strip.createGraphics();
        for(int i=0;i<6;i++){
            Graphics2D row=(Graphics2D)g.create();row.translate(0,i*110);row.setClip(0,0,960,110);
            row.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Game game=new Game();game.visualTicks=i*40;Painter p=new DesktopPainter(row);p.rect(0,0,960,110,0xff071e29);
            TitleRockWaves.draw(game,p,0,92);row.dispose();
        }
        g.dispose();ImageIO.write(strip,"png",new File(dir,"crest-sequence.png"));
    }
}
