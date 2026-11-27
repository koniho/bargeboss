package com.bargeboss.game;
import java.awt.*;import java.awt.image.*;import java.io.*;import javax.imageio.*;
/** Append one mission-only hull while preserving every accepted marina pixel. */
public final class InstallRacingBoat {
 static boolean check;
 static void save(BufferedImage im,File file)throws Exception{
  if(!check){ImageIO.write(im,"png",file);return;}
  BufferedImage old=ImageIO.read(file);if(old.getWidth()!=im.getWidth() || old.getHeight()!=im.getHeight())throw new AssertionError("Stale size "+file);
  for(int y=0;y<im.getHeight();y++)for(int x=0;x<im.getWidth();x++)if(old.getRGB(x,y)!=im.getRGB(x,y))throw new AssertionError("Stale racing derivative "+file);
 }
 public static void main(String[] args)throws Exception{
  check=args.length>0 && args[0].equals("--check");
  File root=new File("docs/art-studies/aint-broke");
  BufferedImage old=ImageIO.read(new File("assets/sprites/montrose-moored-boats-v1.png")),on=ImageIO.read(new File("assets/sprites/montrose-moored-normals-v1.png"));
  BufferedImage color=ImageIO.read(new File(root,"racing-color-v1.png")),norm=ImageIO.read(new File(root,"racing-normal-v1.png"));
  BufferedImage out=new BufferedImage(2048,128*MooredBoats.ROWS,BufferedImage.TYPE_INT_ARGB),normal=new BufferedImage(2048,128*(MooredBoats.BASE_FRAMES/16),BufferedImage.TYPE_INT_ARGB);
  Graphics2D g=out.createGraphics(),n=normal.createGraphics();int oldCount=old.getHeight()/128/4*16;
  for(int f=0;f<192;f++){
   n.drawImage(on.getSubimage(f%16*128,f/16*128,128,128),f%16*128,f/16*128,null);
   for(int layer=0;layer<4;layer++){int src=layer*oldCount+f,dst=layer*MooredBoats.BASE_FRAMES+f;g.drawImage(old.getSubimage(src%16*128,src/16*128,128,128),dst%16*128,dst/16*128,null);}
  }
  for(int pose=0;pose<32;pose++){
   int f=192+pose;BufferedImage cell=color.getSubimage(pose%8*128,pose/8*128,128,128);
   n.drawImage(norm.getSubimage(pose%8*128,pose/8*128,128,128),f%16*128,f/16*128,null);
   for(int layer=0;layer<4;layer++){
    BufferedImage band=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
    for(int y=0;y<128;y++)for(int x=0;x<128;x++){int c=cell.getRGB(x,y),v=Math.min(255,((c>>16&255)+(c>>8&255)+(c&255))*2);band.setRGB(x,y,layer==0?c:(c&0xff000000)|v<<(24-layer*8));}
    int dst=layer*MooredBoats.BASE_FRAMES+f;g.drawImage(band,dst%16*128,dst/16*128,null);
   }
  }
  // Preserve appended mission families when rebuilding the racer.
  for(int f=224;f<Math.min(oldCount,MooredBoats.BASE_FRAMES);f++){
   n.drawImage(on.getSubimage(f%16*128,f/16*128,128,128),f%16*128,f/16*128,null);
   for(int layer=0;layer<4;layer++){int src=layer*oldCount+f,dst=layer*MooredBoats.BASE_FRAMES+f;g.drawImage(old.getSubimage(src%16*128,src/16*128,128,128),dst%16*128,dst/16*128,null);}
  }g.dispose();n.dispose();
  save(out,new File("assets/sprites/montrose-moored-boats-v1.png"));save(normal,new File("assets/sprites/montrose-moored-normals-v1.png"));
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
  save(shadows,new File("assets/sprites/racing-boat-shadows-v1.png"));
  File pack=new File("assets/sprites/frames/racing-boat-shadows-v1");if(!check)pack.mkdirs();
  for(int f=0;f<64;f++)save(shadows.getSubimage(f%8*128,f/8*128,128,128),new File(pack,f+".png"));
  System.out.println(check?"Racing hull, normal, light-band and64 shadow frames are fresh":"Installed racing hull and64 frame-sized shadows");
 }
}
