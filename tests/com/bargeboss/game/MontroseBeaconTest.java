package com.bargeboss.game;

/** Survey identity, root anchors, fixed dusk structure and bounded live drawing. */
public final class MontroseBeaconTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static final class Capture implements Painter {
        int polygons,sprites,circles;long hash=1;
        public void poly(float[] xy,int color){polygons++;hash=hash*31+color;for(float f:xy){check(Float.isFinite(f),"Finite beacon projection");hash=hash*31+Float.floatToIntBits(f);}}
        public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){}
        public void rect(float x,float y,float w,float h,int c){}
        public void circle(float x,float y,float r,int c){circles++;}
        public void line(float x,float y,float xx,float yy,float w,int c){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){sprites++;}
        public void sprite(int atlas,int frame,float x,float y,float w,float h,float r,float alpha){sprites++;}
    }
    public static void run(){
        Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();
        check(MontroseBeaconArt.color(0)==NavigationLights.GREEN && MontroseBeaconArt.color(1)==NavigationLights.RED,"West green/east red inlet identity");
        check(MontroseBeaconArt.color(2)==NavigationLights.GREEN,"Wilson Avenue horn LLNR20130 is green, despite its red steelwork");
        for(int t=0;t<180;t++)check(MontroseBeaconArt.power(t)==MontroseBeaconArt.power(t+180),"All nominal Montrose lights repeat every three seconds");
        check(MontroseBeaconArt.power(15)==1 && MontroseBeaconArt.power(135)==0,"Horn retains true dark interval at old two-second flash time");
        for(int id=0;id<3;id++){
            float x=MontroseBeaconArt.x(id),y=MontroseBeaconArt.y(id);float[] lamp=MontroseBeaconArt.lamp(id);
            check(lamp[0]==x && lamp[1]==y,"Lens remains registered to structure root");
            check(Math.abs(lamp[2]*ArticulatedModel.Z_SCALE-MontroseBeaconArt.rise(id)-76*ArticulatedModel.Z_SCALE)<.001f,"Lens and model use exactly one height transform");
            check(MontroseBeaconArt.faces(id)<300,"Static beacon geometry budget");
            if(id<2)check(Math.abs(x-NavigationLights.anchorX(g,id))<.001f && Math.abs(y-NavigationLights.anchorY(g,id))<.001f,"New inlet model preserves authoritative breakwater endpoints");
            g.layout=Layout.focused(960,600,x,y,1,480,360);g.visualTicks=15;
            int ticks=g.ticks;Capture a=new Capture();MontroseBeaconArt.draw(g,a,id);g.visualTicks=100;Capture b=new Capture();MontroseBeaconArt.draw(g,b,id);
            System.out.println("Beacon "+id+": source="+MontroseBeaconArt.faces(id)+", drawn="+a.polygons+" / dark="+b.polygons);
            check(a.polygons>0 && a.polygons<450 && b.polygons==a.polygons,"Bounded solid model persists between flashes");
            check(a.hash!=b.hash && a.sprites==0 && b.sprites==0,"Lantern lights its real gallery faces; no generic lighthouse sprite");
            check(a.circles>b.circles,"Lamp core emission disappears between flashes");
            check(g.ticks==ticks,"Rendering never advances gameplay");
            float tx=g.tug.x,ty=g.tug.y;g.visualTicks=100;
            g.tug.x=x-20;g.tug.y=y+20;Capture nearBoat=new Capture();MontroseBeaconArt.draw(g,nearBoat,id);
            g.tug.x=x+1000;g.tug.y=y+1000;Capture farBoat=new Capture();MontroseBeaconArt.draw(g,farBoat,id);
            check(nearBoat.hash!=farBoat.hash,"Pole geometry receives nearby vessel work/navigation light");g.tug.x=tx;g.tug.y=ty;
            float[] lit=new float[4],dark=new float[4];g.visualTicks=15;
            MontroseBeaconArt.sample(g,lit,x+30,y+30);g.visualTicks=100;MontroseBeaconArt.sample(g,dark,x+30,y+30);
            check(lit[0]+lit[1]+lit[2]+lit[3]>.1f && dark[0]+dark[1]+dark[2]+dark[3]==0,"Beacon reaches nearby vessel receiver only during flash");
            float[] farLight=new float[4];g.visualTicks=15;MontroseBeaconArt.sample(g,farLight,x+1000,y+1000);
            check(farLight[0]+farLight[1]+farLight[2]+farLight[3]==0,"Beacon falloff bounded in world space");
            g.layout=Layout.focused(960,600,10000,10000,1,480,300);Capture far=new Capture();MontroseBeaconArt.draw(g,far,id);
            check(far.polygons==0 && far.circles==0,"Offscreen beacon culled before model/light work");
        }
        System.out.println(checks+" Montrose beacon identity, geometry, anchor, dusk and draw-budget checks passed");
    }
    public static void main(String[] args){run();}
}
