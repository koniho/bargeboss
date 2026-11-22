import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Register generated flame cells to their bright base, preserving source alpha. */
public final class PrepareFlameSheet {
    public static void main(String[] args)throws Exception{
        BufferedImage source=ImageIO.read(new File("docs/art-studies/flame-v1/generated-sheet.png"));
        BufferedImage atlas=new BufferedImage(1024,1024,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=atlas.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        for(int f=0;f<16;f++){
            int l=f%4*source.getWidth()/4,t=f/4*source.getHeight()/4,r=(f%4+1)*source.getWidth()/4,b=(f/4+1)*source.getHeight()/4;
            BufferedImage cell=source.getSubimage(l,t,r-l,b-t);
            int baseY=0;double baseX=0;int pixels=0;
            for(int y=cell.getHeight()*3/4;y<cell.getHeight();y++)for(int x=cell.getWidth()/3;x<cell.getWidth()*2/3;x++){
                int c=cell.getRGB(x,y);if((c>>>24)>128 && (c>>16&255)>220 && (c>>8&255)>170)baseY=Math.max(baseY,y);
            }
            for(int y=baseY-5;y<=baseY;y++)for(int x=cell.getWidth()/3;x<cell.getWidth()*2/3;x++){
                int c=cell.getRGB(x,y);if((c>>>24)>128 && (c>>16&255)>220 && (c>>8&255)>170){baseX+=x;pixels++;}
            }
            if(pixels==0)throw new IllegalStateException("Missing bright flame base "+f);baseX/=pixels;
            // Constant scale preserves authored flicker heights; room above/below prevents cell bleed.
            double scale=.68;int ox=f%4*256,oy=f/4*256;
            g.setClip(ox+6,oy+6,244,244);
            java.awt.geom.AffineTransform tx=new java.awt.geom.AffineTransform();tx.translate(ox+128-baseX*scale,oy+232-baseY*scale);tx.scale(scale,scale);g.drawImage(cell,tx,null);
            System.out.println("frame "+f+" source base "+baseX+","+baseY+" -> 128,232");
        }
        g.dispose();ImageIO.write(atlas,"png",new File("assets/sprites/flame-loop-v1.png"));
    }
}
