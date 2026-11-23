package com.bargeboss.game;
/** Shared bounded reaction state: recoil, dry-ground retreat, then return to work. */
final class ShoreCrewReaction {
 static final int CAPACITY=64;
 final int[] ticks=new int[CAPACITY];
 final float[] dx=new float[CAPACITY],dy=new float[CAPACITY],awayX=new float[CAPACITY],awayY=new float[CAPACITY];
 private final float[] baseX=new float[CAPACITY],baseY=new float[CAPACITY],at=new float[2];
 private int count;private boolean prepared;
 void reset(){prepared=false;for(int i=0;i<CAPACITY;i++){ticks[i]=0;dx[i]=dy[i]=0;}}
 private void prepare(Game g){if(prepared)return;count=g.montroseStage()?0:6+(g.tutorial!=null?TrainingYardCrew.count():0);if(count>CAPACITY)throw new IllegalStateException("crew capacity");
  for(int i=0;i<count;i++){DockWorkers.basePosition(g,i,at);baseX[i]=at[0];baseY[i]=at[1];}prepared=true;
 }
 int aim(Game g,float px,float py){prepare(g);RiverCamera c=g.layout.camera;
  for(int i=0;i<count;i++)if(i>=6 || g.berthDisplayEnabled(i/2)){float x=c.screenX(baseX[i]+dx[i],baseY[i]+dy[i]),y=c.screenY(baseX[i]+dx[i],baseY[i]+dy[i])-GroundArt.LAND_RISE*c.scale;
   if(Math.abs(px-x)<10*c.scale && py>y-25*c.scale && py<y+3*c.scale)return i;
  }return -1;
 }
 float x(int i){return baseX[i]+dx[i];}float y(int i){return baseY[i]+dy[i];}
 boolean hit(Game g,float x,float y,float vx,float vy){prepare(g);
  for(int i=0;i<count;i++)if((i>=6 || g.berthDisplayEnabled(i/2)) && Physics.length(x-baseX[i]-dx[i],y-baseY[i]-dy[i])<9){
   if(ticks[i]<100){float len=Math.max(1,Physics.length(vx,vy));awayX[i]=vx/len;awayY[i]=vy/len;ticks[i]=150;}
   return true;
  }return false;
 }
 void step(Game g){for(int i=0;i<count;i++)if(ticks[i]>0){int t=--ticks[i];
  if(t>60){float speed=t>132?.36f:.65f;float xx=dx[i]+awayX[i]*speed,yy=dy[i]+awayY[i]*speed;
   if(Physics.length(xx,yy)<=34 && safeRoute(g,baseX[i],baseY[i],xx,yy)){dx[i]=xx;dy[i]=yy;}
   else {float old=awayX[i];awayX[i]=-awayY[i];awayY[i]=old;}
  }else {dx[i]*=.9f;dy[i]*=.9f;if(t==0)dx[i]=dy[i]=0;}
 }}
 private boolean safeRoute(Game g,float x,float y,float dx,float dy){for(int i=1;i<=8;i++)if(!safe(g,x+dx*i/8,y+dy*i/8))return false;return true;}
 private boolean safe(Game g,float x,float y){
  if(g.inWater(x,y)||g.inWater(x-6,y)||g.inWater(x+6,y)||g.inWater(x,y-6)||g.inWater(x,y+6))return false;
  if(g.tutorial!=null){
   for(int i=0;i<FireworksLesson.PILES;i++)if(Math.abs(x-FireworksLesson.x(i)-15)<34 && Math.abs(y-FireworksLesson.y(i))<47)return false;
   for(int i=0;i<TrainingChannelView.FACTORY_PLACEMENTS;i++){int cell=TrainingChannelView.factoryCell(i);if(Math.abs(x-TrainingChannelView.factoryX(i))<FactoryArt.footprintX(cell)+7 && Math.abs(y-TrainingChannelView.factoryY(i))<FactoryArt.footprintY(cell)+7)return false;}
  }return true;
 }
}
