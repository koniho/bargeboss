package com.bargeboss.game;
/** Detached crests move out from a recorded trajectory, without turning with their emitter. */
public final class WakePropagationTest {
    private static int checks;
    private static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
    private static final class Trace implements Painter {
        final RiverCamera camera;final float angle;float widest,maxForward=-10000;int left,right,count;
        final float[] x=new float[16],y=new float[16],ax=new float[16],ay=new float[16],phase=new float[16];
        Trace(RiverCamera c,float a){camera=c;angle=a;}
        public void waterFoam(float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha){
            if(kind!=5)return;
            this.x[count]=camera.worldX(x,y);this.y[count]=camera.worldY(x,y);
            this.ax[count]=ax;this.ay[count]=ay;this.phase[count]=phase;count++;
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
            check(old.widest>young.widest && old.maxForward>young.maxForward,"crests propagate sideways and forward through the water after emitter stops/turns");
            for(int j=0;j<16;j++) {
                check(Math.abs(old.ax[j]-young.ax[j])<.001f && Math.abs(old.ay[j]-young.ay[j])<.001f,"aging translates a crest without stretching its wavelength");
                check(old.phase[j]==young.phase[j],"crest material rides with the water packet instead of boiling in place");
                float dx=old.x[j]-young.x[j],dy=old.y[j]-young.y[j];
                float c=(float)StrictMath.cos(angle),sn=(float)StrictMath.sin(angle),side=j<8?-1:1;
                check(Math.abs(dx-(.24f*c-side*.32f*sn)*80)<.003f && Math.abs(dy-(.24f*sn+side*.32f*c)*80)<.003f,"every point on an arm has the same normal propagation velocity");
            }
            check(w.age[0]==100 && w.heading[0]==angle,"rendering leaves wake history untouched");
        }
        System.out.println(checks+" outward wake propagation assertions passed");
    }
}
