package com.bargeboss.game;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Split canonical offline sheets into the exact production frame packs, without resampling. */
public final class PackBargeFrames {
    public static void main(String[] args)throws Exception{
        boolean check=args.length>0 && args[0].equals("--check");
        boolean base=args.length>0 && args[0].equals("base");long bytes=0;int frames=0;
        for(int atlas:SpriteAtlas.rotationAtlases())if(SpriteAtlas.framePack(atlas)){
            if(base && (ShadowArt.atlas(atlas)||SurfaceLightArt.atlas(atlas)||VesselHighlight.atlas(atlas)))continue;
            BufferedImage sheet=ImageIO.read(new File("assets/sprites/"+SpriteAtlas.file(atlas)));
            int cols=SpriteAtlas.columns(atlas),rows=SpriteAtlas.rows(atlas),w=sheet.getWidth()/cols,h=sheet.getHeight()/rows;
            if(w*cols!=sheet.getWidth()||h*rows!=sheet.getHeight())throw new AssertionError("Nonintegral cells");
            for(int f=0;f<SpriteAtlas.count(atlas);f++){
                File path=new File("assets/sprites/"+SpriteAtlas.packedFile(atlas,f));path.getParentFile().mkdirs();
                BufferedImage cell=sheet.getSubimage(f%cols*w,f/cols*h,w,h);
                if(check){BufferedImage packed=ImageIO.read(path);
                    if(packed.getWidth()!=w || packed.getHeight()!=h)throw new AssertionError("Pack size: "+path);
                    for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(cell.getRGB(x,y)!=packed.getRGB(x,y))throw new AssertionError("Stale pack: "+path);
                }else ImageIO.write(cell,"png",path);bytes+=(long)w*h*4;frames++;
            }
        }
        System.out.println(frames+" frame textures; "+bytes/1048576.0+" MiB decoded, no retained sheet/crop duplication");
    }
}
