package com.bargeboss.game;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
public final class IndianaReview {
 public static void main(String[] args)throws Exception{
  File dir=new File("out/indiana");dir.mkdirs();
  float[][] views={{375,200,.32f},{600,-720,1},{400,440,1},{375,1480,1}};
  for(int i=0;i<views.length;i++){Game g=new Game();g.challengeId=Challenges.INDIANA;g.reset();float[] v=views[i];
   g.layout=Layout.focused(1243,700,v[0],v[1],v[2],621,350);
   BufferedImage image=new BufferedImage(1243,700,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Renderer.draw(g,new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File(dir,"view-"+i+".png"));
  }
 }
}
