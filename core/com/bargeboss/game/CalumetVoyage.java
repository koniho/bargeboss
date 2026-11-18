package com.bargeboss.game;

/** Four contiguous jobs on one surveyed river. Advancing never resets the world. */
public final class CalumetVoyage {
    public static final float HALF_WIDTH=34,HALF_LENGTH=57;
    public int job;
    public boolean complete;
    private int scoreAtStart;
    public int quota(){return job==0 || job==2?2:3;}
    public String title(){return new String[]{"TWO BANKS","BERTH EXCHANGE","BRIDGE CONVOY","BASIN FIVE"}[job];}
    public String status(){return new String[]{"JOB 1 OF 4 / DELIVER TO BOTH BANKS","JOB 2 OF 4 / CLEAR EMPTIES TO EXCHANGE THREE LOADS","JOB 3 OF 4 / TOW BOTH EMPTY BARGES THROUGH THE BRIDGE","JOB 4 OF 4 / WORK THE TERMINAL BERTHS / BASIN FIVE"}[job];}
    public int destination(int cargoKind){return job==0?(cargoKind==1?1:0):job==1?2:job==3?(cargoKind==2?3:cargoKind==0?4:5):2;}
    private static float[] site(int destination){return CalumetRiver.JOB_SITES[Math.max(0,Math.min(CalumetRiver.JOB_SITES.length-1,destination))];}
    public float dockX(int destination){return site(destination)[0];}
    public float dockY(int destination){return site(destination)[1];}
    public float normalX(int destination){return site(destination)[2];}
    public float normalY(int destination){return site(destination)[3];}
    public float heading(int destination){return (float)StrictMath.atan2(normalX(destination),-normalY(destination));}
    public float bollardX(int destination){return dockX(destination)+normalX(destination)*64+RiverCamera.riseX(GroundArt.LAND_RISE);}
    public float bollardY(int destination){return dockY(destination)+normalY(destination)*64+RiverCamera.riseY(GroundArt.LAND_RISE);}
    public boolean berthEnabled(int kind){return !complete && job!=2 && (job!=0 || kind==2 || kind==1);}
    public void setup(Game g){
        g.tug.x=CalumetRiver.START_X;g.tug.y=CalumetRiver.START_Y;g.tug.angle=CalumetRiver.START_ANGLE;
        startJob(g);
        g.layout=Layout.focused(g.layout.width,g.layout.height,g.tug.x,g.tug.y,Game.TRANSIT_ZOOM,g.layout.width*.5f,g.layout.height*.52f);
    }
    private void startJob(Game g){
        scoreAtStart=g.score;
        if(job==2){/* Reuse any two completed exchange hulls. */}
        else if(job==0){spawn(g,2,0,0);spawn(g,1,1,1);}
        else for(int i=0;i<3;i++){int kind=i==0?2:i==1?0:1;spawn(g,kind,destination(kind),i);}
        g.tell(status());
    }
    private void spawn(Game g,int kind,int destination,int order){
        int slot=0;while(slot<g.cargo.length && g.cargo[slot]!=null && g.cargo[slot].active)slot++;
        if(slot==g.cargo.length)throw new IllegalStateException("Calumet voyage must retain eight cargo hulls");
        Body b=new Body(g.nextId++,kind,dockX(destination),dockY(destination),heading(destination));
        b.destination=destination;b.voyageJob=job;
        float tx=-normalY(destination),ty=normalX(destination);
        float wantedX=b.x-normalX(destination)*170+tx*(order-1)*115;
        float wantedY=b.y-normalY(destination)*170+ty*(order-1)*115;
        float best=Float.MAX_VALUE,bx=0,by=0,ba=0;
        // One bounded setup search on surveyed water. Never teleport/reposition old hulls.
        search: for(int r=0;r<=12;r++)for(int a=0;a<24;a++){
            float angle=a*(float)Math.PI/12,x=wantedX+(float)StrictMath.cos(angle)*r*24,y=wantedY+(float)StrictMath.sin(angle)*r*24;
            b.x=x;b.y=y;if(!safeSpawn(g,b))continue;
            float distance=Physics.length(x-wantedX,y-wantedY);if(distance<best){best=distance;bx=x;by=y;ba=b.angle;break search;}
        }
        if(best==Float.MAX_VALUE)for(int i=0;i<CalumetRiver.ROUTE.length;i++){
            float[] p=CalumetRiver.ROUTE[i];b.x=p[0];b.y=p[1];if(i+1<CalumetRiver.ROUTE.length)b.angle=(float)StrictMath.atan2(CalumetRiver.ROUTE[i+1][1]-p[1],CalumetRiver.ROUTE[i+1][0]-p[0]);
            if(!safeSpawn(g,b))continue;float d=Physics.length(b.x-wantedX,b.y-wantedY);if(d<best){best=d;bx=b.x;by=b.y;ba=b.angle;}
        }
        if(best==Float.MAX_VALUE)throw new IllegalStateException("No clear Calumet cargo handoff water");
        b.x=bx;b.y=by;b.angle=ba;b.awaitingPickup=true;b.tiedX=bx;b.tiedY=by;b.tiedAngle=ba;g.cargo[slot]=b;
    }
    private static boolean safeSpawn(Game g,Body b){
        for(int disc=0;disc<3;disc++)for(int a=0;a<12;a++){float angle=a*(float)Math.PI/6;
            if(!CalumetRiver.contains(b.discX(disc)+(b.radius+6)*(float)StrictMath.cos(angle),b.discY(disc)+(b.radius+6)*(float)StrictMath.sin(angle)))return false;}
        if(Physics.touching(b,g.tug,35))return false;
        for(Body other:g.cargo)if(other!=null && other.active && Physics.touching(b,other,35))return false;
        return true;
    }
    public float gateX(){return CalumetRiver.CONVOY_GATE[0];}
    public float gateY(){return CalumetRiver.CONVOY_GATE[1];}
    private boolean through(Body b){
        if(b==null || !b.active)return false;
        float[] gate=CalumetRiver.CONVOY_GATE;
        for(int i=0;i<3;i++)if((b.discX(i)-gate[0])*gate[2]+(b.discY(i)-gate[1])*gate[3]<b.radius+12)return false;
        return true;
    }
    public int progress(Game g){
        if(job==2){int n=0;if(!through(g.tug))return 0;for(Body b:g.cargo)if(b!=null && b.voyageJob==1 && b.deliveryRecorded && g.tow.connected(b,g.tug) && through(b))n++;return Math.min(2,n);}
        int n=0;for(Body b:g.cargo)if(b!=null && b.voyageJob==job && b.deliveryRecorded)n++;return n;
    }
    public void step(Game g){
        if(complete || g.goalFailed() || progress(g)<quota())return;
        if(job==2){g.score+=1200;g.haptic(Game.HAPTIC_SUCCESS);}
        int before=g.career.rank(),stars=Challenges.stars(job,quota(),g.lost,g.hull);
        g.career.record(job,stars,g.score-scoreAtStart);g.rankPromoted|=g.career.rank()>before;
        if(job==3){complete=true;g.earnedStars=stars;return;}
        job++;g.cancelInputs();startJob(g);
    }
    public boolean inTieArea(Body b){
        if(b==null || b.destination<0)return false;
        float dx=b.x-dockX(b.destination),dy=b.y-dockY(b.destination),nx=normalX(b.destination),ny=normalY(b.destination);
        return Math.abs(dx*nx+dy*ny)<HALF_WIDTH+20 && Math.abs(-dx*ny+dy*nx)<HALF_LENGTH+15;
    }
    public boolean fits(Body b){
        if(b==null || b.destination<0)return false;float nx=normalX(b.destination),ny=normalY(b.destination);
        for(int i=0;i<3;i++){float dx=b.discX(i)-dockX(b.destination),dy=b.discY(i)-dockY(b.destination);
            if(Math.abs(dx*nx+dy*ny)+b.radius>HALF_WIDTH || Math.abs(-dx*ny+dy*nx)+b.radius>HALF_LENGTH)return false;}
        return true;
    }
    public Body blocker(Game g,int destination,Body incoming){
        float nx=normalX(destination),ny=normalY(destination);
        for(Body b:g.cargo)if(b!=null && b!=incoming && b.active)for(int i=0;i<3;i++){
            float dx=b.discX(i)-dockX(destination),dy=b.discY(i)-dockY(destination);
            float a=Math.abs(dx*nx+dy*ny)-HALF_WIDTH,c=Math.abs(-dx*ny+dy*nx)-HALF_LENGTH;
            a=Math.max(0,a);c=Math.max(0,c);if(a*a+c*c<b.radius*b.radius)return b;
        }
        return null;
    }
    public void moor(Game g,Body b){
        if(b.destination<0 || blocker(g,b.destination,b)!=null)return;
        if(b.mooringTicks<Mooring.ALIGN_TICKS)b.mooringTicks++;
        float t=b.mooringTicks/(float)Mooring.ALIGN_TICKS,e=t*t*(3-2*t),angle=heading(b.destination);
        if(Math.abs(Physics.angle(angle-b.mooringAngle))>Math.PI/2)angle+=(float)Math.PI;
        b.angle=Physics.angle(b.mooringAngle+Physics.angle(angle-b.mooringAngle)*e);
        b.x=b.mooringX+(dockX(b.destination)-b.mooringX)*e;b.y=b.mooringY+(dockY(b.destination)-b.mooringY)*e;
        CalumetRiver.collide(b,null);
        b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;b.vx=b.vy=b.spin=0;b.dockTime=1.2f;
        if(b.mooringTicks>=Mooring.ALIGN_TICKS && fits(b))b.unloadingTicks++;
    }
}
