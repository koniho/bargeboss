package com.bargeboss.game;

/** Full-hull timed entry, safe cycle, timed exit and deterministic rigid gate checks. */
public final class LockTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static Game fixture(){
        Game g=new Game();g.reset();for(int i=0;i<g.cargo.length;i++)g.cargo[i]=null;
        g.tug.x=300;g.tug.y=600;g.tug.angle=-(float)Math.PI/2;
        g.cargo[0]=new Body(10,0,255,570,-(float)Math.PI/2);
        g.cargo[1]=new Body(11,1,345,570,-(float)Math.PI/2);return g;
    }
    private static void step(Lock lock,Game g,int n){for(int i=0;i<n;i++)lock.step(g);}
    private static void enter(Lock lock,Game g){
        g.tug.y=Lock.ENTRY_Y+120;lock.step(g);step(lock,g,Lock.GATE_TICKS);
        g.tug.y=90;g.cargo[0].y=190;g.cargo[1].y=190;lock.step(g);
    }
    public static void run(){
        Game g=fixture();Lock lock=new Lock();
        check(lock.blocksLine(300,400,300,200),"closed entry blocks tow throws across its face");
        check(!lock.blocksLine(300,500,300,400),"same-side tow throws remain available");
        step(lock,g,60*60*6);check(lock.phase==Lock.READY && lock.secondsRemaining()==0,"preparation is not a timed shift");
        g.cargo[1]=null;g.tug.y=Lock.ENTRY_Y+100;step(lock,g,600);
        check(lock.phase==Lock.READY,"window waits for both feeder loads");
        g.cargo[1]=new Body(11,1,345,570,-(float)Math.PI/2);lock.step(g);
        check(lock.phase==Lock.ENTRY && lock.secondsRemaining()==45,"approach starts a fresh entry window");
        check(!lock.entryPassable() && !lock.exitPassable(),"moving entry and sealed exit remain solid");
        step(lock,g,Lock.GATE_TICKS);check(lock.entryPassable() && !lock.exitPassable(),"entry becomes passable only when fully open");
        check(!lock.blocksLine(300,400,300,200) && lock.blocksLine(300,100,300,-100),"open entry allows a line while closed exit still blocks it");
        g.tug.y=90;g.cargo[0].y=190;g.cargo[1].y=Lock.ENTRY_Y-15;lock.step(g);
        check(lock.phase==Lock.ENTRY && lock.insideCount(g)==1,"barge center inside does not count if stern crosses gate");
        g.cargo[1].y=190;lock.step(g);check(lock.phase==Lock.CLOSING,"both barges and tug fully inside starts closing");
        check(!lock.entryPassable() && !lock.exitPassable(),"cycle seals both ends");
        step(lock,g,Lock.GATE_TICKS);check(lock.phase==Lock.FILLING && lock.waterLevel()==0,"water changes only after entry seals");
        step(lock,g,Lock.CYCLE_TICKS/2);check(Math.abs(lock.waterLevel()-.5f)<.001f && !lock.exitPassable(),"halfway water level stays enclosed");
        step(lock,g,Lock.CYCLE_TICKS/2);check(lock.phase==Lock.OPENING && !lock.exitPassable(),"level matching starts exit opening");
        step(lock,g,Lock.GATE_TICKS);check(lock.phase==Lock.EXIT && lock.secondsRemaining()==35 && lock.exitPassable(),"exit window starts after gate fully opens");
        g.cargo[0].y=-100;g.cargo[1].y=-20;lock.step(g);
        check(lock.clearedCount(g)==1 && !lock.complete(),"whole stern must clear upper gate");
        g.cargo[1].y=-100;lock.step(g);check(lock.complete() && lock.clearedCount(g)==2,"both barges fully out immediately completes objective");
        int ticks=lock.ticks;step(lock,g,6000);check(lock.ticks==ticks && lock.exitPassable(),"completed passage remains safe and stable");

        g=fixture();lock.reset();g.tug.y=Lock.ENTRY_Y+120;lock.step(g);step(lock,g,Lock.ENTRY_TICKS);
        check(lock.failed() && lock.failure().equals("ENTRY WINDOW MISSED"),"missed entry has an explicit retry reason");
        check(lock.waterLevel()==0 && lock.entryOpening()==1,"entry miss freezes machinery without dropping gates on hulls");
        g=fixture();lock.reset();enter(lock,g);step(lock,g,Lock.GATE_TICKS+Lock.CYCLE_TICKS+Lock.GATE_TICKS+Lock.EXIT_TICKS);
        check(lock.failed() && lock.failure().equals("EXIT WINDOW MISSED"),"missed exit has its own deadline");
        check(lock.waterLevel()==1 && lock.exitOpening()==1,"exit failure preserves safe open geometry");
        g=fixture();lock.reset();g.tug.y=Lock.ENTRY_Y+120;lock.step(g);g.cargo[0].active=false;lock.step(g);
        check(lock.failed() && lock.failure().equals("LOCK CARGO MISSING"),"a missing target cannot silently complete");

        for(int frame=0;frame<32;frame++)for(int gate=0;gate<2;gate++)for(int side=-1;side<=1;side+=2){
            lock.reset();float y=gate==0?Lock.EXIT_Y:Lock.ENTRY_Y;
            Body b=new Body(10,0,300,y+side*3,frame*(float)Math.PI/16);b.vy=-side*90;
            float impact=lock.collide(b);check(impact>0,"closed gate stops inward motion for every heading and side");
            check(side>0?Lock.front(b)>=y+Lock.GATE_HALF-.001f:Lock.back(b)<=y-Lock.GATE_HALF+.001f,"all oriented hull discs resolve outside closed gate");
            float old=b.y;lock.collide(b);check(Math.abs(old-b.y)<.001f,"gate contact does not creep on repeat resolution");
        }
        g=fixture();lock.reset();enter(lock,g);lock.phase=Lock.EXIT;lock.ticks=15;
        Body free=new Body(13,0,300,Lock.EXIT_Y,-(float)Math.PI/2);free.vy=-50;
        check(lock.collide(free)==0 && free.y==Lock.EXIT_Y,"open exit permits full hull traversal");
        int phase=lock.phase,lt=lock.ticks,gt=g.ticks;float level=lock.waterLevel();
        g.layout=Layout.focused(1333,600,300,180,1.4f,650,360);
        Capture a=new Capture(),b=new Capture();LockView.water(g,lock,a);LockView.draw(g,lock,a);LockView.water(g,lock,b);LockView.draw(g,lock,b);
        check(a.hash==b.hash && a.calls==b.calls && a.calls>0,"same lock pose draws identical geometry and effects");
        check(lock.phase==phase && lock.ticks==lt && g.ticks==gt && lock.waterLevel()==level,"drawing lock never advances clocks or water state");
        for(int gate=0;gate<2;gate++)for(int sample=0;sample<=4;sample++){
            ArticulatedModel m=LockView.model(gate,sample/4f);ArticulatedModel.Pose pose=m.pose(0);
            float y=LockView.gateY(gate),half=(Physics.RIGHT-Physics.LEFT)*.5f;
            float[] hinge=pose.point(m.nodeId("left-leaf"),0,0,0);
            check(Math.abs(hinge[0]-Physics.LEFT)<.001f && Math.abs(hinge[1]-y)<.001f,"left leaf retains bank hinge at every opening");
            float[] far=pose.point(m.nodeId("left-leaf"),half,0,0);
            check(Math.abs(Physics.length(far[0]-hinge[0],far[1]-hinge[1])-half)<.001f,"rigid leaf never stretches during opening");
            if(sample==4)check(Math.abs(far[0]-Physics.LEFT)<.001f && (gate==0?far[1]<y:far[1]>y),"open leaf lies along correct bank side");
        }
        System.out.println(checks+" lock window, full-hull clearance, safe cycle and rigid gate assertions passed");
    }
    private static final class Capture implements Painter{
        int hash=1,calls;
        void add(float value){hash=hash*31+Float.floatToIntBits(value);}
        void mark(int color){hash=hash*31+color;calls++;}
        public void rect(float x,float y,float w,float h,int c){mark(c);add(x);add(y);add(w);add(h);}
        public void circle(float x,float y,float r,int c){mark(c);add(x);add(y);add(r);}
        public void line(float x,float y,float xx,float yy,float w,int c){mark(c);add(x);add(y);add(xx);add(yy);add(w);}
        public void poly(float[] xy,int c){mark(c);for(float v:xy)add(v);}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){mark(a);add(f);add(x);add(y);add(w);add(h);add(r);add(alpha);}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){sprite(a,f,x,y,w,h,r,alpha);add(ff);add(mix);}
    }
    public static void main(String[] args){run();}
}
