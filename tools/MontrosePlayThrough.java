package com.bargeboss.game;
import java.awt.*;import java.awt.image.BufferedImage;import javax.imageio.ImageIO;import java.io.File;import java.util.*;

/** Usage (from the repo root, after compiling core, tests, desktop, developer/shared and tools/DialogueReview.java):
 *   java -cp <classes> com.bargeboss.game.MontrosePlayThrough out/montrose-play.png [jobId...]
 * Plays the six new Montrose jobs through public touch input only (joystick, lasso drag, tow-release
 * tap, ability taps) with an A* pilot, and reports time, hull, damage, near misses and outcome. */
public final class MontrosePlayThrough {
    static final float X0=-700,Y0=-4500,CELL=20;static final int W=(int)((3600-X0)/CELL),H=(int)((1150-Y0)/CELL);
    static boolean[] free;
    static float clr(Game g,float x,float y,float r){
        for(int k=0;k<12;k++){double a=k*Math.PI/6;if(!MontroseHarbor.contains(x+r*(float)Math.cos(a),y+r*(float)Math.sin(a)))return 0;}
        if(!MontroseHarbor.contains(x,y))return 0;
        // Docks and fingers: a probe hull of radius r must not be pushed by the harbor walls.
        for(int k=0;k<4;k++){Body probe=new Body(-1,-1,x,y,k*(float)Math.PI/4);probe.vx=probe.vy=0;float px=probe.x,py=probe.y;MontroseHarbor.collide(probe,null);if(Physics.length(probe.x-px,probe.y-py)>.01f)return 0;}
        for(MooredFleet.FloatBoat b:g.mooredFleet.boats)if(Math.abs(x-b.x)<90 && Math.abs(y-b.y)<90 && Physics.length(x-b.x,y-b.y)<b.berth.length*.5f+r)return 0;
        return 1;
    }
    static void grid(Game g){free=new boolean[W*H];for(int j=0;j<H;j++)for(int i=0;i<W;i++)free[j*W+i]=clr(g,X0+i*CELL+CELL/2,Y0+j*CELL+CELL/2,26)>0;}
    static int idx(float x,float y){int i=Math.max(0,Math.min(W-1,(int)((x-X0)/CELL))),j=Math.max(0,Math.min(H-1,(int)((y-Y0)/CELL)));return j*W+i;}
    static boolean los(float ax,float ay,float bx,float by){float len=Physics.length(bx-ax,by-ay);for(float t=0;t<=len;t+=CELL*.5f){if(!free[idx(ax+(bx-ax)*t/len,ay+(by-ay)*t/len)])return false;}return true;}
    static float[] path(float sx,float sy,float tx,float ty){
        if(los(sx,sy,tx,ty))return new float[]{tx,ty};
        int s=idx(sx,sy),t=idx(tx,ty);float[] gc=new float[W*H];Arrays.fill(gc,Float.MAX_VALUE);int[] from=new int[W*H];boolean[] done=new boolean[W*H];
        PriorityQueue<float[]> q=new PriorityQueue<>((u,v)->Float.compare(u[0],v[0]));gc[s]=0;q.add(new float[]{0,s});
        while(!q.isEmpty()){int c=(int)q.poll()[1];if(done[c])continue;done[c]=true;if(c==t)break;int ci=c%W,cj=c/W;
            for(int dj=-1;dj<=1;dj++)for(int di=-1;di<=1;di++){if(di==0&&dj==0)continue;int ni=ci+di,nj=cj+dj;if(ni<0||nj<0||ni>=W||nj>=H)continue;int n=nj*W+ni;if(done[n]||!free[n]&&n!=t&&c!=s)continue;
                float ng=gc[c]+(float)Math.hypot(di,dj);if(ng<gc[n]){gc[n]=ng;from[n]=c;q.add(new float[]{ng+(float)Math.hypot(ni-t%W,nj-t/W),n});}}}
        if(!done[t])return new float[]{tx,ty};
        java.util.List<float[]> pts=new ArrayList<>();for(int c=t;c!=s;c=from[c])pts.add(0,new float[]{X0+(c%W)*CELL+CELL/2,Y0+(c/W)*CELL+CELL/2});
        // Furthest waypoint still in line of sight.
        float[] best=pts.get(0);for(float[] p:pts)if(los(sx,sy,p[0],p[1]))best=p;else break;
        return best;
    }
    static int stuck,backing;
    static void drive(Game g,float tx,float ty,float speed){
        // Unstick like a player: pinned while throttling -> back off for a second, slightly turned.
        if(backing>0){if(--backing==0)lift(g);float c=(float)Math.cos(g.tug.angle+.6f),s=(float)Math.sin(g.tug.angle+.6f);ReplayInput.throttle(g,-c*70,-s*70);return;}
        if(g.tug.speed()<4 && Physics.length(tx-g.tug.x,ty-g.tug.y)>60){if(++stuck>90){stuck=0;backing=60;lift(g);}}else stuck=0;
        float[] w=path(g.tug.x,g.tug.y,tx,ty);float dx=w[0]-g.tug.x,dy=w[1]-g.tug.y,len=Physics.length(dx,dy);
        if(g.pointer && g.joystick!=null && !g.joystick.active){ReplayInput.up(g,g.pointerX,g.pointerY);}
        if(len<10 && Physics.length(tx-g.tug.x,ty-g.tug.y)<10){ReplayInput.throttle(g,0,0);return;}
        ReplayInput.throttle(g,dx/len*speed,dy/len*speed);
    }
    /** Towing: steer to a point beyond the target so the trailing hull is pulled across it, easing off near it. */
    static void across(Game g,Body hull,float tx,float ty,float d){
        float dx=tx-hull.x,dy=ty-hull.y,len=Math.max(1,Physics.length(dx,dy));
        if(d<260){tx+=dx/len*110;ty+=dy/len*110;}
        drive(g,tx,ty,d<150?30:d<300?45:60);
    }
    static void stop(Game g){ReplayInput.throttle(g,0,0);}
    static void lift(Game g){if(g.pointer)g.pointerCancel(0);}
    static void lasso(Game g,Body b){lift(g);ReplayInput.down(g,b.x,b.y);ReplayInput.move(g,g.tug.x,g.tug.y);ReplayInput.up(g,g.tug.x,g.tug.y);}
    static void release(Game g,Body b){lift(g);ReplayInput.down(g,b.x,b.y);ReplayInput.up(g,b.x,b.y);}

    static BufferedImage sheet=new BufferedImage(3*444,4*250,BufferedImage.TYPE_INT_RGB);static int shots;
    static void shot(Game g,String label){
        if(shots>=12)return;BufferedImage im=new BufferedImage((int)g.layout.width,(int)g.layout.height,BufferedImage.TYPE_INT_ARGB);Graphics2D p=im.createGraphics();
        p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.render(new DesktopPainter(p));p.dispose();
        Graphics2D ink=sheet.createGraphics();ink.drawImage(im,(shots%3)*444,(shots/3)*250,444,250,null);ink.setColor(Color.YELLOW);ink.drawString(label,(shots%3)*444+6,(shots/3)*250+246);shots++;
    }

    public static void main(String[] a)throws Exception{
        int[] all={Challenges.WAKE_PATROL,Challenges.LOOSE_BOATS,Challenges.WATER_TAXI,Challenges.SANDBAR,Challenges.REGATTA,Challenges.FIREWORKS_NIGHT};int[] ids=all;
        if(a.length>1){ids=new int[a.length-1];for(int i=1;i<a.length;i++)ids[i-1]=Integer.parseInt(a[i]);}
        for(int id:ids){
            Game g=ReplayInput.montroseJob(id);g.configureCanvas(1333,750);g.mode=Game.PLAY;g.dialogue.close(g);
            MontroseJob.start(g,g.tug.x,g.tug.y,g.tug.angle);grid(g);
            int startHull=g.hull,frames=0,max=60*300;boolean shotMid=false;
            while(frames<max && g.challengeId==id && !g.goalFailed() && g.mode==Game.PLAY){
                if(g.dialogue.active){g.dialogue.close(g);}
                if(g.ropePhase==0)pilot(g,id);
                g.step();frames++;
                if(TRACE && frames%600==0)System.out.println("  t="+frames/60+" tug "+(int)g.tug.x+","+(int)g.tug.y+" v="+(int)g.tug.speed()+" rope="+g.ropePhase+" | "+g.goalStatus());
                if(!shotMid && g.delivered>=Math.max(1,g.quota()/2)){shot(g,Challenges.title(id)+" midway");shotMid=true;}
            }
            boolean done=g.challengeId!=id || g.montroseComplete;
            if(!done)shot(g,Challenges.title(id)+" STOPPED");
            System.out.printf("%-16s %s in %5.1fs  progress %d/%d  hull %d->%d  others' damage %d  near misses %d  score %d%n",
                Challenges.title(id),done?"DONE  ":g.goalFailed()?"FAILED":"TIMEOUT",frames/60f,done?Challenges.quota(id):g.delivered,Challenges.quota(id),startHull,g.hull,g.othersDamage,g.arcade.nearMisses,g.score);
        }
        ImageIO.write(sheet,"png",new File(a[0]));
    }
    static boolean TRACE=false;static int lassoCooldown;
    static void pilot(Game g,int id){
        if(lassoCooldown>0)lassoCooldown--;
        MontroseJob job=g.job;if(job==null)return;
        if(id==Challenges.WAKE_PATROL){
            WakePatrolJob w=(WakePatrolJob)job;Body t=null;float best=1e9f;
            for(int i=0;i<w.speeders.length;i++){Body b=w.speeders[i];if(!b.active||w.ticketed[i])continue;float d=Physics.length(b.x-g.tug.x,b.y-g.tug.y);if(d<best){best=d;t=b;}}
            if(t==null){stop(g);return;}
            // Lead the target a little.
            float lead=Math.min(1.2f,best/120f);drive(g,t.x+t.vx*lead,t.y+t.vy*lead,90);return;
        }
        if(id==Challenges.LOOSE_BOATS){
            LooseBoatsJob l=(LooseBoatsJob)job;
            for(int i=0;i<LooseBoatsJob.BOATS;i++){Body b=l.boats[i];if(b.deliveryRecorded||l.warp[i]>0||!g.tow.connected(b,g.tug))continue;
                MooredBoats.Berth s=l.slips[i];float d=Physics.length(b.x-s.x,b.y-s.y);
                across(g,b,s.x,s.y,d);return;}
            Body t=null;float best=1e9f;
            for(int i=0;i<LooseBoatsJob.BOATS;i++){Body b=l.boats[i];if(b.deliveryRecorded||l.warp[i]>0)continue;float d=Physics.length(b.x-g.tug.x,b.y-g.tug.y);if(d<best){best=d;t=b;}}
            if(t==null){stop(g);return;}
            if(best<130 && lassoCooldown==0 && g.canTow(t)){lasso(g,t);lassoCooldown=40;return;}
            drive(g,t.x,t.y,best<200?50:90);return;
        }
        if(id==Challenges.WATER_TAXI){
            WaterTaxiJob w=(WaterTaxiJob)job;int near=-1;float best=1e9f;
            for(int s=0;s<w.waiting.length;s++)if(w.waiting[s]>0){float d=Physics.length(WaterTaxiJob.PICKUPS[s*2]-g.tug.x,WaterTaxiJob.PICKUPS[s*2+1]-g.tug.y);if(d<best){best=d;near=s;}}
            boolean pier=w.aboard>=WaterTaxiJob.SEATS || w.aboard>0 && (near<0 || g.quota()-g.delivered<=w.aboard);
            float tx=pier?WaterTaxiJob.PIER_X:near<0?WaterTaxiJob.PIER_X:WaterTaxiJob.PICKUPS[near*2],ty=pier?WaterTaxiJob.PIER_Y:near<0?WaterTaxiJob.PIER_Y:WaterTaxiJob.PICKUPS[near*2+1];
            float d=Physics.length(tx-g.tug.x,ty-g.tug.y);
            if(d<WaterTaxiJob.REACH-20){stop(g);return;}
            drive(g,tx,ty,d<180?45:95);return;
        }
        if(id==Challenges.SANDBAR){
            SandbarJob s=(SandbarJob)job;Body y=s.yacht;
            if(y.deliveryRecorded||s.warp>0){stop(g);return;}
            boolean towed=g.tow.connected(y,g.tug);
            if(!towed){float d=Physics.length(y.x-g.tug.x,y.y-g.tug.y);if(d<140 && lassoCooldown==0 && g.canTow(y)){lasso(g,y);lassoCooldown=40;return;}drive(g,y.x-60,y.y+110,d<220?45:90);return;}
            if(s.stuck){float dx=g.tug.x-y.x,dy=g.tug.y-y.y,len=Math.max(1,Physics.length(dx,dy));lift(g);ReplayInput.throttle(g,dx/len*90,dy/len*90);return;}
            float d=Physics.length(y.x-SandbarJob.DOCK_X,y.y-SandbarJob.DOCK_Y);
            across(g,y,SandbarJob.DOCK_X,SandbarJob.DOCK_Y,d);return;
        }
        if(id==Challenges.REGATTA){
            RegattaJob r=(RegattaJob)job;if(r.next>=r.gates()){stop(g);return;}
            drive(g,RegattaJob.GATES[r.next*2],RegattaJob.GATES[r.next*2+1],95);return;
        }
        FireworksNightJob f=(FireworksNightJob)job;Body b=f.barge;
        if(f.onStation){
            if(g.abilities.selected!=b || g.abilities.visibleTicks==0){release(g,b);return;}
            if(b.abilityCooldown==0)ReplayInput.tap(g,g.abilities.buttonX(g,0),g.abilities.buttonY(g));
            return;
        }
        boolean towed=g.tow.connected(b,g.tug);
        if(!towed){float d=Physics.length(b.x-g.tug.x,b.y-g.tug.y);if(d<130 && lassoCooldown==0 && g.canTow(b)){lasso(g,b);lassoCooldown=40;return;}drive(g,b.x,b.y+90,d<200?45:90);return;}
        float d=Physics.length(b.x-FireworksNightJob.MARK_X,b.y-FireworksNightJob.MARK_Y);
        across(g,b,FireworksNightJob.MARK_X,FireworksNightJob.MARK_Y,d);
    }
}
