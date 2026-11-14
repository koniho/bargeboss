package com.bargeboss.game;

import java.util.ArrayList;

/** Per-render foreground occlusion. Only already painted vessel pixels can trigger fading.
 * Masks are conservative opaque interiors, shared by every native host. No proximity radius,
 * smoke, wake, rope, objective or persistent game state participates. */
public final class SceneryOcclusion implements Painter {
    private final Painter target;
    private final ArrayList<Sprite> vessels=new ArrayList<Sprite>();
    public SceneryOcclusion(Painter target) {this.target=target;}
    private interface Shape {boolean contains(float x,float y);float left();float top();float right();float bottom();}
    private static final class Sprite implements Shape {
        final long[] rows;final float x,y,w,h,px,py,c,s,l,t,r,b;
        Sprite(int atlas,int frame,float x,float y,float w,float h,float rotation) {
            this.rows=OcclusionMasks.rows(atlas)[frame];this.x=x;this.y=y;
            this.w=w*SpriteAtlas.widthScale(atlas,frame);this.h=h*SpriteAtlas.heightScale(atlas,frame);
            px=SpriteAtlas.pivotX(atlas,frame);py=SpriteAtlas.pivotY(atlas,frame);
            c=(float)StrictMath.cos(rotation);s=(float)StrictMath.sin(rotation);
            float left=Float.MAX_VALUE,top=left,right=-left,bottom=-left;
            for(int i=0;i<4;i++){float dx=(i%2-px)*this.w,dy=(i/2-py)*this.h;
                float xx=x+c*dx-s*dy,yy=y+s*dx+c*dy;
                left=Math.min(left,xx);right=Math.max(right,xx);top=Math.min(top,yy);bottom=Math.max(bottom,yy);}
            l=left;t=top;r=right;b=bottom;
        }
        public float left(){return l;}public float top(){return t;}public float right(){return r;}public float bottom(){return b;}
        public boolean contains(float sx,float sy) {
            float dx=sx-x,dy=sy-y,u=(c*dx+s*dy)/w+px,v=(-s*dx+c*dy)/h+py;
            if(u<0 || u>=1 || v<0 || v>=1)return false;
            return (rows[(int)(v*64)]&(1L<<(int)(u*64)))!=0;
        }
        int overlap(Shape shape) {
            if(shape.right()<=l || shape.left()>=r || shape.bottom()<=t || shape.top()>=b)return 0;
            int firstX=0,lastX=63,firstY=0,lastY=63;
            if(s==0 && c==1) {
                firstX=Math.max(0,(int)StrictMath.floor((shape.left()-l)/w*64));lastX=Math.min(63,(int)StrictMath.floor((shape.right()-l)/w*64));
                firstY=Math.max(0,(int)StrictMath.floor((shape.top()-t)/h*64));lastY=Math.min(63,(int)StrictMath.floor((shape.bottom()-t)/h*64));
            }
            int hits=0;
            for(int yy=firstY;yy<=lastY;yy++) {
                long bits=rows[yy];
                for(int xx=firstX;xx<=lastX;xx++)if((bits&(1L<<xx))!=0) {
                    float dx=((xx+.5f)/64-px)*w,dy=((yy+.5f)/64-py)*h;
                    if(shape.contains(x+c*dx-s*dy,y+s*dx+c*dy) && ++hits==4)return hits;
                }
            }
            return hits;
        }
    }
    private static final class Polygon implements Shape {
        final float[] xy;final float l,t,r,b;
        Polygon(float[] xy) {this.xy=xy;float l=Float.MAX_VALUE,t=l,r=-l,b=-l;
            for(int i=0;i<xy.length;i+=2){l=Math.min(l,xy[i]);r=Math.max(r,xy[i]);t=Math.min(t,xy[i+1]);b=Math.max(b,xy[i+1]);}
            this.l=l;this.t=t;this.r=r;this.b=b;}
        public float left(){return l;}public float top(){return t;}public float right(){return r;}public float bottom(){return b;}
        public boolean contains(float x,float y) {
            boolean inside=false;
            for(int i=0,j=xy.length-2;i<xy.length;j=i,i+=2) {
                float ax=xy[i],ay=xy[i+1],bx=xy[j],by=xy[j+1];
                if((ay>y)!=(by>y) && x<(bx-ax)*(y-ay)/(by-ay)+ax)inside=!inside;
            }
            return inside;
        }
    }
    private static final class Stroke implements Shape {
        final float x,y,xx,yy,r;
        Stroke(float x,float y,float xx,float yy,float width){this.x=x;this.y=y;this.xx=xx;this.yy=yy;r=width*.5f;}
        public float left(){return Math.min(x,xx)-r;}public float top(){return Math.min(y,yy)-r;}
        public float right(){return Math.max(x,xx)+r;}public float bottom(){return Math.max(y,yy)+r;}
        public boolean contains(float sx,float sy){float dx=xx-x,dy=yy-y,d=dx*dx+dy*dy;
            float t=d==0?0:Physics.clamp(((sx-x)*dx+(sy-y)*dy)/d,0,1);dx=sx-x-t*dx;dy=sy-y-t*dy;return dx*dx+dy*dy<r*r;}
    }
    private static SceneryOcclusion context(Painter p) {
        if(p instanceof RiverPainter)p=((RiverPainter)p).canvas();
        return p instanceof SceneryOcclusion?(SceneryOcclusion)p:null;
    }
    private float opacity(Shape shape) {
        int hits=0;
        for(Sprite vessel:vessels){hits+=vessel.overlap(shape);if(hits>=4)return .28f;}
        // Easing starts inside actual opaque overlap, never outside the silhouette.
        float t=hits/4f;t=t*t*(3-2*t);return 1-.72f*t;
    }
    public static float spriteOpacity(Painter p,int atlas,int frame,float x,float y,float w,float h) {
        SceneryOcclusion scene=context(p);return scene==null || scene.vessels.isEmpty()?1:scene.opacity(new Sprite(atlas,frame,x,y,w,h,0));
    }
    public static float polygonOpacity(Painter p,float[] xy) {
        SceneryOcclusion scene=context(p);return scene==null || scene.vessels.isEmpty()?1:scene.opacity(new Polygon(xy));
    }
    /** Code-drawn foreground pieces test their actual polygons/strokes, not a group rectangle. */
    public static Painter obstacles(Painter p) {SceneryOcclusion scene=context(p);return scene==null || scene.vessels.isEmpty()?p:new Obstacles(p,scene);}
    private static final class Obstacles implements Painter {
        final Painter p;final SceneryOcclusion scene;Obstacles(Painter p,SceneryOcclusion scene){this.p=p;this.scene=scene;}
        private int color(int color,Shape shape){if((color>>>24)<160)return color;return (((int)((color>>>24)*scene.opacity(shape)))<<24)|(color&0xffffff);}
        public void rect(float x,float y,float w,float h,int c){p.rect(x,y,w,h,color(c,new Polygon(new float[]{x,y,x+w,y,x+w,y+h,x,y+h})));}
        public void poly(float[] xy,int c){p.poly(xy,color(c,new Polygon(xy)));}
        public void line(float x,float y,float xx,float yy,float w,int c){p.line(x,y,xx,yy,w,color(c,new Stroke(x,y,xx,yy,w)));}
        public void circle(float x,float y,float r,int c){p.circle(x,y,r,color(c,new Stroke(x,y,x,y,r*2)));}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){p.sprite(a,f,x,y,w,h,r,alpha);}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){p.spriteBlend(a,f,ff,x,y,w,h,r,mix,alpha);}
    }
    private void remember(int atlas,int frame,float x,float y,float w,float h,float r,float alpha) {
        if(atlas>=SpriteAtlas.TUG && atlas<=SpriteAtlas.BULK && alpha>=.15f && w>0 && h>0)
            vessels.add(new Sprite(atlas,frame,x,y,w,h,r));
    }
    public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){remember(a,f,x,y,w,h,r,alpha);target.sprite(a,f,x,y,w,h,r,alpha);}
    public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){
        remember(a,f,x,y,w,h,r,(1-mix)*alpha);remember(a,ff,x,y,w,h,r,mix*alpha);
        target.spriteBlend(a,f,ff,x,y,w,h,r,mix,alpha);
    }
    public void rect(float x,float y,float w,float h,int c){target.rect(x,y,w,h,c);}
    public void circle(float x,float y,float r,int c){target.circle(x,y,r,c);}
    public void poly(float[] xy,int c){target.poly(xy,c);}
    public void line(float x,float y,float xx,float yy,float w,int c){target.line(x,y,xx,yy,w,c);}
}
