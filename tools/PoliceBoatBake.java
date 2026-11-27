package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;
/** Deterministic M3 model export; preserves all existing fleet pixels. */
public final class PoliceBoatBake {
    static final File ROOT=new File("docs/art-studies/police-m3");
    public static void main(String[] args)throws Exception{
        boolean check=args.length>0 && args[0].equals("--check");ROOT.mkdirs();
        BufferedImage old=ImageIO.read(new File("assets/sprites/montrose-moored-boats-v1.png")),on=ImageIO.read(new File("assets/sprites/montrose-moored-normals-v1.png"));
        int oldCount=old.getHeight()/128/4*16;
        BufferedImage sheet=new BufferedImage(2048,MooredBoats.ROWS*128,2),normal=new BufferedImage(2048,MooredBoats.BASE_FRAMES/16*128,2);
        Graphics2D s=sheet.createGraphics(),n=normal.createGraphics();
        for(int f=0;f<224;f++){
            n.drawImage(on.getSubimage(f%16*128,f/16*128,128,128),f%16*128,f/16*128,null);
            for(int layer=0;layer<4;layer++){int src=layer*oldCount+f,dst=layer*MooredBoats.BASE_FRAMES+f;s.drawImage(old.getSubimage(src%16*128,src/16*128,128,128),dst%16*128,dst/16*128,null);}
        }
        BufferedImage albedo=new BufferedImage(1024,512,2),materials=new BufferedImage(1024,512,2),normals=new BufferedImage(1024,512,2);
        Graphics2D ag=albedo.createGraphics(),mg=materials.createGraphics(),ng=normals.createGraphics();
        for(int pose=0;pose<32;pose++){
            BakeMooredBoats.angle=pose*(float)(Math.PI/16);BakeMooredBoats.faces=new ArrayList<BakeMooredBoats.Face>();
            final ArrayList<Integer> mats=new ArrayList<Integer>();
            PoliceBoatModel.build((color,material,xyz)->{BakeMooredBoats.face(color,xyz);mats.add(material);});
            int count=BakeMooredBoats.RASTER*BakeMooredBoats.RASTER;
            BakeMooredBoats.owners=new int[count];Arrays.fill(BakeMooredBoats.owners,-1);
            BakeMooredBoats.depths=new float[count];Arrays.fill(BakeMooredBoats.depths,Float.NEGATIVE_INFINITY);BakeMooredBoats.heights=new float[count];
            for(int i=0;i<BakeMooredBoats.faces.size();i++)BakeMooredBoats.rasterFace(i,BakeMooredBoats.faces.get(i));
            BufferedImage color=BakeMooredBoats.resolve(0,0),norm=BakeMooredBoats.resolve(4,0),mat=new BufferedImage(128,128,2),alb=new BufferedImage(128,128,2);
            for(int y=0;y<128;y++)for(int x=0;x<128;x++){
                int coverage=color.getRGB(x,y)>>>24;if(coverage==0)continue;
                int r=0,g=0,b=0,rough=0,samples=0;
                for(int yy=0;yy<BakeMooredBoats.AA;yy++)for(int xx=0;xx<BakeMooredBoats.AA;xx++){
                    int at=(y*BakeMooredBoats.AA+yy)*BakeMooredBoats.RASTER+x*BakeMooredBoats.AA+xx,id=BakeMooredBoats.owners[at];if(id<0)continue;
                    int c=BakeMooredBoats.faces.get(id).color,m=mats.get(id);r+=c>>16&255;g+=c>>8&255;b+=c&255;rough+=m==PoliceBoatModel.GLASS?35:m==PoliceBoatModel.METAL?110:190;samples++;
                }
                alb.setRGB(x,y,(coverage<<24)|((r/samples)<<16)|((g/samples)<<8)|b/samples);
                mat.setRGB(x,y,(coverage<<24)|((rough/samples)<<16));
            }
            int frame=PoliceBoatModel.BASE+pose;
            n.drawImage(norm,frame%16*128,frame/16*128,null);
            for(int layer=0;layer<4;layer++){int dst=layer*MooredBoats.BASE_FRAMES+frame;s.drawImage(layer==0?color:BakeMooredBoats.resolve(layer,0),dst%16*128,dst/16*128,null);}
            ag.drawImage(alb,pose%8*128,pose/8*128,null);mg.drawImage(mat,pose%8*128,pose/8*128,null);ng.drawImage(norm,pose%8*128,pose/8*128,null);
        }
        s.dispose();n.dispose();ag.dispose();mg.dispose();ng.dispose();InstallRacingBoat.check=check;
        InstallRacingBoat.save(sheet,new File("assets/sprites/montrose-moored-boats-v1.png"));InstallRacingBoat.save(normal,new File("assets/sprites/montrose-moored-normals-v1.png"));
        InstallRacingBoat.save(albedo,new File(ROOT,"m3-albedo-v1.png"));InstallRacingBoat.save(normals,new File(ROOT,"m3-normal-v1.png"));InstallRacingBoat.save(materials,new File(ROOT,"m3-material-v1.png"));
        shadows(albedo,check);
        System.out.println("M3: 32 rigid poses, registered albedo/normal/material and legacy light bands "+(check?"verified":"exported"));
    }
    static void shadows(BufferedImage color,boolean check)throws Exception{
  // Source-alpha shadow layers share the waterline pivot and contain no painted light.
  BufferedImage shadows=new BufferedImage(1024,1024,BufferedImage.TYPE_INT_ARGB);
  for(int style=0;style<2;style++)for(int pose=0;pose<32;pose++){
   BufferedImage mask=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB),cell=color.getSubimage(pose%8*128,pose/8*128,128,128);
   for(int y=0;y<128;y++)for(int x=0;x<128;x++){int a=cell.getRGB(x,y)>>>24;mask.setRGB(x,y,a<<24);}
   BufferedImage flat=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);Graphics2D p=flat.createGraphics();p.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
   p.translate(style==0?0:4,89.6*(style==0?.42:.2)+(style==0?0:2));p.scale(1,style==0?.58:.8);p.drawImage(mask,0,0,null);p.dispose();
   int radius=style==0?1:3;BufferedImage soft=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
   for(int y=radius;y<128-radius;y++)for(int x=radius;x<128-radius;x++){int sum=0;for(int yy=-radius;yy<=radius;yy++)for(int xx=-radius;xx<=radius;xx++)sum+=flat.getRGB(x+xx,y+yy)>>>24;soft.setRGB(x,y,(sum/((radius*2+1)*(radius*2+1)))<<24);}
   Graphics2D p2=shadows.createGraphics();int f=style*32+pose;p2.drawImage(soft,f%8*128,f/8*128,null);p2.dispose();
  }
  InstallRacingBoat.save(shadows,new File("assets/sprites/police-m3-shadows-v1.png"));
  File pack=new File("assets/sprites/frames/police-m3-shadows-v1");if(!check)pack.mkdirs();
  for(int f=0;f<64;f++)InstallRacingBoat.save(shadows.getSubimage(f%8*128,f/8*128,128,128),new File(pack,f+".png"));

    }
}
