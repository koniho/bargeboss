package com.bargeboss.game;
/** Pale contact vapor, distinct from the deliberately soot-colored exhaust material. */
final class SteamPuff {
 static final StaticLayer LAYER=new StaticLayer(){
  public int key(){return 0x43000005;}public float width(){return 40;}public float height(){return 40;}public boolean backgroundSafe(){return true;}
  public void paint(Painter p){for(int i=0;i<8;i++){float r=17-i*1.35f;p.circle(20,21,r,0x20e1f5f1);p.circle(15,20,r*.68f,0x22e1f5f1);p.circle(24,16,r*.65f,0x22e1f5f1);}}
 };
 static void draw(Painter p,float x,float y,float r,float alpha){if(alpha>0)p.layer(LAYER,x-r,y-r,r/20,alpha);}
 private SteamPuff(){}
}
