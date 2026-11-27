package com.bargeboss.game;
import java.awt.*;import java.awt.image.*;import java.io.*;import javax.imageio.*;
/** Actual shared-renderer review and registration gates for the M3. */
public final class PoliceBoatReview {
 public static void main(String[] args)throws Exception{
  new File("out").mkdirs();
  BufferedImage board=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D ink=board.createGraphics();ink.setColor(new Color(0x081c27));ink.fillRect(0,0,1280,720);DesktopPainter p=new DesktopPainter(ink);
  BufferedImage alb=ImageIO.read(new File(PoliceBoatBake.ROOT,"m3-albedo-v1.png")),norm=ImageIO.read(new File(PoliceBoatBake.ROOT,"m3-normal-v1.png")),mat=ImageIO.read(new File(PoliceBoatBake.ROOT,"m3-material-v1.png"));
  for(int pose=0;pose<32;pose++){
   int frame=PoliceBoatModel.BASE+pose;float x=80+pose%8*160,y=105+pose/8*180;
   p.sprite(SpriteAtlas.MOORED_BOATS,frame,x,y,230,230,0,1);p.normalSprite(frame,x,y,230,230,-18,12,20,80,0xffffd68d,.15f);
   for(int yy=0;yy<128;yy++)for(int xx=0;xx<128;xx++){
    int sx=pose%8*128+xx,sy=pose/8*128+yy,a=alb.getRGB(sx,sy)>>>24;
    if(a!=(norm.getRGB(sx,sy)>>>24)||a!=(mat.getRGB(sx,sy)>>>24))throw new AssertionError("M3 map registration "+pose);
    if((xx==0||xx==127||yy==0||yy==127)&&a!=0)throw new AssertionError("M3 gutter "+pose);
   }
   // Center roof: upward-facing aluminum must reject light underneath.
   double h=pose*Math.PI/16;float wx=-2*(float)Math.sin(h),wy=2*(float)Math.cos(h);
   int px=Math.round(64+RiverCamera.Q*(wx+wy)*128/96),py=Math.round(89.6f+(RiverCamera.Q*RiverCamera.DEPTH*(wy-wx)-14.3f*ArticulatedModel.Z_SCALE)*128/96);
   int packed=norm.getRGB(pose%8*128+px,pose/8*128+py);
   if(BoatSurfaceLight.response(packed,(px+.5f)/128,(py+.5f)/128,wx,wy,35,90)<.25f || BoatSurfaceLight.response(packed,(px+.5f)/128,(py+.5f)/128,wx,wy,0,90)>.05f)throw new AssertionError("M3 roof normal "+pose);
  }
  ink.dispose();ImageIO.write(board,"png",new File("out/police-m3-turntable-v1.png"));
  Game g=ReplayInput.montroseJob(Challenges.WAKE_PATROL);g.configureCanvas(1333,750);
  Body b=null;for(Body candidate:g.job.bodies())if(candidate!=null && candidate.hullKind==PoliceBoatModel.KIND)b=candidate;
  if(b==null)throw new AssertionError("Patrol does not use M3");
  g.tug.x=b.x-65;g.tug.y=b.y+65;g.layout=Layout.focused(1333,750,b.x,b.y,4,666,420);g.changeLighting(2);g.dialogue.close(g);
  DialogueReview.capture(g,"police-m3-dusk-v1");
  WakePatrolJob job=(WakePatrolJob)g.job;job.radio.lassoed(0);
  for(int i=0;i<24;i++){g.radio.step();job.radio.step(g);}DialogueReview.capture(g,"police-m3-captain-lasso-v1");
  for(int i=0;i<PoliceRadio.SHOW_TICKS;i++){g.radio.step();job.radio.step(g);}
  job.radio.delivered(0);for(int i=0;i<24;i++){g.radio.step();job.radio.step(g);}
  g.visualTicks=60;DialogueReview.capture(g,"police-m3-captain-handover-v1");
  System.out.println("M3 32 poses: registered channels, transparent borders, roof light rejection, mission integration");
 }
}
