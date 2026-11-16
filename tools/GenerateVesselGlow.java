package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Reproducible effect textures derived from the hull alpha, not new vessel art. */
public final class GenerateVesselGlow {
    private static final int N=VesselHighlight.CELL;
    private static float[] blur(float[] src,double sigma) {
        int r=(int)StrictMath.ceil(3*sigma);double[] kernel=new double[r*2+1];double total=0;
        for(int i=-r;i<=r;i++){kernel[i+r]=StrictMath.exp(-i*i/(2*sigma*sigma));total+=kernel[i+r];}
        for(int i=0;i<kernel.length;i++)kernel[i]/=total;
        float[] tmp=new float[N*N],dst=new float[N*N];
        for(int y=0;y<N;y++)for(int x=0;x<N;x++)for(int i=-r;i<=r;i++)if(x+i>=0 && x+i<N)tmp[y*N+x]+=src[y*N+x+i]*kernel[i+r];
        for(int y=0;y<N;y++)for(int x=0;x<N;x++)for(int i=-r;i<=r;i++)if(y+i>=0 && y+i<N)dst[y*N+x]+=tmp[(y+i)*N+x]*kernel[i+r];
        return dst;
    }
    public static void main(String[] args)throws Exception {
        boolean check=args.length>0 && args[0].equals("--check");
        BufferedImage[] sheets=new BufferedImage[5];for(int i=0;i<5;i++)sheets[i]=new BufferedImage(N*8,N*4,BufferedImage.TYPE_INT_ARGB);
        for(int vessel=0;vessel<3;vessel++) {
        int atlas=vessel==2?SpriteAtlas.YACHT:vessel==0?SpriteAtlas.BARGE:SpriteAtlas.TUG;
        BufferedImage source=ImageIO.read(new File("assets/sprites/"+SpriteAtlas.file(atlas)));
        for(int f=0;f<32;f++) {
            BufferedImage cell=new BufferedImage(N,N,BufferedImage.TYPE_INT_ARGB);Graphics2D g=cell.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int l=Math.round(source.getWidth()*SpriteAtlas.sourceLeft(atlas,f)),t=Math.round(source.getHeight()*SpriteAtlas.sourceTop(atlas,f));
            int r=Math.round(source.getWidth()*SpriteAtlas.sourceRight(atlas,f)),b=Math.round(source.getHeight()*SpriteAtlas.sourceBottom(atlas,f));
            int p=VesselHighlight.PAD,end=p+VesselHighlight.INNER;
            g.drawImage(source,p,p,end,end,l,t,r,b,null);g.dispose();
            float[] mask=new float[N*N],dilate=new float[N*N];
            for(int y=0;y<N;y++)for(int x=0;x<N;x++)mask[y*N+x]=Math.max(0,((cell.getRGB(x,y)>>>24)-80)/175f);
            for(int y=0;y<N;y++)for(int x=0;x<N;x++)for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++)
                if(dx*dx+dy*dy<=5 && x+dx>=0 && x+dx<N && y+dy>=0 && y+dy<N)dilate[y*N+x]=Math.max(dilate[y*N+x],mask[(y+dy)*N+x+dx]);
            float[] rim=blur(dilate,.65),near=blur(mask,2.5),far=blur(mask,6);
            for(int y=0;y<N;y++)for(int x=0;x<N;x++) {
                int i=y*N+x;
                int alpha=Math.min(245,Math.round(255*(.12f*mask[i]+(1-mask[i])*(.88f*rim[i]+.9f*near[i]+.85f*far[i]))));
                // Preserve a saturated hue from the feather through the core.
                int gold=0xffee00,mint=0x22ff72;
                sheets[vessel==2?3:vessel==0?0:2].setRGB(f%8*N+x,f/8*N+y,(alpha<<24)|gold);
                if(vessel!=1)sheets[vessel==2?4:1].setRGB(f%8*N+x,f/8*N+y,(alpha<<24)|mint);
                if((x==0 || y==0 || x==N-1 || y==N-1) && alpha!=0)throw new AssertionError("Glow spills into adjacent frame "+f);
            }
        }
        }
        for(int i=0;i<5;i++) {
            File file=new File("assets/sprites/"+(i>=3?"yacht":i==2?"tug":"barge")+"-glow-"+(i==1 || i==4?"mint":"gold")+"-32.png");
            if(check) {
                BufferedImage old=ImageIO.read(file);
                if(old.getWidth()!=sheets[i].getWidth() || old.getHeight()!=sheets[i].getHeight())throw new AssertionError("Glow atlas size");
                for(int y=0;y<old.getHeight();y++)for(int x=0;x<old.getWidth();x++)if(old.getRGB(x,y)!=sheets[i].getRGB(x,y))throw new AssertionError("Regenerate glow atlas: "+file);
            } else ImageIO.write(sheets[i],"png",file);
        }
        System.out.println("160 registered watercraft glow frames validated; padded edges clear");
    }
}
