package com.bargeboss.game;
/** Production vessels remain sprite-only at close and wide zooms after the live-model rollback. */
public final class SpriteBoatRoutingTest {
 public static void run(){Game g=ReplayInput.montroseJob(Challenges.HAULOUT);g.dialogue.close(g);
  for(float scale:new float[]{.5f,3.1f}){
   MooredFleet.FloatBoat boat=g.mooredFleet.boats[0];g.tug.x=boat.x;g.tug.y=boat.y;g.layout=Layout.focused(1333,600,boat.x,boat.y,scale,666,300);
   Probe p=new Probe();MooredBoats.draw(g,p,0);if(p.hulls==0||p.gradients!=0)throw new AssertionError("moored boat must use sprites at every zoom");
   for(int heading=0;heading<32;heading++){p=new Probe();Body b=new Body(10,0,boat.x,boat.y,heading*(float)Math.PI/16,false,true);CruiserView.draw(g,new RiverPainter(p,g.layout.camera),b);if(p.hulls==0||p.gradients!=0)throw new AssertionError("towed cruiser must use sprites");}
  }
  System.out.println("Sprite-only moored and active boats verified at close/wide zoom and 32 headings");
 }
 static class Probe implements Painter {
  int hulls,gradients;
  public void shadedTriangle(float x,float y,int c,float xx,float yy,int cc,float xxx,float yyy,int ccc){gradients++;}
  public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){if(a==SpriteAtlas.MOORED_BOATS)hulls++;}
  public void spriteBlend(int a,int f,int b,float x,float y,float w,float h,float r,float m,float alpha){if(a==SpriteAtlas.MOORED_BOATS)hulls++;}
  public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}public void line(float x,float y,float xx,float yy,float w,int c){}public void poly(float[] xy,int c){}
 }
}
