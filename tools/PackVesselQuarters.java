package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Lossless cell repacking for four 4x4 quarter-turn candidates. No pose synthesis. */
public final class PackVesselQuarters {
    private PackVesselQuarters() {}
    public static void main(String[] args) throws Exception {
        if(args.length!=5) throw new IllegalArgumentException("PackVesselQuarters q0.png q1.png q2.png q3.png output.png");
        BufferedImage[] inputs=new BufferedImage[4];int size=0;
        for(int q=0;q<4;q++) {
            inputs[q]=ImageIO.read(new File(args[q]));
            if(inputs[q]==null || !inputs[q].getColorModel().hasAlpha())
                throw new IllegalArgumentException("Transparent PNG required: "+args[q]);
            if(inputs[q].getWidth()!=inputs[q].getHeight() || inputs[q].getWidth()<4)
                throw new IllegalArgumentException("Square 4x4 sheet required: "+args[q]);
            size=Math.max(size,(inputs[q].getWidth()+3)/4);
        }
        BufferedImage output=new BufferedImage(size*8,size*8,BufferedImage.TYPE_INT_ARGB);
        for(int f=0;f<64;f++) {
            BufferedImage cell=VesselRegistration.cell(inputs[f/16],f%16,4,4);
            int ox=(f%8)*size+(size-cell.getWidth())/2;
            int oy=(f/8)*size+(size-cell.getHeight())/2;
            for(int y=0;y<cell.getHeight();y++) for(int x=0;x<cell.getWidth();x++)
                output.setRGB(ox+x,oy+y,cell.getRGB(x,y));
        }
        File file=new File(args[4]);if(file.getParentFile()!=null)file.getParentFile().mkdirs();
        ImageIO.write(output,"png",file);
        System.out.println(file+": 64 source cells, "+size+"px cells; pixels copied without scaling or rotation. Candidate only: does not validate poses.");
    }
}
