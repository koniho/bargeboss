package com.bargeboss.game;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** Matched, production-renderer review cameras; the same fixtures run on the development phone. */
public final class SceneryAestheticsReview {
 public static void main(String[] args)throws Exception{
  File dir=new File(args.length==0?"out/scenery-review":args[0]);dir.mkdirs();
  for(String scene:new String[]{"montrose-pavilion-study","montrose-concession-study","islands-cottage-study","islands-channel-study"}){
   Game g=new Game();ProfileScenario.prepare(g,scene,1440,900);
   BufferedImage image=new BufferedImage(1440,900,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
   Renderer.draw(g,new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File(dir,scene+".png"));
  }
 }
}
