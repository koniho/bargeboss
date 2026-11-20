package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import javax.imageio.ImageIO;

/** Offline format and geometry regression; does not certify artistic quality. */
public final class WatercraftMaterialCheck {
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static float[] normal(int p){
        float x=((p>>16)&255)/127.5f-1,y=((p>>8)&255)/127.5f-1,z=1-Math.abs(x)-Math.abs(y);
        if(z<0){float old=x;x=(1-Math.abs(y))*Math.copySign(1,old);y=(1-Math.abs(old))*Math.copySign(1,y);}
        float n=(float)Math.sqrt(x*x+y*y+z*z);return new float[]{x/n,y/n,z/n};
    }
    private static void geometry(){
        WatercraftMesh mesh=new WatercraftMesh();
        // Horizontal submerged surface: negative heights must survive the export.
        mesh.face(new double[][]{{-10,-10,-2},{10,-10,-2},{10,10,-2},{-10,10,-2}},0xcccccc);
        BufferedImage[] images=BakeWatercraftMaterials.bake(mesh,0,100);
        int sample=images[1].getRGB(128,188);require((sample>>>24)==255,"Submerged test surface missing");
        float height=(sample&255)*96f/255-8;require(Math.abs(height+2)<.4,"Negative height lost: "+height);
        float[] n=normal(sample);require(n[2]>.999,"Horizontal surface normal not upward");
        // Vertical face with local normal +Y; after camera registration it points (-Q,+Q,0).
        mesh=new WatercraftMesh();mesh.face(new double[][]{{-10,0,0},{-10,0,20},{10,0,20},{10,0,0}},0xcccccc);
        images=BakeWatercraftMaterials.bake(mesh,0,100);
        n=normal(images[1].getRGB(128,160));
        require(n[0]<-.69&&n[1]>.69&&Math.abs(n[2])<.02,"World normal registration wrong");
        float front=-n[0]*.70710678f+n[1]*.70710678f;
        require(front>.99&&Math.max(0,-front)==0,"Opposite light should not illuminate face");
    }
    public static void main(String[] args)throws Exception{
        geometry();File dir=new File(args.length==0?"build/materials/export":args[0]);
        for(String name:BakeWatercraftMaterials.FAMILIES){
            BufferedImage[] maps=new BufferedImage[3];String[] labels={"base","normal-height","material"};
            for(int i=0;i<3;i++){maps[i]=ImageIO.read(new File(dir,name+"-"+labels[i]+".png"));require(maps[i].getWidth()==2048&&maps[i].getHeight()==1024,"Invalid sheet dimensions");}
            HashSet<Long> headings=new HashSet<Long>();
            for(int frame=0;frame<32;frame++){
                BufferedImage packed=ImageIO.read(new File(dir,name+"-frames/"+String.format(java.util.Locale.ROOT,"%02d.png",frame)));
                require(packed.getWidth()==256&&packed.getHeight()==768,"Invalid packed frame");
                int visible=0;long hash=0xcbf29ce484222325L;
                for(int y=0;y<256;y++)for(int x=0;x<256;x++){
                    int sx=frame%8*256+x,sy=frame/8*256+y,alpha=maps[0].getRGB(sx,sy)>>>24;
                    if(alpha>0){visible++;require(x>0&&x<255&&y>0&&y<255,"Clipped silhouette: "+name+" frame "+frame);}
                    for(int map=0;map<3;map++){
                        int pixel=maps[map].getRGB(sx,sy);
                        require((pixel>>>24)==alpha,"Material coverage mismatch");
                        require(pixel==packed.getRGB(x,y+map*256),"Frame pack mismatch");
                        if(map==2)require((pixel&255)==0,"Reserved material channel is nonzero");
                        hash=(hash^pixel)*0x100000001b3L;
                    }
                }
                require(visible>100,"Empty heading");require(headings.add(hash),"Duplicate material heading");
            }
            System.out.println(name+": 32 registered, unclipped frame packs; 24 MiB decoded per complete rotation");
        }
        System.out.println("Material geometry, signed height, face response and packing checks passed. Art/anchor/native acceptance remains separate.");
    }
}
