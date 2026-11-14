package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;
import javax.imageio.ImageIO;

/** Read-only silhouette registration against a known projected vessel guide. */
public final class VesselRegistration {
    private static final int N=128,COUNT=32;
    private VesselRegistration() {}
    static BufferedImage cell(BufferedImage atlas,int frame) {
        int x=Math.round(atlas.getWidth()*(frame%8)/8f),y=Math.round(atlas.getHeight()*(frame/8)/4f);
        int w=Math.round(atlas.getWidth()*(frame%8+1)/8f)-x;
        int h=Math.round(atlas.getHeight()*(frame/8+1)/4f)-y;
        return atlas.getSubimage(x,y,w,h);
    }
    private static boolean[] mask(BufferedImage cell,double scale) {
        boolean[] mask=new boolean[N*N];
        for(int y=0;y<N;y++) for(int x=0;x<N;x++) {
            double u=((x+.5)/N-.5)/scale+.5,v=((y+.5)/N-.72)/scale+.72;
            int sx=(int)StrictMath.floor(u*cell.getWidth()),sy=(int)StrictMath.floor(v*cell.getHeight());
            if(sx>=0 && sy>=0 && sx<cell.getWidth() && sy<cell.getHeight())
                mask[y*N+x]=(cell.getRGB(sx,sy)>>>24)>=128;
        }
        return mask;
    }
    private static int count(boolean[] mask) { int n=0;for(boolean b:mask)if(b)n++;return n; }
    private static double width(BufferedImage image) {
        int lo=image.getWidth(),hi=-1;
        for(int y=0;y<image.getHeight();y++) for(int x=0;x<image.getWidth();x++)
            if((image.getRGB(x,y)>>>24)>=128) { lo=Math.min(lo,x);hi=Math.max(hi,x); }
        if(hi<lo) throw new IllegalArgumentException("Empty cell");
        return (hi-lo+1)/(double)image.getWidth();
    }
    private static double overlap(boolean[] guide,int guideCount,int[] source,int dx,int dy) {
        int hit=0;
        for(int at:source) {
            int x=at%N+dx,y=at/N+dy;
            if(x>=0 && y>=0 && x<N && y<N && guide[y*N+x]) hit++;
        }
        return hit/(double)(guideCount+source.length-hit);
    }
    private static int[] points(boolean[] mask) {
        int[] pts=new int[count(mask)];int i=0;
        for(int at=0;at<mask.length;at++) if(mask[at]) pts[i++]=at;
        return pts;
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=3) throw new IllegalArgumentException("VesselRegistration guide.png candidate.png output.csv (8x4 atlases)");
        BufferedImage guide=ImageIO.read(new File(args[0])),source=ImageIO.read(new File(args[1]));
        if(guide==null || source==null || !guide.getColorModel().hasAlpha() || !source.getColorModel().hasAlpha())
            throw new IllegalArgumentException("Both inputs must be transparent images");
        double[] ratios=new double[COUNT];
        for(int i=0;i<COUNT;i++) ratios[i]=width(cell(guide,i))/width(cell(source,i));
        Arrays.sort(ratios);double scale=(ratios[15]+ratios[16])*.5;
        if(scale<.5 || scale>2) throw new IllegalArgumentException("Guide/candidate scale differs too much for this tool");
        StringBuilder out=new StringBuilder("frame,pivot_x,pivot_y,uniform_scale,raw_iou,registered_iou,shift_x,shift_y\n");
        double rawTotal=0,fitTotal=0,minFit=1;
        for(int frame=0;frame<COUNT;frame++) {
            boolean[] gm=mask(cell(guide,frame),1);int gc=count(gm);
            int[] raw=points(mask(cell(source,frame),1)),scaled=points(mask(cell(source,frame),scale));
            if(gc==0 || raw.length==0 || scaled.length==0) throw new IllegalArgumentException("Empty mask "+frame);
            double rawScore=overlap(gm,gc,raw,0,0),best=-1;int bx=0,by=0;
            for(int dy=-24;dy<=24;dy++) for(int dx=-24;dx<=24;dx++) {
                double score=overlap(gm,gc,scaled,dx,dy);
                if(score>best || (score==best && dx*dx+dy*dy<bx*bx+by*by)) { best=score;bx=dx;by=dy; }
            }
            rawTotal+=rawScore;fitTotal+=best;minFit=Math.min(minFit,best);
            out.append(String.format(Locale.ROOT,"%d,%.7f,%.7f,%.7f,%.6f,%.6f,%d,%d%n",
                frame,.5-bx/(N*scale),.72-by/(N*scale),scale,rawScore,best,bx,by));
        }
        File output=new File(args[2]);if(output.getParentFile()!=null)output.getParentFile().mkdirs();
        Files.write(output.toPath(),out.toString().getBytes(StandardCharsets.UTF_8));
        System.out.printf(Locale.ROOT,"%s: one uniform scale %.4f; mean silhouette overlap raw %.3f, registered %.3f, minimum %.3f%n",
            args[1],scale,rawTotal/COUNT,fitTotal/COUNT,minFit);
        System.out.println("Estimates only: silhouette overlap does not verify heading, waterline, fittings or physical model consistency.");
    }
}
