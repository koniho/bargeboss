package com.bargeboss.game;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
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
    private final Graphics2D g;
    public DesktopPainter(Graphics2D g) { this.g=g; }
    private static synchronized BufferedImage[] atlas(int id) {
        BufferedImage[] frames=ATLASES.get(id); if(frames!=null) return frames;
        String path="assets/sprites/"+SpriteAtlas.file(id);
        try {
            BufferedImage image;
            try(InputStream input=DesktopPainter.class.getResourceAsStream("/"+path)) {
                image=input==null?ImageIO.read(new File(path)):ImageIO.read(input);
            }
            if(image==null) throw new IllegalStateException("Unreadable sprite atlas: "+path);
            frames=new BufferedImage[8];
            for(int f=0;f<frames.length;f++) {
                int col=f%4,row=f/4;
                int x=Math.round(image.getWidth()*col/4f),y=Math.round(image.getHeight()*row/2f);
                int right=Math.round(image.getWidth()*(col+1)/4f),bottom=Math.round(image.getHeight()*(row+1)/2f);
                frames[f]=image.getSubimage(x,y,right-x,bottom-y);
            }
            ATLASES.put(id,frames); return frames;
        } catch(IOException e) { throw new IllegalStateException("Missing sprite atlas: "+path,e); }
    }
    public void sprite(int id,int frame,float cx,float cy,float width,float height,float rotation,float alpha) {
        if(frame<0 || frame>=8) throw new IllegalArgumentException("Invalid sprite frame: "+frame);
        BufferedImage image=atlas(id)[frame]; Graphics2D sprite=(Graphics2D)g.create();
        sprite.translate(cx,cy); sprite.rotate(rotation);
        sprite.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,Math.max(0,Math.min(1,alpha))));
        sprite.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        AffineTransform transform=new AffineTransform();
        transform.translate(-SpriteAtlas.pivotX(id,frame)*width,-SpriteAtlas.pivotY(id,frame)*height);
        transform.scale(width/image.getWidth(),height/image.getHeight());
        sprite.drawImage(image,transform,null); sprite.dispose();
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
