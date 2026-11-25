package com.bargeboss.game;

/** Chicago Harbor Lock: surveyed sector-gate leaves, a levelling chamber and
 * approach-triggered cycles. Leaves rotate about their wall hinges; the same pose
 * drives collision, tow-line blocking and the rigid gate models. No timing is random. */
final class HarborLock {
    static final int IDLE=0,OPENING=1,OPEN=2,CLOSING=3,LEVELLING=4;
    static final int RIVER=0,LAKE=1,GATE_TICKS=4*60,LEVEL_TICKS=9*60,CLEAR_TICKS=90,SETTLE_TICKS=45;
    /** Approach trigger: 210 m of channel either side of the chamber. */
    static final float APPROACH=210*ChicagoSurvey.UNITS_PER_METRE,LEAF_HALF=2.6f,LIFT_METRES=.6f;
    /** Gate 0 is the river (west) gate, gate 1 the lake (east) gate. */
    final float[][] hinge=new float[2][4],apex=new float[2][2];
    final float axisX,axisY,originX,originY,length,halfWidth;
    final float[] leafLength=new float[4],closedAngle=new float[4],openAngle=new float[4];
    int phase,gate=-1,target,ticks,entry=-1;
    /** 0 is river level, 1 is lake level. Gate openings are eased progress values. */
    float level,partyTicks;
    final float[] opening=new float[2];
    HarborLock(){
        float[][] source={ChicagoSurvey.LOCK_WEST,ChicagoSurvey.LOCK_EAST};
        for(int k=0;k<2;k++){float[] s=source[k];
            hinge[k][0]=s[0];hinge[k][1]=s[1];apex[k][0]=s[2];apex[k][1]=s[3];hinge[k][2]=s[4];hinge[k][3]=s[5];}
        float wx=(hinge[0][0]+hinge[0][2])*.5f,wy=(hinge[0][1]+hinge[0][3])*.5f,ex=(hinge[1][0]+hinge[1][2])*.5f,ey=(hinge[1][1]+hinge[1][3])*.5f;
        float len=Physics.length(ex-wx,ey-wy);axisX=(ex-wx)/len;axisY=(ey-wy)/len;originX=wx;originY=wy;length=len;
        halfWidth=(Physics.length(hinge[0][2]-hinge[0][0],hinge[0][3]-hinge[0][1])+Physics.length(hinge[1][2]-hinge[1][0],hinge[1][3]-hinge[1][1]))*.25f;
        for(int k=0;k<2;k++)for(int side=0;side<2;side++){int leaf=k*2+side;float hx=hinge[k][side*2],hy=hinge[k][side*2+1];
            leafLength[leaf]=Physics.length(apex[k][0]-hx,apex[k][1]-hy);
            closedAngle[leaf]=(float)StrictMath.atan2(apex[k][1]-hy,apex[k][0]-hx);
            // Sector leaves retract along the wall toward the lake side, as the mapped apex leans.
            openAngle[leaf]=(float)StrictMath.atan2(axisY,axisX);
        }
        reset();
    }
    void reset(){phase=IDLE;gate=-1;target=RIVER;ticks=0;entry=-1;level=0;partyTicks=0;opening[0]=opening[1]=0;}
    float along(float x,float y){return (x-originX)*axisX+(y-originY)*axisY;}
    float across(float x,float y){return -(x-originX)*axisY+(y-originY)*axisX;}
    float centerX(){return originX+axisX*length*.5f;}
    float centerY(){return originY+axisY*length*.5f;}
    float gateX(int k){return (hinge[k][0]+hinge[k][2])*.5f;}
    float gateY(int k){return (hinge[k][1]+hinge[k][3])*.5f;}
    static float ease(float t){t=Physics.clamp(t,0,1);return t*t*(3-2*t);}
    float leafAngle(int leaf){float a=closedAngle[leaf],b=openAngle[leaf],d=b-a;
        while(d>Math.PI)d-=2*(float)Math.PI;while(d<-Math.PI)d+=2*(float)Math.PI;return a+d*ease(opening[leaf/2]);}
    float leafX(int leaf){return hinge[leaf/2][(leaf%2)*2];}
    float leafY(int leaf){return hinge[leaf/2][(leaf%2)*2+1];}
    float tipX(int leaf){return leafX(leaf)+(float)StrictMath.cos(leafAngle(leaf))*leafLength[leaf];}
    float tipY(int leaf){return leafY(leaf)+(float)StrictMath.sin(leafAngle(leaf))*leafLength[leaf];}
    boolean passable(int k){return opening[k]>=1;}
    /** Every hull disc lies between the gate hinge lines and the chamber walls. Hulls
     * may rest against a closed far gate; the swing test below protects the near gate. */
    boolean inside(Body b){if(b==null || !b.active)return false;
        for(int i=0;i<3;i++){float s=along(b.discX(i),b.discY(i)),a=across(b.discX(i),b.discY(i));
            if(s<0 || s>length || Math.abs(a)>halfWidth+6)return false;}
        return true;
    }
    boolean nearChamber(Body b){float s=along(b.x,b.y);return s>-80 && s<length+80 && Math.abs(across(b.x,b.y))<halfWidth+40;}
    int side(float x,float y){return along(x,y)<length*.5f?RIVER:LAKE;}
    float levelOf(int side){return side==LAKE?1:0;}
    private boolean towed(Game g,Body b){for(int j=0;j<Towline.CAPACITY;j++){Towline line=g.tow.link(j);if(line!=null && line.cargo==b)return true;}return false;}
    private boolean partyInside(Game g){if(!inside(g.tug))return false;
        for(Body b:g.cargo)if(b!=null && b.active && towed(g,b) && !inside(b))return false;return true;}
    /** Enclosed and clear of the swing of gate k, so it can close behind the party. */
    private boolean partyInside(Game g,int k){if(!partyInside(g) || sweeps(g.tug,k))return false;
        for(Body b:g.cargo)if(b!=null && b.active && towed(g,b) && sweeps(b,k))return false;return true;}
    private boolean anyInChamber(Game g){if(nearChamber(g.tug))return true;for(Body b:g.cargo)if(b!=null && b.active && nearChamber(b))return true;return false;}
    private boolean partyClear(Game g,int k){if(nearGate(g.tug,k,90))return false;
        for(Body b:g.cargo)if(b!=null && b.active && nearGate(b,k,70))return false;
        return !anyInChamber(g);
    }
    private boolean nearGate(Body b,int k,float distance){float s=along(b.x,b.y)-(k==0?0:length);return Math.abs(s)<distance+b.half+b.radius && Math.abs(across(b.x,b.y))<halfWidth+40;}
    /** Approaching from outside, bow toward the chamber (or already close to the gate). */
    private int request(Game g){Body t=g.tug;float s=along(t.x,t.y),a=across(t.x,t.y);if(Math.abs(a)>halfWidth+160)return -1;
        float hx=(float)StrictMath.cos(t.angle),hy=(float)StrictMath.sin(t.angle),toward=hx*axisX+hy*axisY;
        if(s<0 && s>-APPROACH && (toward>.35f || s>-140))return RIVER;
        if(s>length && s<length+APPROACH && (toward<-.35f || s<length+140))return LAKE;
        return -1;
    }
    /** The open gate is the far side of the chamber the party entered. */
    boolean exiting(){return entry>=0 && gate==1-entry;}
    private void change(Game g,int next,String words){phase=next;ticks=0;if(words!=null){g.tell(words);g.haptic(Game.HAPTIC_TICK);}}
    void step(Game g){ticks++;
        switch(phase){
            case IDLE:{
                if(partyInside(g) && entry>=0){target=1-entry;change(g,LEVELLING,target==RIVER?"LOCK LOWERING TO RIVER / HOLD POSITION":"LOCK RAISING TO LAKE / HOLD POSITION");break;}
                int want=request(g);if(want<0)break;
                if(level==levelOf(want)){gate=want;change(g,OPENING,(want==LAKE?"LAKE":"RIVER")+" GATES OPENING / WAIT FOR GREEN");}
                else if(!anyInChamber(g)){target=want;change(g,LEVELLING,"LOCK LEVELLING FOR YOUR APPROACH");}
                break;}
            case LEVELLING:{
                float goal=levelOf(target),step=1f/LEVEL_TICKS;
                level=goal>level?Math.min(goal,level+step):Math.max(goal,level-step);
                if(level==goal){gate=target;change(g,OPENING,(target==LAKE?"LAKE":"RIVER")+" GATES OPENING");}
                break;}
            case OPENING:
                opening[gate]=Math.min(1,opening[gate]+1f/GATE_TICKS);
                if(opening[gate]>=1)change(g,OPEN,exiting()?"GREEN / CLEAR THE LOCK":gate==LAKE?"GREEN / ENTER FROM THE LAKE":"GREEN / ENTER FROM THE RIVER");
                break;
            case OPEN:
                // Leaving the chamber completes an exit even while the gate is still open.
                if(exiting() && !anyInChamber(g))entry=-1;
                if(!exiting() && partyInside(g,gate)){if(++partyTicks>=SETTLE_TICKS){entry=gate;partyTicks=0;change(g,CLOSING,"ALL ABOARD / GATES CLOSING");}}
                else{partyTicks=0;
                    if(partyClear(g,gate) && ticks>CLEAR_TICKS){entry=-1;change(g,CLOSING,null);}
                }
                break;
            case CLOSING:
                for(Body b:new Body[]{g.tug})if(sweeps(b,gate)){change(g,OPENING,"GATE HELD / HULL IN THE GATE");return;}
                for(Body b:g.cargo)if(b!=null && b.active && sweeps(b,gate)){change(g,OPENING,"GATE HELD / HULL IN THE GATE");return;}
                opening[gate]=Math.max(0,opening[gate]-1f/GATE_TICKS);
                if(opening[gate]<=0){gate=-1;change(g,IDLE,null);}
                break;
            default:break;
        }
    }
    /** A hull inside the swept quarter-circle of a moving leaf holds the gate open. */
    private boolean sweeps(Body b,int k){for(int side=0;side<2;side++){int leaf=k*2+side;
        for(int i=0;i<3;i++){float dx=b.discX(i)-leafX(leaf),dy=b.discY(i)-leafY(leaf);
            if(Physics.length(dx,dy)>leafLength[leaf]+b.radius)continue;
            float s=along(b.discX(i),b.discY(i))-(k==0?0:length);
            if(s>-b.radius-4 && s<leafLength[leaf]*.9f+b.radius)return true;}}
        return false;
    }
    /** Sluice swirl while the chamber levels; shared deterministic field. */
    float currentX(float x,float y){return flow(x,y,true);}
    float currentY(float x,float y){return flow(x,y,false);}
    private float flow(float x,float y,boolean xAxis){
        if(phase!=LEVELLING)return 0;float s=along(x,y),a=across(x,y);
        if(s<0 || s>length || Math.abs(a)>halfWidth+8)return 0;
        float ends=Physics.clamp(Math.min(s,length-s)/60,0,1),progress=Math.abs(level-.5f)*2;
        float push=(target==LAKE?-1:1)*18*ends*(float)StrictMath.sin(Math.PI*(1-progress))*(float)StrictMath.sin(s*.03+ticks*.02);
        float sway=9*ends*(float)StrictMath.sin(a*.08+ticks*.017);
        float wx=axisX*push-axisY*sway,wy=axisY*push+axisX*sway;return xAxis?wx:wy;
    }
    void integrate(Body b){Physics.integrate(b,currentX(b.x,b.y),currentY(b.x,b.y));}
    boolean blocksLine(float x,float y,float xx,float yy){
        for(int leaf=0;leaf<4;leaf++){if(passable(leaf/2))continue;
            if(intersects(x,y,xx,yy,leafX(leaf),leafY(leaf),tipX(leaf),tipY(leaf)))return true;}
        return false;
    }
    private static boolean intersects(float ax,float ay,float bx,float by,float cx,float cy,float dx,float dy){
        float d1=PolygonWater.cross(cx,cy,dx,dy,ax,ay),d2=PolygonWater.cross(cx,cy,dx,dy,bx,by),d3=PolygonWater.cross(ax,ay,bx,by,cx,cy),d4=PolygonWater.cross(ax,ay,bx,by,dx,dy);
        return (d1>0)!=(d2>0) && (d3>0)!=(d4>0);
    }
    /** Every leaf is solid in every pose; stowed leaves lie along the chamber walls. */
    float collide(Body b,CollisionSplashes splashes){if(b==null || !b.active || !near(b))return 0;float impact=0;
        for(int leaf=0;leaf<4;leaf++)impact=Math.max(impact,segment(b,leafX(leaf),leafY(leaf),tipX(leaf),tipY(leaf),splashes));
        return impact;
    }
    private boolean near(Body b){float s=along(b.x,b.y);return s>-120 && s<length+120 && Math.abs(across(b.x,b.y))<halfWidth+120;}
    private static float segment(Body b,float ax,float ay,float bx,float by,CollisionSplashes splashes){float impact=0,dx=bx-ax,dy=by-ay,len=dx*dx+dy*dy;
        for(int i=0;i<3;i++){float x=b.discX(i),y=b.discY(i),t=Physics.clamp(((x-ax)*dx+(y-ay)*dy)/len,0,1),qx=ax+dx*t,qy=ay+dy*t;
            float ox=x-qx,oy=y-qy,d=Physics.length(ox,oy),reach=b.radius+LEAF_HALF;if(d>=reach)continue;
            float nx,ny;if(d<.001f){float l=(float)StrictMath.sqrt(len);nx=-dy/l;ny=dx/l;if(nx*(b.x-qx)+ny*(b.y-qy)<0){nx=-nx;ny=-ny;}}else{nx=ox/d;ny=oy/d;}
            Physics.wallSplash(splashes,b,qx+nx*LEAF_HALF,qy+ny*LEAF_HALF,nx,ny,x,y);
            b.x+=nx*(reach-d);b.y+=ny*(reach-d);float inward=b.vx*nx+b.vy*ny;
            if(inward<0){impact=Math.max(impact,-inward);b.vx-=inward*nx*1.08f;b.vy-=inward*ny*1.08f;}b.spin*=.8f;
        }return impact;
    }
    /** Signal lamps: green only for a fully open gate whose chamber matches that side. */
    boolean green(int k){return gate==k && phase==OPEN;}
    String signal(){
        if(phase==LEVELLING)return (target==LAKE?"RAISING":"LOWERING")+" "+(int)(LIFT_METRES*100)+" CM / "+(int)StrictMath.ceil(Math.abs(levelOf(target)-level)*LEVEL_TICKS/60f)+"S";
        if(phase==OPENING)return "GATES OPENING";
        if(phase==CLOSING)return "GATES CLOSING";
        if(phase==OPEN)return gate==LAKE?"LAKE GATES OPEN":"RIVER GATES OPEN";
        return "LOCK AT "+(level>.5f?"LAKE":"RIVER")+" LEVEL";
    }
}
