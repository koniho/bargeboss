package com.bargeboss.game;

import java.util.ArrayList;

/** Prepared cargo cranes, depth-sliced rig geometry and independently sorted hook/load. */
public final class DockView {
    private static final ArrayList<Crane> CACHE=new ArrayList<Crane>();
    static final class Crane {
        final float x,y,heading,reach,pickX,pickY;final ArticulatedModel model;final ArticulatedModel.Motion motion;
        Crane(float x,float y,float heading,float reach){this.x=x;this.y=y;this.heading=heading;this.reach=reach;
            pickX=x+reach*(float)StrictMath.cos(heading);pickY=y+reach*(float)StrictMath.sin(heading);
            model=HarborCrane.cargo(x,y,heading,reach);motion=model.motion(0,0,0,1);motion.pose(0);}
        float landX(){return x+(float)StrictMath.cos(heading)*(reach*(float)StrictMath.cos(CargoTransfer.LUFF)-4*(float)StrictMath.sin(CargoTransfer.LUFF));}
        float landY(){return y+(float)StrictMath.sin(heading)*(reach*(float)StrictMath.cos(CargoTransfer.LUFF)-4*(float)StrictMath.sin(CargoTransfer.LUFF));}
    }
    private DockView(){}
    static void prepare(Game g){
        // The Chicago fire call has no cargo berths, so it prepares no cargo cranes.
        g.dockCranes=new Crane[g.calumetStage()?CalumetRiver.JOB_SITES.length:g.chicagoStage()?0:3];if(g.montroseStage())return;
        for(int berth=0;berth<g.dockCranes.length;berth++){
            float[] at=new float[2];
            if(g.calumetStage()){float[] site=CalumetRiver.JOB_SITES[berth];at[0]=site[0]+site[2]*90-site[3]*70;at[1]=site[1]+site[3]*90+site[2]*70;}
            else DockWorkers.shoreAnchor(g,berth,38,16,at);
            float dx=g.calumetStage()?g.voyage.dockX(berth):g.dockX(berth),dy=g.calumetStage()?g.voyage.dockY(berth):g.dockY(berth);
            float heading=(float)StrictMath.atan2(dy-at[1],dx-at[0]);
            // Verify both the complete footing and receiving pad against the actual shoreline.
            float outward=heading+(float)Math.PI,reach=0;boolean placed=false;
            for(int ring=0;ring<48 && !placed;ring++)for(int direction=0;direction<32;direction++){
                float offset=((direction+1)/2)*(float)Math.PI/16*(direction%2==0?1:-1),a=outward+offset,r=116+ring*16;
                float xx=dx+r*(float)StrictMath.cos(a),yy=dy+r*(float)StrictMath.sin(a);
                float length=r*(float)StrictMath.cos(CargoTransfer.LUFF)-4*(float)StrictMath.sin(CargoTransfer.LUFF),lx=xx-length*(float)StrictMath.cos(a),ly=yy-length*(float)StrictMath.sin(a);
                if(dry(g,xx,yy,18) && dry(g,lx,ly,36)){at[0]=xx;at[1]=yy;placed=true;break;}
            }
            if(!placed)throw new IllegalStateException("No dry crane receiving apron: "+berth);
            heading=(float)StrictMath.atan2(dy-at[1],dx-at[0]);reach=Physics.length(dx-at[0],dy-at[1]);
            synchronized(CACHE){
                Crane crane=null;
                for(Crane candidate:CACHE)if(candidate.x==at[0] && candidate.y==at[1] && candidate.heading==heading && candidate.reach==reach){crane=candidate;break;}
                if(crane==null){crane=new Crane(at[0],at[1],heading,reach);if(CACHE.size()>=32)CACHE.remove(0);CACHE.add(crane);}
                g.dockCranes[berth]=crane;
            }
        }
    }
    private static boolean dry(Game g,float x,float y,float radius){
        for(int i=-1;i<=1;i++)for(int j=-1;j<=1;j++)if(g.inWater(x+i*radius,y+j*radius))return false;
        return true;
    }
    static int[] visible(Game g){
        if(g.montroseStage() || g.dockCranes==null)return new int[0];
        int[] ids=new int[g.dockCranes.length];int n=0;RiverCamera c=g.layout.camera;
        for(int i=0;i<ids.length;i++)if(g.calumetStage() || g.berthDisplayEnabled(i)){
            Crane crane=g.dockCranes[i];float x=c.screenX(crane.x,crane.y),y=c.screenY(crane.x,crane.y),r=(crane.reach+140)*c.scale;
            if(x+r>=0 && x-r<=g.layout.width && y+r>=0 && y-r<=g.layout.height)ids[n++]=i;
        }
        return java.util.Arrays.copyOf(ids,n);
    }
    /** IDs below crane count are cable/hook events; IDs above it are carried/landed loads. */
    static int[] ordered(Game g){
        int[] visible=visible(g),ids=new int[visible.length*2];int n=0;
        for(int id:visible){ids[n++]=id;if(new CargoTransfer.Pose(g,g.dockCranes[id],id).cargo())ids[n++]=id+g.dockCranes.length;}
        for(int i=1;i<n;i++){int id=ids[i],j=i;float d=depth(g,id);while(j>0 && depth(g,ids[j-1])>d){ids[j]=ids[j-1];j--;}ids[j]=id;}
        return java.util.Arrays.copyOf(ids,n);
    }
    static float depth(Game g,int id){int n=g.dockCranes.length,dock=id%n;CargoTransfer.Pose s=new CargoTransfer.Pose(g,g.dockCranes[dock],dock);
        return g.layout.camera.screenY(id<n?s.hookX():s.x,id<n?s.hookY():s.y)+(id<n?.04f:.02f);}
    static void lights(Game g,SceneryLighting out){
        for(int dock:visible(g)){Crane c=g.dockCranes[dock];CargoTransfer.Pose s=new CargoTransfer.Pose(g,c,dock);ArticulatedModel.Pose pose=c.motion.pose(s.phase);
            float pulse=s.tick>0 && s.tick<Game.UNLOAD_TICKS?.82f+.18f*(float)StrictMath.sin(g.visualTicks*.065f):.8f;
            for(HarborCrane.Lamp lamp:HarborCrane.CARGO_LIGHTS){float[] at=pose.point(c.model.nodeId(lamp.parent),lamp.x,lamp.y,lamp.z);out.add(at[0],at[1],at[2],145,.55f*pulse*SceneryEffects.intensity(g.visualTicks,at[0]*.013f),0xffffc66e);}
        }
    }
    static void drawRange(Game g,Painter p,int dock,CargoTransfer.Pose s,float min,float max){
        if(s.maxDepth<=min || s.minDepth>max)return;
        Crane crane=g.dockCranes[dock];ArticulatedModel.Pose pose=crane.motion.pose(s.phase);crane.model.drawRange(g,p,pose,min,max);
        for(HarborCrane.Lamp lamp:HarborCrane.CARGO_LIGHTS){float[] at=pose.point(crane.model.nodeId(lamp.parent),lamp.x,lamp.y,lamp.z);
            if(at[1]-at[0]>min && at[1]-at[0]<=max && !pose.occluded(at))glow(g,p,at[0],at[1],at[2],s.tick>0 && s.tick<Game.UNLOAD_TICKS);
        }
    }
    static void draw(Game g,Painter p,int id){
        int n=g.dockCranes.length,dock=id%n;Crane c=g.dockCranes[dock];CargoTransfer.Pose s=new CargoTransfer.Pose(g,c,dock);
        if(id>=n){if(s.cargo())CargoView.lifted(g,p,s.load,s.x,s.y,s.z,s.angle);return;}
        float hx=s.hookX(),hy=s.hookY();
        line(g,p,s.tipX,s.tipY,s.tipZ,hx,hy,s.hookZ,.8f,0xffa6a69a);
        line(g,p,hx,hy,s.hookZ+3,hx,hy,s.hookZ-1,2.6f,0xffd5ac4c);
        if(s.cargo() && CargoTransfer.attached(s.tick)){
            float co=(float)StrictMath.cos(s.angle),si=(float)StrictMath.sin(s.angle);
            for(int end=-1;end<=1;end+=2)for(int side=-1;side<=1;side+=2){
                float x=s.x+co*end*27-si*side*10,y=s.y+si*end*27+co*side*10;
                line(g,p,hx,hy,s.hookZ,x,y,s.z+9,.65f,0xffb4afa0);
            }
        }

    }
    static void line(Game g,Painter p,float x,float y,float z,float xx,float yy,float zz,float width,int color){RiverCamera c=g.layout.camera;p.line(c.screenX(x,y),c.screenY(x,y)-z*ArticulatedModel.Z_SCALE*c.scale,c.screenX(xx,yy),c.screenY(xx,yy)-zz*ArticulatedModel.Z_SCALE*c.scale,width*c.scale,color);}
    private static void glow(Game g,Painter p,float x,float y,float z,boolean working){RiverCamera c=g.layout.camera;float sx=c.screenX(x,y),sy=c.screenY(x,y)-z*ArticulatedModel.Z_SCALE*c.scale;
        float pulse=working?.82f+.18f*(float)StrictMath.sin(g.visualTicks*.065f):.8f;
        SceneryEffects.glow(p,sx,sy,3*c.scale,0xffffc66e,.8f,g.visualTicks,x*.013f,pulse);
        p.circle(sx,sy,1.5f*c.scale,0xffffdeb0);
    }
    static void shadows(Game g,Painter p){
        for(int dock:visible(g)){Crane c=g.dockCranes[dock];CargoTransfer.Pose s=new CargoTransfer.Pose(g,c,dock);
            if(!s.cargo())continue;
            float rise=g.inWater(s.x,s.y)?0:CargoTransfer.landZ(),h=Math.max(0,s.z-rise);
            float co=(float)StrictMath.cos(s.angle),si=(float)StrictMath.sin(s.angle);RiverCamera camera=g.layout.camera;
            for(int blur=2;blur>=0;blur--){float[] q=new float[8];for(int k=0;k<4;k++){float a=(k<2?-1:1)*(30+blur*2),b=(k==0 || k==3?-1:1)*(11+blur*2);
                float x=s.x+co*a-si*b+h*.35f,y=s.y+si*a+co*b+h*.25f;q[k*2]=camera.screenX(x,y);q[k*2+1]=camera.screenY(x,y)-rise*ArticulatedModel.Z_SCALE*camera.scale;}
                p.poly(q,0x16080e16);
            }
        }
    }
}
