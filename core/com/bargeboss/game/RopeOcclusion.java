package com.bargeboss.game;

/** Conservative body-local solids for ropes crossing painted hulls and superstructures. */
public final class RopeOcclusion {
    private RopeOcclusion(){}
    public static boolean hidden(Game g,float x,float y,float z){
        if(blocks(g,g.tug,x,y,z))return true;
        for(Body b:g.cargo)if(b!=null && b.active && blocks(g,b,x,y,z))return true;
        if(g.tutorial==null)for(Feeder f:g.feeders)if(f.phase!=Feeder.IDLE) {
            if(blocks(g,f.boat,x,y,z) || f.load!=null && blocks(g,f.load,x,y,z))return true;
        }
        return g.trafficActive() && blocks(g,g.trafficBoat,x,y,z);
    }
    static boolean blocks(Game g,Body b,float x,float y,float z){
        if(b==null || !b.active)return false;
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        float xx=(x-b.x)*c+(y-b.y)*s,yy=-(x-b.x)*s+(y-b.y)*c;
        z-=WaterSurface.lift(g,b);
        // Orthographic sight ray toward the elevated camera, expressed in hull-local XYZ.
        float dx=-c+s,dy=s+c,dz=2*RiverCamera.Q*RiverCamera.DEPTH;
        float deck=RopeAttachment.height(b);
        if(box(xx,yy,z,dx,dy,dz,-(b.half+b.radius*.65f),b.half+b.radius*.65f,-b.radius*.72f,b.radius*.72f,0,deck-.4f))return true;
        if(b.berth<0 || b.yacht) {
            float length=b.yacht?b.half*.55f:b.half*.72f;
            return box(xx,yy,z,dx,dy,dz,-length,length,-b.radius*.53f,b.radius*.53f,deck,b.yacht?29:30);
        }
        if(b.deliveryRecorded || b.unloadingTicks>=Game.UNLOAD_TICKS)return false;
        // Cargo occupies the middle deck; end cleats stay outside this occluder.
        return box(xx,yy,z,dx,dy,dz,-CargoArt.HOLD_LENGTH*.5f,CargoArt.HOLD_LENGTH*.5f,-CargoArt.HOLD_WIDTH*.5f,CargoArt.HOLD_WIDTH*.5f,deck,b.berth==2?15:24);
    }
    private static boolean box(float x,float y,float z,float dx,float dy,float dz,float minX,float maxX,float minY,float maxY,float minZ,float maxZ){
        float near=.02f,far=Float.MAX_VALUE;
        if(Math.abs(dx)<.00001f){if(x<minX || x>maxX)return false;}
        else {float a=(minX-x)/dx,b=(maxX-x)/dx;near=Math.max(near,Math.min(a,b));far=Math.min(far,Math.max(a,b));}
        if(Math.abs(dy)<.00001f){if(y<minY || y>maxY)return false;}
        else {float a=(minY-y)/dy,b=(maxY-y)/dy;near=Math.max(near,Math.min(a,b));far=Math.min(far,Math.max(a,b));}
        float a=(minZ-z)/dz,b=(maxZ-z)/dz;near=Math.max(near,a);far=Math.min(far,b);
        return near<far;
    }
}
