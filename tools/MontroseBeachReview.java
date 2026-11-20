package com.bargeboss.game;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** Actual shared renderer at newly reachable shoreline and the retained marina. */
public final class MontroseBeachReview {
    static void capture(Game g,String name)throws Exception{
        BufferedImage image=new BufferedImage(1440,900,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Renderer.draw(g,new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File("out/montrose-beach/"+name+".png"));
    }
    public static void main(String[] args)throws Exception{
        new File("out/montrose-beach").mkdirs();Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();g.configureCanvas(1440,900);g.visualTicks=20;
        g.layout=MontroseHarbor.overview(1440,900);capture(g,"overview");
        String[] names={"beach","beacon","pavilion","inlet"};float[][] places={{1600,-600,.55f},{2200,-1380,1.1f},{1080,-600,1.1f},{453,1300,2}};
        for(int i=0;i<places.length;i++){float[] a=places[i];g.tug.x=MontroseHarbor.mapX(a[0]+100);g.tug.y=MontroseHarbor.mapY(a[1]-100);g.layout=Layout.focused(1440,900,MontroseHarbor.mapX(a[0]),MontroseHarbor.mapY(a[1]),a[2],720,450);capture(g,names[i]);}
        System.out.println("Actual Montrose beach renderer: out/montrose-beach");
    }
}
