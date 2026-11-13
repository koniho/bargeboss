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
    private final Map<String,Bitmap> sourceImages=new HashMap<String,Bitmap>();
    private Canvas canvas;
    public CanvasPainter(AssetManager assets) {
        this.assets=assets;
        spritePaint.setFilterBitmap(false);
        atlas(0); atlas(1);
    }
    private static final class Atlas {
        final Bitmap[] images;
        final Rect[] frames;
        Atlas(int atlas,AssetManager assets,Map<String,Bitmap> sources) throws IOException {
            int count=SpriteAtlas.count(atlas);
            images=new Bitmap[count]; frames=new Rect[count];
            for(int f=0;f<count;f++) {
                String path="sprites/"+SpriteAtlas.file(atlas,f);
                Bitmap image=sources.get(path);
                if(image==null) {
                    try(InputStream input=assets.open(path)) {
                        BitmapFactory.Options options=new BitmapFactory.Options(); options.inScaled=false;
                        image=BitmapFactory.decodeStream(input,null,options);
                    }
                    if(image==null) throw new IllegalStateException("Unreadable sprite atlas: "+path);
                    sources.put(path,image);
                }
                images[f]=image;
                frames[f]=new Rect(Math.round(image.getWidth()*SpriteAtlas.sourceLeft(atlas,f)),Math.round(image.getHeight()*SpriteAtlas.sourceTop(atlas,f)),
                        Math.round(image.getWidth()*SpriteAtlas.sourceRight(atlas,f)),Math.round(image.getHeight()*SpriteAtlas.sourceBottom(atlas,f)));
            }
        }
    }
    private Atlas atlas(int id) {
        Atlas cached=atlases.get(id); if(cached!=null) return cached;
        try {
            cached=new Atlas(id,assets,sourceImages); atlases.put(id,cached); return cached;
        } catch(IOException e) { throw new IllegalStateException("Missing sprite atlas for asset "+id,e); }
    }
    public void sprite(int id,int frame,float cx,float cy,float width,float height,float rotation,float alpha) {
        if(frame<0 || frame>=SpriteAtlas.count(id)) throw new IllegalArgumentException("Invalid sprite frame: "+frame);
        width*=SpriteAtlas.widthScale(id,frame); height*=SpriteAtlas.heightScale(id,frame);
        Atlas atlas=atlas(id);
        float left=-SpriteAtlas.pivotX(id,frame)*width,top=-SpriteAtlas.pivotY(id,frame)*height;
        canvas.save(); canvas.translate(cx,cy); canvas.rotate(rotation*180f/(float)Math.PI);
        spritePaint.setAlpha(Math.round(Math.max(0,Math.min(1,alpha))*255));
        canvas.drawBitmap(atlas.images[frame],atlas.frames[frame],new RectF(left,top,left+width,top+height),spritePaint);
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
        float aw=width*SpriteAtlas.widthScale(id,frameA),ah=height*SpriteAtlas.heightScale(id,frameA);
        float bw=width*SpriteAtlas.widthScale(id,frameB),bh=height*SpriteAtlas.heightScale(id,frameB);
        float ax=-SpriteAtlas.pivotX(id,frameA)*aw,ay=-SpriteAtlas.pivotY(id,frameA)*ah;
        float bx=-SpriteAtlas.pivotX(id,frameB)*bw,by=-SpriteAtlas.pivotY(id,frameB)*bh;
        canvas.save(); canvas.translate(cx,cy); canvas.rotate(rotation*180f/(float)Math.PI);
        canvas.saveLayerAlpha(Math.min(ax,bx)-2,Math.min(ay,by)-2,
                Math.max(ax+aw,bx+bw)+2,Math.max(ay+ah,by+bh)+2,Math.round(Math.max(0,Math.min(1,alpha))*255));
        spritePaint.setAlpha(255-weight);
        canvas.drawBitmap(atlas.images[frameA],atlas.frames[frameA],new RectF(ax,ay,ax+aw,ay+ah),spritePaint);
        spritePaint.setAlpha(weight); spritePaint.setXfermode(ADD);
        canvas.drawBitmap(atlas.images[frameB],atlas.frames[frameB],new RectF(bx,by,bx+bw,by+bh),spritePaint);
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
