package com.bargeboss.game;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Source PNG header audit, no image decode. Models the hosts' retained atlas sources. */
public final class RetainedSpriteMemoryAudit {
    private static final class Capture implements Painter {
        final TreeSet<Integer> atlases=new TreeSet<Integer>();
        public void sprite(int atlas,int f,float x,float y,float w,float h,float r,float a){atlases.add(atlas);}
        public void spriteBlend(int atlas,int f,int ff,float x,float y,float w,float h,float r,float mix,float a){atlases.add(atlas);}
        public void rect(float x,float y,float w,float h,int c){}
        public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}
        public void poly(float[] xy,int c){}
    }
    public static void main(String[] args)throws Exception {
        Capture painter=new Capture();painter.atlases.add(0);painter.atlases.add(1);
        Game title=new Game();title.configureCanvas(1333,600);Renderer.draw(title,painter);
        for(String scene:ScenePerformanceBenchmark.SCENES)Renderer.draw(ScenePerformanceBenchmark.fixture(1333,scene),painter);
        Map<String,String> sources=new TreeMap<String,String>();
        for(int atlas:painter.atlases)for(int f=0;f<SpriteAtlas.count(atlas);f++) {
            String path=SpriteAtlas.file(atlas,f);sources.put(SpriteGrade.cacheKey(atlas,path),path);
        }
        long bytes=0;System.out.println("cacheKey,width,height,argbBytes");
        for(Map.Entry<String,String> entry:sources.entrySet())try(DataInputStream in=new DataInputStream(new FileInputStream("assets/sprites/"+entry.getValue()))) {
            in.skipBytes(16);int width=in.readInt(),height=in.readInt();long size=(long)width*height*4;bytes+=size;
            System.out.println(entry.getKey()+","+width+","+height+","+size);
        }
        System.out.printf(java.util.Locale.ROOT,"TOTAL title+tutorial sources=%d decodedARGB=%.3fMiB; excludes GPU copies, Java/native objects and new scene-layer caches%n",sources.size(),bytes/1048576.0);
    }
}
