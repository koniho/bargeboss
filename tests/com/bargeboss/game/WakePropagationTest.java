package com.bargeboss.game;
/** Detached crests move out from a recorded trajectory, without turning with their emitter. */
public final class WakePropagationTest {
    private static int checks;
    private static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
    private static final class Trace implements Painter {
        final RiverCamera camera;final float angle;float widest,maxForward=-10000;int left,right;
        Trace(RiverCamera c,float a){camera=c;angle=a;}
        public void waterFoam(float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha){
            if(kind!=5)return;
            float dx=camera.worldX(x,y)-300,dy=camera.worldY(x,y)-500,c=(float)StrictMath.cos(angle),s=(float)StrictMath.sin(angle);
            float side=-dx*s+dy*c;widest=Math.max(widest,Math.abs(side));maxForward=Math.max(maxForward,dx*c+dy*s);
            if(side<0)left++;else right++;
        }
        public void line(float x,float y,float xx,float yy,float w,int c){}public void rect(float x,float y,float w,float h,int c){}
        public void circle(float x,float y,float r,int c){}public void poly(float[] p,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
    }
    public static void run(){
        for(int heading=0;heading<32;heading++) {
            Game g=new Game();g.reset();WaterSurface w=new WaterSurface();float angle=heading*(float)Math.PI/16;
            w.x[0]=300;w.y[0]=500;w.heading[0]=angle;w.strength[0]=1;w.age[0]=20;
            Trace young=new Trace(g.layout.camera,angle);w.draw(g,new RiverPainter(young,g.layout.camera));
            w.age[0]=100;g.tug.angle=angle+1;g.tug.vx=g.tug.vy=0;
            Trace old=new Trace(g.layout.camera,angle);w.draw(g,new RiverPainter(old,g.layout.camera));
            check(young.left==8 && young.right==8 && old.left==8 && old.right==8,"bounded paired V arms at every heading");
            check(old.widest>young.widest*2 && old.maxForward<0,"crests spread outward and astern after emitter stops/turns");
            check(w.age[0]==100 && w.heading[0]==angle,"rendering leaves wake history untouched");
        }
        System.out.println(checks+" outward wake propagation assertions passed");
    }
}
