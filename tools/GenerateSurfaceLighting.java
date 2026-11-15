package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Deterministic response data derived from existing art; does not repaint source PNGs.
 * Broad rounded surface estimates preserve painted detail. Replace with authored normals
 * per receiver when available; the runtime directional response contract stays unchanged. */
public final class GenerateSurfaceLighting {
    private static final int N=SurfaceLightArt.CELL;
    public static void main(String[] args)throws Exception{
        boolean check=args.length>0 && args[0].equals("--check");int frames=0;
        for(int source:SurfaceLightArt.sources()){
            int atlas=SurfaceLightArt.forSource(source),count=SpriteAtlas.count(source);
            BufferedImage sheet=new BufferedImage(8*N,SurfaceLightArt.rows(atlas)*N,BufferedImage.TYPE_INT_ARGB);
            BufferedImage image=null;String previous="";
            for(int f=0;f<count;f++){
                String path=SpriteAtlas.file(source,f);
                if(!path.equals(previous)){image=ImageIO.read(new File("assets/sprites/"+path));previous=path;}
                int l=Math.round(image.getWidth()*SpriteAtlas.sourceLeft(source,f)),t=Math.round(image.getHeight()*SpriteAtlas.sourceTop(source,f));
                int r=Math.round(image.getWidth()*SpriteAtlas.sourceRight(source,f)),b=Math.round(image.getHeight()*SpriteAtlas.sourceBottom(source,f));
                int[] pixels=new int[N*N];int minX=N,minY=N,maxX=0,maxY=0;
                for(int y=0;y<N;y++)for(int x=0;x<N;x++){
                    int left=l+x*(r-l)/N,right=l+(int)StrictMath.ceil((x+1)*(r-l)/(double)N);
                    int top=t+y*(b-t)/N,bottom=t+(int)StrictMath.ceil((y+1)*(b-t)/(double)N);
                    int alpha=255;long red=0,green=0,blue=0,total=0;
                    // Conservative support: the whole low-resolution texel must be on the sprite.
                    for(int yy=Math.max(t,top-1);yy<Math.min(b,bottom+1);yy++)for(int xx=Math.max(l,left-1);xx<Math.min(r,right+1);xx++){
                        int pixel=image.getRGB(xx,yy),a=pixel>>>24;alpha=Math.min(alpha,a);
                        red+=((pixel>>>16)&255)*a;green+=((pixel>>>8)&255)*a;blue+=(pixel&255)*a;total+=a;
                    }
                    if(x==0 || y==0 || x==N-1 || y==N-1 || alpha<245 || total==0)continue;
                    int rr=Math.min(255,(int)(red/(double)total*1.5+42)),gg=Math.min(255,(int)(green/(double)total*1.42+30)),bb=Math.min(255,(int)(blue/(double)total*1.25+18));
                    pixels[y*N+x]=(alpha<<24)|(rr<<16)|(gg<<8)|bb;
                    minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                }
                float cx=(minX+maxX)*.5f,cy=(minY+maxY)*.5f,rx=Math.max(1,(maxX-minX)*.5f),ry=Math.max(1,(maxY-minY)*.5f);
                for(int d=0;d<4;d++){
                    int out=SurfaceLightArt.frame(source,d,f);frames++;
                    float lx=(float)StrictMath.cos(d*Math.PI/2),ly=(float)StrictMath.sin(d*Math.PI/2);
                    for(int y=0;y<N;y++)for(int x=0;x<N;x++){
                        int pixel=pixels[y*N+x];if(pixel==0)continue;
                        float nx=(x-cx)/rx,ny=(y-cy)/ry,len=Physics.length(nx,ny);
                        if(len>1){nx/=len;ny/=len;}
                        float nz=(float)StrictMath.sqrt(Math.max(0,1-nx*nx-ny*ny));
                        float response=Physics.clamp((nx*lx+ny*ly)*.85f+nz*.35f,0,1);
                        int alpha=Math.round((pixel>>>24)*response);
                        sheet.setRGB(out%8*N+x,out/8*N+y,(alpha<<24)|(pixel&0xffffff));
                    }
                }
            }
            File file=new File("assets/sprites/"+SurfaceLightArt.file(atlas));
            if(check){
                BufferedImage old=ImageIO.read(file);
                if(old.getWidth()!=sheet.getWidth() || old.getHeight()!=sheet.getHeight())throw new AssertionError("Surface atlas dimensions: "+file);
                for(int y=0;y<old.getHeight();y++)for(int x=0;x<old.getWidth();x++)if(old.getRGB(x,y)!=sheet.getRGB(x,y))throw new AssertionError("Regenerate surface atlas: "+file);
            }else ImageIO.write(sheet,"png",file);
        }
        System.out.println(frames+" surface response frames match source support and materials");
    }
}
