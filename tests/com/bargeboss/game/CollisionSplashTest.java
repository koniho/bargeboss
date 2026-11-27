package com.bargeboss.game;

/** Contact-driven, direction-preserving stamps; no dependency on render rate or damage thresholds. */
public final class CollisionSplashTest {
    private static int checks;
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    private static Body hull(int id,float x,float y,float a){return new Body(id,id==0?-1:0,x,y,a);}
    private static float diff(float a,float b){return Math.abs(Physics.angle(a-b));}
    private static Game practice(){Game g=new Game();ReplayInput.startPractice(g);return g.activeGame();}
    public static void run(){
        for(int f=0;f<32;f++)for(boolean swapped:new boolean[]{false,true}){
            CollisionSplashes s=new CollisionSplashes();float angle=(float)(f*StrictMath.PI/16),c=(float)StrictMath.cos(angle),sn=(float)StrictMath.sin(angle);
            Body a=hull(0,300,500,angle),b=hull(1,300+c*64.25f,500+sn*64.25f,angle);
            a.vx=c*60;a.vy=sn*60;
            Body plainA=hull(0,a.x,a.y,angle),plainB=hull(1,b.x,b.y,angle);plainA.vx=a.vx;plainA.vy=a.vy;
            if(swapped)Physics.collide(plainB,plainA);else Physics.collide(plainA,plainB);
            if(swapped)Physics.collide(b,a,s);else Physics.collide(a,b,s);
            check(a.x==plainA.x && a.y==plainA.y && a.vx==plainA.vx && a.vy==plainA.vy && a.spin==plainA.spin
                    && b.x==plainB.x && b.y==plainB.y && b.vx==plainB.vx && b.vy==plainB.vy && b.spin==plainB.spin,"reporting splash contacts never alters collision physics");
            check(s.emitted==1 && diff(s.direction[0],angle)<.001f,"32 headings use incoming collision direction regardless of argument order");
            for(int j=0;j<CollisionSplashes.STAMPS;j++)check(s.vx[j]*c+s.vy[j]*sn>0,"every spray stamp travels within forward collision fan");
            float contactX=s.originX[0],contactY=s.originY[0];
            check(Physics.length(contactX-300,contactY-500)>20 && Physics.length(contactX-300,contactY-500)<32,"splash starts at hull contact rather than body center");
        }
        CollisionSplashes s=new CollisionSplashes();Body a=hull(0,300,500,0),b=hull(1,340,500,0);
        s.contact(a,b,320,500,-35,0,0,0,35);check(diff(s.direction[0],(float)Math.PI)<.001f,"astern motion sprays astern even when heading is forward");
        s=new CollisionSplashes();s.contact(a,b,320,500,10,50,0,0,10);
        check(diff(s.direction[0],(float)StrictMath.atan2(50,10))<.001f,"glancing spray follows relative contact motion");
        float narrow=Math.abs(Physics.angle((float)StrictMath.atan2(s.vy[0],s.vx[0])-s.direction[0]));
        CollisionSplashes head=new CollisionSplashes();head.contact(a,b,320,500,50,0,0,0,50);
        check(narrow<Math.abs((float)StrictMath.atan2(head.vy[0],head.vx[0])) && s.power[0]<head.power[0],"glancing/light hits have narrower, weaker bursts");
        CollisionSplashes rotating=new CollisionSplashes();Body spinner=hull(0,300,500,0),other=hull(1,312.5f,532,0);spinner.spin=1;
        Physics.collide(spinner,other,rotating);
        check(rotating.emitted==1 && diff(rotating.direction[0],(float)Math.PI/2)<.001f,"rotating hull tip supplies its actual contact velocity");
        CollisionSplashes departing=new CollisionSplashes();Body away=hull(0,300,500,0),still=hull(1,364.25f,500,0);away.vx=-30;
        Physics.collide(away,still,departing);check(departing.emitted==0,"separating overlapping hulls do not splash");
        s=new CollisionSplashes();s.contact(a,b,320,500,0,0,0,0,0);s.contact(a,b,320,500,1,0,0,0,1);
        check(s.emitted==0,"stationary overlap and subthreshold jitter produce no splash");
        s.contact(a,b,320,500,30,0,0,0,30);
        for(int i=0;i<20;i++)s.contact(i%2==0?a:b,i%2==0?b:a,320,500,30,0,0,0,30);
        check(s.emitted==1,"disc solver iterations and swapped participants cannot duplicate a burst");
        Game g=new Game();g.resetPracticeYard();for(int i=0;i<CollisionSplashes.COOLDOWN;i++)s.step(g);
        s.contact(a,b,320,500,30,0,0,0,30);check(s.emitted==2,"a later renewed impact can splash again");
        a.x=400;a.y=700;float x=s.x[6],y=s.y[6];s.step(g);
        check(s.x[6]>x && Math.abs(s.y[6]-y)<1,"released stamps move independently of relocated source hull");
        for(int i=0;i<100;i++)s.step(g);check(s.ordered(g).length==0,"bursts finish and retire their airborne stamps");
        // Red/green floating channel markers and yellow practice buoys share the watercraft event path.
        for(int kind=0;kind<3;kind++){
            g=kind==2?practice():new Game();if(kind<2)g.resetPracticeYard();
            int index=kind==2?NavigationLights.count(g)-2:kind;
            Buoys.Buoy buoy=g.buoys.get(g,index);check(buoy instanceof Watercraft && g.tug instanceof Watercraft,"vessels and floating marks are watercraft participants");
            g.tug.x=buoy.x;g.tug.y=buoy.y+g.tug.half+g.tug.radius+buoy.radius-2;
            g.tug.angle=-(float)Math.PI/2;g.tug.vy=-40;
            Buoys.contact(g,buoy,g.tug,false);
            check(g.splashes.emitted==1 && diff(g.splashes.direction[0],-(float)Math.PI/2)<.001f,"each floating marker variant emits directional collision stamps");
        }
        // Bank, quay, tutorial shoreline, bridge and lock contacts use the same pre-impulse sample.
        s=new CollisionSplashes();a=hull(0,Physics.LEFT+10,500,0);a.vx=-40;
        Physics.banks(a,Physics.TOP,Physics.BOTTOM,false,s);
        check(s.emitted==1 && diff(s.direction[0],(float)Math.PI)<.001f,"bank impact preserves incoming direction after rebound");
        s=new CollisionSplashes();a=hull(0,PhysicalBerths.LEFT+10,450,0);a.vx=-40;
        PhysicalBerths.collide(1,a,s);check(s.emitted==1,"solid quay contacts splash");
        s=new CollisionSplashes();a=hull(0,300,Bridge.Y+20,-(float)Math.PI/2);a.vy=-30;
        new Bridge().collide(a,s);check(s.emitted==1,"closed bridge contact splashes");
        s=new CollisionSplashes();a=hull(0,300,Lock.ENTRY_Y+20,-(float)Math.PI/2);a.vy=-30;
        new Lock().collide(a,s);check(s.emitted==1,"closed lock gate contact splashes");
        g=practice();s=new CollisionSplashes();a=hull(0,Physics.LEFT+10,700,0);a.vx=-40;
        TrainingChannel.collideWater(a,s);check(s.emitted==1,"tutorial shoreline and shore-mounted marker frontage splash");
        g=new Game();g.resetPracticeYard();s=g.splashes;s.contact(g.tug,null,Physics.LEFT+.75f,500,-60,0,0,0,60);
        for(int i=0;i<20;i++)s.step(g);
        for(int j=0;j<CollisionSplashes.STAMPS;j++)check(WaterEffectsPainter.contains(g,s.x[j],s.y[j]),"spray hitting bank rises at the water edge instead of travelling onto land");
        g.layout=Layout.focused(960,600,Physics.LEFT,500,2,480,350);
        int[] order=s.ordered(g);for(int i=1;i<order.length;i++)check(s.depth(g,order[i-1])<=s.depth(g,order[i]),"airborne stamps sort by water-plane footprint, not height");
        Capture capture=new Capture();s.water(g,new RiverPainter(capture,g.layout.camera));for(int j:order)s.draw(g,capture,j);
        check(capture.circles==0,"splash clusters contain no geometric round particle draws");
        check(capture.stamps>0 && capture.sparks>0 && capture.clips==0,"heavy impacts shade spray and emit hot streaks inside balanced clips");
        CollisionSplashes light=new CollisionSplashes();light.contact(g.tug,null,300,500,20,0,0,0,20);
        Capture lightCapture=new Capture();for(int j:light.ordered(g))light.draw(g,lightCapture,j);
        check(lightCapture.stamps>0 && lightCapture.sparks==0 && lightCapture.clips==0,"light bumps splash without hot sparks");
        for(int tick=20;tick<25;tick++)s.step(g);
        Capture cooled=new Capture();for(int j:s.ordered(g))s.draw(g,cooled,j);
        check(cooled.sparks==0,"sparks cool before the longer splash lifetime expires");
        x=s.x[0];int age=s.age[0],emitted=s.emitted;Renderer.draw(g,new Parity());Renderer.draw(g,new Parity());
        check(s.x[0]==x && s.age[0]==age && s.emitted==emitted,"rendering cannot advance effects or emit new contacts");
        g.background();g.advance(.2);check(s.age[0]==age,"pause freezes splash lifecycle");
        g.resetPracticeYard();check(g.splashes.emitted==0 && g.splashes.ordered(g).length==0,"reset clears all old contacts and stamps");
        g.resetPracticeYard();g.feeders[0].phase=Feeder.RETURNING;g.feeders[0].load=null;g.feeders[0].boat.y=700;g.feeders[0].boat.vy=62;
        g.tug.x=g.feeders[0].boat.x;g.tug.y=740;g.tug.angle=-(float)Math.PI/2;g.tug.vy=-30;g.step();
        check(g.splashes.emitted>0,"scripted feeder collision emits through the same watercraft path");
        Game low=new Game(),high=new Game();low.resetPracticeYard();high.resetPracticeYard();
        low.splashes.contact(low.tug,null,300,500,40,0,0,0,40);high.splashes.contact(high.tug,null,300,500,40,0,0,0,40);
        for(int i=0;i<15;i++)low.advance(1.0/30);for(int i=0;i<72;i++)high.advance(1.0/144);
        check(low.splashes.x[0]==high.splashes.x[0] && low.splashes.z[0]==high.splashes.z[0] && low.splashes.age[0]==high.splashes.age[0],"30/144 Hz splash trajectories agree");
        s=new CollisionSplashes();for(int i=0;i<100;i++)s.contact(hull(i,300,500,0),null,300,500,60,0,0,0,60);
        check(s.ordered(low).length<=CollisionSplashes.CAPACITY*CollisionSplashes.STAMPS,"simultaneous collision stamp pool stays bounded");
        System.out.println(checks+" collision splash direction, stamp, watercraft, wall, lifecycle and depth assertions passed");
    }
    private static final class Capture implements Painter{
        int stamps,other,clips,sparks,circles;
        public void waterFoam(float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha){stamps++;}
        public void pushClipOutside(float[] p){clips++;}public void popClip(){clips--;}
        public void rect(float x,float y,float w,float h,int c){other++;}public void circle(float x,float y,float r,int c){other++;circles++;}
        public void line(float x,float y,float xx,float yy,float w,int c){other++;if((c&0xffffff)==0xff742b)sparks++;}public void poly(float[] p,int c){other++;}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){other++;}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){other++;}
        public void spriteAffine(int a,int f,float x,float y,float ax,float ay,float bx,float by,float alpha){check((a==SpriteAtlas.SPLASH || a==SpriteAtlas.RIPPLES) && f>=0 && f<4,"splash uses a registered impact stamp");stamps++;}
    }
    public static void main(String[] args){run();}
}
