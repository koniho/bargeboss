package com.bargeboss.game;

/** Water strokes remain inside real scenery footprints, including joined canal reaches. */
public final class WaterEffectsTest {
    private static int checks;
    private static void check(boolean value,String message) {checks++;if(!value) throw new AssertionError(message);}
    private static final class Capture implements Painter {
        Game game;double area;long hash=1;int polygons;
        Capture(Game game) {this.game=game;}
        public void poly(float[] points,int color) {
            polygons++;double a=0;
            float cx=0,cy=0;for(int i=0;i<points.length;i+=2) {cx+=points[i];cy+=points[i+1];}
            cx/=points.length/2;cy/=points.length/2;
            for(int i=0;i<points.length;i+=2) {
                int next=(i+2)%points.length;
                a+=points[i]*(double)points[next+1]-points[next]*(double)points[i+1];
                // Sample toward the polygon interior to avoid boundary roundoff.
                for(float t:new float[]{.001f,.25f,.5f,.75f,1}) {
                    float x=points[i]+(cx-points[i])*t,y=points[i+1]+(cy-points[i+1])*t;
                    check(WaterEffectsPainter.contains(game,x,y),"water effect paints solid scenery at "+x+","+y);
                }
                hash=hash*31+Float.floatToIntBits(points[i]);hash=hash*31+Float.floatToIntBits(points[i+1]);
            }
            area+=Math.abs(a)*.5;hash=hash*31+color;
        }
        public void rect(float x,float y,float w,float h,int color) {poly(new float[]{x,y,x+w,y,x+w,y+h,x,y+h},color);}
        public void line(float x,float y,float xx,float yy,float width,int color) {throw new AssertionError("Unclipped stroke");}
        public void circle(float x,float y,float r,int color) {throw new AssertionError("Unclipped circle");}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha) {throw new AssertionError("Unclipped sprite");}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha) {throw new AssertionError("Unclipped sprite");}
    }
    public static void run() {
        Game g;Painter p;
        for(int stage:new int[]{1,3}){
            g=new Game();g.challengeId=stage;g.reset();
            Capture pier=new Capture(g);p=new WaterEffectsPainter(g,pier);
            p.rect(100,100,400,750,0xffffffff);
            int wet=0;
            for(int x=100;x<500;x++)for(int y=100;y<850;y++)if(WaterEffectsPainter.contains(g,x+.5f,y+.5f))wet++;
            check(Math.abs(pier.area-wet)<.1,"physical berth quays and dead-end cap are subtracted exactly without double alpha");
            p.line(180,480,350,480,18,0xffffffff);p.circle(228,480,28,0xffffffff);
            p.line(199,300,350,620,7,0xffffffff);
        }
        g=new Game();g.startTutorial();g=g.activeGame();g.tutorial.lesson=Tutorial.CHANNEL;
        Capture channel=new Capture(g);p=new WaterEffectsPainter(g,channel);
        p.rect(TrainingChannel.ENTRY_LEFT-20,200,Physics.RIGHT-TrainingChannel.ENTRY_LEFT+40,650,0xffffffff);
        int expected=0;
        for(int x=(int)TrainingChannel.ENTRY_LEFT;x<Physics.RIGHT+20;x++) for(int y=200;y<850;y++) if(TrainingChannel.contains(x+.5f,y+.5f)) expected++;
        check(Math.abs(channel.area-expected)<.1,"overlapping training reaches have one fill per water pixel");
        for(int side=-1;side<=1;side+=2) {
            float[] bank=TrainingChannel.bank(side);
            for(int i=0;i<bank.length-2;i+=2) {
                p.circle(bank[i],bank[i+1],26,0xffffffff);
                p.line(bank[i]-40,bank[i+1]-40,bank[i]+40,bank[i+1]+40,12,0xffffffff);
            }
        }
        Body b=new Body(0,-1,290,440,0);b.vx=55;b.vy=-20;
        int emitted=g.water.emitted;float heave=b.heave;
        g.water.x[0]=290;g.water.y[0]=440;g.water.heading[0]=0;g.water.age[0]=130;g.water.strength[0]=1;
        g.water.draw(g,new Capture(g));
        check(g.water.emitted==emitted && b.heave==heave && g.water.age[0]==130,"drawing waves never advances simulation");
        System.out.println(checks+" water clipping, wake, and render purity assertions passed");
    }
    public static void main(String[] args) {run();}
}
