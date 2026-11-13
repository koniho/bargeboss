package com.bargeboss.game;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

public final class CanvasPainter implements Painter {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private Canvas canvas;
    public void bind(Canvas canvas) { this.canvas=canvas; }
    public void rect(float x,float y,float w,float h,int c) { paint.setColor(c); canvas.drawRect(x,y,x+w,y+h,paint); }
    public void circle(float x,float y,float r,int c) { paint.setColor(c); canvas.drawCircle(x,y,r,paint); }
    public void poly(float[] xy,int c) {
        paint.setColor(c); path.reset(); path.moveTo(xy[0],xy[1]);
        for(int i=2;i<xy.length;i+=2) path.lineTo(xy[i],xy[i+1]); path.close(); canvas.drawPath(path,paint);
    }
    public void line(float x,float y,float xx,float yy,float width,int c) {
        paint.setColor(c); paint.setStrokeWidth(width); paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(x,y,xx,yy,paint);
    }
}
