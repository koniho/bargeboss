package com.bargeboss.game;

import java.util.Arrays;

/** Clips water-plane effects to the same solid banks and piers used by gameplay.
 * Entry/exit ends of the main river remain open water beyond the navigation bounds.
 * All clipping is shared Java; native hosts receive ordinary primitives. */
public final class WaterEffectsPainter implements Painter {
    private static final float LIMIT=100000;
    private static final float[][] RIVER={{Physics.LEFT,-LIMIT,Physics.RIGHT,LIMIT}};
    private static final float[][] CHANNEL=partition(TrainingChannel.WATER_RECTANGLES,null);
    private static final float[][] SHARED=partition(RIVER,PhysicalBerths.solids(PhysicalBerths.SHARED)),DEEP=partition(RIVER,PhysicalBerths.solids(PhysicalBerths.DEEP));
    private static final float[] CIRCLE_X=new float[12],CIRCLE_Y=new float[12];
    static {for(int i=0;i<12;i++){double a=i*StrictMath.PI/6;CIRCLE_X[i]=(float)StrictMath.cos(a);CIRCLE_Y[i]=(float)StrictMath.sin(a);}}
    private final Painter target;
    private final RiverCamera camera;
    private final float screenWidth,screenHeight;
    private final float[][] water;
    private final boolean marina,calumet,indiana;
    public WaterEffectsPainter(Game g,Painter target) {
        this.target=target;marina=g.montroseStage();calumet=g.calumetStage();indiana=g.indianaStage();camera=target instanceof RiverPainter?((RiverPainter)target).camera:null;screenWidth=g.layout.width;screenHeight=g.layout.height;water=g.indianaStage()||g.montroseStage()||g.calumetStage()?null:CHANNEL;
    }
    public static boolean contains(Game g,float x,float y) {
        if(g.calumetStage())return CalumetRiver.contains(x,y);
        if(g.indianaStage())return IndianaHarbor.contains(x,y);
        if(g.montroseStage())return MontroseHarbor.contains(x,y);
        return CalumetMap.contains(x,y);
    }
    private static boolean inside(float[][] rectangles,float x,float y) {
        if(rectangles==null) return false;
        for(float[] r:rectangles) if(x>r[0] && x<r[2] && y>r[1] && y<r[3]) return true;
        return false;
    }
    /** Disjoint strips avoid double alpha where the training reaches overlap. */
    static float[][] partition(float[][] source,float[][] holes) {
        int count=source.length+(holes==null?0:holes.length);
        float[] xs=new float[count*2],ys=new float[count*2];int n=0;
        for(int group=0;group<2;group++) {
            float[][] rects=group==0?source:holes;if(rects==null) continue;
            for(float[] r:rects) {xs[n]=r[0];ys[n++]=r[1];xs[n]=r[2];ys[n++]=r[3];}
        }
        Arrays.sort(xs);Arrays.sort(ys);
        float[][] strips=new float[(n-1)*(n-1)][4];int used=0;
        for(int row=0;row<n-1;row++) {
            float top=ys[row],bottom=ys[row+1];if(bottom<=top) continue;
            float left=0,right=0;boolean open=false;
            for(int col=0;col<n-1;col++) {
                if(xs[col+1]<=xs[col]) continue;
                float x=(xs[col]+xs[col+1])*.5f,y=(top+bottom)*.5f;
                boolean valid=inside(source,x,y) && !inside(holes,x,y);
                if(valid) {if(!open) left=xs[col];right=xs[col+1];open=true;}
                if(open && (!valid || col==n-2)) {
                    strips[used++]=new float[]{left,top,right,bottom};open=false;
                }
            }
            if(open) strips[used++]=new float[]{left,top,right,bottom};
        }
        return Arrays.copyOf(strips,used);
    }
    public void rect(float x,float y,float width,float height,int color) {
        poly(new float[]{x,y,x+width,y,x+width,y+height,x,y+height},color);
    }
    public void circle(float x,float y,float radius,int color) {
        if(radius<=0 || (color>>>24)==0) return;
        float[] points=new float[24];
        for(int i=0;i<12;i++) {points[i*2]=x+radius*CIRCLE_X[i];points[i*2+1]=y+radius*CIRCLE_Y[i];}
        poly(points,color);
    }
    public void line(float x,float y,float xx,float yy,float width,int color) {
        if((color>>>24)==0)return;
        float dx=xx-x,dy=yy-y,length=Physics.length(dx,dy);
        if(length<.0001f) {circle(x,y,width*.5f,color);return;}
        float nx=-dy/length*width*.5f,ny=dx/length*width*.5f;
        poly(new float[]{x+nx,y+ny,xx+nx,yy+ny,xx-nx,yy-ny,x-nx,y-ny},color);
    }
    public void poly(float[] points,int color) {
        if(points.length<6 || (color>>>24)==0) return;
        float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        for(int i=0;i<points.length;i+=2) {
            // Bounds only: ordered comparisons avoid Math's signed-zero bit work
            // on Android. Keep every authored vertex untouched; reject NaN input.
            float x=points[i],y=points[i+1];if(x!=x || y!=y)return;
            if(x<minX)minX=x;if(x>maxX)maxX=x;
            if(y<minY)minY=y;if(y>maxY)maxY=y;
        }
        if(camera!=null && (camera.screenX(maxX,maxY)<-1 || camera.screenX(minX,minY)>screenWidth+1
                || camera.screenY(minX,maxY)<-1 || camera.screenY(maxX,minY)>screenHeight+1))return;
        if(calumet){CalumetRiver.paintWater(target,points,color);return;}
        if(indiana){IndianaHarbor.paintWater(target,points,color);return;}
        if(marina){MontroseHarbor.paintWater(target,points,color);return;}
        for(float[] r:water) {
            if(maxX<=r[0] || minX>=r[2] || maxY<=r[1] || minY>=r[3]) continue;
            if(minX>=r[0] && maxX<=r[2] && minY>=r[1] && maxY<=r[3]) {target.poly(points,color);return;}
            float[] clipped=points;
            // Only crossing planes need clipping. Interior copies formerly allocated
            // two arrays per plane even when every point already satisfied it.
            if(minX<r[0])clipped=clip(clipped,0,r[0],true);
            if(clipped.length>=6 && minY<r[1])clipped=clip(clipped,1,r[1],true);
            if(clipped.length>=6 && maxX>r[2])clipped=clip(clipped,0,r[2],false);
            if(clipped.length>=6 && maxY>r[3])clipped=clip(clipped,1,r[3],false);
            if(clipped.length>=6) target.poly(clipped,color);
        }
    }
    private static float[] clip(float[] points,int axis,float bound,boolean minimum) {
        float[] out=new float[points.length+8];int used=0;
        float ax=points[points.length-2],ay=points[points.length-1];
        float a=axis==0?ax:ay;boolean previous=minimum?a>=bound:a<=bound;
        for(int i=0;i<points.length;i+=2) {
            float bx=points[i],by=points[i+1],b=axis==0?bx:by;
            boolean current=minimum?b>=bound:b<=bound;
            if(current!=previous) {
                float t=(bound-a)/(b-a);out[used++]=ax+(bx-ax)*t;out[used++]=ay+(by-ay)*t;
            }
            if(current) {out[used++]=bx;out[used++]=by;}
            ax=bx;ay=by;a=b;previous=current;
        }
        return Arrays.copyOf(out,used);
    }
    public void sprite(int atlas,int frame,float x,float y,float width,float height,float rotation,float alpha) {
        throw new IllegalArgumentException("Water effects must use clipped water-plane primitives");
    }
    public void spriteBlend(int atlas,int a,int b,float x,float y,float width,float height,float rotation,float blend,float alpha) {
        throw new IllegalArgumentException("Water effects must use clipped water-plane primitives");
    }
}
