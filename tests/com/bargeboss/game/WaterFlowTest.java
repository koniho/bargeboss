package com.bargeboss.game;
/** Exact scalar oracle protects shore visibility, wave direction and replay physics. */
public final class WaterFlowTest {
 static int checks;
 static void same(float a,float b,String why){checks++;if(Float.floatToIntBits(a)!=Float.floatToIntBits(b))throw new AssertionError(why+": "+a+" != "+b);}
 static Game fixture(boolean reference){Game g=ReplayInput.montroseJob(Challenges.RAFT_RESCUE);g.rescue.rafted=false;g.dialogue.close(g);g.water.referenceSampling=reference;
  g.tug.x=MontroseHarbor.mapX(1100);g.tug.y=MontroseHarbor.mapY(550);g.tug.angle=(float)StrictMath.PI;
  for(int i=0;i<2;i++){Body b=g.cargo[i];b.x=g.tug.x+90;b.y=g.tug.y+(i==0?-30:30);b.angle=g.tug.angle;if(!g.tow.attach(g,b,g.tug))throw new AssertionError("tow fixture");}
  return g;
 }
 static void body(Body a,Body b){same(a.x,b.x,"x");same(a.y,b.y,"y");same(a.vx,b.vx,"vx");same(a.vy,b.vy,"vy");same(a.angle,b.angle,"heading");same(a.heave,b.heave,"heave");same(a.spin,b.spin,"spin");}
 public static void run(){
  float[] out=new float[4];Game g=fixture(false);
  for(int stage=0;stage<2;stage++){
   if(stage==1){g=new Game();g.reset();}
   for(int i=0;i<WaterSurface.CAPACITY;i++){g.water.x[i]=MontroseHarbor.mapX(400+i*7);g.water.y[i]=MontroseHarbor.mapY(350+i%13*23);g.water.age[i]=i*11%360;g.water.heading[i]=i*.31f;g.water.strength[i]=(i%9)*.14f;}
   g.water.rebuildWakeBounds();
   for(int i=0;i<800;i++){
    g.settings.turnWaves(1);if(i%100==0)g.settings.adjustWaves(i%200==0?-4:6);
    float x=MontroseHarbor.mapX(350+i%37*45),y=MontroseHarbor.mapY(250+i%29*43),tick=i*17.5f;
    g.water.flow(g,x,y,tick,i%2==0?g.tug:null,out);
    g.water.referenceSampling=true;
    same(out[0],g.water.value(g,x,y,tick,i%2==0?g.tug:null,1),"flow X");same(out[1],g.water.value(g,x,y,tick,i%2==0?g.tug:null,2),"flow Y");
    same(out[2],SeaWaves.value(g,x,y,tick,1),"sea X");same(out[3],SeaWaves.value(g,x,y,tick,2),"sea Y");
    float height=g.water.height(g,x,y,tick,null);g.water.referenceSampling=false;
    same(height,g.water.height(g,x,y,tick,null),"bounded height");
   }
  }
  g=fixture(false);
  for(int tick=0;tick<720;tick++){
   g.tug.x=1000+(tick%80)*9;g.tug.y=-400+(tick%61)*5;g.tug.vx=50;g.tug.wakeTrailTicks=7;
   g.water.step(g);g.visualTicks++;
   for(int k=0;k<5;k++){float x=g.tug.x-k*90,y=g.tug.y+k*30;
    g.water.flow(g,x,y,g.visualTicks,null,out);g.water.referenceSampling=true;
    same(out[0],g.water.value(g,x,y,g.visualTicks,null,1),"aged/replaced wake X");same(out[1],g.water.value(g,x,y,g.visualTicks,null,2),"aged/replaced wake Y");g.water.referenceSampling=false;}
  }
  for(int tick=0;tick<WaterSurface.LIFE;tick++)g.water.advanceWakes();
  g.water.flow(g,500,-400,g.visualTicks,null,out);same(out[0],g.water.value(g,500,-400,g.visualTicks,null,1),"expired pool");
  Game a=fixture(false),b=fixture(true);
  for(int tick=0;tick<300;tick++){
   ReplayInput.throttle(a,-70,0);ReplayInput.throttle(b,-70,0);a.step();b.step();body(a.tug,b.tug);
   for(int i=0;i<2;i++)body(a.cargo[i],b.cargo[i]);
   for(int i=0;i<a.mooredFleet.boats.length;i++){MooredFleet.FloatBoat x=a.mooredFleet.boats[i],y=b.mooredFleet.boats[i];same(x.x,y.x,"fleet X");same(x.y,y.y,"fleet Y");same(x.heave,y.heave,"fleet heave");same(x.yaw,y.yaw,"fleet yaw");}
  }
  if(a.tow.count()!=2 || a.water.emitted<60)throw new AssertionError("replay must retain two active tows and wakes: "+a.tow.count()+" emitted "+a.water.emitted+" speed "+a.tug.speed()+" hull "+a.hull);
  System.out.println(checks+" bit-exact paired wave/wake and moving two-tow replay assertions passed");
 }
 public static void main(String[] args){run();}
}
