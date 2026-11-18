package com.bargeboss.game;
/** Colored, local shore receivers. No NPC mooring lights; never paints across water or hulls. */
public final class VesselShoreLight {
    private VesselShoreLight(){}
    static float falloff(float distanceSquared,float radius){float v=Math.max(0,1-distanceSquared/(radius*radius));return v*v;}
    public static void draw(Game g,Painter p){
        if(g.montroseStage()){draw(g,p,true);return;}
        walls(g,p);draw(g,p,false);
    }
    static void walls(Game g,Painter p){
        p.pushClipOutside(ShoreTopClip.get(g));
        try {draw(g,p,true);} finally {p.popClip();}
    }
    private static void draw(Game g,Painter p,boolean walls){
        vessel(g,p,g.tug,walls);
        for(Body b:g.cargo)if(b!=null && b.active)vessel(g,p,b,walls);
        for(Feeder f:g.feeders)if(f.phase!=Feeder.IDLE)vessel(g,p,f.boat,walls);
        if(g.trafficActive())vessel(g,p,g.trafficBoat,walls);
    }
    private static void vessel(Game g,Painter p,Body b,boolean walls){if(!b.active)return;
        // Warm working lamps, separately colored from red/green/white navigation lamps.
        if(b.berth<0)lamp(g,p,b.x,b.y,22,85,0xffffbc6b,.30f*SceneryEffects.workIntensity(g.visualTicks,b.id*.71f),b.angle,-1,walls);
        else if(!b.yacht)for(int end=0;end<2;end++){float u=VesselLighting.bargeLocalX(b,end);lamp(g,p,b.x+u*(float)StrictMath.cos(b.angle),b.y+u*(float)StrictMath.sin(b.angle),5,42,0xffffbc6b,.16f*SceneryEffects.workIntensity(g.visualTicks,b.id*.71f),b.angle,-1,walls);}
        for(int fixture=0;fixture<VesselNavigationLights.sourceCount(b);fixture++){
            if(!VesselNavigationLights.emitting(g,b,fixture))continue;
            lamp(g,p,VesselNavigationLights.sourceX(g,b,fixture),VesselNavigationLights.sourceY(g,b,fixture),VesselNavigationLights.height(b,fixture),fixture<2?48:60,VesselNavigationLights.color(fixture),fixture<2?.24f:.16f,VesselNavigationLights.sourceHeading(g,b),fixture,walls);
        }
    }
    /** Illumination uses direction to the receiver, never lamp visibility from the camera. */
    static float sector(float angle,int fixture,float dx,float dy){
        if(fixture<0 || fixture==VesselNavigationLights.ANCHOR)return 1;
        float bearing=Physics.angle((float)StrictMath.atan2(dy,dx)-angle),edge;
        if(fixture==VesselNavigationLights.PORT)edge=Math.min(bearing+(float)Math.PI*.625f,-bearing);
        else if(fixture==VesselNavigationLights.STARBOARD)edge=Math.min((float)Math.PI*.625f-bearing,bearing);
        else if(fixture==VesselNavigationLights.STERN)edge=Math.abs(bearing)-(float)Math.PI*.625f;
        else edge=(float)Math.PI*.625f-Math.abs(bearing);
        return Physics.clamp(1+edge/.15f,0,1);
    }
    private static void lamp(Game g,Painter p,float lx,float ly,float z,float radius,int color,float strength,float angle,int fixture,boolean walls){RiverCamera c=g.layout.camera;
        float sx=c.screenX(lx,ly),sy=c.screenY(lx,ly),pad=(radius+32)*c.scale;
        if(sx+pad<0||sx-pad>g.layout.width||sy+pad<0||sy-pad>g.layout.height)return;
        if(g.montroseStage())DockTopLight.draw(g,p,lx,ly,z,radius,color,strength,angle,fixture);
        float[] q=new float[8];
        for(ShorelineView.Segment s:ShorelineView.near(g,lx-radius-24,ly-radius-24,lx+radius+24,ly+radius+24)){
            if(!g.montroseStage() && (walls?!s.wallVisible:z<=s.height))continue;
            float mx=(s.ax+s.bx)*.5f,my=(s.ay+s.by)*.5f,dx=lx-mx,dy=ly-my;
            if(Math.abs(dx)>radius+24 || Math.abs(dy)>radius+24 || dx*s.nx+dy*s.ny<=0)continue;
            float len=Physics.length(s.bx-s.ax,s.by-s.ay);int pieces=Math.max(1,(int)StrictMath.ceil(len/8));
            for(int i=0;i<pieces;i++){float a=i/(float)pieces,b=(i+1)/(float)pieces;
                float x=s.ax+(s.bx-s.ax)*a,y=s.ay+(s.by-s.ay)*a,xx=s.ax+(s.bx-s.ax)*b,yy=s.ay+(s.by-s.ay)*b;
                float rx=(x+xx)*.5f,ry=(y+yy)*.5f,ddx=rx-lx,ddy=ry-ly;
                float power=strength*falloff(ddx*ddx+ddy*ddy+(z-s.height)*(z-s.height),radius)*sector(angle,fixture,ddx,ddy);
                int alpha=(int)(power*255);if(alpha<2 || !clear(g,lx,ly,rx+s.nx,ry+s.ny))continue;
                if(walls && s.wallVisible){quad(c,q,x,y,xx,yy,0,s.height);p.poly(q,(alpha<<24)|(color&0xffffff));}
                if(walls && !g.montroseStage())continue;
                // Narrow landward lip: validate both inner corners, so thin docks/corners stay dry-only.
                if(z<=s.height || g.montroseStage() && s.height<=4)continue;
                float inset=Math.min(20,5+(z-s.height));
                float ix=x-s.nx*inset,iy=y-s.ny*inset,jx=xx-s.nx*inset,jy=yy-s.ny*inset;
                boolean wet=WaterEffectsPainter.contains(g,ix,iy)||WaterEffectsPainter.contains(g,jx,jy);
                if(!wet){q[0]=c.screenX(x,y);q[1]=c.screenY(x,y)-s.height*c.scale;q[2]=c.screenX(xx,yy);q[3]=c.screenY(xx,yy)-s.height*c.scale;q[4]=c.screenX(jx,jy);q[5]=c.screenY(jx,jy)-s.height*c.scale;q[6]=c.screenX(ix,iy);q[7]=c.screenY(ix,iy)-s.height*c.scale;p.poly(q,((alpha/2)<<24)|(color&0xffffff));}
            }
        }
    }
    private static boolean clear(Game g,float x,float y,float xx,float yy){int n=Math.max(1,(int)(Physics.length(xx-x,yy-y)/6));
        for(int i=1;i<n;i++){float t=i/(float)n,px=x+(xx-x)*t,py=y+(yy-y)*t;
            if(!WaterEffectsPainter.contains(g,px,py))return false;
        }return true;
    }
    private static void quad(RiverCamera c,float[] q,float x,float y,float xx,float yy,float low,float high){q[0]=c.screenX(x,y);q[1]=c.screenY(x,y)-low*c.scale;q[2]=c.screenX(xx,yy);q[3]=c.screenY(xx,yy)-low*c.scale;q[4]=q[2];q[5]=c.screenY(xx,yy)-high*c.scale;q[6]=q[0];q[7]=c.screenY(x,y)-high*c.scale;}
}
