package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
/** Downsample four padded authoring cells into a small runtime texture; preserve generated alpha. */
public final class PackSplashStamps {
    public static void main(String[] args)throws Exception{
        BufferedImage source=ImageIO.read(new File(args[0]));
        if(!source.getColorModel().hasAlpha() || source.getWidth()%2!=0 || source.getHeight()%2!=0)throw new AssertionError("RGBA 2x2 source required");
        BufferedImage result=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=result.createGraphics();g.setComposite(AlphaComposite.Src);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        int sw=source.getWidth()/2,sh=source.getHeight()/2;
        for(int i=0;i<4;i++){int x=i%2*128,y=i/2*128;g.drawImage(source,x,y,x+128,y+128,i%2*sw,i/2*sh,(i%2+1)*sw,(i/2+1)*sh,null);}
        g.dispose();
        for(int i=0;i<4;i++){int painted=0,open=0;for(int y=0;y<128;y++)for(int x=0;x<128;x++){
            int a=result.getRGB(i%2*128+x,i/2*128+y)>>>24;
            if((x<4||y<4||x>=124||y>=124)&&a>2)throw new AssertionError("Unpadded splash cell "+i);
            if(a>50)painted++;if(a==0)open++;
        }if(painted<100||open<8000)throw new AssertionError("Missing or opaque splash "+i);}
        ImageIO.write(result,"png",new File("assets/sprites/collision-splash-stamps-v1.png"));
        System.out.println("Four transparent padded splash stamps: 128px cells, 256 KiB decoded atlas");
    }
}
