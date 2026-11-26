package com.bargeboss.game;

/** Challenge basin enclosure, barge spawns, public tie-off counting (incl. trains), clock and record. */
public final class TowingChallengeTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static boolean water(float x,float y){return TrainingChannel.contains(x,y);}

    /** Practice at the challenge, briefing dismissed and the tug just across the start line. */
    static Game running(Game root){
        ReplayInput.startPractice(root);Game g=root.activeGame();
        g.tutorial.lesson=Tutorial.CHALLENGE;g.tutorial.setup(g);
        check(g.dialogue.active && g.tutorial.challenge.phase==TowingChallenge.INTRO,"explicit challenge setup opens the instructor briefing");
        g.dialogue.close(g);g.step();
        check(g.tutorial.challenge.phase==TowingChallenge.APPROACH,"closing the briefing waits for the basin approach");
        g.tug.y=TowingChallenge.START_Y-30;g.step();
        check(g.tutorial.challenge.phase==TowingChallenge.RUN && g.tutorial.challenge.remaining==TowingChallenge.TICKS,"crossing the basin mouth starts the 60 second clock");
        return g;
    }
    private static void geometry(){
        float[] b=TrainingChannel.WATER_BOUNDARY;int n=b.length/2;
        check(b.length%2==0 && n>40,"training boundary is a closed vertex ring");
        // Simple polygon: no two non-adjacent edges touch, so the basin cannot leak or overlap other water.
        for(int i=0;i<n;i++)for(int j=i+1;j<n;j++){
            if(j==i+1 || i==0 && j==n-1)continue;
            float ax=b[2*i],ay=b[2*i+1],bx=b[2*((i+1)%n)],by=b[2*((i+1)%n)+1];
            float cx=b[2*j],cy=b[2*j+1],dx=b[2*((j+1)%n)],dy=b[2*((j+1)%n)+1];
            check(!segmentsTouch(ax,ay,bx,by,cx,cy,dx,dy),"training shoreline does not self-intersect at edges "+i+"/"+j);
        }
        float left=TrainingChannel.CHALLENGE_LEFT,right=TrainingChannel.CHALLENGE_RIGHT,top=TrainingChannel.CHALLENGE_TOP,bottom=TrainingChannel.CHALLENGE_BOTTOM;
        check((right-left)*(bottom-top)>3*(Physics.RIGHT-Physics.LEFT)*(TrainingChannel.NARROW_EXIT-TrainingChannel.TOW_END),"challenge basin is much larger than the train basin it opens from");
        for(float x=left-40;x<=right+40;x+=10)for(float y=top-40;y<=TrainingChannel.TOW_END+20;y+=10)
            check(water(x,y)==PolygonWater.contains(b,x,y) || onBoundary(b,x,y),"basin water fill and collision shoreline agree at "+x+","+y);
        for(float x=left+5;x<right;x+=40){check(!water(x,top-5),"north quay closes the basin");check(water(x,top+5),"basin water reaches the north quay");}
        for(float y=top+5;y<bottom;y+=40){check(!water(left-5,y) && !water(right+5,y),"east and west quays close the basin");}
        for(float x=left+5;x<right;x+=20){
            boolean mouth=x>TrainingChannel.CHALLENGE_MOUTH_LEFT && x<TrainingChannel.CHALLENGE_MOUTH_RIGHT;
            check(water(x,bottom+5)==mouth,"south quay is solid except for the basin mouth");
        }
        for(float y=bottom;y<=TrainingChannel.TOW_END+30;y+=10)check(water(TowingChallenge.START_X,y),"mouth connects the train basin to the challenge basin");
        // Solid walls: hulls thrown at each quay are pushed back into water.
        float[][] probes={{left+20,-2300,-260,0},{right-20,-2300,260,0},{200,top+20,0,-260},{0,bottom-20,0,260},{TrainingChannel.CHALLENGE_MOUTH_LEFT+15,-1590,-260,0}};
        for(float[] probe:probes){
            Body hull=new Body(5,2,probe[0],probe[1],probe[2]==0?(float)Math.PI/2:0);
            for(int i=0;i<90;i++){hull.vx=probe[2];hull.vy=probe[3];Physics.integrate(hull);TrainingChannel.collideWater(hull);}
            for(int d=0;d<3;d++)check(water(hull.discX(d),hull.discY(d)),"quay collision keeps every hull disc in water");
        }
        // Shared ground, scenery bounds and the chart extent all grow from the same boundary.
        check(TrainingScenery.MIN_Y<=top-TrainingScenery.MARGIN && TrainingScenery.MAX_X>=right+TrainingScenery.MARGIN,"finite scenery covers the challenge basin and its quay margin");
        check(TrainingGroundView.intersectsWater(300,-2300,64,64) && !TrainingGroundView.intersectsWater(right+64,-2300,64,64),"ground tiles subtract the basin water exactly");
    }
    private static boolean onBoundary(float[] b,float x,float y){
        for(int i=0;i<b.length;i+=2){int j=(i+2)%b.length;
            float minX=Math.min(b[i],b[j]),maxX=Math.max(b[i],b[j]),minY=Math.min(b[i+1],b[j+1]),maxY=Math.max(b[i+1],b[j+1]);
            if(x>=minX-.01f && x<=maxX+.01f && y>=minY-.01f && y<=maxY+.01f)return true;}
        return false;
    }
    private static boolean segmentsTouch(float ax,float ay,float bx,float by,float cx,float cy,float dx,float dy){
        float d1=cross(cx,cy,dx,dy,ax,ay),d2=cross(cx,cy,dx,dy,bx,by),d3=cross(ax,ay,bx,by,cx,cy),d4=cross(ax,ay,bx,by,dx,dy);
        if(((d1>0 && d2<0) || (d1<0 && d2>0)) && ((d3>0 && d4<0) || (d3<0 && d4>0)))return true;
        return d1==0 && within(cx,cy,dx,dy,ax,ay) || d2==0 && within(cx,cy,dx,dy,bx,by) || d3==0 && within(ax,ay,bx,by,cx,cy) || d4==0 && within(ax,ay,bx,by,dx,dy);
    }
    private static float cross(float ax,float ay,float bx,float by,float px,float py){return (bx-ax)*(py-ay)-(by-ay)*(px-ax);}
    private static boolean within(float ax,float ay,float bx,float by,float px,float py){return px>=Math.min(ax,bx) && px<=Math.max(ax,bx) && py>=Math.min(ay,by) && py<=Math.max(ay,by);}

    private static void spawns(){
        Game root=new Game();root.configureCanvas(1333,600);Game g=running(root);TowingChallenge c=g.tutorial.challenge;
        int count=0;
        for(Body b:c.barges){
            check(b!=null && b.active && b.berth>=0 && b.awaitingPickup,"ten moored barges wait for the player");count++;
            check(b.x<TrainingChannel.CHALLENGE_LEFT+100,"barges wait along the west quay");
            for(int d=0;d<3;d++)for(int k=0;k<4;k++){
                float x=b.discX(d)+(k==0?b.radius:k==1?-b.radius:0),y=b.discY(d)+(k==2?b.radius:k==3?-b.radius:0);
                check(water(x,y),"each challenge hull spawns entirely on water");
            }
            for(Body other:c.barges)if(other!=b)check(!Physics.touching(b,other,0),"challenge barges spawn without overlap");
            boolean inCargo=false;for(Body slot:g.cargo)inCargo|=slot==b;check(inCargo,"challenge hulls are ordinary cargo bodies");
        }
        check(count==TowingChallenge.BARGES,"exactly ten challenge barges");
        for(int row=0;row<TowingChallenge.BARGES;row++){
            check(water(TowingChallenge.BERTH_X,TowingChallenge.rowY(row)) && !water(TowingChallenge.bollardX(),TowingChallenge.bollardY(row)),"each far-side berth has a quay bollard behind it");
        }
        for(int i=0;i<300;i++)g.step();
        for(Body b:c.barges)check(Physics.length(b.x-b.tiedX,b.y-b.tiedY)<.01f,"moored barges hold station until collected");
        check(g.tutorial.lesson<Tutorial.CHALLENGE+1 && g.tutorial.challenge.tied==0,"nothing counts before a tie-off");
    }
    /** Lay a body against the east quay at the given row, ready to receive a line. */
    private static void place(Body b,int row){b.x=TowingChallenge.BERTH_X;b.y=TowingChallenge.rowY(row);b.angle=-(float)Math.PI/2;b.vx=b.vy=b.spin=0;b.awaitingPickup=false;}
    private static void focus(Game g,float y){g.layout=Layout.focused(g.layout.width,g.layout.height,TowingChallenge.BERTH_X,y,1.4f,g.layout.width*.45f,g.layout.height*.55f);}
    private static void tie(Game root,Body b){
        Game g=root.activeGame();focus(g,b.y);
        check(g.canTie(b),"hull alongside the far quay is ready to tie");
        ReplayInput.lasso(root,b);
        for(int i=0;i<Game.ROPE_FLIGHT_TICKS+Game.ROPE_HIT_TICKS+2;i++)root.step();
        check(b.lineAttached,"public lasso gesture ties the barge to its far-quay bollard");
    }
    private static void counting(){
        Game root=new Game();root.configureCanvas(1333,600);Game g=running(root);TowingChallenge c=g.tutorial.challenge;
        int delivered=g.delivered;Body single=c.barges[0];place(single,0);tie(root,single);
        check(c.tied==1 && g.delivered==delivered,"one tied barge counts once, without a career delivery");
        for(int i=0;i<120;i++)root.step();
        check(c.tied==1 && Math.abs(single.x-(TrainingChannel.CHALLENGE_RIGHT-single.radius-10))<.5f && Math.abs(single.y-TowingChallenge.rowY(0))<.5f,"winch draws the hull alongside its claimed berth");
        check(!g.canTie(single) && !g.tow.canAttach(g,single,g.tug),"a tied barge cannot be counted twice or towed away");
        // A three-barge train: tug -> A -> B -> C, laid along free berths. Tie the tail first.
        Body a=c.barges[1],b=c.barges[2],cc=c.barges[3];
        place(a,3);place(b,4);place(cc,5);
        g.tug.x=TowingChallenge.BERTH_X;g.tug.y=TowingChallenge.rowY(2);g.tug.angle=-(float)Math.PI/2;g.tug.vx=g.tug.vy=g.tug.spin=0;
        g.tow.attach(g,a,g.tug);g.tow.attach(g,b,a);g.tow.attach(g,cc,b);
        check(g.tow.loadCount(g.tug)==3,"barge train of three is towed by the tug");
        tie(root,cc);check(c.tied==2 && g.tow.loadCount(g.tug)==2,"tying the tail keeps the rest of the train");
        place(b,4);tie(root,b);check(c.tied==3,"middle barge of a train counts");
        place(a,3);tie(root,a);check(c.tied==4 && g.tow.loadCount(g.tug)==0,"every barge of a train counts once tied");
        // Leader first: the follower drifts free and still counts when tied.
        Body lead=c.barges[4],follow=c.barges[5];place(lead,7);place(follow,8);
        g.tug.x=TowingChallenge.BERTH_X;g.tug.y=TowingChallenge.rowY(6);g.tug.vx=g.tug.vy=0;
        g.tow.attach(g,lead,g.tug);g.tow.attach(g,follow,lead);
        tie(root,lead);check(c.tied==5 && g.tow.incoming(follow)==null,"tying a train leader releases its follower");
        place(follow,8);tie(root,follow);check(c.tied==6,"released follower is still counted once tied");
        // A claimed berth is skipped: a hull beside an occupied row targets the nearest free one.
        Body late=c.barges[6];place(late,0);late.x-=60;
        check(c.berthFor(late)!=0 && c.berthFor(late)==1,"occupied berth redirects the next hull to the nearest free berth");
        // Tie the remaining four: ten ends the challenge early with time on the clock.
        int[] rows={1,2,6,9};for(int i=0;i<4;i++){Body hull=c.barges[6+i];place(hull,rows[i]);tie(root,hull);}
        check(c.tied==TowingChallenge.BARGES && c.phase==TowingChallenge.RESULT && c.remaining>0,"all ten tied ends the challenge before time");
        check(g.settings.towingChallengeBest()==TowingChallenge.BARGES,"perfect run is the saved best");
    }
    private static void clock(){
        Game root=new Game();root.configureCanvas(1333,600);Game g=running(root);TowingChallenge c=g.tutorial.challenge;
        int before=g.progressVersion();
        for(int i=0;i<TowingChallenge.TICKS-1;i++)root.step();
        check(c.phase==TowingChallenge.RUN && c.remaining==1 && "0:01".equals(c.clockLabel()),"clock still runs at 59.98 seconds");
        g.pause();for(int i=0;i<120;i++)root.advance(1/60.0);g.pause();
        check(c.remaining==1,"pausing freezes the challenge clock");
        root.step();
        check(c.phase==TowingChallenge.RESULT && c.remaining==0 && "0:00".equals(c.clockLabel()),"the challenge ends exactly at 60 seconds");
        check(g.progressVersion()==before && g.settings.towingChallengeBest()==0,"an empty run records no best");
        for(int i=0;i<TowingChallenge.RESULT_DELAY+20;i++)root.step();
        check(g.dialogue.active && g.dialogue.count()==2,"result offers finish and retry");
        ReplayInput.tap(root,g.dialogue.choiceBox(g,1));for(int i=0;i<30;i++)root.step();
        check(c.phase==TowingChallenge.APPROACH && c.tied==0 && c.remaining==TowingChallenge.TICKS && g.tug.y>TowingChallenge.START_Y,"retry restores ten barges and the start line");
        for(Body b:c.barges)check(b.awaitingPickup && b.x==TowingChallenge.SPAWN_X,"retry re-moors every barge on the west quay");
        int cargo=0;for(Body b:g.cargo)if(b!=null)cargo++;check(cargo==4+TowingChallenge.BARGES,"retry replaces, never duplicates, the challenge hulls");
        // Second run: two tied barges become the saved record, kept across save/restore.
        g.tug.y=TowingChallenge.START_Y-30;root.step();
        for(int i=0;i<2;i++){Body b=c.barges[i];place(b,i);tie(root,b);}
        while(c.phase==TowingChallenge.RUN)root.step();
        check(c.tied==2 && c.newBest() && g.settings.towingChallengeBest()==2 && g.progressVersion()>before,"a better result saves a new best");
        String save=root.saveProgress();check(save.startsWith("BBG4\n") && save.endsWith("\n2"),"best travels in the shared progress snapshot");
        Game restored=new Game();restored.restoreProgress(save);check(restored.settings.towingChallengeBest()==2,"saved best restores");
        restored.restoreProgress(save.substring(0,save.length()-1)+"11");check(restored.settings.towingChallengeBest()==2,"invalid best is rejected atomically");
        Game legacy=new Game();legacy.restoreProgress("BBG2\n"+g.career.save()+"\n1\n100,0,100,0");check(legacy.settings.towingChallengeBest()==0 && legacy.career.save().equals(g.career.save()),"BBG2 saves still restore");
        g.settings.recordTowingChallenge(1);check(g.settings.towingChallengeBest()==2,"a worse run keeps the best");
        check(root.career.save().equals(new Game().career.save()),"practice best never touches career stars");
        for(int i=0;i<TowingChallenge.RESULT_DELAY+20;i++)root.step();
        ReplayInput.tap(root,g.dialogue.choiceBox(g,0));for(int i=0;i<30;i++)root.step();
        check(g.tutorial.lesson==Tutorial.DONE,"finish training leaves the challenge");
    }
    private static void hudAndCamera(){
        for(int width:new int[]{960,1333,1800}){
            Game root=new Game();root.configureCanvas(width,600);Game g=running(root);
            float left=ChallengeView.left(g);
            check(left>=200 && left+ChallengeView.WIDTH<=g.layout.width-200,"scoreboard stays top-center, away from the top-left HUD corner");
            check(ChallengeView.TOP>=56 && ChallengeView.TOP+ChallengeView.HEIGHT<=130,"scoreboard sits directly below the HUD ribbon");
            for(int i=0;i<200;i++)root.step();
            RiverCamera cam=g.layout.camera;
            float sx=cam.screenX(g.tug.x,g.tug.y),sy=cam.screenY(g.tug.x,g.tug.y);
            check(sx>0 && sx<g.layout.width && sy>ChallengeView.TOP+ChallengeView.HEIGHT && sy<g.layout.height,"camera keeps the tug visible below the scoreboard");
            check(cam.zoom<=TowingChallenge.ZOOM+.001f,"large basin uses a transit-scale zoom");
        }
    }
    public static void run(){
        geometry();spawns();counting();clock();hudAndCamera();
        System.out.println(checks+" towing-challenge basin, spawn, tie-off, train, clock and record assertions passed");
    }
    public static void main(String[] args){run();}
}
