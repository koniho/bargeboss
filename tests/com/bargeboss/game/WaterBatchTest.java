package com.bargeboss.game;

/** Batch overlap barriers, ordering and bounded submission reduction. */
public final class WaterBatchTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static final class Count implements Painter {
        int calls,polys,groups;final java.util.ArrayList<Float> order=new java.util.ArrayList<Float>();
        public void poly(float[] q,int c){calls++;polys++;order.add(q[0]);}
        public void polygons(float[][] q,int count,int c){calls++;groups++;polys+=count;
            for(int i=0;i<count;i++){order.add(q[i][0]);for(int j=0;j<i;j++)check(!overlap(q[i],q[j]),"batch never merges intersecting or touching AA bounds");}
        }
        public void rect(float x,float y,float w,float h,int c){calls++;order.add(-999f);}
        public void line(float x,float y,float xx,float yy,float w,int c){calls++;}
        public void circle(float x,float y,float r,int c){calls++;}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){calls++;}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){calls++;}
    }
    private static boolean overlap(float[] a,float[] b){float[] aa=box(a),bb=box(b);return aa[0]<=bb[2] && aa[2]>=bb[0] && aa[1]<=bb[3] && aa[3]>=bb[1];}
    private static float[] box(float[] q){float l=Float.MAX_VALUE,t=l,r=-l,b=-l;for(int i=0;i<q.length;i+=2){l=Math.min(l,q[i]);r=Math.max(r,q[i]);t=Math.min(t,q[i+1]);b=Math.max(b,q[i+1]);}return new float[]{l-2,t-2,r+2,b+2};}
    private static float[] quad(float x,float y){return new float[]{x,y,x+8,y,x+8,y+3,x,y+3};}
    public static void run(){
        Count c=new Count();WaterBatchPainter p=new WaterBatchPainter(c,960,600);
        for(int i=0;i<100;i++)p.poly(quad(i%20*30,i/20*30),0x44447799);p.flush();
        check(c.polys==100 && c.calls==1,"disjoint same-color shapes use one native fill");
        c=new Count();p=new WaterBatchPainter(c,960,600);p.poly(quad(10,10),0x44447799);p.poly(quad(12,10),0x44779944);p.poly(quad(14,10),0x44447799);p.flush();
        check(c.calls==3 && c.order.get(0)==10 && c.order.get(1)==12 && c.order.get(2)==14,"translucent overlap retains painter order across colors");
        p.poly(quad(100,100),0x44447799);p.rect(0,0,1,1,0xff000000);p.poly(quad(200,100),0x44447799);p.flush();
        check(c.order.get(4)==-999,"non-polygon primitives flush pending shapes before drawing");
        c=new Count();p=new WaterBatchPainter(c,960,600);for(int i=0;i<1000;i++)p.poly(quad(i%20*30,i/20*10),0x44447799);p.flush();
        check(c.polys==1000,"capacity flushes never drop polygons");
        Game root=new Game();root.startTutorial();Game g=root.activeGame();g.tutorial.lesson=Tutorial.HOLD;g.tutorial.setup(g);
        c=new Count();WaveView.surface(g,c);check(c.groups>0 && c.calls<c.polys,"real harbor waves reduce native submissions");
        System.out.println(checks+" water batching, overlap, capacity and submission assertions passed");
    }
    public static void main(String[] args){run();}
}
