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
import javax.imageio.ImageIO;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

public final class DesktopPainter implements Painter {
    private static final Map<Integer,BufferedImage[]> ATLASES=new HashMap<Integer,BufferedImage[]>();
    private static final Map<String,BufferedImage> SOURCE_IMAGES=new HashMap<String,BufferedImage>();
    private static final ThreadLocal<BufferedImage> BLEND_BUFFER=new ThreadLocal<BufferedImage>();
    private final Graphics2D g;
    public DesktopPainter(Graphics2D g) { this.g=g; }
    private static synchronized BufferedImage[] atlas(int id) {
        BufferedImage[] frames=ATLASES.get(id); if(frames!=null) return frames;
        Map<String,BufferedImage> sources=SOURCE_IMAGES;
        frames=new BufferedImage[SpriteAtlas.count(id)];
        for(int f=0;f<frames.length;f++) {
            String path="assets/sprites/"+SpriteAtlas.file(id,f);
            String key=SpriteGrade.cacheKey(id,path);
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
                        image.getRGB(0,y,w,1,row,0,w);SpriteGrade.applyArgb(id,row);graded.setRGB(0,y,w,1,row,0,w);
                    }
                    image=graded;
                }
                sources.put(key,image);
            }
            int x=Math.round(image.getWidth()*SpriteAtlas.sourceLeft(id,f)),y=Math.round(image.getHeight()*SpriteAtlas.sourceTop(id,f));
            int right=Math.round(image.getWidth()*SpriteAtlas.sourceRight(id,f)),bottom=Math.round(image.getHeight()*SpriteAtlas.sourceBottom(id,f));
            frames[f]=image.getSubimage(x,y,right-x,bottom-y);
        }
        ATLASES.put(id,frames); return frames;
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
    public void poly(float[] xy,int c) {
        color(c); Path2D.Float path=new Path2D.Float(); path.moveTo(xy[0],xy[1]);
        for(int i=2;i<xy.length;i+=2) path.lineTo(xy[i],xy[i+1]); path.closePath(); g.fill(path);
    }
    public void line(float x1,float y1,float x2,float y2,float width,int c) {
        color(c); g.setStroke(new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Line2D.Float(x1,y1,x2,y2));
    }
}
