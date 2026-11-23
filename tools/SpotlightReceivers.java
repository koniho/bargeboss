package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Offline registered receiver derivatives. No invented scenery or source-art changes. */
public final class SpotlightReceivers {
 public static void main(String[] args)throws Exception{
  for(int atlas:SpotlightArt.SOURCES)for(int frame=0;frame<SpriteAtlas.count(atlas);frame++){
   BufferedImage src=ImageIO.read(new File("assets/sprites/"+SpriteAtlas.file(atlas,frame)));
   int x=Math.round(src.getWidth()*SpriteAtlas.sourceLeft(atlas,frame)),y=Math.round(src.getHeight()*SpriteAtlas.sourceTop(atlas,frame));
   int w=Math.round(src.getWidth()*SpriteAtlas.sourceRight(atlas,frame))-x,h=Math.round(src.getHeight()*SpriteAtlas.sourceBottom(atlas,frame))-y;
   float scale=Math.min(1,256f/Math.max(w,h));BufferedImage out=new BufferedImage(Math.max(1,Math.round(w*scale)),Math.max(1,Math.round(h*scale)),BufferedImage.TYPE_INT_ARGB);
   Graphics2D g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
   g.drawImage(src,0,0,out.getWidth(),out.getHeight(),x,y,x+w,y+h,null);g.dispose();
   File file=new File("assets/sprites/frames/spotlight-"+atlas+"/"+frame+".png");file.getParentFile().mkdirs();ImageIO.write(out,"PNG",file);
  }
 }
}
