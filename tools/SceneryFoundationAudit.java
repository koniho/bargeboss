package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Read-only source audit. Choose ranges on the two visible bottom foundation edges. */
public final class SceneryFoundationAudit {
    private static double[] edge(BufferedImage image,int from,int to) {
        double sx=0,sy=0,sxx=0,sxy=0;int count=0;
        for(int x=from;x<=to;x++) {
            int y=image.getHeight()-1;
            while(y>=0 && (image.getRGB(x,y)>>>24)<128) y--;
            if(y<0) continue;
            sx+=x;sy+=y;sxx+=(double)x*x;sxy+=(double)x*y;count++;
        }
        if(count<2) throw new IllegalArgumentException("Need at least two opaque edge samples");
        double slope=(count*sxy-sx*sy)/(count*sxx-sx*sx),intercept=(sy-slope*sx)/count,error2=0;
        for(int x=from;x<=to;x++) {
            int y=image.getHeight()-1;
            while(y>=0 && (image.getRGB(x,y)>>>24)<128) y--;
            if(y<0) continue;
            double error=y-slope*x-intercept;error2+=error*error;
        }
        return new double[]{slope,intercept,StrictMath.sqrt(error2/count)};
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=5) throw new IllegalArgumentException("PNG leftStartX leftEndX rightStartX rightEndX");
        BufferedImage image=ImageIO.read(new File(args[0]));
        if(image==null) throw new IllegalArgumentException("Unreadable PNG: "+args[0]);
        int left=image.getWidth(),top=image.getHeight(),right=-1,bottom=-1;
        for(int y=0;y<image.getHeight();y++) for(int x=0;x<image.getWidth();x++) if((image.getRGB(x,y)>>>24)>=128) {
            left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);
        }
        if(right<0) throw new IllegalArgumentException("No alpha>=128 source pixels");
        System.out.println("Image "+image.getWidth()+"x"+image.getHeight()+"; alpha>=128 bounds (inclusive) "+left+","+top+","+right+","+bottom);
        System.out.println("Gutters left/top/right/bottom "+left+","+top+","+(image.getWidth()-right-1)+","+(image.getHeight()-bottom-1));
        double[][] lines=new double[2][];
        for(int i=0;i<2;i++) {
            int from=Integer.parseInt(args[1+i*2]),to=Integer.parseInt(args[2+i*2]);
            if(from<0 || to>=image.getWidth() || from>=to) throw new IllegalArgumentException("Invalid foundation edge range");
            lines[i]=edge(image,from,to);
            System.out.printf(java.util.Locale.ROOT,"Edge%d x%d..%d: angle %.3f degrees; slope %.6f; intercept %.3f; RMS %.3fpx%n",
                    i,from,to,StrictMath.toDegrees(StrictMath.atan(lines[i][0])),lines[i][0],lines[i][1],lines[i][2]);
        }
        double x=(lines[1][1]-lines[0][1])/(lines[0][0]-lines[1][0]),y=lines[0][0]*x+lines[0][1];
        System.out.printf(java.util.Locale.ROOT,"Fitted front corner %.3f,%.3f%n",x,y);
        System.out.println("Review ranges on the source. Roof overhangs, shadows and bevels can invalidate silhouette fits; this does not certify an asset.");
    }
}
