package com.bargeboss.game;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
/** Verifies the denser guide preserves every original authored pose exactly. */
public final class VesselGuideCheck {
 public static void main(String[] args)throws Exception {
  for(String stem:new String[]{"tug-isometric-v5","barge-isometric-v1","cargo-steel-isometric-v1","cargo-container-isometric-v1","cargo-coal-isometric-v2"}) {
   BufferedImage a=ImageIO.read(new File("docs/art-studies/"+stem+"-pose-guide.png")), b=ImageIO.read(new File("docs/art-studies/"+stem+"-64-pose-guide.png"));
   int differences=0;
   for(int f=0;f<32;f++)for(int y=0;y<384;y++)for(int x=0;x<384;x++)if(a.getRGB(f%8*384+x,f/8*384+y)!=b.getRGB((f*2)%8*384+x,(f*2)/8*384+y))differences++;
   System.out.println(stem+": even-pose pixel differences="+differences);
   if(differences!=0)throw new AssertionError(stem);
  }
 }
}
