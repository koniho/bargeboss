package com.bargeboss.game;

/** Real linking gestures, graph safety, load physics and per-link depth/release behavior. */
public final class TowTrainTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Game fixture(int loads) {
        Game g=new Game();g.challengeId=1;g.reset();g.bridge.phase=Bridge.OPEN;
        for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}
        g.tug=new Body(0,-1,300,100,-(float)Math.PI/2);
        for(int i=0;i<loads;i++){g.addCargo(2,300,100+110*(i+1));g.cargo[i].angle=-(float)Math.PI/2;}
        return g;
    }
    private static void steps(Game g,int n){for(int i=0;i<n;i++)g.step();}
    private static void cast(Game g,Body b,Body parent) {
        g.down(b.x,b.y);g.move(parent.x,parent.y);
        check(g.towAiming && g.tow.incoming(b)==null,"drag arms link without attaching before release");
        g.up(parent.x,parent.y);check(g.ropePhase==1 && g.ropeTowTarget==parent,"release locks the chosen vessel target");
        steps(g,Game.ROPE_FLIGHT_TICKS);check(g.tow.incoming(b)!=null && g.tow.incoming(b).leader==parent,"throw catches selected leader");
        steps(g,Game.ROPE_HIT_TICKS);
    }
    private static float cruise(int count) {
        Game g=fixture(count);for(int i=0;i<count;i++)g.tow.attach(g,g.cargo[i],i==0?g.tug:g.cargo[i-1]);
        g.down(g.tug.x,g.tug.y);g.move(g.downX,g.downY-100);
        float total=0;for(int i=0;i<180;i++){g.move(g.downX,g.downY-100);g.step();if(i>=150)total+=g.tug.speed();}
        check(g.tow.loadCount(g.tug)==count,"train remains intact under power");return total/30;
    }
    public static void run() {
        Game g=fixture(3);Body a=g.cargo[0],b=g.cargo[1],c=g.cargo[2];
        cast(g,a,g.tug);cast(g,b,a);cast(g,c,b);
        check(g.tow.count()==3 && g.tow.loadCount(g.tug)==3,"three-barge train connects to tug through parent links");
        check(!g.tow.canAttach(g,a,c) && !g.tow.canAttach(g,a,a),"cycles, self-links and duplicate incoming lines rejected");
        float avx=b.vx,avy=b.vy;g.down(b.x,b.y);g.up(b.x,b.y);
        check(g.tow.incoming(b)==null && g.tow.incoming(c).leader==b && g.tow.loadCount(g.tug)==1,"tap middle barge uncouples its incoming line while downstream train remains linked");
        check(b.vx==avx && b.vy==avy,"uncoupling preserves momentum");
        g.tow.detachBody(g,a);check(g.tow.loadCount(g.tug)==0,"mooring releases incident links without dragging secured hull");
        check(g.tow.attach(g,b,g.tug) && g.tow.loadCount(g.tug)==2,"reconnecting the train head brings downstream load back onto tug");
        b.active=false;g.tow.prune(g);check(g.tow.count()==0,"expired parent removes both incident links in the same tick");
        g=fixture(2);a=g.cargo[0];b=g.cargo[1];
        check(g.tow.attach(g,b,a) && !g.tow.canAttach(g,a,b),"prebuilt floating train cannot form a cycle");
        check(g.tow.attach(g,a,g.tug) && g.tow.loadCount(g.tug)==2,"prebuilt train can subsequently be connected to tug");
        g=fixture(2);b=g.cargo[1];b.x=370;b.y=100;cast(g,g.cargo[0],g.tug);cast(g,b,g.tug);
        check(g.tow.loadCount(g.tug)==2 && g.tow.incoming(b).leader==g.tug,"multiple independent tug-to-barge lines supported");
        g.background();float bx=b.x,by=b.y;steps(g,50);check(b.x==bx && b.y==by && g.tow.count()==2,"pause preserves the complete link graph");
        g.pause();g.configureCanvas(1333,600);check(g.tow.count()==2,"resize and resume preserve links");
        for(int heading=0;heading<32;heading++) {
            g.tug.angle=heading*(float)Math.PI/16;b.angle=-g.tug.angle;g.cargo[0].angle=g.tug.angle+1;
            TowView.Segments spans=new TowView.Segments(g);check(spans.count==48,"every link contributes its own 24 spans");
            float last=-Float.MAX_VALUE;for(int i=0;i<spans.count;i++){float depth=spans.depth(g.layout.camera,i);check(depth>=last,"all links share one stable scene depth sort");last=depth;}
            check(Parity.frame(g)==Parity.frame(g),"multi-rope rendering is pure at every orientation");
        }
        g.reset();check(g.tow.count()==0,"reset drops all links");
        float empty=cruise(0),one=cruise(1),two=cruise(2),three=cruise(3);
        check(empty>one*1.08f && one>two*1.08f && two>three*1.05f,"actual cruising speed decreases with each additional barge: "+empty+","+one+","+two+","+three);
        System.out.println(checks+" barge train gesture, graph, load speed and depth assertions passed; speeds "+empty+" / "+one+" / "+two+" / "+three);
    }
    public static void main(String[] args){run();}
}
