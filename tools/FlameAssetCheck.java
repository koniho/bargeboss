import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
public final class FlameAssetCheck {
 public static void main(String[] args)throws Exception{
  BufferedImage image=ImageIO.read(new File("assets/sprites/flame-loop-v1.png"));
  if(image.getWidth()!=1024 || image.getHeight()!=1024 || !image.getColorModel().hasAlpha())throw new AssertionError("flame atlas dimensions/alpha");
  java.util.HashSet<Long> hashes=new java.util.HashSet<>();
  for(int f=0;f<16;f++){
   int occupied=0,soft=0,base=0;long hash=17;
   for(int y=0;y<256;y++)for(int x=0;x<256;x++){
    int c=image.getRGB(f%4*256+x,f/4*256+y),a=c>>>24;hash=31*hash+c;
    if((x<6 || y<6 || x>=250 || y>=250) && a!=0)throw new AssertionError("frame bleed "+f);
    if(a>0)occupied++;if(a>0 && a<255)soft++;
    if(Math.abs(x-128)<8 && Math.abs(y-232)<5 && a>128 && (c>>8&255)>170)base++;
   }
   if(occupied<1000 || soft<500 || base<8 || !hashes.add(hash))throw new AssertionError("empty/opaque/duplicate or unregistered flame "+f);
  }
  System.out.println("16 unique flame cells: transparent gutters, soft alpha and registered bright bases passed");
 }
}
