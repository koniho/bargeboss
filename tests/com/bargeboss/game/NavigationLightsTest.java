package com.bargeboss.game;

/** Stable lateral sides, flash rhythm, land/water registration and render-only animation. */
public final class NavigationLightsTest {
    private static int checks;
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    public static void run(){
        for(int stage:new int[]{-1,0,Challenges.INDIANA,Challenges.MONTROSE}){
            Game g=new Game();if(stage<0){g.startTutorial();g=g.activeGame();}else {g.challengeId=stage;g.reset();}
            NavigationLights.Entrance[] entrances=NavigationLights.entrances(g);
            check(entrances.length==(stage==Challenges.INDIANA?2:1),"only distinct region mouths carry a navigation pair");
            if(stage<0)check(entrances[0].x==TrainingChannel.MIN+12 && entrances[0].y==TrainingChannel.Y[0],"tutorial pair marks driving basin to winding canal only");
            for(NavigationLights.Entrance e:entrances){
                float rx=e.markerX(true)-e.x,ry=e.markerY(true)-e.y,gx=e.markerX(false)-e.x,gy=e.markerY(false)-e.y;
                check(rx*(-e.dy)+ry*e.dx>0 && gx*(-e.dy)+gy*e.dx<0,"red is starboard and green port in the declared returning direction");
                check(Math.abs(rx+gx)<.001f && Math.abs(ry+gy)<.001f,"markers flank the entrance symmetrically");
                for(boolean red:new boolean[]{false,true}) {
                    float mx=e.markerX(red),my=e.markerY(red);
                    if(stage<0) {
                        float half=LandMarkerArt.FOOTPRINT_HALF;
                        check(!TrainingGroundView.intersectsWater(mx-half,my-half,half*2,half*2),"complete concrete foundation sits back on dry land");
                    } else if(stage==Challenges.MONTROSE){int dock=MontroseHarbor.DOCK_LINES.length-(red?1:2);float[] line=MontroseHarbor.DOCK_LINES[dock];
                        check(Math.abs(mx-line[2])<.001f && Math.abs(my-line[3])<.001f,"Montrose fixed beacon is registered to breakwater endpoint");}
                    else check(WaterEffectsPainter.contains(g,mx,my),"shift floating marks sit on navigable water beside their entrance");
                }
            }
            for(int i=0;i<NavigationLights.count(g);i++){
                float x=NavigationLights.x(g,i),y=NavigationLights.y(g,i);int color=NavigationLights.color(g,i);
                check(NavigationLights.land(g,i)==((stage<0 || stage==Challenges.MONTROSE) && i<entrances.length*2),"tutorial and Montrose entrance markers use fixed land variants");
                g.tug.angle+=1.7f;g.configureCanvas(1333,600);
                check(NavigationLights.x(g,i)==x && NavigationLights.y(g,i)==y && NavigationLights.color(g,i)==color,"heading or screen aspect never flips lateral markers");
            }
            if(stage==Challenges.MONTROSE){
                check(g.buoys.get(g,0)==null && g.buoys.get(g,1)==null,"Breakwater fixtures have no buoy body or duplicate collision");
                check(g.buoys.get(g,2)!=null,"Marina mooring cans retain floating physics");
                check(LandMarkerArt.baseRise(g)==MontroseHarbor.DOCK_RISE,"Entrance foundations use marina deck plane");
                for(int i=0;i<2;i++){
                    float x=NavigationLights.x(g,i),y=NavigationLights.y(g,i);
                    for(int tick=0;tick<10;tick++)g.buoys.step(g);
                    check(NavigationLights.x(g,i)==x && NavigationLights.y(g,i)==y,"Fixed towers cannot drift with waves or collision simulation");
                }
            }
            g.layout=Layout.focused(1333,600,entrances[0].x,entrances[0].y,1.5f,650,350);
            int[] order=NavigationLights.ordered(g);
            for(int i=1;i<order.length;i++)check(NavigationLights.depth(g,order[i-1])<=NavigationLights.depth(g,order[i]),"markers join increasing world depth order");
            int ticks=g.ticks,visual=g.visualTicks,water=g.water.emitted;float tx=g.tug.x,ty=g.tug.y;
            Capture a=new Capture(),b=new Capture();NavigationLights.reflections(g,a);NavigationLights.landShadows(g,a);for(int i:order)NavigationLights.draw(g,a,i);
            NavigationLights.reflections(g,b);NavigationLights.landShadows(g,b);for(int i:order)NavigationLights.draw(g,b,i);
            check(a.hash==b.hash && a.calls==b.calls,"same frame produces identical lights and reflections");
            check(g.ticks==ticks && g.visualTicks==visual && g.water.emitted==water && g.tug.x==tx && g.tug.y==ty,"buoy animation never advances gameplay or water");
            g.visualTicks=15;Capture bright=new Capture();for(int i:order)NavigationLights.draw(g,bright,i);
            g.visualTicks=70;Capture dark=new Capture();for(int i:order)NavigationLights.draw(g,dark,i);
            check(bright.hash!=dark.hash && dark.calls>0,"flash visibly changes while the unlit colored buoy remains present");
            if(stage==Challenges.MONTROSE){
                g.visualTicks=135;Capture middle=new Capture();for(int i:order)NavigationLights.draw(g,middle,i);
                g.visualTicks=195;Capture repeated=new Capture();for(int i:order)NavigationLights.draw(g,repeated,i);
                check(bright.calls==repeated.calls && bright.hash!=middle.hash,"Montrose repeats lit energy after three seconds, not two");
                check(bright.calls>middle.calls,"Montrose beacon is dark at the generic two-second flash time");
                for(int i=0;i<2;i++){
                    float x=NavigationLights.x(g,i),y=NavigationLights.y(g,i);float[] cut=LandMarkerArt.waterCutout(g,x,y);
                    check(cut.length>0 && cut.length%6==0,"Breakwater receiver cutout is triangle-partitioned");
                    for(int k=0;k<cut.length;k+=6){
                        float sx=(cut[k]+cut[k+2]+cut[k+4])/3,sy=(cut[k+1]+cut[k+3]+cut[k+5])/3;
                        float wx=g.layout.camera.worldX(sx,sy+LandMarkerArt.baseRise(g)*g.layout.camera.scale),wy=g.layout.camera.worldY(sx,sy+LandMarkerArt.baseRise(g)*g.layout.camera.scale);
                        check(!PolygonWater.contains(MontroseHarbor.DOCKS[MontroseHarbor.DOCKS.length-2+i],wx,wy),"Excluded receiver triangles never cover breakwater deck");
                    }
                }
            }
            g.layout=Layout.focused(960,600,10000,10000,1,480,300);
            check(NavigationLights.ordered(g).length==0,"offscreen marks are culled before lighting work");
        }
        for(int phase:new int[]{0,19,38,57,76}){
            int lit=0,dark=0;for(int tick=0;tick<120;tick++){
                float f=NavigationLights.flash(tick,phase);
                check(f>=0 && f<=1 && f==NavigationLights.flash(tick+120,phase),"flash has stable two-second period and finite energy");
                if(f>0)lit++;else dark++;
            }
            check(lit>0 && dark>lit,"single flashes have a longer true dark interval");
        }
        Game g=new Game();g.startTutorial();g=g.activeGame();
        int first=NavigationLights.count(g)-2;
        for(int i=0;i<2;i++)check(NavigationLights.x(g,first+i)==Tutorial.DRIVE_X[i] && NavigationLights.y(g,first+i)==Tutorial.DRIVE_Y[i] && NavigationLights.color(g,first+i)==NavigationLights.YELLOW,"practice buoy models stay at actual circling centers and use distinct yellow lamps");
        System.out.println(checks+" lateral light side, flash, land/water placement, depth and animation assertions passed");
    }
    private static final class Capture implements Painter {
        int hash=1,calls;
        private void add(float f){hash=31*hash+Float.floatToIntBits(f);}
        private void mark(int color){hash=31*hash+color;calls++;}
        public void rect(float x,float y,float w,float h,int c){mark(c);add(x);add(y);add(w);add(h);}
        public void circle(float x,float y,float r,int c){mark(c);add(x);add(y);add(r);}
        public void line(float x,float y,float xx,float yy,float w,int c){mark(c);add(x);add(y);add(xx);add(yy);add(w);}
        public void poly(float[] xy,int c){mark(c);for(float v:xy)add(v);}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){mark(a);add(f);add(x);add(y);add(w);add(h);add(r);add(alpha);}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){sprite(a,f,x,y,w,h,r,alpha);add(ff);add(mix);}
    }
    public static void main(String[] args){run();}
}
