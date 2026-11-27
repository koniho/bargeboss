package com.bargeboss.game;
/** Patrol event edges and the shared spotlight geometry, without modal input interception. */
public final class PoliceBoatTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static void ticks(Game g,WakePatrolJob job,int n){for(int i=0;i<n;i++){g.radio.step();job.step(g);}}
 static final class BeamProbe implements Painter {
  public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){if(a==SpriteAtlas.POLICE_PORTRAIT)portraits++;}
  public void spriteAffine(int a,int f,float x,float y,float ax,float ay,float bx,float by,float alpha){if(a==SpriteAtlas.POLICE_PORTRAIT && alpha>0)portraits++;}
  public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
  public void circle(float x,float y,float r,int c){} public void rect(float x,float y,float w,float h,int c){}
  public void poly(float[] xy,int c){} public void line(float x,float y,float xx,float yy,float w,int c){}
  int portraits,triangles,red,blue;float sum;
  public void shadedTriangle(float x,float y,int a,float xx,float yy,int b,float xxx,float yyy,int c){triangles++;sum+=x+y+xx+yy+xxx+yyy;if((a&0xffffff)==0xff3040)red++;if((a&0xffffff)==0x3a7bff)blue++;}
 }
 public static void main(String[] args){
  Game g=ReplayInput.montroseJob(Challenges.WAKE_PATROL);g.dialogue.close(g);WakePatrolJob job=(WakePatrolJob)g.job;Body b=job.speeders[0];
  job.ticketed[0]=true;g.cargo[0]=b;b.x=g.tug.x+40;b.y=g.tug.y;b.awaitingPickup=true;
  check(g.tow.attach(g,b,g.tug),"Real lasso connection accepted");ticks(g,job,2);
  check(job.radio.content()!=null && job.radio.content().title.contains("CATCH"),"First attached step queues captain call");
  check(SpeakerPortrait.atlas(g,job.radio.content().header)==SpriteAtlas.POLICE_PORTRAIT,"Police portrait selected");
  check(g.radio.transmitter()==RadioAudio.POLICE && g.radio.transmitting(g,job.police),"Police boat owns transmission");
  check(!g.conversation(),"Captain call leaves controls usable");
  ticks(g,job,PoliceRadio.SHOW_TICKS+2);check(job.radio.pending()==0 && !job.radio.busy(),"Holding the same tow never repeats call");
  g.tow.detachBody(g,b);b.awaitingPickup=false;b.x=WakePatrolJob.RING_X;b.y=WakePatrolJob.RING_Y;b.vx=b.vy=0;
  ticks(g,job,2);check(job.warp[0]>0 && job.radio.content().title.contains("TAKE IT"),"Real impound handover transmits");
  ticks(g,job,24);BeamProbe portrait=new BeamProbe();job.radio.draw(g,portrait);check(portrait.portraits>0,"Handover reopens the portrait panel after earlier call expired");
  job.radio.delivered(0);check(job.radio.pending()==0,"Repeated delivery is deduplicated");
  job.radio.delivered(1);job.radio.delivered(2);check(job.radio.pending()==2,"Simultaneous handovers retained");
  g.delivered=g.quota();check(!job.done(g),"Final delivery waits for acknowledgement before next dispatch");
  ticks(g,job,3*PoliceRadio.SHOW_TICKS+5);check(!job.radio.busy() && job.done(g),"Queued acknowledgements finish");
  BeamProbe first=new BeamProbe(),again=new BeamProbe(),turned=new BeamProbe();g.visualTicks=0;PoliceBoatArt.water(g,first,job.police);PoliceBoatArt.water(g,again,job.police);
  check(first.triangles==320 && first.red==160 && first.blue==160,"Two bounded colored player-style beams");
  check(first.sum==again.sum && g.visualTicks==0,"Rendering is repeatable and does not advance rotation");
  g.visualTicks=60;PoliceBoatArt.water(g,turned,job.police);check(first.sum!=turned.sum,"Beams sweep as fixed simulation clock advances");
  check(Math.abs(Physics.angle(PoliceBoatArt.beamAngle(job.police,0,0)-PoliceBoatArt.beamAngle(job.police,240,0)))<.0001f,"Seamless four-second revolution");
  g.reset();check(!(g.job instanceof WakePatrolJob) || !((WakePatrolJob)g.job).radio.busy(),"Restart clears radio state");
  System.out.println("Police: lasso/handover edge calls, portrait, queue, final acknowledgement, nonmodal input and deterministic red/blue rotation passed");
 }
}
