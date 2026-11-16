package com.bargeboss.game;

/** Correct hull-relative colors and sectors, registered motion and shared coverage. */
public final class VesselNavigationLightsTest {
    private static int checks;
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    private static boolean near(float a,float b){return Math.abs(a-b)<.002f;}
    public static void run(){
        bargeModes();
        broadsideStern();
        float view=(float)(StrictMath.PI*.75);
        check(VesselNavigationLights.visibility(view-(float)Math.PI/2,1)==1 && VesselNavigationLights.visibility(view-(float)Math.PI/2,0)==0,"starboard view shows green, not port red");
        check(VesselNavigationLights.visibility(view+(float)Math.PI/2,0)==1 && VesselNavigationLights.visibility(view+(float)Math.PI/2,1)==0,"port view shows red, not starboard green");
        check(VesselNavigationLights.visibility(view+(float)Math.PI,2)==1 && VesselNavigationLights.visibility(view+(float)Math.PI,3)==0,"astern view sees white stern, not forward masthead");
        check(VesselNavigationLights.visibility(view,3)==1 && VesselNavigationLights.visibility(view,2)==0,"ahead view sees white mast, not stern");
        // The elevated game camera can see neighboring lamp lenses together.
        // Check authored poses on both sides of each handoff, not just the exact seam.
        float step=(float)(StrictMath.PI/16),sideArc=step*10;
        for(int offset=-1;offset<=1;offset++) {
            float heading=view+offset*step;
            check(VesselNavigationLights.visibility(heading,0)>.49f && VesselNavigationLights.visibility(heading,1)>.49f,
                "both red and green remain readable across three forward poses");
            for(int side:new int[]{-1,1}) {
                heading=view-side*sideArc+offset*step;
                check(VesselNavigationLights.visibility(heading,side<0?0:1)>.49f && VesselNavigationLights.visibility(heading,2)>.49f,
                    "side and stern lights overlap across rear-quarter poses");
            }
        }
        for(int fixture=0;fixture<4;fixture++)for(int degree=0;degree<360;degree++) {
            float heading=degree*(float)(StrictMath.PI/180);
            check(Math.abs(VesselNavigationLights.visibility(heading,fixture)-VesselNavigationLights.visibility(heading+.001f,fixture))<.005f,
                "turning never pops a navigation lamp across an overlap edge");
        }
        Game g=new Game();g.reset();g.visualTicks=123;RiverCamera c=new RiverCamera(0,0,2);
        for(boolean powered:new boolean[]{false,true})for(int frame=-32;frame<64;frame++){
            Body b=new Body(powered?0:1,powered?-1:2,300,500,frame*(float)(StrictMath.PI/16));
            for(int fixture=0;fixture<VesselNavigationLights.count(powered);fixture++){
                float x=VesselNavigationLights.worldX(b,fixture)-b.x,y=VesselNavigationLights.worldY(b,fixture)-b.y;
                float side=-x*(float)StrictMath.sin(b.angle)+y*(float)StrictMath.cos(b.angle);
                check(near(side,VesselNavigationLights.localY(b,fixture)),"body rotation preserves port/starboard side through every wrap and heading");
                check(fixture!=0 || side<0,"red fixture remains on hull port side");
                check(fixture!=1 || side>0,"green fixture remains on hull starboard side");
                float opacity=VesselNavigationLights.visibility(b.angle,fixture);
                check(opacity>=0 && opacity<=1 && near(opacity,VesselNavigationLights.visibility(b.angle+(float)Math.PI*2,fixture)),"sector opacity is bounded and wraps smoothly");
                check(near(VesselNavigationLights.screenY(b,fixture,powered,c,0)-6,VesselNavigationLights.screenY(b,fixture,powered,c,3)),"all fixtures share exact hull bob");
            }
            Capture first=new Capture(),repeat=new Capture();
            VesselNavigationLights.draw(g,new RiverPainter(first,c),b,powered,1,false);
            if(!powered)VesselNavigationLights.draw(g,new RiverPainter(first,c),b,false,1,true);
            VesselNavigationLights.draw(g,new RiverPainter(repeat,c),b,powered,1,false);
            if(!powered)VesselNavigationLights.draw(g,new RiverPainter(repeat,c),b,false,1,true);
            check(first.calls>0 && first.calls<=24,"running lights remain visible within a bounded cached drawing budget");
            check(first.hash==repeat.hash,"repeated paused rendering is deterministic");
            check(b.x==300 && b.y==500 && b.heave==0 && g.visualTicks==123,"rendering never changes hull or animation state");
        }
        // Player, feeder-sized hull, crossing NPC and every barge cargo type use
        // exactly the same VesselView entry point and cannot omit running lamps.
        for(int type=0;type<6;type++){
            boolean powered=type<3;Body b=new Body(type==0?0:type==1?97:-1,powered?-1:type-3,300,500,(float)Math.PI/2);
            Capture p=new Capture();VesselView.draw(g,new RiverPainter(p,c),b,powered,false);
            check(p.green>0,"player/NPC/title-compatible craft and every cargo type render navigation green through VesselView: "+type);
        }
        for(boolean powered:new boolean[]{false,true}) {
            Body bow=new Body(powered?0:5,powered?-1:2,300,500,view);
            Capture lights=new Capture();VesselView.draw(g,new RiverPainter(lights,c),bow,powered,false);
            check(lights.red>0 && lights.green>0,"forward pose draws red and green together through cargo layering");
            check(!powered || lights.white>0,"powered forward pose also draws its white masthead");
        }
        Body shore=new Body(1,2,Physics.LEFT+20,500,(float)Math.PI/2);Capture water=new Capture();
        VesselNavigationLights.reflections(g,new RiverPainter(water,c),shore,false);
        check(water.lines>0 && water.clips.isEmpty(),"reflection has visible water samples and restores its clip");
        for(int i=0;i<water.lines;i++)for(int end=0;end<2;end++){
            float x=c.worldX(water.endpoints[i][end*2],water.endpoints[i][end*2+1]),y=c.worldY(water.endpoints[i][end*2],water.endpoints[i][end*2+1]);
            check(CalumetMap.contains(x,y),"colored water reflection is clipped at the physical bank");
        }
        System.out.println(checks+" navigation light sectors, orientation, bob, coverage, clipping and purity assertions passed");
    }
    private static void broadsideStern(){
        Game g=new Game();g.reset();
        for(int side=0;side<2;side++){
            float heading=(float)(StrictMath.PI/4+side*StrictMath.PI);
            Body b=new Body(1,2,300,500,heading);
            check(VesselNavigationLights.energy(g,b,false,VesselNavigationLights.STERN)<.001f,"barge previous sector restored");
            g.tug.angle=heading;
            check(VesselNavigationLights.energy(g,g.tug,true,VesselNavigationLights.STERN)>.999f,"tug stern visible directly screen left/right");
            Body yacht=new Body(2,0,300,500,heading,true);
            check(VesselNavigationLights.energy(g,yacht,false,VesselNavigationLights.STERN)<.001f,"yacht sector unchanged");
            for(int sample=-500;sample<=500;sample++){
                float angle=heading+sample*.001f;
                check(Math.abs(VesselNavigationLights.tugSternVisibility(angle)-VesselNavigationLights.tugSternVisibility(angle+.001f))<.004f,"stern fades continuously across broadside and forward quarter");
            }
        }
        check(VesselNavigationLights.tugSternVisibility((float)(Math.PI*.75))==0,"tug stern still hidden bow-on");
        check(VesselNavigationLights.tugSternVisibility((float)(Math.PI*1.75))==1,"tug stern fully visible from astern");
    }
    private static void bargeModes(){
        int pulses=0;float previous=0;
        for(int tick=0;tick<3600;tick++){
            float power=VesselNavigationLights.specialPulse(tick);
            if(previous==0 && power>0)pulses++;
            check(power>=0 && power<=1,"flasher bounded");
            check(Math.abs(power-previous)<.101f,"pulse eases without hard switching");
            check(near(power,VesselNavigationLights.specialPulse(tick+60)),"one-second pulse repeats exactly");
            previous=power;
        }
        check(pulses==60,"60 eased flashes per minute");
        check(VesselNavigationLights.specialPulse(0)==0 && VesselNavigationLights.specialPulse(15)==1 && VesselNavigationLights.specialPulse(45)==0,"rise, peak, fall and dark interval");
        for(int end:new int[]{-1,1})for(int frame=0;frame<32;frame++){
            Game g=new Game();g.reset();g.visualTicks=18;
            Body b=new Body(1,2,300,500,frame*(float)Math.PI/16);g.cargo[0]=b;
            g.tug.angle=b.angle+(end<0?(float)Math.PI:0);
            g.tug.x=b.x-65*(float)StrictMath.cos(g.tug.angle);g.tug.y=b.y-65*(float)StrictMath.sin(g.tug.angle);
            g.pushing=true;
            check(VesselNavigationLights.pushedEnd(g,b)==end,"contact identifies leading end at every heading");
            check(VesselNavigationLights.energy(g,b,false,VesselNavigationLights.STERN)==0,"pushing suppresses stern lamp");
            check(near(VesselNavigationLights.energy(g,b,false,VesselNavigationLights.SPECIAL),VesselNavigationLights.visibility(g.tug.angle,VesselNavigationLights.SPECIAL)),"yellow pulse uses actual leading end sector");
            g.tug.x-=150*(float)StrictMath.cos(g.tug.angle);g.tug.y-=150*(float)StrictMath.sin(g.tug.angle);
            check(VesselNavigationLights.pushedEnd(g,b)==0,"distant pickup intent is not pushing");
            check(VesselNavigationLights.energy(g,b,false,VesselNavigationLights.SPECIAL)==0,"no flasher before contact");
            g.tug.x=b.x+end*70*(float)StrictMath.cos(b.angle);g.tug.y=b.y+end*70*(float)StrictMath.sin(b.angle);
            check(g.tow.attach(g,b,g.tug),"real tow link attaches");
            check(VesselNavigationLights.energy(g,b,false,VesselNavigationLights.SPECIAL)==0,"astern tow has no pushed-barge flasher");
            check(near(VesselNavigationLights.energy(g,b,false,VesselNavigationLights.STERN),VesselNavigationLights.visibility(g.tug.angle,VesselNavigationLights.STERN)),"tow stern sector follows attached leading end");
            b.lineAttached=true;g.tow.release(g);
            check(VesselNavigationLights.pushedEnd(g,b)==0,"moored barge cannot show push rig");
        }
        Game g=new Game();g.reset();Feeder feeder=g.feeders[0];
        check(VesselNavigationLights.pushedEnd(g,feeder.load)==1,"NPC inbound load gets push rig even when waiting");
        Body yacht=new Body(9,0,300,500,0,true);
        check(VesselNavigationLights.energy(g,yacht,false,VesselNavigationLights.SPECIAL)==0,"yacht remains on yacht running rig");
    }
    private static final class Capture implements Painter {
        int calls,red,green,white,lines;long hash=17;float[][] endpoints=new float[128][4];
        final java.util.ArrayList<float[]> clips=new java.util.ArrayList<float[]>();
        public void pushClipOutside(float[] triangles){clips.add(triangles);}
        public void popClip(){clips.remove(clips.size()-1);}
        public void waterReflection(float x,float y,float ax,float ay,float bx,float by,float phase,int color) {
            add(x,y,color);
            // Reflection now submits a clipped patch, not pre-cut line endpoints.
            // Probe its visible interior against the same exclusion triangles as native painters.
            for(int row=1;row<10;row++)for(int col=1;col<10;col++) {
                float xx=x+ax*(col/10f-.5f)+bx*(row/10f-.5f),yy=y+ay*(col/10f-.5f)+by*(row/10f-.5f);
                boolean dry=false;for(float[] clip:clips)if(ShoreOcclusion.contains(clip,xx,yy)){dry=true;break;}
                if(!dry && lines<endpoints.length)endpoints[lines++]=new float[]{xx,yy,xx,yy};
            }
        }
        private void add(float x,float y,int color){calls++;if((color&0xffffff)==(VesselNavigationLights.RED&0xffffff) && (color>>>24)>0)red++;if((color&0xffffff)==(VesselNavigationLights.WHITE&0xffffff) && (color>>>24)>0)white++;if((color&0xffffff)==(VesselNavigationLights.GREEN&0xffffff) && (color>>>24)>0)green++;hash=31*hash+Float.floatToIntBits(x);hash=31*hash+Float.floatToIntBits(y);hash=31*hash+color;}
        public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){add(x,y,color);}
        public void layer(StaticLayer l,float x,float y,float scale,float alpha){add(x,y,l.key());}
        public void circle(float x,float y,float r,int color){add(x,y,color);}
        public void rect(float x,float y,float w,float h,int color){add(x,y,color);}
        public void poly(float[] xy,int color){add(xy[0],xy[1],color);}
        public void line(float x,float y,float xx,float yy,float w,int color){if(lines<endpoints.length)endpoints[lines++]=new float[]{x,y,xx,yy};add(x,y,color);}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float opacity){add(x,y,a);}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float opacity){add(x,y,a);}
    }
    public static void main(String[] args){run();}
}
