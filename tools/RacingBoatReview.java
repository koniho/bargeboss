package com.bargeboss.game;
import java.awt.*;import java.awt.image.*;import javax.imageio.*;import java.io.*;
public final class RacingBoatReview {
 public static void main(String[] args)throws Exception{
  Game g=ReplayInput.montroseJob(Challenges.HAULOUT);g.configureCanvas(1333,750);Body boat=g.cargo[0];
  g.tug.x=boat.x-60;g.tug.y=boat.y+65;g.layout=Layout.focused(1333,750,boat.x,boat.y,3.3f,666,410);
  DialogueReview.capture(g,"aint-broke-star-dock");
  BufferedImage sheet=ImageIO.read(new File("assets/sprites/montrose-moored-boats-v1.png")),normal=ImageIO.read(new File("assets/sprites/montrose-moored-normals-v1.png")),material=ImageIO.read(new File("docs/art-studies/aint-broke/racing-material-v1.png"));
  BufferedImage review=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D ink=review.createGraphics();ink.setColor(new Color(0x081c27));ink.fillRect(0,0,1280,720);DesktopPainter p=new DesktopPainter(ink);
  for(int pose=0;pose<32;pose++){
   int frame=192+pose;float x=80+pose%8*160,y=100+pose/8*180;
   p.sprite(SpriteAtlas.MOORED_BOATS,frame,x,y,200,200,0,1);p.normalSprite(frame,x,y,200,200,-18,12,20,80,0xffffd68d,.11f);
   double heading=pose*Math.PI/16;float wx=12*(float)Math.cos(heading)-2*(float)Math.sin(heading),wy=12*(float)Math.sin(heading)+2*(float)Math.cos(heading);
   int px=Math.round(64+RiverCamera.Q*(wx+wy)*128/96),py=Math.round(89.6f+(RiverCamera.Q*RiverCamera.DEPTH*(wy-wx)-5.5f*ArticulatedModel.Z_SCALE)*128/96);
   int packed=normal.getRGB(frame%16*128+px,frame/16*128+py);
   if(BoatSurfaceLight.response(packed,(px+.5f)/128,(py+.5f)/128,wx,wy,25,90)<.2f || BoatSurfaceLight.response(packed,(px+.5f)/128,(py+.5f)/128,wx,wy,-15,90)>.02f)throw new AssertionError("Racing deck normal faces down "+pose);
   for(int yy=0;yy<128;yy++)for(int xx=0;xx<128;xx++){
    int sx=frame%16*128+xx,sy=frame/16*128+yy,a=sheet.getRGB(sx,sy)>>>24;
    if(a!=(normal.getRGB(sx,sy)>>>24) || a!=(material.getRGB(pose%8*128+xx,pose/8*128+yy)>>>24))throw new AssertionError("Unregistered racing maps "+pose);
    if((xx==0||xx==127||yy==0||yy==127)&&a!=0)throw new AssertionError("Racing crop overflow");
   }
  }ink.dispose();ImageIO.write(review,"png",new File("out/aint-broke-turntable.png"));
  g=ReplayInput.montroseJob(Challenges.HAULOUT);g.configureCanvas(1333,750);boat=g.cargo[0];
  g.haulout.requested=true;g.haulout.liftTicks=240;g.haulout.rig.update(1);g.haulout.towed=g.haulout.pushed=true;
  boat.x=MontroseHaulout.X;boat.y=MontroseHaulout.Y;boat.angle=MontroseHaulout.ANGLE;boat.awaitingPickup=false;
  g.tug.x=MontroseHaulout.X;g.tug.y=MontroseHaulout.Y+100;
  g.layout=Layout.focused(1333,750,MontroseHaulout.X,MontroseHarbor.mapY(255),2.4f,666,420);
  DialogueReview.capture(g,"aint-broke-lift-lowered");for(int i=0;i<240;i++)g.haulout.step(g);
  DialogueReview.capture(g,"aint-broke-lift-raised");
  g=ReplayInput.montroseJob(Challenges.RAFT_RESCUE);g.configureCanvas(1333,750);g.rescue.distress(g);g.dialogue.close(g);
  g.layout=Layout.focused(1333,750,MontroseRescue.RAFT_X-100,MontroseRescue.RAFT_Y+30,2.4f,666,410);
  DialogueReview.capture(g,"montrose-storm-before");g.weather.eveningTicks=1800;g.weather.stormTicks=600;g.weather.towTicks=3600;
  DialogueReview.capture(g,"montrose-storm-after");
  System.out.println("32 racing poses: identical color/normal/material coverage and clear gutters");
 }
}
