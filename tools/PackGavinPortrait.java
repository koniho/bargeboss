package com.bargeboss.game;
import java.awt.*;import java.awt.image.*;import javax.imageio.*;import java.io.*;import java.util.*;
/** Packs the six generated alpha cutouts without per-frame stretching. */
public final class PackGavinPortrait {
 public static void main(String[] args)throws Exception{
  BufferedImage im=ImageIO.read(new File(args.length>0?args[0]:"docs/art-studies/gavin/gavin-source-v1.png"));int w=im.getWidth(),h=im.getHeight();boolean[] seen=new boolean[w*h];int[] queue=new int[w*h];ArrayList<int[]> boxes=new ArrayList<int[]>();
  for(int i=0;i<w*h;i++)if(!seen[i]&&(im.getRGB(i%w,i/w)>>>24)>128){int start=0,end=1;queue[0]=i;seen[i]=true;int l=w,r=0,t=h,b=0;
   while(start<end){int at=queue[start++],x=at%w,y=at/w;l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);for(int d:new int[]{-1,1,-w,w}){int n=at+d;if(n<0||n>=w*h||Math.abs(n%w-x)>1||seen[n]||(im.getRGB(n%w,n/w)>>>24)<=128)continue;seen[n]=true;queue[end++]=n;}}
   if(end>10000)boxes.add(new int[]{l,t,r,b});
  }
  if(boxes.size()!=6)throw new AssertionError("Expected six separate alpha cutouts: "+boxes.size());boxes.sort((a,b)->a[1]/(h/2)!=b[1]/(h/2)?Integer.compare(a[1],b[1]):Integer.compare(a[0],b[0]));
  float scale=1;for(int[] b:boxes)scale=Math.min(scale,Math.min(178f/(b[2]-b[0]+1),178f/(b[3]-b[1]+1)));
  BufferedImage out=new BufferedImage(576,384,BufferedImage.TYPE_INT_ARGB);Graphics2D g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
  for(int f=0;f<6;f++){int[] b=boxes.get(f);int l=w,r=0;for(int y=b[3]-20;y<=b[3];y++)for(int x=b[0];x<=b[2];x++)if((im.getRGB(x,y)>>>24)>128){l=Math.min(l,x);r=Math.max(r,x);}
   float cx=(l+r)*.5f;java.awt.geom.AffineTransform tr=new java.awt.geom.AffineTransform();tr.translate(f%3*192+96-(cx-b[0])*scale,f/3*192+183-(b[3]-b[1])*scale);tr.scale(scale,scale);g.drawImage(im.getSubimage(b[0],b[1],b[2]-b[0]+1,b[3]-b[1]+1),tr,null);
  }g.dispose();ImageIO.write(out,"png",new File(args.length>1?args[1]:"assets/sprites/portrait-gavin-dockmaster-v1.png"));
 }
}
