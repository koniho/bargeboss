package com.bargeboss.game;

/** One registered load, with a shared geometric foreground coaming cutoff. */
public final class CargoView {
    private static final ThreadLocal<float[]> CLIP=new ThreadLocal<float[]>(){
        protected float[] initialValue(){return new float[24];}
    };
    private CargoView(){}
    private static void edge(float[] q,int at,RiverCamera camera,float x,float y,float c,float s,float ax,float ay,float bx,float by){
        float x1=camera.screenX(x+c*ax-s*ay,y+s*ax+c*ay),y1=camera.screenY(x+c*ax-s*ay,y+s*ax+c*ay)-CargoArt.RIM*.81649658f*camera.scale;
        float x2=camera.screenX(x+c*bx-s*by,y+s*bx+c*by),y2=camera.screenY(x+c*bx-s*by,y+s*bx+c*by)-CargoArt.RIM*.81649658f*camera.scale;
        float low=Math.max(y1,y2)+200*camera.scale;
        q[at]=x1;q[at+1]=y1;q[at+2]=x2;q[at+3]=y2;q[at+4]=x2;q[at+5]=low;
        q[at+6]=x1;q[at+7]=y1;q[at+8]=x2;q[at+9]=low;q[at+10]=x1;q[at+11]=low;
    }
    public static void draw(Game g,Painter p,Body b,float x,float y,float size){
        float fill=CargoArt.quantity(b);if(fill<=0)return;
        int atlas=b.berth==0?SpriteAtlas.STEEL:b.berth==2?SpriteAtlas.BULK:SpriteAtlas.CONTAINER;
        RiverPainter river=p instanceof RiverPainter?(RiverPainter)p:null;
        if(river!=null){
            float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle),half=CargoArt.HOLD_LENGTH*.5f,beam=CargoArt.HOLD_WIDTH*.5f;
            float[] clip=CLIP.get();
            float nearX=s-c>=0?half:-half,nearY=c+s>=0?beam:-beam;
            edge(clip,0,river.camera,x,y,c,s,nearX,-beam,nearX,beam);
            edge(clip,12,river.camera,x,y,c,s,-half,nearY,half,nearY);
            river.canvas().pushClipOutside(clip);
        }
        try{
            SpriteAtlas.draw(p,atlas,b.angle,x,y,size,size,fill);
            SurfaceLighting.directional(g,p,atlas,b.angle,x,y,size,fill,b);
        }finally{if(river!=null)river.canvas().popClip();}
    }
}
