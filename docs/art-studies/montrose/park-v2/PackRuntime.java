import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

/** Offline source-preserving park copy / uniform waterfront material downsample. */
public class PackRuntime {
  public static void main(String[] args)throws Exception{
    BufferedImage park=ImageIO.read(new File("docs/art-studies/montrose/park-v2/parkland-atlas-v2-alpha.png"));
    BufferedImage dock=ImageIO.read(new File("docs/art-studies/montrose/waterfront-v2/waterfront-v2-candidate.png"));
    BufferedImage out=new BufferedImage(1776,888,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g=out.createGraphics();g.setComposite(AlphaComposite.Src);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(dock,0,0,444,444,627,0,1254,627,null);
    g.drawImage(dock,444,0,888,444,0,0,627,627,null);g.dispose();
    int[] source={0,1,2,3,4,7};
    for(int j=0;j<6;j++){int f=source[j],sx=Math.round((f%4)*park.getWidth()/4f),sy=Math.round((f/4)*park.getHeight()/2f),w=Math.round((f%4+1)*park.getWidth()/4f)-sx,h=Math.round((f/4+1)*park.getHeight()/2f)-sy,df=j+2;
      for(int y=0;y<h;y++)for(int x=0;x<w;x++)out.setRGB(df%4*444+x,df/4*444+y,park.getRGB(sx+x,sy+y));
    }
    ImageIO.write(out,"png",new File("assets/sprites/marina-tiles-v1.png"));
    System.out.println("1776x888;444px cells; waterfront uniformly downsampled627->444; park copies bit-exact with transparent right/bottom padding");
  }
}
