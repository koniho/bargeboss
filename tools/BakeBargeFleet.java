package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

/** One rigid hull and its registered loads, rendered at 64 true headings. */
public final class BakeBargeFleet {
    static final String[] STEMS={"barge-isometric-v4","cargo-steel-isometric-v2","cargo-container-isometric-v3","cargo-coal-isometric-v4"};
    public static void main(String[]args)throws Exception{
        File dir=new File(args.length==0?"docs/art-studies/barge-fleet-64":args[0]);dir.mkdirs();
        WatercraftMesh hull=new WatercraftMesh();BargePoseGuide.materialModel(hull::face);
        for(int kind=0;kind<4;kind++){
            WatercraftMesh mesh=hull;
            if(kind>0){mesh=new WatercraftMesh();BargePoseGuide.cargoModel(kind==1?0:kind==2?1:3,mesh::face);}
            BufferedImage[] sheets=new BufferedImage[3];for(int m=0;m<3;m++)sheets[m]=new BufferedImage(2048,2048,BufferedImage.TYPE_INT_ARGB);
            StringBuilder csv=new StringBuilder("frame,pivot_x,pivot_y,uniform_scale,heading_degrees\n");
            for(int f=0;f<64;f++){
                BufferedImage[] maps=BakeWatercraftMaterials.bake(mesh,f*.5,77.5/.78,kind==0?null:hull,true);
                // Gentle neutral form shading for current non-normal-map production painters.
                // The geometry-derived maps remain separate for the future material renderer.
                for(int y=0;y<256;y++)for(int x=0;x<256;x++){
                    int p=maps[0].getRGB(x,y),n=maps[1].getRGB(x,y);if((p>>>24)==0)continue;
                    double nx=((n>>16)&255)/127.5-1,ny=((n>>8)&255)/127.5-1,nz=1-Math.abs(nx)-Math.abs(ny);
                    double light=.82+.22*Math.max(0,nz)+.16*Math.max(0,-nx-ny);
                    int r=Math.min(255,(int)(((p>>16)&255)*light)),g=Math.min(255,(int)(((p>>8)&255)*light)),b=Math.min(255,(int)((p&255)*light));maps[0].setRGB(x,y,(p&0xff000000)|(r<<16)|(g<<8)|b);
                }
                for(int m=0;m<3;m++)sheets[m].getRaster().setRect(f%8*256,f/8*256,maps[m].getRaster());
                csv.append(f).append(",0.5,0.72,1,").append(f*5.625).append("\n");
            }
            String stem=STEMS[kind];String[] suffix={"-64.png","-normal-height.png","-material.png"};
            for(int m=0;m<3;m++)ImageIO.write(sheets[m],"png",new File(dir,stem+suffix[m]));
            Files.write(new File(dir,stem+"-registration.csv").toPath(),csv.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println(stem+": 64 rigid poses; "+mesh.faces.size()+" source faces; fixed waterline and hold datum");
        }
    }
}
