package com.bargeboss.game;

/** Incoming light must alter real faces, without moving geometry or creating emissive receivers. */
final class MarinaReceiverTest {
    private static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
    private static int brightness(int c){return (c>>16&255)+(c>>8&255)+(c&255);}
    static void run(){
        MooredBoatLight.Lights lights=new MooredBoatLight.Lights();int white=0xffeeeeee;
        int ambient=MarinaSurfaceLight.shade(LightingProfile.NIGHT,lights,white,0,0,0,0,0,1);
        lights.add(0,0,10,50,1,0xffff0000);
        int front=MarinaSurfaceLight.shade(LightingProfile.NIGHT,lights,white,0,0,0,0,0,1);
        check((front>>16&255)>(ambient>>16&255) && (front&65535)==(ambient&65535),"red source affects only red surface response");
        int back=MarinaSurfaceLight.shade(LightingProfile.NIGHT,lights,white,0,0,0,0,0,-1);
        lights.count=0;
        check(back==MarinaSurfaceLight.shade(LightingProfile.NIGHT,lights,white,0,0,0,0,0,-1),"back face rejects incoming light");
        lights.add(0,0,10,50,1,0xffffffff);
        int distant=MarinaSurfaceLight.shade(LightingProfile.NIGHT,lights,white,100,100,0,0,0,1);
        lights.count=0;
        check(distant==ambient,"point light has finite reach");
        check(brightness(MarinaSurfaceLight.shade(LightingProfile.DAYLIGHT,lights,white,0,0,0,0,0,1))>brightness(ambient),"daylight and night share the scene grade");
        Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();g.changeLighting(3);
        for(Body body:g.cargo)if(body!=null)body.active=false;
        float wx=MontroseHarbor.MOORINGS[8][0],wy=MontroseHarbor.MOORINGS[8][1];
        g.rescue.x[0]=wx;g.rescue.y[0]=wy;g.layout=Layout.focused(960,600,wx,wy,5,480,300);
        Capture p=new Capture();
        for(int receiver=0;receiver<2;receiver++){
            g.tug.x=wx+1000;g.tug.y=wy+1000;p.reset();draw(g,p,receiver,wx,wy);
            long geometry=p.geometry,unlit=p.colors;int calls=p.calls;
            check(calls>=10 && calls<=64,"bounded real 3-D receiver faces");
            g.tug.x=wx-10;g.tug.y=wy+10;p.reset();draw(g,p,receiver,wx,wy);
            check(p.geometry==geometry && p.calls==calls,"moving source never changes mesh");
            check(p.colors!=unlit,"nearby tug lights reach actual receiver faces");
            long lit=p.colors;p.reset();draw(g,p,receiver,wx,wy);check(p.colors==lit,"same frame remains deterministic");
        }
        System.out.println("Marina 3-D buoy/swimmer normal, colored light, distance, time-of-day and deterministic mesh checks passed");
    }
    private static void draw(Game g,Painter p,int receiver,float x,float y){if(receiver==0)MooringBuoyArt.draw(g,p,x,y,10);else g.rescue.swimmerModel.draw(g,p,0);}
    private static final class Capture implements Painter {
        int calls;long geometry,colors;
        void reset(){calls=0;geometry=colors=17;}
        void value(float f){check(Float.isFinite(f),"finite 3-D projection");geometry=geometry*31+Float.floatToIntBits(f);}
        public void convexQuad(float a,float b,float c,float d,float e,float f,float h,float i,int color){calls++;value(a);value(b);value(c);value(d);value(e);value(f);value(h);value(i);colors=colors*31+color;}
        public void poly(float[] p,int color){throw new AssertionError("bounded face primitive expected");}
        public void circle(float x,float y,float r,int c){throw new AssertionError("receiver must be real 3-D faces");}
        public void line(float a,float b,float c,float d,float w,int col){throw new AssertionError("no lamp ornament");}
        public void rect(float a,float b,float c,float d,int col){throw new AssertionError("no flat receiver");}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){throw new AssertionError("no receiver sprite");}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){throw new AssertionError("no receiver sprite");}
    }
    public static void main(String[] args){run();}
}
