package com.bargeboss.game;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class CanvasPainter implements Painter {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final Paint spritePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final PorterDuffXfermode ADD=new PorterDuffXfermode(PorterDuff.Mode.ADD);
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
        final Rect[] frames;
        Atlas(Bitmap image,int atlas) {
            this.image=image; frames=new Rect[SpriteAtlas.count(atlas)];
            int columns=SpriteAtlas.columns(atlas),rows=SpriteAtlas.rows(atlas);
            for(int f=0;f<frames.length;f++) {
                int col=f%columns,row=f/columns;
                frames[f]=new Rect(Math.round(image.getWidth()*col/(float)columns),Math.round(image.getHeight()*row/(float)rows),
                        Math.round(image.getWidth()*(col+1)/(float)columns),Math.round(image.getHeight()*(row+1)/(float)rows));
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
            cached=new Atlas(bitmap,id); atlases.put(id,cached); return cached;
        } catch(IOException e) { throw new IllegalStateException("Missing sprite atlas: "+path,e); }
    }
    public void sprite(int id,int frame,float cx,float cy,float width,float height,float rotation,float alpha) {
        if(frame<0 || frame>=SpriteAtlas.count(id)) throw new IllegalArgumentException("Invalid sprite frame: "+frame);
        Atlas atlas=atlas(id);
        float left=-SpriteAtlas.pivotX(id,frame)*width,top=-SpriteAtlas.pivotY(id,frame)*height;
        canvas.save(); canvas.translate(cx,cy); canvas.rotate(rotation*180f/(float)Math.PI);
        spritePaint.setAlpha(Math.round(Math.max(0,Math.min(1,alpha))*255));
        canvas.drawBitmap(atlas.image,atlas.frames[frame],new RectF(left,top,left+width,top+height),spritePaint);
        canvas.restore();
    }
    public void spriteBlend(int id,int frameA,int frameB,float cx,float cy,float width,float height,
                            float rotation,float blend,float alpha) {
        int count=SpriteAtlas.count(id);
        if(frameA<0 || frameA>=count || frameB<0 || frameB>=count)
            throw new IllegalArgumentException("Invalid sprite blend frames");
        int weight=Math.round(Math.max(0,Math.min(1,blend))*255);
        if(weight==0 || frameA==frameB) { sprite(id,frameA,cx,cy,width,height,rotation,alpha); return; }
        if(weight==255) { sprite(id,frameB,cx,cy,width,height,rotation,alpha); return; }
        Atlas atlas=atlas(id);
        float ax=-SpriteAtlas.pivotX(id,frameA)*width,ay=-SpriteAtlas.pivotY(id,frameA)*height;
        float bx=-SpriteAtlas.pivotX(id,frameB)*width,by=-SpriteAtlas.pivotY(id,frameB)*height;
        canvas.save(); canvas.translate(cx,cy); canvas.rotate(rotation*180f/(float)Math.PI);
        canvas.saveLayerAlpha(Math.min(ax,bx)-2,Math.min(ay,by)-2,
                Math.max(ax,bx)+width+2,Math.max(ay,by)+height+2,Math.round(Math.max(0,Math.min(1,alpha))*255));
        spritePaint.setAlpha(255-weight);
        canvas.drawBitmap(atlas.image,atlas.frames[frameA],new RectF(ax,ay,ax+width,ay+height),spritePaint);
        spritePaint.setAlpha(weight); spritePaint.setXfermode(ADD);
        canvas.drawBitmap(atlas.image,atlas.frames[frameB],new RectF(bx,by,bx+width,by+height),spritePaint);
        spritePaint.setXfermode(null); canvas.restore(); canvas.restore();
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
