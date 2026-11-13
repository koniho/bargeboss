package com.bargeboss.game;

/** Independent painted scenery cells, registered to the shared ground plane. */
public final class SceneryView {
    private SceneryView() {}
    private static float overlap(float x,float y,float w,float h,float fx,float fy,float rx,float above,float below,float margin) {
        float dx=Math.max(x-fx-rx,fx-rx-x-w),dy=Math.max(y-fy-below,fy-above-y-h);
        float distance=Math.max(0,Math.max(dx,dy));
        float t=Physics.clamp(distance/margin,0,1); t=t*t*(3-2*t);
        return .28f+.72f*t;
    }
    private static float bodyAlpha(Game g,Body b,float x,float y,float w,float h) {
        RiverCamera c=g.layout.camera; float s=c.scale;
        return overlap(x,y,w,h,c.screenX(b.x,b.y),c.screenY(b.x,b.y),(b.half+b.radius)*s,65*s,22*s,24*s);
    }
    /** Spatial easing keeps fades smooth and rendering pure, including while paused. */
    public static float opacity(Game g,float x,float y,float w,float h) {
        float alpha=bodyAlpha(g,g.tug,x,y,w,h);
        if(g.trafficActive()) alpha=Math.min(alpha,bodyAlpha(g,g.trafficBoat,x,y,w,h));
        for(Body b:g.cargo) if(b!=null && b.active) alpha=Math.min(alpha,bodyAlpha(g,b,x,y,w,h));
        if(g.tutorial==null) for(Feeder f:g.feeders) if(f.phase!=Feeder.IDLE) {
            alpha=Math.min(alpha,bodyAlpha(g,f.boat,x,y,w,h));
            if(f.load!=null) alpha=Math.min(alpha,bodyAlpha(g,f.load,x,y,w,h));
        }
        Body rope=g.ropeDragging?g.heldCargo():g.ropeBody;
        if(rope!=null && (g.ropeDragging || g.ropePhase!=0)) {
            float ex=g.ropeDragging?g.aimX(rope,g.pointerX,g.pointerY):g.ropeEndX;
            float ey=g.ropeDragging?g.aimY(rope,g.pointerX,g.pointerY):g.ropeEndY;
            RiverCamera c=g.layout.camera;
            for(int i=0;i<=12;i++) {
                float t=i/12f,wx=rope.x+(ex-rope.x)*t,wy=rope.y+(ey-rope.y)*t-30*(float)StrictMath.sin(t*Math.PI);
                alpha=Math.min(alpha,overlap(x,y,w,h,c.screenX(wx,wy),c.screenY(wx,wy),9*c.scale,9*c.scale,9*c.scale,20*c.scale));
            }
        }
        return alpha;
    }
    public static void ground(Game g,Painter p) {
        RiverCamera c=g.layout.camera; float s=c.scale;
        p.rect(0,0,g.layout.width,g.layout.height,0xff76677a);
        float waterTop=c.screenY(Physics.RIGHT,492),waterBottom=c.screenY(Physics.LEFT,492);
        float tw=SpriteAtlas.artWidth(SpriteAtlas.WATER,0)*s,th=SpriteAtlas.artHeight(SpriteAtlas.WATER,0)*s;
        for(float y=waterTop;y<waterBottom;y+=th) for(float x=0;x<g.layout.width;x+=tw)
            p.sprite(SpriteAtlas.WATER,0,x+tw/2,y+th/2,tw,th,0,1);
        for(int side=0;side<2;side++) {
            int atlas=side==0?SpriteAtlas.NORTH_QUAY:SpriteAtlas.SOUTH_QUAY;
            float w=SpriteAtlas.artWidth(atlas,0)*s,h=SpriteAtlas.artHeight(atlas,0)*s;
            float edge=side==0?waterTop:waterBottom;
            for(float x=-w*.3f;x<g.layout.width;x+=w)
                p.sprite(atlas,0,x+w/2,edge+(side==0?-h/2:h/2),w+.2f,h,0,1);
        }
    }
    public static void props(Game g,Painter p,boolean foreground) {
        RiverCamera c=g.layout.camera; float s=c.scale;
        float origin=c.screenY(foreground?Physics.LEFT:Physics.RIGHT,492)-(foreground?680:223)*688f/1732*s;
        for(int f=foreground?5:0;f<(foreground?10:5);f++) {
            float x=c.left+SpriteAtlas.artX(SpriteAtlas.SCENERY,f)*s,y=origin+SpriteAtlas.artY(SpriteAtlas.SCENERY,f)*s;
            float w=SpriteAtlas.artWidth(SpriteAtlas.SCENERY,f)*s,h=SpriteAtlas.artHeight(SpriteAtlas.SCENERY,f)*s;
            p.sprite(SpriteAtlas.SCENERY,f,x,y,w,h,0,opacity(g,x-w/2,y-h/2,w,h));
        }
    }
    public static void bridge(Game g,Painter p) {
        RiverCamera c=g.layout.camera;float scale=c.scale;
        float cx=c.screenX(300,178),cy=c.screenY(300,178);
        for(int frame=0;frame<2;frame++) {
            float w=SpriteAtlas.artWidth(SpriteAtlas.BRIDGE,frame)*scale,h=SpriteAtlas.artHeight(SpriteAtlas.BRIDGE,frame)*scale;
            float y=cy+(frame==0?-70:70)*scale;
            float alpha=opacity(g,cx-w/2,y-h/2,w,h);
            p.sprite(SpriteAtlas.BRIDGE,frame,cx,y,w,h,0,alpha);
        }
    }
    public static void piers(Game g,Painter p) {
        RiverCamera c=g.layout.camera; float s=c.scale;
        for(int f=0;f<2;f++) {
            float origin=c.screenY(f==0?Physics.RIGHT:Physics.LEFT,492)-(f==0?223:680)*688f/1732*s;
            float x=c.left+(SpriteAtlas.artX(SpriteAtlas.PIERS,f)+HarborGeometry.PIER_SHIFT[f])*s,y=origin+SpriteAtlas.artY(SpriteAtlas.PIERS,f)*s;
            float w=SpriteAtlas.artWidth(SpriteAtlas.PIERS,f)*s,h=SpriteAtlas.artHeight(SpriteAtlas.PIERS,f)*s;
            float alpha=opacity(g,x-w/2,y-h/2,w,h);
            p.sprite(SpriteAtlas.PIERS,f,x,y,w,h,0,alpha);
            if(alpha<.95f) {
                float[] r=HarborGeometry.PIERS[f];
                float left=c.screenX(r[0],r[1]),right=c.screenX(r[0],r[3]);
                float top=c.screenY(r[2],r[1]),bottom=c.screenY(r[0],r[1]);
                p.line(left,top,right,top,1.5f,0x99ffce65);p.line(left,bottom,right,bottom,1.5f,0x99ffce65);
                p.line(left,top,left,bottom,1.5f,0x99ffce65);p.line(right,top,right,bottom,1.5f,0x99ffce65);
            }
        }
    }
}
