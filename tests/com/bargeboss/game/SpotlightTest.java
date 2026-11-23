package com.bargeboss.game;
final class SpotlightTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static void run(){
  Game g=ReplayInput.montroseJob(Challenges.SWIMMER_RESCUE);MontroseRescue r=g.rescue;
  for(int i=0;i<MontroseRescue.PEOPLE;i++){
   check(!r.found[i] && MontroseHarbor.contains(r.x[i],r.y[i]),"Unknown offshore swimmer stays in water");
   for(int j=0;j<i;j++)check(Physics.length(r.x[i]-r.x[j],r.y[i]-r.y[j])>55,"Irregular targets have safe separation");
  }
  check(MontroseRescueView.ordered(g).length==0 && r.swimmerAt(g,r.x[0],r.y[0])<0,"Unknown positions cannot be drawn or tapped");
  g.tug.x=r.x[0];g.tug.y=r.y[0]+200;g.tug.angle=0;g.configureCanvas(1333,750);g.layout=Layout.focused(1333,750,g.tug.x,g.tug.y,2,600,420);
  ReplayInput.down(g,g.tug.x,g.tug.y);ReplayInput.up(g,g.tug.x,g.tug.y);
  check(g.abilities.selected==g.tug && g.spotlight.on,"Public tug tap activates spotlight equipment");
  float x=g.abilities.buttonX(g,0),y=g.abilities.buttonY(g);g.pointerDown(7,x,y);
  RiverCamera camera=g.layout.camera;float dx=camera.screenX(0,-60)-camera.screenX(0,0),dy=camera.screenY(0,-60)-camera.screenY(0,0);
  g.pointerMove(7,x+dx,y+dy);g.pointerUp(7,x+dx,y+dy);
  check(g.spotlight.illuminates(g,r.x[0],r.y[0]),"Aim control projects into world direction");
  r.step(g);check(r.found[0] && MontroseRescueView.ordered(g).length>0,"Lit swimmer becomes rescuable");
  float aim=g.spotlight.relativeAngle;g.tug.angle+=1;check(g.spotlight.relativeAngle==aim && !g.spotlight.illuminates(g,r.x[0],r.y[0]),"Beam rotates with tug without losing relative aim");
  g.spotlight.on=false;r.step(g);check(r.found[0],"Found swimmers remain marked after beam turns away");
  g.tug.angle=0;g.spotlight.aim(g,0);float rx=g.tug.x+100,ry=g.tug.y;
  float inside=g.spotlight.energy(g,rx,ry,0),edge=g.spotlight.energy(g,g.tug.x+100,ry+27,0);
  check(inside>edge && edge>=0 && g.spotlight.energy(g,g.tug.x-100,ry,0)==0,"Soft directional cone excludes rear receivers");
  check(g.spotlight.energy(g,g.tug.x+470,ry,0)<inside*.05f,"Light fades with distance");
  float lit=g.spotlight.face(g,rx,ry,0,-1,0,0);
  check(lit>0 && g.spotlight.face(g,rx,ry,0,1,0,0)==0,"Only source-facing geometry brightens");
  int color=SceneryLighting.get(g).shade(0xff202020,0xff909090,rx,ry,0,-1,0,0);
  float[] weights=SurfaceLighting.sample(g,rx,ry,null);
  MooredBoatLight.Lights lights=MooredBoatLight.lights(g,rx,ry,0);boolean beam=false;
  for(int i=0;i<lights.count;i++)beam|=lights.radius[i]==TugSpotlight.RANGE;
  check(beam,"Moored boats and swimmers receive directional source");
  g.spotlight.on=false;
  int dark=SceneryLighting.get(g).shade(0xff202020,0xff909090,rx,ry,0,-1,0,0);
  float[] unlit=SurfaceLighting.sample(g,rx,ry,null);float sum=0,dim=0;for(float v:weights)sum+=v;for(float v:unlit)dim+=v;
  check((color&255)>(dark&255) && sum>dim,"Machinery and painted hulls brighten under beam");
  for(int source:SpotlightArt.SOURCES){int alias=SpotlightArt.receiver(source);
   for(int f=0;f<SpriteAtlas.count(source);f++)check(SpriteAtlas.pivotX(source,f)==SpriteAtlas.pivotX(alias,f) && SpriteAtlas.pivotY(source,f)==SpriteAtlas.pivotY(alias,f),"Scenery light layers preserve registration");
  }
  g.reset();check(!g.spotlight.on,"New location resets lamp");
  System.out.println("Offshore scattering, concealed targets, tug tap, projected relative aiming and discovery passed");
 }
}
