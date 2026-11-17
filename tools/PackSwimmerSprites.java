package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
/** Register generated art to a shared water-contact pivot and bounded runtime cells. */
public final class PackSwimmerSprites {
    public static void main(String[] args)throws Exception{
        BufferedImage source=ImageIO.read(new File("docs/art-prompts/sources/swimmers-rescue-v1-source.png"));
        if(source.getWidth()!=1448 || source.getHeight()!=1086 || !source.getColorModel().hasAlpha())throw new AssertionError("Expected RGBA 4x3 source");
        int[] contact={295,300,300,300,265,270,270,280,325,325,325,325};
        BufferedImage atlas=new BufferedImage(512,384,BufferedImage.TYPE_INT_ARGB);
        for(int cell=0;cell<12;cell++){
            BufferedImage frame=source.getSubimage(cell%4*362,cell/4*362,362,362);
            BufferedImage target=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=target.createGraphics();g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.translate(64-181*.30,92-contact[cell]*.30);g.scale(.30,.30);g.drawImage(frame,0,0,null);g.dispose();
            int painted=0;
            for(int y=0;y<128;y++)for(int x=0;x<128;x++){
                int argb=target.getRGB(x,y),a=argb>>>24;
                // Generation leaves saturated RGB in near-transparent padding; clear only that residue.
                if(a<8){argb=0;a=0;}
                if((x<4||y<4||x>=124||y>=124)&&a>2)throw new AssertionError("Cell edge residue "+cell);
                if(a>32)painted++;
                atlas.setRGB(cell%4*128+x,cell/4*128+y,argb);
            }
            if(painted<500)throw new AssertionError("Empty swimmer "+cell);
        }
        ImageIO.write(atlas,"png",new File("assets/sprites/swimmers-rescue-v1.png"));
        System.out.println("12 RGBA cells, 128x128; pivot 64,92; atlas 512x384 / 768 KiB decoded");
    }
}
