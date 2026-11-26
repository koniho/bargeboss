package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Offline white-paper matte recovery. Never resizes, shifts or blurs the authored cells. */
public final class CleanPortraitEdges {
    private static int channel(int c,int shift){return (c>>>shift)&255;}
    private static int low(int c){return Math.min(channel(c,16),Math.min(channel(c,8),channel(c,0)));}
    private static int high(int c){return Math.max(channel(c,16),Math.max(channel(c,8),channel(c,0)));}
    private static boolean pale(int c){return low(c)>45 && high(c)-low(c)<35;}
    public static void main(String[] args)throws Exception{
        if(args.length!=2)throw new IllegalArgumentException("source.png output.png");
        BufferedImage image=ImageIO.read(new File(args[0]));int w=image.getWidth(),h=image.getHeight();
        int[] pixels=image.getRGB(0,0,w,h,null,0,w),output=pixels.clone();
        int cell=w/3,band=Math.max(1,(int)Math.ceil(cell/192f*3)),reach=Math.max(3,(int)Math.ceil(cell/192f*5));
        // Flood only paper-colored pixels connected to transparent space. Interior highlights
        // remain behind the dark ink contour, even when they have the same color as the paper.
        int[] distance=new int[pixels.length],queue=new int[pixels.length];java.util.Arrays.fill(distance,-1);
        int head=0,tail=0;
        for(int i=0;i<pixels.length;i++)if((pixels[i]>>>24)<8){distance[i]=0;queue[tail++]=i;}
        while(head<tail){int at=queue[head++],x=at%w,y=at/w;if(distance[at]>=band)continue;
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
                int xx=x+dx,yy=y+dy;if(xx<0||xx>=w||yy<0||yy>=h)continue;int n=yy*w+xx;
                if(distance[n]<0 && pale(pixels[n])){distance[n]=distance[at]+1;queue[tail++]=n;}
            }
        }
        int changed=0;
        for(int i=0;i<pixels.length;i++){
            int color=pixels[i],a=color>>>24;
            if(a<12){output[i]=0;continue;}
            if(distance[i]<=0)continue;
            int x=i%w,y=i/w,best=0,bestD=Integer.MAX_VALUE;
            for(int dy=-reach;dy<=reach;dy++)for(int dx=-reach;dx<=reach;dx++){
                int xx=x+dx,yy=y+dy,d=dx*dx+dy*dy;
                if(d>=bestD || xx<0||xx>=w||yy<0||yy>=h || xx/cell!=x/cell || yy/(h/2)!=y/(h/2))continue;
                int candidate=pixels[yy*w+xx];
                if((candidate>>>24)>220 && high(candidate)<70){best=candidate;bestD=d;}
            }
            if(bestD==Integer.MAX_VALUE){output[i]=0;changed++;continue;}
            double coverage=0;
            for(int shift:new int[]{16,8,0})coverage+=(255-channel(color,shift))/(double)(255-channel(best,shift));
            coverage=Math.max(0,Math.min(1,coverage/3));
            int alpha=(int)Math.round(a*coverage);
            output[i]=alpha==0?0:(alpha<<24)|(best&0xffffff);changed++;
        }
        image.setRGB(0,0,w,h,output,0,w);ImageIO.write(image,"png",new File(args[1]));
        System.out.println(args[1]+": recovered "+changed+" outer matte pixels; dimensions and cell origins unchanged");
    }
}
