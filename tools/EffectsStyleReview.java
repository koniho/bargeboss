package com.bargeboss.game;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** Real renderer and event lifecycles, at repeatable early impact / release / settling phases. */
public final class EffectsStyleReview {
 public static void main(String[] args)throws Exception{
  File dir=new File(args.length>0?args[0]:"out/effects-style");dir.mkdirs();
  for(String scene:new String[]{"montrose-boost-review","damage-impact-review","damage-sink-review","montrose-markers-review"}){
   Game g=new Game();ProfileScenario.prepare(g,scene,1440,900);Layout camera=g.layout;int target=0;
   int[] frames=scene.equals("montrose-markers-review")?new int[]{15}:new int[]{0,4,10,20,40,70,100,126,138,149,180};
   for(int tick:frames){while(target<tick){g.step();g.layout=camera;target++;}
    BufferedImage im=new BufferedImage(1440,900,BufferedImage.TYPE_INT_RGB);Graphics2D p=im.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Renderer.draw(g,new DesktopPainter(p));p.dispose();
    ImageIO.write(im,"png",new File(dir,scene+"-"+String.format(java.util.Locale.ROOT,"%03d",tick)+".png"));
   }
  }
 }
}
