package com.bargeboss.game;
public final class FlameArtTest {
 private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 public static void run(){
  Game g=new Game();g.resetPracticeYard();g.tutorial=new Tutorial();g.tutorial.fireworks.ignite();
  for(int tick=0;tick<32;tick++){g.visualTicks=tick;FlameArt.step(g);}
  check(g.smoke.emitted==20,"each of ten sources emits twice in 32 ticks");
  for(int pile=0;pile<FireworksLesson.PILES;pile++)for(int i=0;i<FlameArt.INSTANCES;i++){
   int found=0;for(int j=0;j<g.smoke.emitted;j++)if(g.smoke.x[j]==FlameArt.x(i) && g.smoke.y[j]==FlameArt.y(i)+FireworksLesson.y(pile)-FireworksLesson.Y){found++;check(g.smoke.rise[j]>GroundArt.LAND_RISE,"smoke starts inside its own rising flame");}
   check(found==2,"each flame owns a distinct registered smoke source");
  }
  for(int tick=0;tick<1000;tick++){
   g.visualTicks=tick;Capture p=new Capture(g);FlameArt.draw(g,p);check(p.count==5,"bounded five blended flame draws");
  }
  check(g.smoke.emitted==20,"drawing cannot emit smoke");
  g.tutorial.fireworks.heat=0;FlameArt.step(g);check(g.smoke.emitted==20,"extinguished flames stop every smoke source");
  boolean prepared=false;for(int a:LevelAssets.tutorial().preparationAtlases())prepared|=a==SpriteAtlas.FLAME;
  check(prepared && SpriteAtlas.count(SpriteAtlas.FLAME)==16,"all flame frames are prepared with upcoming tutorial");
  System.out.println("Ten registered flame/smoke anchors, bounded blended draws and lifecycle passed");
 }
 private static final class Capture implements Painter {
  final Game g;int count;Capture(Game g){this.g=g;}
  public void spriteBlend(int a,int f,int next,float x,float y,float w,float h,float r,float mix,float alpha){
   check(a==SpriteAtlas.FLAME && f>=0 && f<16 && next==(f+1)%16,"loop frames stay valid including seam");
   check(mix>=0 && mix<1 && alpha>0 && alpha<1,"continuous translucent frame blend");
   check(x==g.layout.camera.screenX(FlameArt.x(count),FlameArt.y(count)),"base X remains planted");
   check(y==g.layout.camera.screenY(FlameArt.x(count),FlameArt.y(count))-GroundArt.LAND_RISE*g.layout.camera.scale,"base Y remains planted");count++;
  }
  public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){throw new AssertionError();}
  public void rect(float x,float y,float w,float h,int c){} public void circle(float x,float y,float r,int c){} public void poly(float[] xy,int c){} public void line(float x,float y,float xx,float yy,float w,int c){}
 }
 public static void main(String[] args){run();}
}
