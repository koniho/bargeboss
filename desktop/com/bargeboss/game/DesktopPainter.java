package com.bargeboss.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

public final class DesktopPainter implements Painter {
    private final Graphics2D g;
    public DesktopPainter(Graphics2D g) { this.g=g; }
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
