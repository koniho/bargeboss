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
        for(Body b:g.cargo) if(b!=null && b.active && (g.canTie(b) || b.lineAttached)) {
            RiverCamera c=g.layout.camera;float s=c.scale;
            alpha=Math.min(alpha,overlap(x,y,w,h,c.screenX(Game.TIE_X[b.berth],g.dockY(b.berth)),
                    c.screenY(Game.TIE_X[b.berth],g.dockY(b.berth)),24*s,24*s,24*s,20*s));
        }
        Body rope=g.ropeDragging || g.towAiming?g.heldCargo():g.ropeBody;
        if(rope!=null && (g.ropeDragging || g.towAiming || g.ropePhase!=0)) {
            float ex=g.towAiming?g.pointerX:g.ropeDragging?g.aimX(rope,g.pointerX,g.pointerY):g.ropeEndX;
            float ey=g.towAiming?g.pointerY:g.ropeDragging?g.aimY(rope,g.pointerX,g.pointerY):g.ropeEndY;
            RiverCamera c=g.layout.camera;
            for(int i=0;i<=12;i++) {
                float t=i/12f,height=30*(float)StrictMath.sin(t*Math.PI);
                float wx=rope.x+(ex-rope.x)*t+RiverCamera.riseX(height);
                float wy=rope.y+(ey-rope.y)*t+RiverCamera.riseY(height);
                alpha=Math.min(alpha,overlap(x,y,w,h,c.screenX(wx,wy),c.screenY(wx,wy),9*c.scale,9*c.scale,9*c.scale,20*c.scale));
            }
        }
        if(g.tow.cargo!=null) {
            RiverCamera c=g.layout.camera;
            for(int i=0;i<=12;i++) {
                float t=i/12f,wx=g.tow.cargoX()+(g.tow.tugX(g.tug)-g.tow.cargoX())*t;
                float wy=g.tow.cargoY()+(g.tow.tugY(g.tug)-g.tow.cargoY())*t;
                alpha=Math.min(alpha,overlap(x,y,w,h,c.screenX(wx,wy),c.screenY(wx,wy),9*c.scale,9*c.scale,9*c.scale,20*c.scale));
            }
        }
        return alpha;
    }
    public static void ground(Game g,Painter p) {
        RiverCamera c=g.layout.camera;float s=c.scale;
        WaveView.draw(p,g.visualTicks,0,0,g.layout.width,g.layout.height,s,c.screenX(300,148));
        Painter world=new RiverPainter(p,c);
        // Clip the visible world with the real banks. Both coordinates of every
        // corner pass through the same camera as the collision footprints.
        float minX=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,minY=Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        for(int corner=0;corner<4;corner++) {
            float sx=corner%2*g.layout.width,sy=corner/2*g.layout.height;
            float x=c.worldX(sx,sy),y=c.worldY(sx,sy);
            minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
        }
        minX-=80;maxX+=80;minY-=80;maxY+=80;
        if(minX<Physics.LEFT) world.rect(minX,minY,Physics.LEFT-minX,maxY-minY,0xff67788c);
        if(maxX>Physics.RIGHT) world.rect(Physics.RIGHT,minY,maxX-Physics.RIGHT,maxY-minY,0xff7c8a99);
        for(int bank=0;bank<2;bank++) {
            float edge=bank==0?Physics.RIGHT:Physics.LEFT,side=bank==0?1:-1;
            world.line(edge,minY,edge,maxY,6,VisualStyle.OUTLINE);
            world.line(edge+side*3,minY,edge+side*3,maxY,2,0xffc3b89b);
            for(int row=(int)StrictMath.floor(minY/64);row*64<maxY;row++) {
                float y=row*64;
                world.line(edge+side*5,y,edge+side*65,y,1,0x4446576b);
                for(int mark=0;mark<3;mark++) {
                    float yy=y+mark*7;
                    world.line(edge+side*5,yy,edge+side*9,yy+3,2,bank==0?0xffe4b859:0xff233647);
                }
            }
        }
        LightingView.quays(g,p);
    }
    public static void props(Game g,Painter p,boolean foreground) {
        RiverCamera c=g.layout.camera;float s=c.scale;
        int[] cells=foreground?new int[]{5,3,7,6}:new int[]{0,4,2,1};
        float[] along={70,250,465,625};
        float[] sizes=foreground?new float[]{82,80,76,94}:new float[]{100,84,91,100};
        float bank=foreground?Physics.LEFT-82:Physics.RIGHT+48;
        for(int segment=g.bridgeStage()?-2:0;segment<=0;segment++) for(int i=0;i<cells.length;i++) {
            float worldY=148+along[i]+segment*688;
            if(g.bridgeStage() && Math.abs(worldY-Bridge.Y)<95) continue;
            boolean warehouse=cells[i]==0 || cells[i]==1;
            float size=(warehouse?144:sizes[i])*s,x=c.screenX(bank,worldY),foot=c.screenY(bank,worldY);
            float y=warehouse?foot:foot-size*.47f;
            float px=warehouse?StaticHarborArt.pivotX(StaticHarborArt.WAREHOUSE):.5f;
            float py=warehouse?StaticHarborArt.pivotY(StaticHarborArt.WAREHOUSE):.5f;
            float left=x-px*size,top=y-py*size;
            if(left+size<0 || left>g.layout.width || top+size<0 || top>g.layout.height) continue;
            float alpha=opacity(g,left,top,size,size);
            LightingView.shadow(p,x,foot,size*.83f,size*.14f,alpha);
            p.sprite(warehouse?SpriteAtlas.STATIC_HARBOR:SpriteAtlas.SCENERY,
                    warehouse?StaticHarborArt.WAREHOUSE:cells[i],x,y,size,size,0,alpha);
            if(warehouse) StaticHarborArt.lights(p,x,y,size,alpha,g.visualTicks);
        }
    }
    public static void bridge(Game g,Painter p) {
        if(g.challengeId==0) return;
        if(g.bridgeStage()) { BridgeView.draw(g,p);return; }
        RiverCamera c=g.layout.camera;float s=c.scale;
        for(int frame=0;frame<2;frame++) {
            float bank=frame==0?Physics.RIGHT+35:Physics.LEFT-35;
            float x=c.screenX(bank,178),foot=c.screenY(bank,178),size=108*s,y=foot-size*.4f;
            float alpha=opacity(g,x-size/2,y-size/2,size,size);
            LightingView.shadow(p,x,foot,size*.8f,size*.13f,alpha);
            p.sprite(SpriteAtlas.BRIDGE,frame,x,y,size,size,0,alpha);
        }
    }
    public static void piers(Game g,Painter p) {
        if(g.challengeId!=2) return;
        RiverCamera c=g.layout.camera;Painter world=new RiverPainter(p,c);
        for(float[] r:HarborGeometry.PIERS) {
            // Project the exact solid rectangle; the old painted pier crops had
            // baked-in axes and could no longer mark the playable edge reliably.
            world.rect(r[0],r[1],r[2]-r[0],r[3]-r[1],VisualStyle.OUTLINE);
            world.rect(r[0]+2,r[1]+2,r[2]-r[0]-4,r[3]-r[1]-4,0xff7c8a99);
            world.line(r[0],r[1],r[2],r[1],2,0xffc3b89b);
            world.line(r[2],r[1],r[2],r[3],2,0xffc3b89b);
            for(float y=r[1]+9;y<r[3]-4;y+=14) {
                world.line(r[0]+3,y,r[0]+6,y+5,3,VisualStyle.GOLD);
                world.line(r[2]-6,y,r[2]-3,y+5,3,VisualStyle.OUTLINE);
            }
        }
    }
}
