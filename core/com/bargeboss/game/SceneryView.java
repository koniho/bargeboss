package com.bargeboss.game;

/** Independent painted scenery cells, registered to the shared ground plane. */
public final class SceneryView {
    private static final float[] PIER_BODY_WIDTH={321/362f,247/362f};
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
        RiverCamera c=g.layout.camera;float s=c.scale;
        float top=c.screenY(Physics.RIGHT,492),bottom=c.screenY(Physics.LEFT,492);
        p.rect(0,0,g.layout.width,g.layout.height,0xff67788c);
        float tile=300*s,drift=(g.visualTicks*.025f*s)%tile;
        float swell=(float)StrictMath.sin(g.visualTicks*.018)*1.2f*s;
        for(float y=top-tile+swell;y<bottom;y+=tile) for(float x=drift-tile;x<g.layout.width;x+=tile)
            p.sprite(SpriteAtlas.WATER,0,x+tile/2,y+tile/2,tile,tile,0,1);
        // Flat ground follows the same orthographic plane as physics. Broad planes,
        // one lit rim and one shadow face replace the noisy painted pavement.
        p.rect(0,0,g.layout.width,top,0xff7c8a99);
        p.rect(0,bottom,g.layout.width,g.layout.height-bottom,0xff7c8a99);
        p.rect(0,top-13*s,g.layout.width,13*s,0xff25394a);
        p.rect(0,top-15*s,g.layout.width,4*s,0xffc3b89b);
        p.rect(0,bottom,g.layout.width,7*s,0xffc3b89b);
        p.rect(0,bottom+7*s,g.layout.width,5*s,0xff46586c);
        for(float x=c.left-20*s;x<g.layout.width;x+=96*s) {
            p.line(x,top-15*s,x-18*s,top-48*s,1.2f*s,0x5546576b);
            p.line(x,bottom+12*s,x-18*s,bottom+52*s,1.2f*s,0x5546576b);
            for(int i=0;i<3;i++) {
                float xx=x+i*8*s;
                p.poly(new float[]{xx,top-15*s,xx+4*s,top-15*s,xx+8*s,top-11*s,xx+4*s,top-11*s},0xffe4b859);
                p.poly(new float[]{xx,bottom,xx+4*s,bottom,xx+8*s,bottom+7*s,xx+4*s,bottom+7*s},0xff233647);
            }
        }
        LightingView.quays(g,p);
    }
    public static void props(Game g,Painter p,boolean foreground) {
        RiverCamera c=g.layout.camera;float s=c.scale;
        int[] cells=foreground?new int[]{5,3,7,6}:new int[]{0,4,2,1};
        float[] along={70,250,465,625};
        float[] sizes=foreground?new float[]{82,80,76,94}:new float[]{100,84,91,100};
        float edge=c.screenY(foreground?Physics.LEFT:Physics.RIGHT,492);
        for(int i=0;i<cells.length;i++) {
            float size=sizes[i]*s,x=c.left+along[i]*s;
            float foot=edge+(foreground?83:-10)*s;
            float y=foot-size*.47f;
            float alpha=opacity(g,x-size/2,y-size/2,size,size);
            LightingView.shadow(p,x,foot,size*.83f,size*.14f,alpha);
            p.sprite(SpriteAtlas.SCENERY,cells[i],x,y,size,size,0,alpha);
        }
    }
    public static void bridge(Game g,Painter p) {
        RiverCamera c=g.layout.camera;float s=c.scale;
        float x=c.screenX(300,178),middle=c.screenY(300,178);
        for(int frame=0;frame<2;frame++) {
            float size=108*s,y=middle+(frame==0?-86:86)*s;
            float alpha=opacity(g,x-size/2,y-size/2,size,size);
            LightingView.shadow(p,x,y+size*.36f,size*.8f,size*.13f,alpha);
            p.sprite(SpriteAtlas.BRIDGE,frame,x,y,size,size,0,alpha);
        }
    }
    public static void piers(Game g,Painter p) {
        RiverCamera c=g.layout.camera;float s=c.scale;
        for(int f=0;f<2;f++) {
            float[] r=HarborGeometry.PIERS[f];
            float left=c.screenX(r[0],r[1]),right=c.screenX(r[0],r[3]);
            float top=c.screenY(r[2],r[1]),bottom=c.screenY(r[0],r[1]);
            // Registered opaque bounds of the two cells, preserving uniform sprite scale.
            float size=(right-left)/PIER_BODY_WIDTH[f];
            float x=(left+right)/2-(182.5f/362-.5f)*size,y=bottom-(351/362f-.5f)*size;
            float alpha=opacity(g,x-size/2,y-size/2,size,size);
            LightingView.shadow(p,x,bottom,(right-left),8*s,alpha);
            p.sprite(SpriteAtlas.PIERS,f,x,y,size,size,0,alpha);
            // The visible edge remains registered to the solid footprint beneath the prop.
            if(alpha<.95f) {
                p.line(left,top,right,top,1.5f,0x99ffce65);p.line(left,bottom,right,bottom,1.5f,0x99ffce65);
                p.line(left,top,left,bottom,1.5f,0x99ffce65);p.line(right,top,right,bottom,1.5f,0x99ffce65);
            }
        }
    }
}
