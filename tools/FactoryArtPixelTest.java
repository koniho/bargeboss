package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

/** Desktop-only source-pixel validation; never shipped in shared gameplay. */
public final class FactoryArtPixelTest {
    private static int checks;
    private static void check(boolean ok,String why) {checks++;if(!ok) throw new AssertionError(why);}
    public static void main(String[] args) throws Exception {
        String studies="docs/art-studies/factory-v1/";
        List<String> ranges=Files.readAllLines(Paths.get(studies+"foundation-review-ranges.csv"));
        check(ranges.size()==FactoryArt.COUNT+1,"all sixteen source-inspected foundations have review ranges");
        for(int i=0;i<FactoryArt.COUNT;i++) {
            String[] row=ranges.get(i+1).split(",");String id=FactoryArt.id(i);
            check(row[0].equals(id),"review source matches runtime cell "+i);
            File production=new File("assets/sprites/"+FactoryArt.file(i));
            check(Arrays.equals(Files.readAllBytes(production.toPath()),Files.readAllBytes(Paths.get(studies+row[1]))),
                    id+" production pixels remain the unwarped reviewed source");
            BufferedImage image=ImageIO.read(production);int w=image.getWidth(),h=image.getHeight();
            check(image.getColorModel().hasAlpha() && w==1254 && h==1254,id+" retains the reviewed RGBA canvas");
            int[] pixels=image.getRGB(0,0,w,h,null,0,w);int left=w,top=h,right=-1,bottom=-1;
            for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
                int alpha=pixels[y*w+x]>>>24;
                // Generated PNGs retain isolated alpha=1 quantization noise; never mask or rewrite sources.
                if(x==0 || y==0 || x==w-1 || y==h-1) check(alpha<=1,id+" has clear image edges within one alpha quantization step");
                if(alpha>=128) {left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
            }
            check(right>left && bottom>top && left>=20 && top>=20 && w-right>20 && h-bottom>20,id+" has content and safe opaque gutters");
            double[] a=fit(pixels,w,h,Integer.parseInt(row[2]),Integer.parseInt(row[3]));
            double[] b=fit(pixels,w,h,Integer.parseInt(row[4]),Integer.parseInt(row[5]));
            check(Math.abs(Math.toDegrees(Math.atan(a[0]))-30)<=1.5 && Math.abs(Math.toDegrees(Math.atan(b[0]))+30)<=1.5,
                    id+" visible foundation axes agree with the camera within the reviewed freestanding tolerance");
            check(a[2]<1 && b[2]<1,id+" inspected silhouette ranges still follow straight foundation edges");
            double lx=left,ly=a[0]*left+a[1],rx=right,ry=b[0]*right+b[1];
            double fx=(b[1]-a[1])/(a[0]-b[0]),fy=a[0]*fx+a[1],px=(lx+rx)*.5,py=(ly+ry)*.5;
            check(Math.abs(FactoryArt.pivotX(i)*w-px)<1 && Math.abs(FactoryArt.pivotY(i)*h-py)<1,
                    id+" runtime pivot registers the measured foundation center");
            double[][] corners={{lx,ly},{fx,fy},{rx,ry},{lx+rx-fx,ly+ry-fy}};double reach=0;
            for(double[] c:corners) {
                double dx=(c[0]-px)*FactoryArt.size(i)/w,dy=(c[1]-py)*FactoryArt.size(i)/h;
                double wx=dx/(2*Math.sqrt(.5))-dy/(2*Math.sqrt(.5)/Math.sqrt(3));
                double wy=dx/(2*Math.sqrt(.5))+dy/(2*Math.sqrt(.5)/Math.sqrt(3));
                check(Math.abs(wx)<=FactoryArt.footprintX(i)+.1 && Math.abs(wy)<=FactoryArt.footprintY(i)+.1,
                        id+" actual source foundation fits reserved grid occupancy without stretching");
                reach=Math.max(reach,Math.max(Math.abs(wx)/FactoryArt.footprintX(i),Math.abs(wy)/FactoryArt.footprintY(i)));
            }
            check(reach>.99 && reach<1.01,id+" uniform registration uses the measured footprint scale");
            check(FactoryArt.lightCount(i)>0,id+" has authored lighting points");
            for(int k=0;k<FactoryArt.lightCount(i);k++) {
                float x=FactoryArt.lightX(i,k),y=FactoryArt.lightY(i,k);
                check(anchor(pixels,w,h,x,y,false),id+" light point falls on source content");
                check(anchor(pixels,w,h,x,y,true),id+" light point has warm luminous source pixels nearby");
            }
            for(int k=0;k<FactoryArt.smokeCount(i);k++) check(anchor(pixels,w,h,FactoryArt.smokeX(i,k),FactoryArt.smokeY(i,k),false),
                    id+" smoke point falls on a source feature");
        }
        System.out.println(checks+" factory source pixels, camera axes, registration and emitter checks passed");
    }
    private static double[] fit(int[] p,int w,int h,int from,int to) {
        double sx=0,sy=0,sxx=0,sxy=0;int n=0;
        for(int x=from;x<=to;x++) {int y=bottom(p,w,h,x);check(y>=0,"reviewed edge has source pixels");sx+=x;sy+=y;sxx+=(double)x*x;sxy+=(double)x*y;n++;}
        double m=(n*sxy-sx*sy)/(n*sxx-sx*sx),b=(sy-m*sx)/n,e=0;
        for(int x=from;x<=to;x++) {double d=bottom(p,w,h,x)-m*x-b;e+=d*d;}
        return new double[]{m,b,Math.sqrt(e/n)};
    }
    private static int bottom(int[] p,int w,int h,int x) {int y=h-1;while(y>=0 && (p[y*w+x]>>>24)<128)y--;return y;}
    private static boolean anchor(int[] p,int w,int h,float u,float v,boolean warm) {
        if(!(u>0 && v>0 && u<1 && v<1)) return false;
        int cx=Math.round(u*w),cy=Math.round(v*h),radius=warm?24:5;
        for(int y=Math.max(0,cy-radius);y<=Math.min(h-1,cy+radius);y++) for(int x=Math.max(0,cx-radius);x<=Math.min(w-1,cx+radius);x++) {
            int c=p[y*w+x],r=c>>16&255,g=c>>8&255,b=c&255;
            if((c>>>24)>=128 && (!warm || r>=180 && g>=95 && r>b*1.4 && g>b*1.2)) return true;
        }
        return false;
    }
}
