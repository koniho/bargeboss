package com.bargeboss.game;

import java.util.ArrayList;

/** Prepared, world-depth-sorted instances of the Montrose rigid crane at cargo berths. */
public final class DockView {
    private static final ArrayList<Crane> CACHE=new ArrayList<Crane>();
    static final class Crane {
        final float x,y,heading;final ArticulatedModel model;final ArticulatedModel.Pose pose;
        Crane(float x,float y,float heading){this.x=x;this.y=y;this.heading=heading;
            model=HarborCrane.build(x,y,GroundArt.LAND_RISE,heading);pose=model.pose(0);model.prepare(pose);}
    }
    private DockView(){}
    /** Called on level reset, never from drawing. Cache is bounded across level switches. */
    static void prepare(Game g){
        g.dockCranes=new Crane[g.calumetStage()?CalumetRiver.JOB_SITES.length:3];if(g.montroseStage())return;
        for(int berth=0;berth<g.dockCranes.length;berth++){
            float[] at=new float[2];
            if(g.calumetStage()){float[] site=CalumetRiver.JOB_SITES[berth];at[0]=site[0]+site[2]*90-site[3]*70;at[1]=site[1]+site[3]*90+site[2]*70;}
            else DockWorkers.shoreAnchor(g,berth,38,16,at);
            float dx=g.calumetStage()?g.voyage.dockX(berth):g.dockX(berth),dy=g.calumetStage()?g.voyage.dockY(berth):g.dockY(berth);
            float heading=(float)StrictMath.atan2(dy-at[1],dx-at[0]);
            synchronized(CACHE){
                Crane crane=null;
                for(Crane candidate:CACHE)if(candidate.x==at[0] && candidate.y==at[1] && candidate.heading==heading){crane=candidate;break;}
                if(crane==null){crane=new Crane(at[0],at[1],heading);if(CACHE.size()>=32)CACHE.remove(0);CACHE.add(crane);}
                g.dockCranes[berth]=crane;
            }
        }
    }
    static int[] ordered(Game g){
        if(g.montroseStage() || g.dockCranes==null)return new int[0];
        int[] ids=new int[g.dockCranes.length];int count=0;
        for(int berth=0;berth<g.dockCranes.length;berth++)if(g.calumetStage() || g.berthDisplayEnabled(berth)){
            Crane crane=g.dockCranes[berth];RiverCamera c=g.layout.camera;
            float x=c.screenX(crane.x,crane.y),y=c.screenY(crane.x,crane.y);
            if(x+100*c.scale<0 || x-100*c.scale>g.layout.width || y+80*c.scale<0 || y-150*c.scale>g.layout.height)continue;
            int j=count;while(j>0 && depth(g,ids[j-1])>depth(g,berth)){ids[j]=ids[j-1];j--;}
            ids[j]=berth;count++;
        }
        return java.util.Arrays.copyOf(ids,count);
    }
    static float depth(Game g,int berth){Crane c=g.dockCranes[berth];return g.layout.camera.screenY(c.x,c.y);}
    static void draw(Game g,Painter canvas,int berth){Crane c=g.dockCranes[berth];c.model.draw(g,canvas,c.pose);}
}
