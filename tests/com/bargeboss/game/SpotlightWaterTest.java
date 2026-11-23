package com.bargeboss.game;
/** The shared surface pass must retain searchlights even with ambient waves disabled. */
final class SpotlightWaterTest {
 static void run(){
  Game root=new Game();root.startTutorial();Game g=root.activeGame();g.tug.x=-1500;g.tug.y=600;g.tug.angle=-(float)Math.PI/2;
  g.configureCanvas(1333,600);g.layout=Layout.focused(1333,600,g.tug.x,g.tug.y,2.4f,700,340);g.settings.waterDetail=0;
  Probe p=new Probe();WaveView.surface(g,p);if(p.count!=0)throw new AssertionError("Off searchlight emits no surface mesh");
  g.spotlight.aim(g,g.tug.angle);WaveView.surface(g,p);
  if(p.count!=160 || !p.visible)throw new AssertionError("Training/low-detail surface pass lost the searchlight");
  float size=2*(g.tug.half+g.tug.radius)/TugArt.HULL_FRACTION;
  float lampY=g.layout.camera.screenY(g.tug.x,g.tug.y)+(TugLightArt.offset(0,g.tug.angle,true,true)*size-WaterSurface.lift(g,g.tug))*g.layout.camera.scale;
  if(Math.abs(p.airOriginY-lampY)>.001f)throw new AssertionError("Descending cone must begin at the registered lamp");
  float reach=TugSpotlight.RANGE*g.layout.camera.scale*(float)StrictMath.sqrt(2f/3f)/g.layout.width;
  if(reach<.45f || reach>.65f)throw new AssertionError("Beam reach should be roughly half the screenshot width at close zoom");
  System.out.println("Training/low-detail spotlight surface pass, descending lamp anchor and bounded beam reach passed");
 }
 static final class Probe implements Painter {
  int count;boolean visible;float airOriginY;
  public void shadedTriangle(float a,float b,int ac,float c,float d,int cc,float e,float f,int fc){
   if(count==80)airOriginY=b;count++;visible|=(ac>>>24)>0 || (cc>>>24)>0 || (fc>>>24)>0;
   if(!Float.isFinite(a+b+c+d+e+f))throw new AssertionError("Invalid beam mesh");
  }
  public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
  public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
  public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}
  public void poly(float[] xy,int c){}public void line(float x,float y,float xx,float yy,float w,int c){}
 }
}
