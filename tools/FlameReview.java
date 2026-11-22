package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Real renderer previews and short sequence for reviewing planted flame animation. */
public final class FlameReview {
 public static void main(String[] args)throws Exception{
  Game root=new Game();root.configureCanvas(1243,600);ReplayInput.startPractice(root);Game g=root.activeGame();g.tutorial.lesson=Tutorial.EXTINGUISH;g.tutorial.setup(g);
  for(int i=0;i<100;i++)g.step();
  new File("out/flame-animation").mkdirs();
  for(int i=0;i<64;i++){
   for(int tick=0;tick<3;tick++)g.step();
   BufferedImage image=new BufferedImage(1243,600,BufferedImage.TYPE_INT_ARGB);Graphics2D canvas=image.createGraphics();canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);root.render(new DesktopPainter(canvas));canvas.dispose();
   ImageIO.write(image,"png",new File(String.format("out/flame-animation/%03d.png",i)));
  }
 }
}
