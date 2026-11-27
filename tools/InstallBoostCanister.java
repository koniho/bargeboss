package com.bargeboss.game;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** Offline runtime-size export; alpha and the complete authored perspective are preserved. */
public final class InstallBoostCanister {
 public static void main(String[] args)throws Exception{
  if(args.length==1 && args[0].equals("--check")){
   BufferedImage im=ImageIO.read(new File("assets/sprites/boost-canister-v1.png"));
   if(im.getWidth()!=192 || im.getHeight()!=288 || !im.getColorModel().hasAlpha())throw new AssertionError("Bounded alpha frame required");
   int visible=0;for(int y=0;y<im.getHeight();y++)for(int x=0;x<im.getWidth();x++){int alpha=im.getRGB(x,y)>>>24;if(alpha>128)visible++;if((x==0 || x==191 || y==0 || y==287) && alpha>8)throw new AssertionError("Painted silhouette touches frame edge");}
   if(visible<12000 || visible>48000)throw new AssertionError("Missing canister or opaque backdrop");
   System.out.println("Boost canister: 192x288 alpha frame, clear gutter, bounded 216 KiB decoded texture");return;
  }
  BufferedImage source=ImageIO.read(new File(args[0]));if(!source.getColorModel().hasAlpha())throw new IllegalArgumentException("Transparent source required");
  BufferedImage frame=new BufferedImage(192,288,BufferedImage.TYPE_INT_ARGB);Graphics2D g=frame.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(source,0,0,192,288,null);g.dispose();
  ImageIO.write(frame,"png",new File("assets/sprites/boost-canister-v1.png"));
 }
}
