package com.bargeboss.game;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
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
import java.util.LinkedHashMap;
import java.util.Iterator;

public final class CanvasPainter implements Painter {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final Paint spritePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint affinePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Matrix affineMatrix=new Matrix();
    private final float[] affineValues=new float[9];
    private final RectF affineUnit=new RectF(0,0,1,1);
    private static final PorterDuffXfermode ADD=new PorterDuffXfermode(PorterDuff.Mode.ADD);
    private final AssetManager assets;
    private final Map<Integer,Atlas> atlases=new HashMap<Integer,Atlas>();
    private final Map<String,Bitmap> sourceImages=new HashMap<String,Bitmap>();
    private final LinkedHashMap<Long,Bitmap> layers=new LinkedHashMap<Long,Bitmap>(16,.75f,true);
    private final Paint layerPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private long layerBytes;
    private int layerBuilds;
    private boolean paintingLayer;
    private Canvas canvas;
    public void pushClipOutside(float[] triangles) {
        canvas.save();path.reset();path.setFillType(Path.FillType.WINDING);
        for(int i=0;i<triangles.length;i+=6){path.moveTo(triangles[i],triangles[i+1]);path.lineTo(triangles[i+2],triangles[i+3]);path.lineTo(triangles[i+4],triangles[i+5]);path.close();}
        canvas.clipPath(path,android.graphics.Region.Op.DIFFERENCE);
    }
    public void popClip(){canvas.restore();}
    public void beginFrame() {if(!paintingLayer) layerBuilds=0;}
    public void layer(StaticLayer layer,float x,float y,float scale,float alpha) {
        float width=layer.width()*scale,height=layer.height()*scale;
        if(!(width>0 && height>0 && alpha>0) || !Float.isFinite(width) || !Float.isFinite(height)) return;
        if(canvas.quickReject(x,y,x+width,y+height,Canvas.EdgeType.AA)) return;
        if(paintingLayer) {drawLayerDirect(layer,x,y,scale,alpha);return;}
        float density=StaticLayer.rasterScale(scale);
        double pw=StrictMath.ceil(layer.width()*density),ph=StrictMath.ceil(layer.height()*density);
        long key=((long)layer.key()<<32)^(Float.floatToIntBits(density)&0xffffffffL);
        Bitmap image=layers.get(key);
        if(image==null) {
            if(pw<=0 || ph<=0 || pw*ph*4>StaticLayer.cacheBudgetBytes()
                    || layerBuilds>=StaticLayer.maxBuildsPerFrame()) {drawLayerDirect(layer,x,y,scale,alpha);return;}
            long bytes=4L*(int)pw*(int)ph;layerBuilds++;
            Iterator<Map.Entry<Long,Bitmap>> entries=layers.entrySet().iterator();
            while(layerBytes+bytes>StaticLayer.cacheBudgetBytes() && entries.hasNext()) {
                Bitmap old=entries.next().getValue();layerBytes-=old.getAllocationByteCount();entries.remove();
                // Hardware display lists may still reference this frame's images;
                // let the VM release evictions after those references disappear.
            }
            image=Bitmap.createBitmap((int)pw,(int)ph,Bitmap.Config.ARGB_8888);image.setDensity(Bitmap.DENSITY_NONE);
            Canvas previous=canvas;Paint previousPaint=new Paint(paint),previousSpritePaint=new Paint(spritePaint);
            paintingLayer=true;canvas=new Canvas(image);canvas.scale(density,density);canvas.clipRect(0,0,layer.width(),layer.height());
            try {layer.paint(this);} finally {canvas=previous;paintingLayer=false;paint.set(previousPaint);spritePaint.set(previousSpritePaint);}
            layers.put(key,image);layerBytes+=image.getAllocationByteCount();
        }
        int saved=canvas.save();
        try {
            canvas.clipRect(x,y,x+width,y+height);canvas.translate(x,y);canvas.scale(scale/density,scale/density);
            layerPaint.setAlpha(Math.round(Math.max(0,Math.min(1,alpha))*255));
            canvas.drawBitmap(image,0,0,layerPaint);
        } finally {canvas.restoreToCount(saved);}
    }
    private void drawLayerDirect(StaticLayer layer,float x,float y,float scale,float alpha) {
        int saved=canvas.save();boolean previousLayer=paintingLayer;
        Paint previousPaint=new Paint(paint),previousSpritePaint=new Paint(spritePaint);paintingLayer=true;
        try {
            canvas.clipRect(x,y,x+layer.width()*scale,y+layer.height()*scale);
            if(alpha<1) canvas.saveLayerAlpha(x,y,x+layer.width()*scale,y+layer.height()*scale,Math.round(Math.max(0,alpha)*255));
            canvas.translate(x,y);canvas.scale(scale,scale);layer.paint(this);
        } finally {canvas.restoreToCount(saved);paintingLayer=previousLayer;paint.set(previousPaint);spritePaint.set(previousSpritePaint);}
    }
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
                String key=SpriteGrade.cacheKey(atlas,path);
                Bitmap image=sources.get(key);
                if(image==null) {
                    try(InputStream input=assets.open(path)) {
                        BitmapFactory.Options options=new BitmapFactory.Options(); options.inScaled=false;
                        options.inPreferredConfig=Bitmap.Config.ARGB_8888;options.inMutable=SpriteGrade.applies(atlas);
                        image=BitmapFactory.decodeStream(input,null,options);
                    }
                    if(image==null) throw new IllegalStateException("Unreadable sprite atlas: "+path);
                    if(SpriteGrade.applies(atlas)) {
                        int w=image.getWidth(),h=image.getHeight();int[] row=new int[w];
                        for(int y=0;y<h;y++) {
                            image.getPixels(row,0,w,0,y,w,1);SpriteGrade.applyArgb(atlas,row);
                            image.setPixels(row,0,w,0,y,w,1);
                        }
                    }
                    sources.put(key,image);
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
    /** Map the complete cropped source cell onto supplied axes without another size correction. */
    public void spriteAffine(int id,int frame,float x,float y,float ax,float ay,float bx,float by,float alpha) {
        if(frame<0 || frame>=SpriteAtlas.count(id)) throw new IllegalArgumentException("Invalid affine sprite frame: "+frame);
        if(!(alpha>0) || ax*by-ay*bx==0) return;
        Atlas atlas=atlas(id);float px=SpriteAtlas.pivotX(id,frame),py=SpriteAtlas.pivotY(id,frame);
        affineValues[0]=ax;affineValues[1]=bx;affineValues[2]=x-ax*px-bx*py;
        affineValues[3]=ay;affineValues[4]=by;affineValues[5]=y-ay*px-by*py;
        affineValues[6]=affineValues[7]=0;affineValues[8]=1;affineMatrix.setValues(affineValues);
        affinePaint.setAlpha(Math.round(Math.max(0,Math.min(1,alpha))*255));
        int saved=canvas.save();
        try {canvas.concat(affineMatrix);canvas.drawBitmap(atlas.images[frame],atlas.frames[frame],affineUnit,affinePaint);}
        finally {canvas.restoreToCount(saved);}
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
