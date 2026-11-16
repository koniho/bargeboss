package com.bargeboss.game;

/** Correct hull-relative colors and sectors, registered motion and shared coverage. */
public final class VesselNavigationLightsTest {
    private static int checks;
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    private static boolean near(float a,float b){return Math.abs(a-b)<.002f;}
    public static void run(){
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
        Body shore=new Body(1,2,Physics.LEFT+2,500,(float)Math.PI/2);Capture water=new Capture();
        VesselNavigationLights.reflections(g,new RiverPainter(water,c),shore,false);
        for(int i=0;i<water.lines;i++)for(int end=0;end<2;end++){
            float x=c.worldX(water.endpoints[i][end*2],water.endpoints[i][end*2+1]),y=c.worldY(water.endpoints[i][end*2],water.endpoints[i][end*2+1]);
            check(x>=Physics.LEFT-.002f && x<=Physics.RIGHT+.002f,"colored water reflection is clipped at the physical bank");
        }
        System.out.println(checks+" navigation light sectors, orientation, bob, coverage, clipping and purity assertions passed");
    }
    private static final class Capture implements Painter {
        int calls,red,green,white,lines;long hash=17;float[][] endpoints=new float[128][4];
        private void add(float x,float y,int color){calls++;if((color&0xffffff)==(VesselNavigationLights.RED&0xffffff) && (color>>>24)>0)red++;if((color&0xffffff)==(VesselNavigationLights.WHITE&0xffffff) && (color>>>24)>0)white++;if((color&0xffffff)==(VesselNavigationLights.GREEN&0xffffff) && (color>>>24)>0)green++;hash=31*hash+Float.floatToIntBits(x);hash=31*hash+Float.floatToIntBits(y);hash=31*hash+color;}
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
