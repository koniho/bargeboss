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
            System.out.println("Desktop sprite pixels passed: opaque blend, premultiplied edges, group alpha, endpoints");
        } finally { if(old==null) cache.remove(SpriteAtlas.TUG); else cache.put(SpriteAtlas.TUG,old); }
    }
    private static BufferedImage cell(int color) {
        BufferedImage image=new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<8;y++) for(int x=0;x<8;x++) image.setRGB(x,y,color);
        return image;
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
