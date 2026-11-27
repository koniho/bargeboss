import java.awt.*;import java.awt.image.*;import java.io.*;import javax.imageio.*;
/** Offline only: six padded 256px runtime frames, preserving the generated alpha. */
public final class PackIslandWoodland {
 public static void main(String[] args)throws Exception{
  BufferedImage src=ImageIO.read(new File("docs/art-studies/islands/woodland-v1/source.png"));
  File out=new File("assets/sprites/frames/islands-woodland-v1");out.mkdirs();
  for(int frame=0;frame<6;frame++){
   BufferedImage cell=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
   Graphics2D g=cell.createGraphics();g.setComposite(AlphaComposite.Src);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
   int x=frame%3*512,y=frame/3*512;g.drawImage(src,4,4,252,252,x,y,x+512,y+512,null);g.dispose();
   ImageIO.write(cell,"png",new File(out,frame+".png"));
  }
 }
}
