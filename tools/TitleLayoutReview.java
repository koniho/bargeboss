package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Actual shared title composition at phone, desktop and tablet sizes. */
public final class TitleLayoutReview {
    public static void main(String[] args)throws Exception {
        File dir=new File(args.length==0?"out/title-layout":args[0]);dir.mkdirs();
        for(int[] size:new int[][]{{960,600},{1243,600},{1333,600},{960,720}}) {
            Game game=new Game();game.configureCanvas(size[0],size[1]);game.visualTicks=180;game.challengeId=1;
            BufferedImage image=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Renderer.draw(game,new DesktopPainter(g));g.dispose();
            ImageIO.write(image,"png",new File(dir,"title-"+size[0]+"x"+size[1]+".png"));
        }
    }
}
