package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Independent near/soft shadow masks generated from production source alpha. */
public final class GenerateShadowMasks {
    private static final int N=ShadowArt.CELL;
    private static float[] blur(float[] source,double sigma) {
        int r=(int)StrictMath.ceil(sigma*3);double sum=0;double[] kernel=new double[2*r+1];
        for(int i=-r;i<=r;i++){kernel[i+r]=StrictMath.exp(-i*i/(2*sigma*sigma));sum+=kernel[i+r];}
        for(int i=0;i<kernel.length;i++)kernel[i]/=sum;
        float[] tmp=new float[N*N],out=new float[N*N];
        for(int y=0;y<N;y++)for(int x=0;x<N;x++)for(int i=-r;i<=r;i++)if(x+i>=0 && x+i<N)tmp[y*N+x]+=source[y*N+x+i]*kernel[i+r];
        for(int y=0;y<N;y++)for(int x=0;x<N;x++)for(int i=-r;i<=r;i++)if(y+i>=0 && y+i<N)out[y*N+x]+=tmp[(y+i)*N+x]*kernel[i+r];
        return out;
    }
    public static void main(String[] args)throws Exception {
        boolean check=args.length>0 && args[0].equals("--check");int frames=0;
        for(int atlas=ShadowArt.FIRST;atlas<=ShadowArt.LAST;atlas++) {
            int source=ShadowArt.source(atlas),count=SpriteAtlas.count(source),cols=ShadowArt.columns(atlas);
            BufferedImage sheet=new BufferedImage(cols*N,ShadowArt.rows(atlas)*N,BufferedImage.TYPE_INT_ARGB);
            BufferedImage image=null;String previous="";
            for(int frame=0;frame<count;frame++) {
                String file=SpriteAtlas.file(source,frame);
                if(!file.equals(previous)){image=ImageIO.read(new File("assets/sprites/"+file));previous=file;}
                int l=Math.round(image.getWidth()*SpriteAtlas.sourceLeft(source,frame)),t=Math.round(image.getHeight()*SpriteAtlas.sourceTop(source,frame));
                int r=Math.round(image.getWidth()*SpriteAtlas.sourceRight(source,frame)),b=Math.round(image.getHeight()*SpriteAtlas.sourceBottom(source,frame));
                BufferedImage cell=new BufferedImage(N,N,BufferedImage.TYPE_INT_ARGB);Graphics2D p=cell.createGraphics();
                p.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                p.drawImage(image,ShadowArt.PAD,ShadowArt.PAD,ShadowArt.PAD+ShadowArt.INNER,ShadowArt.PAD+ShadowArt.INNER,l,t,r,b,null);p.dispose();
                float[] mask=new float[N*N];
                for(int y=0;y<N;y++)for(int x=0;x<N;x++)mask[y*N+x]=Math.max(0,((cell.getRGB(x,y)>>>24)-64)/191f);
                for(int soft=0;soft<2;soft++) {
                    float[] alpha=blur(mask,soft==0?1.0:3.2);int output=frame+soft*count;frames++;
                    for(int y=0;y<N;y++)for(int x=0;x<N;x++) {
                        int a=Math.round(alpha[y*N+x]*255);
                        if((x==0 || y==0 || x==N-1 || y==N-1) && a!=0)throw new AssertionError("Shadow cell bleed: "+file);
                        sheet.setRGB(output%cols*N+x,output/cols*N+y,(a<<24)|0x01060d);
                    }
                }
            }
            File file=new File("assets/sprites/"+ShadowArt.file(atlas));
            if(check) {
                BufferedImage old=ImageIO.read(file);
                if(old.getWidth()!=sheet.getWidth() || old.getHeight()!=sheet.getHeight())throw new AssertionError("Shadow dimensions: "+file);
                for(int y=0;y<old.getHeight();y++)for(int x=0;x<old.getWidth();x++)if(old.getRGB(x,y)!=sheet.getRGB(x,y))throw new AssertionError("Regenerate shadow masks: "+file);
            } else ImageIO.write(sheet,"png",file);
        }
        System.out.println(frames+" independent shadow masks match source alpha; all padded edges transparent");
    }
}
