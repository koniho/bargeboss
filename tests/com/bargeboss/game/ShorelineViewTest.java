package com.bargeboss.game;

/** Immutable shore strips preserve spatial phase and consult wakes only for visible geometry. */
public final class ShorelineViewTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static final class Trace implements Painter {
        int foam,walls;float maxEnergy;long hash=7;
        public void shoreline(float[] xy,float[] uv,float phase,float energy,int kind,int color){
            check(xy.length==8 && uv.length==8,"bounded four-corner strip");
            check(energy>=0 && energy<=1,"wake response bounded");
            check(phase>=0,"simulation phase");
            if(kind==0)foam++;else walls++;maxEnergy=Math.max(maxEnergy,energy);
            for(float v:xy){check(Float.isFinite(v),"finite projected shore");hash=hash*31+Float.floatToIntBits(v);}
            for(float v:uv)hash=hash*31+Float.floatToIntBits(v);
            hash=hash*31+Float.floatToIntBits(phase);hash=hash*31+Float.floatToIntBits(energy);
        }
        public void poly(float[] xy,int c){}public void rect(float x,float y,float w,float h,int c){}
        public void circle(float x,float y,float r,int c){}public void line(float x,float y,float xx,float yy,float w,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
    }
    private static Trace render(Game g){Trace t=new Trace();ShorelineView.water(g,t);ShorelineView.walls(g,t);return t;}
    public static void run(){
        for(boolean marina:new boolean[]{false,true}) {
            ShorelineView.Segment[] strips=ShorelineView.segments(marina);check(strips.length>0 && strips.length<5000,"fixed shoreline geometry budget");
            ShorelineView.Segment previous=null;
            for(ShorelineView.Segment s:strips){
                float length=Physics.length(s.bx-s.ax,s.by-s.ay);
                check(length>0 && length<=ShorelineView.MAX_LENGTH+.002f,"short shore patch");
                check(Math.abs(length-(s.u1-s.u0))<.01f,"world-distance UV scale");
                check(Math.abs(s.nx*s.nx+s.ny*s.ny-1)<.0001f,"unit water normal");
                float mx=(s.ax+s.bx)*.5f+s.nx*.5f,my=(s.ay+s.by)*.5f+s.ny*.5f;
                check(marina?MontroseHarbor.contains(mx,my):CalumetMap.contains(mx,my),"strip normal points to exposed water");
                check(s.foamUV[5]==12 && s.wallUV[5]<=6,"foam width and wet climb bounded");
                check(Math.abs((s.foam[4]-s.bx)*s.nx+(s.foam[5]-s.by)*s.ny-12)<.001f,"foam grows toward water");
                if(previous!=null && previous.bx==s.ax && previous.by==s.ay)
                    check(previous.u1==s.u0,"adjacent segments share exact UV phase");
                previous=s;
            }
            Game g=new Game();g.challengeId=marina?Challenges.MONTROSE:Challenges.TWO_BANKS;g.reset();g.configureCanvas(960,600);
            ShorelineView.Segment sample=strips[strips.length/2];float x=(sample.ax+sample.bx)*.5f,y=(sample.ay+sample.by)*.5f;
            g.layout=Layout.focused(960,600,x,y,2,480,300);g.visualTicks=120;
            float[] uv=sample.foamUV.clone();int ticks=g.ticks,emitted=g.water.emitted;
            Trace a=render(g),b=render(g);check(a.foam>0,"close-up coastline visible");check(a.hash==b.hash,"repeated rendering is deterministic");
            check(g.ticks==ticks && g.water.emitted==emitted,"drawing does not advance physics or emit wakes");
            g.visualTicks++;Trace later=render(g);check(later.hash!=a.hash,"continuous phase advances each tick");
            check(Math.abs(ShorelineView.phase(g)-121*Physics.DT)<.000001f,"phase is exact fixed-step seconds");
            g.layout=Layout.focused(960,600,x+7,y-9,1.3f,480,300);render(g);
            for(int i=0;i<uv.length;i++)check(uv[i]==sample.foamUV[i],"pan and zoom do not swim material UVs");
            // Exercise the actual wake emitter, then place an emitted crest at this shore sample.
            g.tug.vx=50;g.tug.vy=0;for(int i=0;i<8;i++)g.water.step(g);
            check(g.water.emitted>0,"real vessel emits wake sample");
            int packet=0;while(packet<WaterSurface.CAPACITY && g.water.age[packet]>=WaterSurface.LIFE)packet++;
            g.water.x[packet]=x+sample.nx*6;g.water.y[packet]=y+sample.ny*6;g.water.age[packet]=0;g.water.strength[packet]=1.8f;
            check(ShorelineView.energy(g,sample)>.9f,"arriving wake intensifies shoreline response");
            g.layout=Layout.focused(960,600,100000,100000,2,480,300);Trace outside=render(g);
            check(outside.foam==0 && outside.walls==0,"offscreen shores cull before material submission");
            System.out.println((marina?"Marina":"Calumet")+" shoreline: "+strips.length+" fixed segments, close view "+a.foam+" foam / "+a.walls+" wet");
        }
        System.out.println("ShorelineViewTest: "+checks+" checks");
    }
    public static void main(String[] args){run();}
}
