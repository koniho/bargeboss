package com.bargeboss.game;

/** Body-local deck rig for tug, cargo barge and yacht. Visual fittings do not alter tow torque. */
public final class RopeAttachment {
    private RopeAttachment(){}
    public static float along(Body b,float end) {
        float reach=b.yacht?b.half+b.radius*.70f:b.berth<0?b.half+b.radius*.65f:CargoArt.HOLD_LENGTH*.5f+3;
        return end<0?-reach:reach;
    }
    public static float height(Body b){return b.cruiser?4f:b.yacht?8.0f:b.berth<0?8.5f:CargoArt.RIM*.81649658f+1;}
    public static float x(Body b,float end){return b.x+along(b,end)*(float)StrictMath.cos(b.angle);}
    public static float y(Body b,float end){return b.y+along(b,end)*(float)StrictMath.sin(b.angle);}
    /** Pick a stable end/side fitting facing a shore bollard, rather than a moving hull-ray intersection. */
    public static void mooring(Body b,float tx,float ty,float[] out){
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        boolean caught=b.lineAttached && (b.mooringX!=0 || b.mooringY!=0);
        float angle=caught?b.mooringAngle:b.angle,cc=(float)StrictMath.cos(angle),ss=(float)StrictMath.sin(angle);
        float dx=tx-(caught?b.mooringX:b.x),dy=ty-(caught?b.mooringY:b.y);
        float end=dx*cc+dy*ss>=0?1:-1,side=-dx*ss+dy*cc>=0?1:-1;
        float along=along(b,end)*.95f,across=b.radius*.80f*side;
        out[0]=b.x+along*c-across*s;out[1]=b.y+along*s+across*c;
    }
    public static void cast(Body b,float tx,float ty,boolean tow,float[] out) {
        if(!tow){mooring(b,tx,ty,out);return;}
        float end=(tx-b.x)*(float)StrictMath.cos(b.angle)+(ty-b.y)*(float)StrictMath.sin(b.angle);
        out[0]=x(b,end);out[1]=y(b,end);
    }
    public static void fitting(Game g,Painter p,Body b,float end) {
        paintFitting(g,p,b,x(b,end),y(b,end));
    }
    private static void paintFitting(Game g,Painter p,Body b,float x,float y){
        float h=height(b)+WaterSurface.lift(g,b),scale=1;
        if(p instanceof RiverPainter){RiverPainter r=(RiverPainter)p;float sx=r.camera.screenX(x,y);y=r.camera.screenY(x,y)-h*r.camera.scale;x=sx;scale=r.camera.scale;p=r.canvas();}
        else y-=h;
        // A small iron crossbar and warm binding read as part of the painted deck.
        p.line(x-2.4f*scale,y,x+2.4f*scale,y,2.4f*scale,0xff313a3c);
        p.line(x-2.1f*scale,y-.7f*scale,x+2.1f*scale,y-.7f*scale,.7f*scale,0xff9faaa2);
        p.ropeSegment(x-1.1f*scale,y,x+1.1f*scale,y,1.8f*scale,0,.5f,RopeMaterial.COLOR);
    }
    public static void fittings(Game g,Painter p,Body b){
        boolean bow=false,stern=false;
        for(int i=0;i<Towline.CAPACITY;i++){Towline l=g.tow.link(i);if(l==null || l.cargo==null)continue;
            float end=l.cargo==b?l.cargoEnd:l.leader==b?l.leaderEnd:0;
            if(end>0)bow=true;else if(end<0)stern=true;
        }
        if(bow)fitting(g,p,b,1);if(stern)fitting(g,p,b,-1);
        if(b.lineAttached && !b.yacht && (b.berth>=0 || b==g.tug)){float[] at=new float[2];mooring(b,g.bollardX(b),g.bollardY(b),at);paintFitting(g,p,b,at[0],at[1]);}
    }
}
