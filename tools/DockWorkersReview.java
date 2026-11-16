package com.bargeboss.game;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Production-renderer crew/ground review at working berth and close detail. */
public final class DockWorkersReview {
 public static void main(String[] args)throws Exception {
  new File("out").mkdirs();
  for(int stage=0;stage<4;stage++) {
   Game g=new Game();g.challengeId=stage;g.reset();g.mode=Game.PLAY;g.configureCanvas(1333,750);g.visualTicks=35;
   for(int zoom=0;zoom<2;zoom++) {
    float x=stage==1 || stage==3?545:120,y=g.dockY(stage==1?2:0);
    g.layout=Layout.focused(1333,750,x,y,zoom==0?1.1f:3,666,460);
    BufferedImage frame=new BufferedImage(1333,750,BufferedImage.TYPE_INT_RGB);Graphics2D p=frame.createGraphics();
    p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    g.render(new DesktopPainter(p));p.dispose();ImageIO.write(frame,"png",new File("out/dock-workers-"+stage+"-"+zoom+".png"));
   }
  }
 }
}
