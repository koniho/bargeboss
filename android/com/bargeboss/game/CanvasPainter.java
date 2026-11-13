package com.bargeboss.game;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class CanvasPainter implements Painter {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final Paint spritePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final AssetManager assets;
    private final Map<Integer,Atlas> atlases=new HashMap<Integer,Atlas>();
    private Canvas canvas;
    public CanvasPainter(AssetManager assets) {
        this.assets=assets;
        spritePaint.setFilterBitmap(false);
        atlas(0); atlas(1);
    }
    private static final class Atlas {
        final Bitmap image;
        final Rect[] frames=new Rect[8];
        Atlas(Bitmap image) {
            this.image=image;
            for(int f=0;f<frames.length;f++) {
                int col=f%4,row=f/4;
                frames[f]=new Rect(Math.round(image.getWidth()*col/4f),Math.round(image.getHeight()*row/2f),
                        Math.round(image.getWidth()*(col+1)/4f),Math.round(image.getHeight()*(row+1)/2f));
            }
        }
    }
    private Atlas atlas(int id) {
        Atlas cached=atlases.get(id); if(cached!=null) return cached;
        String path="sprites/"+SpriteAtlas.file(id);
        try(InputStream input=assets.open(path)) {
            BitmapFactory.Options options=new BitmapFactory.Options(); options.inScaled=false;
            Bitmap bitmap=BitmapFactory.decodeStream(input,null,options);
            if(bitmap==null) throw new IllegalStateException("Unreadable sprite atlas: "+path);
            cached=new Atlas(bitmap); atlases.put(id,cached); return cached;
        } catch(IOException e) { throw new IllegalStateException("Missing sprite atlas: "+path,e); }
    }
    public void sprite(int id,int frame,float cx,float cy,float width,float height,float rotation,float alpha) {
        if(frame<0 || frame>=8) throw new IllegalArgumentException("Invalid sprite frame: "+frame);
        Atlas atlas=atlas(id);
        float left=-SpriteAtlas.pivotX(id,frame)*width,top=-SpriteAtlas.pivotY(id,frame)*height;
        canvas.save(); canvas.translate(cx,cy); canvas.rotate(rotation*180f/(float)Math.PI);
        spritePaint.setAlpha(Math.round(Math.max(0,Math.min(1,alpha))*255));
        canvas.drawBitmap(atlas.image,atlas.frames[frame],new RectF(left,top,left+width,top+height),spritePaint);
        canvas.restore();
    }
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
