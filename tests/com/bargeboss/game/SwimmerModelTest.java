package com.bargeboss.game;
/** Continuous idle/distress geometry, deterministic rendering and bounded draw cost. */
public final class SwimmerModelTest {
 private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void run(){
  Game g=ReplayInput.montroseJob(Challenges.SWIMMER_RESCUE);Capture p=new Capture();int max=0;
  float x=g.rescue.x[0],y=g.rescue.y[0];
  for(int state=0;state<2;state++)for(int id=0;id<20;id++){
   g.rescue.state[id]=state;long previous=0;
   for(int tick=0;tick<240;tick++){
    g.visualTicks=tick;p.reset();g.rescue.swimmerModel.draw(g,p,id);long hash=p.hash;
    check(p.calls>10 && p.calls<=64,"bounded nonempty 3-D mesh");max=Math.max(max,p.calls);
    if(tick>0)check(hash!=previous,"every idle and distress tick moves continuously");previous=hash;
    p.reset();g.rescue.swimmerModel.draw(g,p,id);check(p.hash==hash,"same tick reproduces geometry");
    p.reset();SwimmerModel.water(g,p,id);check(p.calls==3,"three bounded motion ripple/splash calls");
   }
  }
  check(g.rescue.x[0]==x && g.rescue.y[0]==y && !g.rescue.failed && g.delivered==0,"render does not change rescue rules/state");
  long start=System.nanoTime();for(int tick=0;tick<1000;tick++){g.visualTicks=tick;for(int id=0;id<20;id++)g.rescue.swimmerModel.draw(g,p,id);}
  System.out.println("Swimmer model: 9600 continuous poses passed; max "+max+" mesh calls/person; 20-person reference CPU "+((System.nanoTime()-start)/1000000.0/1000)+" ms/frame (not native rendering)");
 }
 public static void main(String[] args){run();}
 private static final class Capture implements Painter{
  int calls;long hash;void reset(){calls=0;hash=17;}void v(float f){check(Float.isFinite(f),"finite vertex");hash=31*hash+Float.floatToIntBits(f);}
  public void convexQuad(float a,float b,float c,float d,float e,float f,float g,float h,int color){calls++;v(a);v(b);v(c);v(d);v(e);v(f);v(g);v(h);hash=31*hash+color;}
  public void poly(float[] p,int c){calls++;for(float f:p)v(f);hash=31*hash+c;}
  public void circle(float x,float y,float r,int c){calls++;v(x);v(y);v(r);hash=31*hash+c;}
  public void line(float a,float b,float c,float d,float w,int col){throw new AssertionError("unexpected line");}
  public void rect(float a,float b,float c,float d,int col){throw new AssertionError("unexpected rectangle");}
  public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){throw new AssertionError("no swimmer sprite");}
  public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){throw new AssertionError("no swimmer sprite");}
 }
}
