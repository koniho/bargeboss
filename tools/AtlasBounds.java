package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Read-only sprite-atlas registration analysis. Never crops, rewrites or generates images.
 * Usage: AtlasBounds image.png columns rows [--alpha 32] [--frame N]
 *        [--roi left top right bottom] [--row-scan]
 * ROI coordinates and reported pivots are fractions of the complete atlas cell.
 * Ink and dense-band centers are measurements/candidates, not automatic physical hull pivots.
 */
public final class AtlasBounds {
    private static final class Stats {
        int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,maxX=-1,maxY=-1,count;
        long weight,sumX,sumY,sumXX,sumYY,sumXY;
        void add(int x,int y,int alpha) {
            minX=Math.min(minX,x); minY=Math.min(minY,y); maxX=Math.max(maxX,x); maxY=Math.max(maxY,y);
            count++; weight+=alpha; sumX+=(long)x*alpha; sumY+=(long)y*alpha;
            sumXX+=(long)x*x*alpha; sumYY+=(long)y*y*alpha; sumXY+=(long)x*y*alpha;
        }
        double x(int width) { return count==0?Double.NaN:(minX+maxX+1.0)/(2*width); }
        double y(int height) { return count==0?Double.NaN:(minY+maxY+1.0)/(2*height); }
        double centroidX(int width) { return count==0?Double.NaN:(sumX/(double)weight+.5)/width; }
        double centroidY(int height) { return count==0?Double.NaN:(sumY/(double)weight+.5)/height; }
        double axisDegrees() {
            if(count==0) return Double.NaN;
            double cx=sumX/(double)weight,cy=sumY/(double)weight;
            double xx=sumXX/(double)weight-cx*cx,yy=sumYY/(double)weight-cy*cy,xy=sumXY/(double)weight-cx*cy;
            double angle=.5*StrictMath.atan2(2*xy,xx-yy)*180/Math.PI;
            return angle<0?angle+180:angle;
        }
    }
    private AtlasBounds() {}
    public static void main(String[] args) throws Exception {
        if(args.length<3) throw new IllegalArgumentException("Usage: AtlasBounds image.png columns rows [--alpha N] [--frame N] [--roi L T R B] [--row-scan]");
        int columns=Integer.parseInt(args[1]),rows=Integer.parseInt(args[2]),threshold=32,onlyFrame=-1;
        double left=0,top=0,right=1,bottom=1; boolean rowScan=false;
        for(int i=3;i<args.length;i++) {
            if(args[i].equals("--alpha")) threshold=Integer.parseInt(args[++i]);
            else if(args[i].equals("--frame")) onlyFrame=Integer.parseInt(args[++i]);
            else if(args[i].equals("--roi")) {
                left=Double.parseDouble(args[++i]); top=Double.parseDouble(args[++i]);
                right=Double.parseDouble(args[++i]); bottom=Double.parseDouble(args[++i]);
            } else if(args[i].equals("--row-scan")) rowScan=true;
            else throw new IllegalArgumentException("Unknown option: "+args[i]);
        }
        if(columns<1 || rows<1 || threshold<1 || threshold>255 || left<0 || top<0 || right>1 || bottom>1 || right<=left || bottom<=top)
            throw new IllegalArgumentException("Invalid grid, alpha threshold or normalized ROI");
        BufferedImage atlas=ImageIO.read(new File(args[0]));
        if(atlas==null) throw new IllegalArgumentException("Unreadable image: "+args[0]);
        if(atlas.getWidth()<columns || atlas.getHeight()<rows)
            throw new IllegalArgumentException("Grid cells must have nonzero size");
        if(onlyFrame>=columns*rows || onlyFrame< -1) throw new IllegalArgumentException("Frame outside atlas");
        System.out.printf(Locale.ROOT,"# file=%s image=%dx%d grid=%dx%d nominal_cell=%.3fx%.3f alpha>=%d roi=%.3f,%.3f,%.3f,%.3f%n",
                args[0],atlas.getWidth(),atlas.getHeight(),columns,rows,atlas.getWidth()/(double)columns,atlas.getHeight()/(double)rows,threshold,left,top,right,bottom);
        System.out.println("frame,source_x,source_y,cell_width,cell_height,min_x,min_y,max_x,max_y,pixels,ink_x,ink_y,alpha_center_x,alpha_center_y,largest_x,largest_y,dense_x,dense_y,axis_degrees");
        for(int frame=0;frame<columns*rows;frame++) {
            if(onlyFrame>=0 && frame!=onlyFrame) continue;
            int column=frame%columns,row=frame/columns;
            // Mirror the native adapters exactly when a generated PNG has odd dimensions.
            int ox=Math.round(atlas.getWidth()*column/(float)columns),oy=Math.round(atlas.getHeight()*row/(float)rows);
            int width=Math.round(atlas.getWidth()*(column+1)/(float)columns)-ox;
            int height=Math.round(atlas.getHeight()*(row+1)/(float)rows)-oy;
            int[] alpha=new int[width*height],rowCounts=new int[height];
            int lx=(int)StrictMath.floor(left*width),ty=(int)StrictMath.floor(top*height);
            int rx=(int)StrictMath.ceil(right*width),by=(int)StrictMath.ceil(bottom*height);
            Stats all=new Stats(); int maxRow=0;
            for(int y=ty;y<by;y++) for(int x=lx;x<rx;x++) {
                int a=atlas.getRGB(ox+x,oy+y)>>>24;
                if(a<threshold) continue;
                alpha[y*width+x]=a; all.add(x,y,a); rowCounts[y]++; maxRow=Math.max(maxRow,rowCounts[y]);
            }
            Stats dense=new Stats();
            for(int y=ty;y<by;y++) if(rowCounts[y]>=maxRow*.55 && maxRow>0)
                for(int x=lx;x<rx;x++) if(alpha[y*width+x]!=0) dense.add(x,y,alpha[y*width+x]);
            Stats largest=largestComponent(alpha,width,height);
            System.out.printf(Locale.ROOT,"%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.3f%n",
                    frame,ox,oy,width,height,all.count==0?-1:all.minX,all.count==0?-1:all.minY,all.maxX,all.maxY,all.count,
                    all.x(width),all.y(height),all.centroidX(width),all.centroidY(height),largest.x(width),largest.y(height),dense.x(width),dense.y(height),all.axisDegrees());
            if(rowScan) for(int y=ty;y<by;y++) if(rowCounts[y]>0) {
                Stats scan=new Stats();
                for(int x=lx;x<rx;x++) if(alpha[y*width+x]!=0) scan.add(x,y,alpha[y*width+x]);
                System.out.printf(Locale.ROOT,"# row frame=%d y=%d pixels=%d minX=%d maxX=%d centerX=%.6f%n",frame,y,scan.count,scan.minX,scan.maxX,scan.x(width));
            }
        }
    }
    private static Stats largestComponent(int[] alpha,int width,int height) {
        boolean[] seen=new boolean[alpha.length]; int[] queue=new int[alpha.length]; Stats largest=new Stats();
        for(int start=0;start<alpha.length;start++) {
            if(alpha[start]==0 || seen[start]) continue;
            int head=0,tail=0; queue[tail++]=start; seen[start]=true; Stats current=new Stats();
            while(head<tail) {
                int at=queue[head++],x=at%width,y=at/width; current.add(x,y,alpha[at]);
                for(int dy=-1;dy<=1;dy++) for(int dx=-1;dx<=1;dx++) {
                    int nx=x+dx,ny=y+dy;
                    if(nx<0 || nx>=width || ny<0 || ny>=height) continue;
                    int next=ny*width+nx;
                    if(alpha[next]!=0 && !seen[next]) { seen[next]=true; queue[tail++]=next; }
                }
            }
            if(current.count>largest.count) largest=current;
        }
        return largest;
    }
}
