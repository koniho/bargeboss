package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Map;

/** Pixel checks for the native desktop primitive, independent of production artwork. */
public final class DesktopPainterTest {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Field field=DesktopPainter.class.getDeclaredField("ATLASES"); field.setAccessible(true);
        Map<Integer,BufferedImage[]> cache=(Map<Integer,BufferedImage[]>)field.get(null);
        BufferedImage[] old=cache.get(SpriteAtlas.TUG),cells=new BufferedImage[SpriteAtlas.count(SpriteAtlas.TUG)];
        for(int i=0;i<cells.length;i++) cells[i]=cell(0);
        try {
            cache.put(SpriteAtlas.TUG,cells);
            cells[0]=cell(0xffff0000); cells[1]=cell(0xff0000ff);
            int mid=render(.5f,1,false).getRGB(64,64);
            check((mid>>>24)==255,"opaque midpoint must remain fully opaque");
            check(Math.abs(((mid>>16)&255)-127)<=1 && Math.abs((mid&255)-128)<=1,
                    "midpoint must add weighted premultiplied colors");
            check(Math.abs((render(.5f,.5f,false).getRGB(64,64)>>>24)-128)<=1,
                    "overall alpha applies once after cell blend");
            cells[1]=cell(0x000000ff);
            int edge=render(.5f,1,false).getRGB(64,64);
            check(Math.abs((edge>>>24)-127)<=1 && ((edge>>16)&255)==255 && (edge&255)==0,
                    "transparent cell colors must not contaminate partially transparent edges");
            cells[0]=cell(0x80ff0000); cells[1]=cell(0x800000ff);
            int half=render(.5f,1,false).getRGB(64,64);
            check(Math.abs((half>>>24)-128)<=1,"half-transparent overlap keeps weighted alpha");
            cells[0]=cell(0xffe7a431);
            BufferedImage endpoint=render(0,1,false),single=render(0,1,true);
            for(int y=0;y<128;y++) for(int x=0;x<128;x++)
                check(endpoint.getRGB(x,y)==single.getRGB(x,y),"blend endpoint matches one sprite");
            cells[0]=footprintMarker(0); cells[1]=footprintMarker(1);
            for(float blend:new float[]{0,.25f,.5f,.75f,1}) checkMarker(render(blend,1,false));
            System.out.println("Desktop sprite pixels passed: opaque blend, premultiplied edges, group alpha, endpoints, calibrated anchors");
        } finally { if(old==null) cache.remove(SpriteAtlas.TUG); else cache.put(SpriteAtlas.TUG,old); }
    }
    private static BufferedImage cell(int color) {
        BufferedImage image=new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<8;y++) for(int x=0;x<8;x++) image.setRGB(x,y,color);
        return image;
    }
    /** A 20-unit footprint centered on each source view's registration point. */
    private static BufferedImage footprintMarker(int frame) {
        int size=512; BufferedImage image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        float px=SpriteAtlas.pivotX(SpriteAtlas.TUG,frame),py=SpriteAtlas.pivotY(SpriteAtlas.TUG,frame);
        float sx=64*SpriteAtlas.widthScale(SpriteAtlas.TUG,frame),sy=64*SpriteAtlas.heightScale(SpriteAtlas.TUG,frame);
        for(int y=0;y<size;y++) for(int x=0;x<size;x++) {
            float dx=((x+.5f)/size-px)*sx,dy=((y+.5f)/size-py)*sy;
            if(dx*dx+dy*dy<=100) image.setRGB(x,y,0xffffffff);
        }
        return image;
    }
    private static void checkMarker(BufferedImage image) {
        double sum=0,xsum=0,ysum=0; int left=128,top=128,right=0,bottom=0;
        for(int y=0;y<128;y++) for(int x=0;x<128;x++) {
            int alpha=image.getRGB(x,y)>>>24; if(alpha==0) continue;
            sum+=alpha; xsum+=(x+.5)*alpha; ysum+=(y+.5)*alpha;
            if(alpha>=64) { left=Math.min(left,x); top=Math.min(top,y); right=Math.max(right,x); bottom=Math.max(bottom,y); }
        }
        check(sum>0 && Math.abs(xsum/sum-64)<1 && Math.abs(ysum/sum-64)<1,
                "calibrated footprint anchor stays at the requested center throughout blending");
        check(Math.abs(right-left+1-20)<=2 && Math.abs(bottom-top+1-20)<=2,
                "each frame's independent X/Y calibration preserves footprint size");
    }
    private static BufferedImage render(float blend,float alpha,boolean single) {
        BufferedImage image=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics(); DesktopPainter p=new DesktopPainter(g);
        if(single) p.sprite(SpriteAtlas.TUG,0,64,64,64,64,.15f,alpha);
        else p.spriteBlend(SpriteAtlas.TUG,0,1,64,64,64,64,.15f,blend,alpha);
        g.dispose(); return image;
    }
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
}
