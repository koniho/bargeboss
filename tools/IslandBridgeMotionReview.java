package com.bargeboss.game;
import java.awt.*;import java.awt.image.*;import java.io.*;import javax.imageio.*;
/** Production-renderer closed, lifting and raised views using exact continuous transforms. */
public final class IslandBridgeMotionReview {
 public static void main(String[] args)throws Exception{
  File dir=new File("out/scenery-after/bridge-motion");dir.mkdirs();Game g=new Game();ProfileScenario.prepare(g,"islands-channel-study",1440,900);
  for(int tick:new int[]{0,15,30,45,60}){
   for(int k=0;k<g.arena.open.length;k++){g.arena.openTicks[k]=tick;g.arena.open[k]=tick/60f;g.arena.footbridges[k].update(g.arena.open[k]);}
   BufferedImage b=new BufferedImage(1440,900,BufferedImage.TYPE_INT_RGB);Graphics2D p=b.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Renderer.draw(g,new DesktopPainter(p));p.dispose();ImageIO.write(b,"png",new File(dir,tick+".png"));
  }
 }
}
