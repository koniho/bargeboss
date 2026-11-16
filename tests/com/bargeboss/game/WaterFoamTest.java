package com.bargeboss.game;

/** Direction, bounded submissions, transform forwarding and render-only semantics. */
public final class WaterFoamTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static final class Trace implements Painter {
        int count,reflections,lastColor,smoke,materials;float energy,x,y;
        public void waterFoam(float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha) {
            check(Float.isFinite(x+y+ax+ay+bx+by+phase+alpha),"finite foam inputs");
            check(kind>=0 && kind<=5 && alpha>0 && alpha<=1,"bounded foam inputs");
            count++;energy+=alpha;this.x=x;this.y=y;
        }
        public void waterReflection(float x,float y,float ax,float ay,float bx,float by,float phase,int color) {
            check(Float.isFinite(x+y+ax+ay+bx+by+phase),"finite reflection inputs");
            reflections++;lastColor=color;
        }
        public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){materials++;this.x=x;this.y=y;lastColor=color;}
        public void smokePuff(float x,float y,float radius,float phase,int color){smoke++;lastColor=color;}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
        public void rect(float x,float y,float w,float h,int c){}
        public void poly(float[] p,int c){}
        public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}
    }
    public static void run() {
        Game g=new Game();
        for(int kind=0;kind<3;kind++)for(int heading=0;heading<32;heading++) {
            Body b=new Body(kind,kind-1,300,400,heading*(float)StrictMath.PI/16,kind==2);
            float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
            Trace idle=new Trace();WaterFoam.bow(g,idle,b);check(idle.count==0,"idle has no bow pressure wake");
            b.vx=c*15;b.vy=s*15;Trace slow=new Trace();WaterFoam.bow(g,slow,b);
            b.vx=c*60;b.vy=s*60;Trace fast=new Trace();WaterFoam.bow(g,fast,b);
            check(fast.count==2 && slow.count==2 && fast.energy>slow.energy,"two bounded crests grow with forward speed");
            b.vx=-c*40;b.vy=-s*40;Trace reverse=new Trace();WaterFoam.bow(g,reverse,b);check(reverse.count==0,"no bow wave when going astern");
            b.vx=-s*40;b.vy=c*40;Trace sideways=new Trace();WaterFoam.bow(g,sideways,b);check(sideways.count==0,"sideways motion does not manufacture forward pressure");
            check(b.x==300 && b.y==400 && g.visualTicks==0,"drawing does not advance physics");
        }
        for(int tick=0;tick<500;tick++)check(WaterFoam.phase(tick,7)==WaterFoam.phase(tick+240,7),"seamless phase period");
        Trace t=new Trace();RiverCamera c=new RiverCamera(17,29,2);
        Painter p=new RiverPainter(new SceneryOcclusion(new OpacityPainter(new ViewportPainter(t,10000,10000),.5f)),c);
        p.waterFoam(300,400,20,0,0,10,1,2,.8f);
        check(t.count==1 && Math.abs(t.energy-.4f)<.001f,"shader primitive and opacity survive wrappers");
        check(t.x==c.screenX(300,400) && t.y==c.screenY(300,400),"shared camera owns projection");
        Trace root=new Trace();g.render(root);
        check(root.count>0,"real Renderer root forwards water patches instead of silently falling back");
        Trace scaled=new Trace();ScreenView.scaled(new SceneryOcclusion(scaled),10,20,2).waterFoam(3,4,5,0,0,2,0,2,.5f);
        check(scaled.count==1 && scaled.x==16 && scaled.y==28,"screen transform forwards shader patch");
        p.waterReflection(300,400,20,0,0,10,1,0xc0ff4422);
        check(t.reflections==1 && t.lastColor==0x60ff4422,"reflection tint/opacity survive actual wrapper chain");
        WaterReflection.draw(g,new RiverPainter(new SceneryOcclusion(t),c),300,400,10,20,0x8000ff00,4);
        check(t.reflections==2 && t.lastColor==0x8000ff00,"shared water reflection reaches native painter");
        p.smokePuff(300,400,20,1,0xc01c201b);
        check(t.smoke==1 && t.lastColor==0x601c201b,"dark smoke tint/opacity survive wrapper chain");
        p.material(300,400,20,0,0,10,EffectMaterial.GLOW,0,1,0xc0ffaa44);
        check(t.materials==1 && t.lastColor==0x60ffaa44 && t.x==c.screenX(300,400),"material projection and opacity survive actual wrapper chain");
        check(root.materials>0,"actual renderer forwards procedural light materials");
        Trace ring=new Trace();SoftCue.ring(ring,30,40,20,.75f,0xffffffff,1);
        check(ring.materials==3,"progress rings have a fixed submission budget");
        System.out.println(checks+" water shader direction, phase, bounds and wrapper assertions passed");
    }
}
