package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Exact-output, Java2D pixel, and isolated CPU review against the original marina clipper. */
public final class MontroseWaterClipReview {
    private static final float[][] WATER_BOUNDS=bounds(MontroseHarbor.WATER_TRIANGLES),DOCK_BOUNDS=bounds(MontroseHarbor.DOCKS);
    private static float[][] bounds(float[][] p){float[][] out=new float[p.length][];for(int i=0;i<p.length;i++)out[i]=PolygonWater.bounds(p[i]);return out;}
    /** Frozen original algorithm; keep independent after runtime delegates to the index. */
    private static void baseline(Painter p,float[] points,int color){float[] box=PolygonWater.bounds(points);
        for(int i=0;i<MontroseHarbor.WATER_TRIANGLES.length;i++)if(PolygonWater.overlaps(box,WATER_BOUNDS[i])){
            float[] q=PolygonWater.clip(points,MontroseHarbor.WATER_TRIANGLES[i]);if(q.length<6)continue;
            ArrayList<float[]> pieces=new ArrayList<float[]>();pieces.add(q);
            for(int h=0;h<MontroseHarbor.DOCKS.length;h++)if(PolygonWater.overlaps(box,DOCK_BOUNDS[h])){
                ArrayList<float[]> next=new ArrayList<float[]>();for(float[] piece:pieces)PolygonWater.subtract(piece,MontroseHarbor.DOCKS[h],next);pieces=next;
            }
            for(float[] piece:pieces)p.poly(piece,color);
        }
    }
    private static class Sink implements Painter {
        long checksum;int count;
        public void poly(float[] p,int color){checksum+=p.length+color+Float.floatToIntBits(p[0]);count++;}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float b,float alpha){}
        public void line(float x,float y,float xx,float yy,float w,int c){}public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}
    }
    private static final class Capture extends Sink {
        final ArrayList<float[]> polygons=new ArrayList<float[]>();
        public void poly(float[] p,int color){super.poly(p,color);polygons.add(p.clone());}
    }
    private static float[] stamp(float x,float y,float radius,int sides,float angle){float[] out=new float[sides*2];for(int i=0;i<sides;i++){
        double a=angle+i*StrictMath.PI*2/sides;out[2*i]=x+radius*(float)StrictMath.cos(a);out[2*i+1]=y+radius*.63f*(float)StrictMath.sin(a);}return out;}
    private static ArrayList<float[]> fixtures(){ArrayList<float[]> out=new ArrayList<float[]>();
        for(int row=0;row<46;row++)for(int col=0;col<60;col++)out.add(stamp(-700+col*55,-1400+row*55,3+(row*17+col*31)%35,4+(row+col)%9,col*.713f));
        for(float[] edge:MontroseHarbor.DOCKS)for(int i=0;i<edge.length;i+=2)for(int size=0;size<3;size++)out.add(stamp(edge[i],edge[i+1],.2f+size*12,12,size*.17f));
        for(float[] tri:MontroseHarbor.WATER_TRIANGLES)for(int i=0;i<6;i+=2){int j=(i+2)%6;for(int side=-1;side<=1;side++)out.add(stamp((tri[i]+tri[j])*.5f+side*.005f,(tri[i+1]+tri[j+1])*.5f,7,8,.3f));}
        float[] coast=MontroseHarbor.COAST;for(int i=0;i<coast.length;i+=2)for(int size=0;size<3;size++)out.add(stamp(coast[i],coast[i+1],.1f+size*33,12,.51f));
        for(int i=0;i<8;i++)out.add(stamp(-800+i*600,700-i*170,250+i*95,12,i*.1f));
        // Large/off-grid fallback, water polygon outer bounds, and index/cell boundaries.
        out.add(stamp(10000,10000,9000,12,0));out.add(stamp(-2000,-2000,250,12,0));
        for(int i=-8;i<24;i++)out.add(new float[]{i*128,0,i*128+128,0,i*128+128,128,i*128,128});
        return out;
    }
    private static BufferedImage render(ArrayList<float[]> fixtures,boolean optimized){BufferedImage img=new BufferedImage(1440,1000,BufferedImage.TYPE_INT_ARGB);Graphics2D g=img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(new java.awt.Color(0x081923));g.fillRect(0,0,1440,1000);
        Painter p=new RiverPainter(new DesktopPainter(g),MontroseHarbor.overview(1440,1000).camera);
        for(int i=0;i<fixtures.size();i++){int color=((80+i%110)<<24)|0x71b9c8;if(optimized)MontroseWaterClip.paint(p,fixtures.get(i),color);else baseline(p,fixtures.get(i),color);}
        g.dispose();return img;
    }
    private static long time(ArrayList<float[]> fixtures,boolean optimized,Sink sink){long start=System.nanoTime();for(float[] p:fixtures){if(optimized)MontroseWaterClip.paint(sink,p,0x9171b9c8);else baseline(sink,p,0x9171b9c8);}return System.nanoTime()-start;}
    private static Object field(Object target,String name)throws Exception{Class<?> c=target instanceof Class?(Class<?>)target:target.getClass();Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(target instanceof Class?null:target);}
    public static void main(String[] args)throws Exception {
        File dir=new File(args.length>0?args[0]:"out/montrose-water-clip-review");dir.mkdirs();StringBuilder report=new StringBuilder();
        ArrayList<float[]> fixtures=fixtures();Sink warm=new Sink();long init=System.nanoTime();MontroseWaterClip.paint(warm,fixtures.get(0),0x9171b9c8);init=System.nanoTime()-init;
        long comparisons=0,polygons=0;for(int i=0;i<fixtures.size();i++){Capture a=new Capture(),b=new Capture();baseline(a,fixtures.get(i),0x9171b9c8);MontroseWaterClip.paint(b,fixtures.get(i),0x9171b9c8);
            if(a.polygons.size()!=b.polygons.size())throw new AssertionError("polygon count at fixture "+i+": "+a.polygons.size()+" != "+b.polygons.size());
            for(int p=0;p<a.polygons.size();p++){float[] x=a.polygons.get(p),y=b.polygons.get(p);if(!Arrays.equals(x,y))throw new AssertionError("exact coordinates at fixture "+i+" polygon "+p);comparisons+=x.length;polygons++;}}
        BufferedImage a=render(fixtures,false),b=render(fixtures,true);long pixels=0;for(int y=0;y<a.getHeight();y++)for(int x=0;x<a.getWidth();x++){if(a.getRGB(x,y)!=b.getRGB(x,y))throw new AssertionError("native-independent raster mismatch "+x+","+y);pixels++;}
        ImageIO.write(a,"png",new File(dir,"baseline.png"));ImageIO.write(b,"png",new File(dir,"indexed.png"));
        Object index=field(MontroseWaterClip.class,"INDEX");int[][] tb=(int[][])field(index,"triangles"),db=(int[][])field(index,"docks");int refs=0,full=0;
        for(int[] ids:tb)refs+=ids.length;for(int[] ids:db)refs+=ids.length;for(int id:(int[])field(index,"fullTriangle"))if(id>=0)full++;
        report.append("triangles=").append(MontroseHarbor.WATER_TRIANGLES.length).append(" docks=").append(MontroseHarbor.DOCKS.length).append("\n");
        report.append("fixtures=").append(fixtures.size()).append(" exact_polygons=").append(polygons).append(" exact_float_coordinates=").append(comparisons).append(" equal_java2d_pixels=").append(pixels).append("\n");
        report.append("index_cells=").append(tb.length).append(" index_references=").append(refs).append(" full_triangle_cells=").append(full).append(" int_payload_bytes=").append(4*(refs+tb.length)).append(" lazy_initialization_ms=").append(init/1e6).append("\n");
        ArrayList<float[]> work=new ArrayList<float[]>();for(float[] p:fixtures){float[] bounds=PolygonWater.bounds(p);if(bounds[2]-bounds[0]<100 && MontroseHarbor.openWater(p[0],p[1]))work.add(p);}
        time(work,false,warm);time(work,true,warm);time(work,true,warm);time(work,false,warm);
        report.append("isolated_java_cpu_fixture_count=").append(work.size()).append("; excludes pixel/trace capture; not Android frame time\nround,order,baseline_ms,indexed_ms\n");
        for(int round=0;round<6;round++){long oldTime,newTime;if(round%2==0){oldTime=time(work,false,warm);newTime=time(work,true,warm);}else{newTime=time(work,true,warm);oldTime=time(work,false,warm);}
            report.append(round).append(round%2==0?",baseline-first,":",indexed-first,").append(oldTime/1e6).append(',').append(newTime/1e6).append('\n');}
        report.append("sink_checksum=").append(warm.checksum).append("\n");
        try(FileWriter out=new FileWriter(new File(dir,"results.txt"))){out.write(report.toString());}System.out.print(report);
    }
}
