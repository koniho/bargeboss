package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Production 3-D swimmer poses in a water-plane detail sheet and the complete shared scene. */
public final class SwimmerReview {
 public static void main(String[] args)throws Exception{
  new File("out").mkdirs();Game g=ReplayInput.montroseJob(Challenges.SWIMMER_RESCUE);
  BufferedImage im=new BufferedImage(1200,600,BufferedImage.TYPE_INT_RGB);Graphics2D gg=im.createGraphics();gg.setColor(new Color(0x142e38));gg.fillRect(0,0,1200,600);gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
  for(int row=0;row<2;row++)for(int col=0;col<6;col++){
   int id=col;g.visualTicks=col*13;g.rescue.state[id]=row;
   g.layout=Layout.focused(1200,600,g.rescue.x[id],g.rescue.y[id],13,100+col*200,180+row*280);
   Painter p=new DesktopPainter(gg);SwimmerModel.water(g,new RiverPainter(p,g.layout.camera),id);MontroseRescueView.draw(g,p,id);
  }
  gg.dispose();ImageIO.write(im,"png",new File("out/swimmer-3d-poses.png"));
  g=ReplayInput.montroseJob(Challenges.SWIMMER_RESCUE);g.visualTicks=50;g.rescue.state[2]=1;g.rescue.state[3]=1;
  g.layout=Layout.focused(1200,750,g.rescue.x[5],g.rescue.y[5],6,600,430);g.tug.x=g.rescue.x[5]-40;g.tug.y=g.rescue.y[5]+45;
  im=new BufferedImage(1200,750,BufferedImage.TYPE_INT_RGB);gg=im.createGraphics();gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Renderer.draw(g,new DesktopPainter(gg));gg.dispose();ImageIO.write(im,"png",new File("out/swimmer-3d-scene.png"));
 }
}
