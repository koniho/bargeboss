package com.bargeboss.game;
/** Bounded, world-anchored contact steam; emitted only by real water impacts. */
final class FireSteam {
 static final int CAPACITY=48,LIFE=70;
 private final float[] x=new float[CAPACITY],y=new float[CAPACITY];
 private final int[] age=new int[CAPACITY],pile=new int[CAPACITY],cooldown;
 private int cursor;int emitted;
 FireSteam(){this(FireworksLesson.PILES);}
 FireSteam(int sources){cooldown=new int[sources];reset();}
 void reset(){for(int i=0;i<CAPACITY;i++)age[i]=LIFE;for(int i=0;i<cooldown.length;i++)cooldown[i]=0;cursor=emitted=0;}
 void step(){for(int i=0;i<CAPACITY;i++)if(age[i]<LIFE)age[i]++;for(int i=0;i<cooldown.length;i++)if(cooldown[i]>0)cooldown[i]--;}
 void hit(int source,float xx,float yy){if(cooldown[source]>0)return;cooldown[source]=4;int i=cursor;cursor=(cursor+1)%CAPACITY;x[i]=xx;y[i]=yy;pile[i]=source;age[i]=0;emitted++;}
 void draw(Game g,Painter p,int source){draw(g,p,source,0);}
 /** Rise is extra sprite-unit height above the shore, for fires above deck level. */
 void draw(Game g,Painter p,int source,float rise){RiverCamera c=g.layout.camera;
  for(int i=0;i<CAPACITY;i++)if(pile[i]==source && age[i]<LIFE){float t=age[i]/(float)LIFE;
   float wx=x[i]-t*12,wy=y[i]-t*7,alpha=Physics.clamp(age[i]/5f,0,1)*(1-t)*.72f;
   SteamPuff.draw(p,c.screenX(wx,wy),c.screenY(wx,wy)-(GroundArt.LAND_RISE+rise+8+t*40)*c.scale,(6+t*15)*c.scale,alpha);
  }
 }
}
