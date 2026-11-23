package com.bargeboss.game;

/** Two fixed dockhands, timed casts and depth-sorted working lines. All state is shared. */
final class DockLineCrew {
    static final int WINDUP=24,CATCH=60;
    private final boolean lift;
    Body boat;int age,cheer;
    DockLineCrew(boolean lift){this.lift=lift;}
    void start(Body b){boat=b;age=0;cheer=0;}
    void finish(){boat=null;cheer=120;}
    void step(){if(boat!=null)age++;if(cheer>0)cheer--;}
    boolean caught(){return boat!=null && age>=CATCH;}
    float x(int worker){return MontroseHarbor.mapX(lift?(worker==0?1031:1089):(worker==0?982:988));}
    float y(int worker){return MontroseHarbor.mapY(lift?(worker==0?228:257):(worker==0?400:440));}
    float rise(){return lift?MontroseHarbor.LAND_RISE:MontroseHarbor.DOCK_RISE;}
    float arm(Game g,int worker){
        if(boat!=null){if(age<WINDUP)return .2f+1.9f*age/WINDUP;if(age<CATCH)return 2.1f-1.7f*(age-WINDUP)/(CATCH-WINDUP);
            return .65f+.3f*(float)StrictMath.sin(age*.16+worker*2);}
        return cheer>0?2.4f+.2f*(float)StrictMath.sin(cheer*.2):.08f+.06f*(float)StrictMath.sin(g.visualTicks*.035+worker);
    }
    void hand(Game g,int worker,float[] out){
        float a=arm(g,worker),dx=.4f,dz=-5.5f;
        out[0]=x(worker)+3.1f+dx*(float)StrictMath.cos(a)-dz*(float)StrictMath.sin(a);
        out[1]=y(worker);out[2]=rise()+15+dx*(float)StrictMath.sin(a)+dz*(float)StrictMath.cos(a);
    }
    void drawWorker(Game g,Painter p,int worker){
        RiverCamera c=g.layout.camera;float sx=c.screenX(x(worker),y(worker)),sy=c.screenY(x(worker),y(worker))-rise()*c.scale;
        if(sx<-30*c.scale || sx>g.layout.width+30*c.scale || sy<-30*c.scale || sy>g.layout.height+30*c.scale)return;
        DockWorkers.dockhand(g,p,x(worker),y(worker),rise(),arm(g,worker),0);
    }
    int append(Game g,TowView.Segments s,int used,int base){
        if(boat==null || age<WINDUP || !boat.active)return used;
        float flight=Math.min(1,(age-WINDUP)/(float)(CATCH-WINDUP));
        float[] hand=new float[3],fit=new float[2];
        for(int worker=0;worker<2;worker++){
            hand(g,worker,hand);RopeAttachment.mooring(boat,hand[0],hand[1],fit);
            float endHeight=RopeAttachment.height(boat)+WaterSurface.lift(g,boat);
            int start=base+worker*25;
            for(int i=0;i<=24;i++){
                float t=i/24f*flight;
                s.x[start+i]=hand[0]+(fit[0]-hand[0])*t;s.y[start+i]=hand[1]+(fit[1]-hand[1])*t;
                float arc=age<CATCH?28:-Math.max(1,7*(1-Math.min(1,(age-CATCH)/24f)));
                s.height[start+i]=hand[2]+(endHeight-hand[2])*t+4*t*(1-t)*arc;
                if(i>0)s.strand[start+i]=s.strand[start+i-1]+Physics.length(s.x[start+i]-s.x[start+i-1],s.y[start+i]-s.y[start+i-1])/5;
            }
            for(int i=0;i<24;i++){s.points[used]=start+i;s.colors[used]=RopeMaterial.COLOR;s.mooring[used]=true;used++;}
        }
        return used;
    }
    static DockLineCrew crew(Game g,int id){return id<2?g.haulout.lineCrew:g.mission.lineCrew;}
    static float depth(Game g,int id){DockLineCrew c=crew(g,id);return g.layout.camera.screenY(c.x(id%2),c.y(id%2));}
    static void draw(Game g,Painter p,int id){crew(g,id).drawWorker(g,p,id%2);}
}
