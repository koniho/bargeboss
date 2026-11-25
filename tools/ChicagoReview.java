package com.bargeboss.game;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** Headless review frames of the Chicago River stage through the shared renderer. */
public final class ChicagoReview {
 public static void main(String[] args)throws Exception{
  File dir=new File("out/chicago");dir.mkdirs();
  String[] names={"overview","navy-pier-fire","harbor-lock","engine-2","ogden-slip","michigan-avenue","wolf-point"};
  double[][] at={{41.8880,-87.6185,.16},{41.8910,-87.6070,1.2},{41.8885,-87.6066,1.2},{41.8881,-87.6115,1.4},{41.8902,-87.6150,1.1},{41.8887,-87.6260,.9},{41.8870,-87.6380,.9}};
  for(int i=0;i<names.length;i++){Game g=new Game();g.challengeId=Challenges.CHICAGO;g.reset();
   for(int t=0;t<90;t++)g.step();
   float x=ChicagoRiver.north(at[i][0]),y=ChicagoRiver.east(at[i][1]);
   g.layout=Layout.focused(1243,700,x,y,(float)at[i][2],621,350);
   BufferedImage image=new BufferedImage(1243,700,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();
   p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
   long start=System.nanoTime();Renderer.draw(g,new DesktopPainter(p));long ms=(System.nanoTime()-start)/1000000;p.dispose();
   ImageIO.write(image,"png",new File(dir,names[i]+".png"));System.out.println(names[i]+" "+ms+" ms");
  }
  // Tug among the towers: fades settle over 60 ticks and its lamps light nearby walls.
  double[][] tugs={{41.88690,-87.63700,-Math.PI/2},{41.88855,-87.62550,Math.PI/2}};String[] tugNames={"wolf-tug","downtown-tug"};
  for(int i=0;i<tugs.length;i++){Game g=new Game();g.challengeId=Challenges.CHICAGO;g.reset();
   g.tug.x=ChicagoRiver.north(tugs[i][0]);g.tug.y=ChicagoRiver.east(tugs[i][1]);g.tug.angle=(float)tugs[i][2];g.tug.vx=g.tug.vy=0;
   g.layout=Layout.focused(1243,700,g.tug.x,g.tug.y,1.1f,621,350);
   for(int t=0;t<60;t++){g.step();g.tug.vx=g.tug.vy=0;g.layout=Layout.focused(1243,700,g.tug.x,g.tug.y,1.1f,621,350);}
   BufferedImage image=new BufferedImage(1243,700,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();
   p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
   Renderer.draw(g,new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File(dir,tugNames[i]+".png"));System.out.println(tugNames[i]);
  }
 }
}
