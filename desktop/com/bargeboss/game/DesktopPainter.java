package com.bargeboss.game;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import javax.imageio.ImageIO;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

public final class DesktopPainter implements Painter {
    private static final class Images {
        LightingProfile profile=LightingProfile.DUSK;
        final Map<Integer,BufferedImage[]> atlases=new HashMap<Integer,BufferedImage[]>();
        final Map<String,BufferedImage> sources=new HashMap<String,BufferedImage>();
    }
    private static final ThreadLocal<Images> IMAGES=new ThreadLocal<Images>(){protected Images initialValue(){return new Images();}};
    private LightingProfile lightingProfile=LightingProfile.DUSK;
    public LightingProfile lighting(){return lightingProfile;}
    public void prepareScene(LightingProfile profile,int[] atlases){lighting(profile);for(int id:atlases)atlas(id);}
    public void lighting(LightingProfile profile){
        lightingProfile=profile;Images images=IMAGES.get();
        if(images.profile==profile)return;
        images.atlases.clear();images.sources.clear();images.profile=profile;
        LAYERS.get().images.clear();LAYERS.get().bytes=0;
    }
    private static final ThreadLocal<BufferedImage> BLEND_BUFFER=new ThreadLocal<BufferedImage>();
    private static final ThreadLocal<BufferedImage> LAYER_SCRATCH=new ThreadLocal<BufferedImage>();
    private static final ThreadLocal<LayerCache> LAYERS=new ThreadLocal<LayerCache>() {
        protected LayerCache initialValue() {return new LayerCache();}
    };
    private static final class LayerCache {
        final LinkedHashMap<Long,BufferedImage> images=new LinkedHashMap<Long,BufferedImage>(16,.75f,true);
        long bytes;int builds;
        void room(long incoming) {
            Iterator<Map.Entry<Long,BufferedImage>> entries=images.entrySet().iterator();
            while(bytes+incoming>StaticLayer.cacheBudgetBytes() && entries.hasNext()) {
                BufferedImage old=entries.next().getValue();bytes-=4L*old.getWidth()*old.getHeight();entries.remove();old.flush();
            }
        }
    }
    private final Graphics2D g;
    private final boolean paintingLayer;
    private final java.util.ArrayList<java.awt.Shape> clipStack=new java.util.ArrayList<java.awt.Shape>();
    public boolean supportsUnionClips(){return true;}
    public void pushClipOutside(float[] triangles) {
        java.awt.Shape previous=g.getClip();clipStack.add(previous);
        java.awt.geom.Area visible=new java.awt.geom.Area(previous==null?new Rectangle2D.Float(-1000000,-1000000,2000000,2000000):previous);
        Path2D.Float holes=new Path2D.Float(Path2D.WIND_NON_ZERO);
        for(int i=0;i<triangles.length;i+=6){holes.moveTo(triangles[i],triangles[i+1]);holes.lineTo(triangles[i+2],triangles[i+3]);holes.lineTo(triangles[i+4],triangles[i+5]);holes.closePath();}
        visible.subtract(new java.awt.geom.Area(holes));g.setClip(visible);
    }
    public void popClip(){g.setClip(clipStack.remove(clipStack.size()-1));}
    public DesktopPainter(Graphics2D g) {this(g,false,IMAGES.get().profile);}
    private DesktopPainter(Graphics2D g,boolean paintingLayer) {this(g,paintingLayer,LightingProfile.DUSK);}
    private DesktopPainter(Graphics2D g,boolean paintingLayer,LightingProfile profile){this.g=g;this.paintingLayer=paintingLayer;lighting(profile);}
    public void beginFrame() {if(!paintingLayer) LAYERS.get().builds=0;}
    public void layer(StaticLayer layer,float x,float y,float scale,float alpha) {
        float width=layer.width()*scale,height=layer.height()*scale;
        if(!(width>0 && height>0 && alpha>0) || !Float.isFinite(width) || !Float.isFinite(height)) return;
        if(!g.hitClip((int)StrictMath.floor(x),(int)StrictMath.floor(y),(int)StrictMath.ceil(width)+1,(int)StrictMath.ceil(height)+1)) return;
        if(paintingLayer) {drawLayerDirect(layer,x,y,scale,alpha);return;}
        float density=StaticLayer.rasterScale(scale);
        double pw=StrictMath.ceil(layer.width()*density),ph=StrictMath.ceil(layer.height()*density);
        long key=((long)layer.key()<<32)^(Float.floatToIntBits(density)&0xffffffffL);
        LayerCache cache=LAYERS.get();BufferedImage image=cache.images.get(key);
        if(image==null) {
            if(pw<=0 || ph<=0 || pw*ph*4>StaticLayer.cacheBudgetBytes()
                    || cache.builds>=StaticLayer.maxBuildsPerFrame()) {drawLayerDirect(layer,x,y,scale,alpha);return;}
            int pixelsW=(int)pw,pixelsH=(int)ph;long bytes=4L*pixelsW*pixelsH;
            cache.builds++;cache.room(bytes);
            image=new BufferedImage(pixelsW,pixelsH,BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D offscreen=image.createGraphics();
            try {
                offscreen.setRenderingHints(g.getRenderingHints());offscreen.scale(density,density);
                offscreen.clip(new Rectangle2D.Float(0,0,layer.width(),layer.height()));
                layer.paint(new DesktopPainter(offscreen,true,lightingProfile));
            } finally {offscreen.dispose();}
            cache.images.put(key,image);cache.bytes+=bytes;
        }
        Graphics2D target=(Graphics2D)g.create();
        try {
            target.clip(new Rectangle2D.Float(x,y,width,height));
            target.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,Math.max(0,Math.min(1,alpha))));
            target.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            AffineTransform transform=new AffineTransform();transform.translate(x,y);transform.scale(scale/density,scale/density);
            target.drawImage(image,transform,null);
        } finally {target.dispose();}
    }
    private void drawLayerDirect(StaticLayer layer,float x,float y,float scale,float alpha) {
        if(alpha>=1) {
            Graphics2D target=(Graphics2D)g.create();
            try {
                target.clip(new Rectangle2D.Float(x,y,layer.width()*scale,layer.height()*scale));
                target.translate(x,y);target.scale(scale,scale);layer.paint(new DesktopPainter(target,true,lightingProfile));
            } finally {target.dispose();}
            return;
        }
        // Faded cold/oversized layers still require group alpha. A reusable
        // 1026-square scratch surface bounds working memory; invisible tiles
        // are rejected, and one-pixel gutters prevent filtering seams.
        float density=StaticLayer.rasterScale(scale);int tile=1024;
        int width=(int)StrictMath.ceil(layer.width()*density),height=(int)StrictMath.ceil(layer.height()*density);
        java.awt.Rectangle clip=g.getClipBounds();
        int firstX=0,firstY=0,lastX=width,lastY=height;
        if(clip!=null) {
            firstX=Math.max(0,(int)StrictMath.floor((clip.x-x)*density/scale/tile)*tile);
            firstY=Math.max(0,(int)StrictMath.floor((clip.y-y)*density/scale/tile)*tile);
            lastX=Math.min(width,(int)StrictMath.ceil((clip.x+clip.width-x)*density/scale));
            lastY=Math.min(height,(int)StrictMath.ceil((clip.y+clip.height-y)*density/scale));
        }
        for(int ty=firstY;ty<lastY;ty+=tile) for(int tx=firstX;tx<lastX;tx+=tile) {
            int tw=Math.min(tile,width-tx),th=Math.min(tile,height-ty);
            float left=x+tx*scale/density,top=y+ty*scale/density;
            float right=Math.min(x+layer.width()*scale,left+tw*scale/density),bottom=Math.min(y+layer.height()*scale,top+th*scale/density);
            if(!g.hitClip((int)StrictMath.floor(left),(int)StrictMath.floor(top),(int)StrictMath.ceil(right-left)+1,(int)StrictMath.ceil(bottom-top)+1)) continue;
            BufferedImage scratch=LAYER_SCRATCH.get();
            if(scratch==null || scratch.getWidth()<tw+2 || scratch.getHeight()<th+2) {
                scratch=new BufferedImage(Math.max(tw+2,scratch==null?0:scratch.getWidth()),
                        Math.max(th+2,scratch==null?0:scratch.getHeight()),BufferedImage.TYPE_INT_ARGB_PRE);LAYER_SCRATCH.set(scratch);
            }
            Graphics2D offscreen=scratch.createGraphics();
            try {
                offscreen.setComposite(AlphaComposite.Clear);offscreen.fillRect(0,0,tw+2,th+2);
                offscreen.setComposite(AlphaComposite.SrcOver);offscreen.setRenderingHints(g.getRenderingHints());
                offscreen.translate(1-tx,1-ty);offscreen.scale(density,density);
                offscreen.clip(new Rectangle2D.Float(0,0,layer.width(),layer.height()));
                layer.paint(new DesktopPainter(offscreen,true,lightingProfile));
            } finally {offscreen.dispose();}
            Graphics2D target=(Graphics2D)g.create();
            try {
                target.clip(new Rectangle2D.Float(left,top,right-left,bottom-top));
                target.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,Math.max(0,Math.min(1,alpha))));
                target.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                target.translate(x+(tx-1)*scale/density,y+(ty-1)*scale/density);target.scale(scale/density,scale/density);
                target.drawImage(scratch,0,0,tw+2,th+2,0,0,tw+2,th+2,null);
            } finally {target.dispose();}
        }
    }
    private BufferedImage[] atlas(int id) {
        BufferedImage[] frames=IMAGES.get().atlases.get(id); if(frames!=null) return frames;
        Map<String,BufferedImage> sources=IMAGES.get().sources;
        frames=new BufferedImage[SpriteAtlas.count(id)];
        for(int f=0;f<frames.length;f++) {
            String path="assets/sprites/"+SpriteAtlas.file(id,f);
            String key=SpriteGrade.cacheKey(id,path,lightingProfile);
            BufferedImage image=sources.get(key);
            if(image==null) {
                try(InputStream input=DesktopPainter.class.getResourceAsStream("/"+path)) {
                    image=input==null?ImageIO.read(new File(path)):ImageIO.read(input);
                } catch(IOException e) { throw new IllegalStateException("Missing sprite atlas: "+path,e); }
                if(image==null) throw new IllegalStateException("Unreadable sprite atlas: "+path);
                if(SpriteGrade.applies(id)) {
                    int w=image.getWidth(),h=image.getHeight();int[] row=new int[w];
                    BufferedImage graded=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
                    for(int y=0;y<h;y++) {
                        image.getRGB(0,y,w,1,row,0,w);SpriteGrade.applyArgbRow(id,row,lightingProfile,y,h);graded.setRGB(0,y,w,1,row,0,w);
                    }
                    image=graded;
                }
                sources.put(key,image);
            }
            int x=Math.round(image.getWidth()*SpriteAtlas.sourceLeft(id,f)),y=Math.round(image.getHeight()*SpriteAtlas.sourceTop(id,f));
            int right=Math.round(image.getWidth()*SpriteAtlas.sourceRight(id,f)),bottom=Math.round(image.getHeight()*SpriteAtlas.sourceBottom(id,f));
            frames[f]=image.getSubimage(x,y,right-x,bottom-y);
        }
        IMAGES.get().atlases.put(id,frames); return frames;
    }
    public void normalSprite(int frame,float x,float y,float w,float h,float nx,float ny,float nz,float radius,int color,float alpha){
        BufferedImage source=atlas(SpriteAtlas.MOORED_NORMALS)[frame],lit=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int yy=0;yy<source.getHeight();yy++)for(int xx=0;xx<source.getWidth();xx++){int c=source.getRGB(xx,yy);float a=(c>>>24)/255f;if(a==0)continue;
            int coverage=Math.round(255*a*alpha*BoatSurfaceLight.response(c,(xx+.5f)/source.getWidth(),(yy+.5f)/source.getHeight(),nx,ny,nz,radius));lit.setRGB(xx,yy,(coverage<<24)|(color&0xffffff));}
        AffineTransform transform=new AffineTransform();transform.translate(x-w*.5f,y-h*.7f);transform.scale(w/source.getWidth(),h/source.getHeight());g.drawImage(lit,transform,null);
    }
    public void sprite(int id,int frame,float cx,float cy,float width,float height,float rotation,float alpha) {
        if(frame<0 || frame>=SpriteAtlas.count(id)) throw new IllegalArgumentException("Invalid sprite frame: "+frame);
        width*=SpriteAtlas.widthScale(id,frame); height*=SpriteAtlas.heightScale(id,frame);
        BufferedImage image=atlas(id)[frame]; Graphics2D sprite=(Graphics2D)g.create();
        sprite.translate(cx,cy); sprite.rotate(rotation);
        sprite.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,Math.max(0,Math.min(1,alpha))));
        sprite.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        AffineTransform transform=new AffineTransform();
        transform.translate(-SpriteAtlas.pivotX(id,frame)*width,-SpriteAtlas.pivotY(id,frame)*height);
        transform.scale(width/image.getWidth(),height/image.getHeight());
        sprite.drawImage(image,transform,null); sprite.dispose();
    }
    /** Axes already include caller scaling; this path deliberately skips atlas size registration. */
    public void spriteAffine(int id,int frame,float x,float y,float ax,float ay,float bx,float by,float alpha) {
        if(frame<0 || frame>=SpriteAtlas.count(id)) throw new IllegalArgumentException("Invalid affine sprite frame: "+frame);
        if(!(alpha>0) || ax*by-ay*bx==0) return;
        BufferedImage image=atlas(id)[frame];float px=SpriteAtlas.pivotX(id,frame),py=SpriteAtlas.pivotY(id,frame);
        AffineTransform transform=new AffineTransform(ax/image.getWidth(),ay/image.getWidth(),
                bx/image.getHeight(),by/image.getHeight(),x-ax*px-bx*py,y-ay*px-by*py);
        Graphics2D target=(Graphics2D)g.create();
        try {
            target.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,Math.max(0,Math.min(1,alpha))));
            target.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            target.drawImage(image,transform,null);
        } finally {target.dispose();}
    }
    public void spriteBlend(int id,int frameA,int frameB,float cx,float cy,float width,float height,
                            float rotation,float blend,float alpha) {
        int count=SpriteAtlas.count(id);
        if(frameA<0 || frameA>=count || frameB<0 || frameB>=count)
            throw new IllegalArgumentException("Invalid sprite blend frames");
        int weight=Math.round(Math.max(0,Math.min(1,blend))*255);
        if(weight==0 || frameA==frameB) { sprite(id,frameA,cx,cy,width,height,rotation,alpha); return; }
        if(weight==255) { sprite(id,frameB,cx,cy,width,height,rotation,alpha); return; }
        BufferedImage[] frames=atlas(id);
        float aw=width*SpriteAtlas.widthScale(id,frameA),ah=height*SpriteAtlas.heightScale(id,frameA);
        float bw=width*SpriteAtlas.widthScale(id,frameB),bh=height*SpriteAtlas.heightScale(id,frameB);
        float ax=-SpriteAtlas.pivotX(id,frameA)*aw,ay=-SpriteAtlas.pivotY(id,frameA)*ah;
        float bx=-SpriteAtlas.pivotX(id,frameB)*bw,by=-SpriteAtlas.pivotY(id,frameB)*bh;
        double scale=Math.max(.25,StrictMath.sqrt(Math.abs(g.getTransform().getDeterminant())));
        float left=(float)(StrictMath.floor(Math.min(ax,bx)*scale)/scale-2);
        float top=(float)(StrictMath.floor(Math.min(ay,by)*scale)/scale-2);
        int pixelsW=(int)StrictMath.ceil((Math.max(ax+aw,bx+bw)-left+2)*scale);
        int pixelsH=(int)StrictMath.ceil((Math.max(ay+ah,by+bh)-top+2)*scale);
        BufferedImage buffer=BLEND_BUFFER.get();
        if(buffer==null || buffer.getWidth()<pixelsW || buffer.getHeight()<pixelsH) {
            buffer=new BufferedImage(Math.max(pixelsW,buffer==null?0:buffer.getWidth()),
                    Math.max(pixelsH,buffer==null?0:buffer.getHeight()),BufferedImage.TYPE_INT_ARGB_PRE);
            BLEND_BUFFER.set(buffer);
        }
        Graphics2D layer=buffer.createGraphics();
        layer.setComposite(AlphaComposite.Clear); layer.fillRect(0,0,pixelsW,pixelsH);
        layer.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        layer.scale(scale,scale); layer.translate(-left,-top);
        layer.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,(255-weight)/255f));
        drawCell(layer,frames[frameA],ax,ay,aw,ah);
        layer.setComposite(new AdditiveComposite(weight/255f));
        drawCell(layer,frames[frameB],bx,by,bw,bh); layer.dispose();
        Graphics2D target=(Graphics2D)g.create(); target.translate(cx,cy); target.rotate(rotation);
        target.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,Math.max(0,Math.min(1,alpha))));
        target.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        target.translate(left,top); target.scale(1/scale,1/scale);
        target.drawImage(buffer,0,0,pixelsW,pixelsH,0,0,pixelsW,pixelsH,null); target.dispose();
    }
    private static void drawCell(Graphics2D target,BufferedImage cell,float x,float y,float width,float height) {
        AffineTransform transform=new AffineTransform(); transform.translate(x,y);
        transform.scale(width/cell.getWidth(),height/cell.getHeight()); target.drawImage(cell,transform,null);
    }
    /** Sum premultiplied colors/alpha; ordinary SRC_OVER would fade opaque overlaps to 75%. */
    private static final class AdditiveComposite implements Composite {
        private final float weight;
        AdditiveComposite(float weight) { this.weight=weight; }
        public CompositeContext createContext(final ColorModel source,final ColorModel destination,RenderingHints hints) {
            return new CompositeContext() {
                public void dispose() {}
                public void compose(Raster src,Raster dst,WritableRaster out) {
                    int width=Math.min(src.getWidth(),dst.getWidth()),height=Math.min(src.getHeight(),dst.getHeight());
                    Object from=null,to=null,result=null;
                    for(int y=0;y<height;y++) for(int x=0;x<width;x++) {
                        from=src.getDataElements(src.getMinX()+x,src.getMinY()+y,from);
                        to=dst.getDataElements(dst.getMinX()+x,dst.getMinY()+y,to);
                        int a=source.getRGB(from),b=destination.getRGB(to);
                        int sa=Math.round((a>>>24)*weight),da=b>>>24,alpha=Math.min(255,sa+da);
                        int rgb=0;
                        if(alpha>0) {
                            int red=Math.min(255,(((a>>16)&255)*sa+((b>>16)&255)*da+alpha/2)/alpha);
                            int green=Math.min(255,(((a>>8)&255)*sa+((b>>8)&255)*da+alpha/2)/alpha);
                            int blue=Math.min(255,((a&255)*sa+(b&255)*da+alpha/2)/alpha);
                            rgb=(alpha<<24)|(red<<16)|(green<<8)|blue;
                        }
                        result=destination.getDataElements(rgb,result);
                        out.setDataElements(out.getMinX()+x,out.getMinY()+y,result);
                    }
                }
            };
        }
    }
    private void color(int c) { g.setColor(new Color(c,true)); }
    public void rect(float x,float y,float w,float h,int c) { color(c); g.fill(new Rectangle2D.Float(x,y,w,h)); }
    public void circle(float x,float y,float r,int c) { color(c); g.fill(new Ellipse2D.Float(x-r,y-r,r*2,r*2)); }
    public void shadedTriangle(float ax,float ay,int ac,float bx,float by,int bc,float cx,float cy,int cc){
        if(Math.abs((bx-ax)*(cy-ay)-(by-ay)*(cx-ax))<.00001f)return;
        Path2D path=new Path2D.Float();path.moveTo(ax,ay);path.lineTo(bx,by);path.lineTo(cx,cy);path.closePath();
        g.setPaint(new TrianglePaint(ax,ay,ac,bx,by,bc,cx,cy,cc));g.fill(path);
    }
    public void poly(float[] xy,int c) {
        color(c); Path2D.Float path=new Path2D.Float(); path.moveTo(xy[0],xy[1]);
        for(int i=2;i<xy.length;i+=2) path.lineTo(xy[i],xy[i+1]); path.closePath(); g.fill(path);
    }
    public void polygons(float[][] xy,int count,int c) {
        color(c);Path2D.Float path=new Path2D.Float();
        for(int n=0;n<count;n++){
            float[] q=xy[n];path.moveTo(q[0],q[1]);
            for(int i=2;i<q.length;i+=2)path.lineTo(q[i],q[i+1]);path.closePath();
        }
        g.fill(path);
    }
    public void line(float x1,float y1,float x2,float y2,float width,int c) {
        color(c); g.setStroke(new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Line2D.Float(x1,y1,x2,y2));
    }
}
