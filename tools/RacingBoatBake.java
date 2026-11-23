package com.bargeboss.game;
import java.awt.*;import java.awt.image.*;import java.io.*;import javax.imageio.*;import java.util.*;
/** Offline painted racing-hull export. Runtime uses only registered PNG cells. */
public final class RacingBoatBake {
 static final int RED=0xffcb382e,DECK=0xffe3cfac,GLASS=0xff253d47;
 static BufferedImage paint;
 static void face(int color,float... points){BakeMooredBoats.face(color,points);}
 static void box(float x,float y,float z,float l,float w,float h,int top,int side){BakeMooredBoats.box(x,y,z,l,w,h,top,side);}
 static void sideFace(int side,int color,float... p){
  if(side>0){for(int i=0,j=p.length-3;i<j;i+=3,j-=3)for(int k=0;k<3;k++){float v=p[i+k];p[i+k]=p[j+k];p[j+k]=v;}}
  face(color,p);
 }
 static void model(){
  // A long, tapered offshore racing hull: +X bow; origin is waterline center.
  float[] xx={-27,-24,-18,-10,0,10,18,24,28};
  float[] widths={4.8f,6,6.8f,7.1f,7,5.9f,4.1f,1.9f,.12f};
  for(int i=0;i<xx.length-1;i++)for(int side=-1;side<=1;side+=2){
   float x=xx[i],n=xx[i+1],w=widths[i]*side,v=widths[i+1]*side;
   // Rounded chine, rub rail, red upper shell and broad cream foredeck.
   sideFace(side,0xff663027,x,w*.82f,0,n,v*.82f,0,n,v,2.2f,x,w,2.2f);
   sideFace(side,RED,x,w,2.2f,n,v,2.2f,n,v*.96f,4.5f,x,w*.96f,4.5f);
   sideFace(side,0xffe2d0b3,x,w*.96f,4.5f,n,v*.96f,4.5f,n,v*.94f,4.8f,x,w*.94f,4.8f);
   sideFace(side,RED,x,w*.94f,4.8f,n,v*.94f,4.8f,n,v*.82f,5.2f,x,w*.82f,5.2f);
   sideFace(side,DECK,x,w*.82f,5.2f,n,v*.82f,5.2f,n,0,5.5f,x,0,5.5f);
   // Stable red racing flash on the foredeck, painted in hull-local coordinates.
   if(i>=4)sideFace(side,RED,x,w*.28f,5.42f,n,v*.28f,5.42f,n,0,5.56f,x,0,5.56f);
  }
  face(0xff8d302a,-27,-4.8f,0,-27,4.8f,0,-27,4.6f,4.5f,-27,-4.6f,4.5f);
  // Open cockpit, twin bucket seats, aft padded engine hatch: no cabin or bimini.
  box(-13,-4.8f,5.4f,14,9.6f,.35f,0xff172a32,0xff2b3436);
  box(-24,-5.3f,5.3f,10,10.6f,.75f,0xffc4b798,0xff817962);
  for(int side=-1;side<=1;side+=2){
   box(-8,side*2.6f-1.6f,5.8f,4.5f,3.2f,1.1f,0xffe0d3b8,0xff9b927c);
   box(-8.5f,side*2.6f-1.6f,6.3f,1.1f,3.2f,2.8f,0xffece0c4,0xffa89b81);
   sideFace(-side,GLASS,2,0,5.5f,0,side*5,5.3f,-2,side*4.8f,8.1f,0,0,8.4f);
   face(0xff8a9b9b,0,side*5,5.3f,-4,side*5,5.3f,-2,side*4.8f,8.1f);
   box(-21,side*3.8f-.6f,6.1f,5,1.2f,.25f,0xff34434a,0xff34434a);
   for(int k=0;k<4;k++)box(-20.7f+k,side*3.8f-.7f,6.4f,.35f,1.4f,.15f,0xff7c8988,0xff7c8988);
   // Low chrome cleats and bow navigation lenses, never baked glow.
   for(int end=-1;end<=1;end+=2)box(end*23,side*3.2f-.35f,5.4f,1.5f,.7f,.5f,0xffa7b5ae,0xff576766);
   box(4,side*5.2f-.3f,5.4f,1.3f,.6f,.45f,0xffa0aaa3,0xff405159);
  }
  // Two partially submerged sterndrives and an aft all-round/anchor-light pole.
  for(int side=-1;side<=1;side+=2)box(-29,side*2.8f-.7f,0,2,1.4f,2.8f,0xff555f61,0xff283a43);
  box(-20,-.25f,6.1f,.5f,.5f,3.5f,0xff9faeac,0xff596b72);
 }
 static int paint(int rgb,int px,int py,float z,float angle){
  if(rgb!=RED || paint==null)return rgb;
  float sx=(px+.5f-64)*96/128f,sy=(py+.5f-89.6f)*96/128f+z*ArticulatedModel.Z_SCALE;
  float wx=(sx/RiverCamera.Q-sy/(RiverCamera.Q*RiverCamera.DEPTH))*.5f,wy=(sx/RiverCamera.Q+sy/(RiverCamera.Q*RiverCamera.DEPTH))*.5f;
  float u=wx*(float)Math.cos(angle)+wy*(float)Math.sin(angle),v=-wx*(float)Math.sin(angle)+wy*(float)Math.cos(angle);
  int x=Math.floorMod((int)((u/58+.5f)*paint.getWidth()),paint.getWidth()),y=Math.floorMod((int)((v/15+.5f)*paint.getHeight()),paint.getHeight());
  int texture=paint.getRGB(x,y);float variation=.77f+.30f*((texture>>16&255)/255f);
  return 0xff000000|(Math.min(255,Math.round((rgb>>16&255)*variation))<<16)|(Math.round((rgb>>8&255)*variation)<<8)|Math.round((rgb&255)*variation);
 }
 public static void main(String[] args)throws Exception{
  paint=ImageIO.read(new File("docs/art-studies/aint-broke/red-fiberglass-v1.png"));
  File directory=new File("docs/art-studies/aint-broke");directory.mkdirs();
  BufferedImage sheet=new BufferedImage(1024,512,BufferedImage.TYPE_INT_ARGB),normal=new BufferedImage(1024,512,BufferedImage.TYPE_INT_ARGB),material=new BufferedImage(1024,512,BufferedImage.TYPE_INT_ARGB);
  Graphics2D g=sheet.createGraphics(),n=normal.createGraphics(),m=material.createGraphics();
  for(int pose=0;pose<32;pose++){
   BakeMooredBoats.angle=pose*(float)(Math.PI*2/32);BakeMooredBoats.faces=new ArrayList<BakeMooredBoats.Face>();model();
   BakeMooredBoats.owners=new int[BakeMooredBoats.RASTER*BakeMooredBoats.RASTER];Arrays.fill(BakeMooredBoats.owners,-1);
   BakeMooredBoats.depths=new float[BakeMooredBoats.owners.length];Arrays.fill(BakeMooredBoats.depths,Float.NEGATIVE_INFINITY);BakeMooredBoats.heights=new float[BakeMooredBoats.owners.length];
   for(int i=0;i<BakeMooredBoats.faces.size();i++)BakeMooredBoats.rasterFace(i,BakeMooredBoats.faces.get(i));
   BufferedImage color=BakeMooredBoats.resolve(0,0),norm=BakeMooredBoats.resolve(4,0),mat=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
   for(int y=0;y<128;y++)for(int x=0;x<128;x++){
    int at=y*BakeMooredBoats.AA*BakeMooredBoats.RASTER+x*BakeMooredBoats.AA,id=BakeMooredBoats.owners[at];if(id<0){mat.setRGB(x,y,(color.getRGB(x,y)&0xff000000)|(145<<16));continue;}
    BakeMooredBoats.Face f=BakeMooredBoats.faces.get(id);int c=paint(f.color,x,y,BakeMooredBoats.heights[at],BakeMooredBoats.angle);
    // Dusk base, preserving a saturated red hull and readable cream deck.
    int old=color.getRGB(x,y),alpha=old>>>24;
    if(f.color==RED)color.setRGB(x,y,(alpha<<24)|((c>>16&255)*65/100<<16)|((c>>8&255)*65/100<<8)|(c&255)*65/100);
    mat.setRGB(x,y,(alpha<<24)|((f.color==GLASS?32:145)<<16));
   }
   g.drawImage(color,pose%8*128,pose/8*128,null);n.drawImage(norm,pose%8*128,pose/8*128,null);m.drawImage(mat,pose%8*128,pose/8*128,null);
  }g.dispose();n.dispose();m.dispose();ImageIO.write(sheet,"png",new File(directory,"racing-color-v1.png"));ImageIO.write(normal,"png",new File(directory,"racing-normal-v1.png"));ImageIO.write(material,"png",new File(directory,"racing-material-v1.png"));
 }
}
