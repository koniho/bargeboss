package com.bargeboss.game;
import java.awt.*;import java.awt.image.*;import javax.imageio.ImageIO;import java.io.File;
/** Actual shared scene and all 181 unload poses for visual review. */
public final class CargoOffloadReview {
 public static void main(String[] args)throws Exception{
  File dir=new File("out/cargo-offload");dir.mkdirs();
  Game g=new Game();g.mode=Game.PLAY;int dock=0;DockView.Crane crane=g.dockCranes[dock];
  for(int i=0;i<g.cargo.length;i++)g.cargo[i]=null;
  Body b=new Body(100,2,crane.pickX,crane.pickY,g.voyage.heading(dock));b.destination=dock;b.lineAttached=true;b.mooringTicks=Mooring.ALIGN_TICKS;b.tiedAngle=b.angle;b.tiedX=b.x;b.tiedY=b.y;g.cargo[0]=b;
  g.tug.x=b.x-80;g.tug.y=b.y+80;
  g.layout=Layout.focused(1000,850,(b.x+crane.x)*.5f,(b.y+crane.y)*.5f,1.65f,500,600);
  BufferedImage sheet=new BufferedImage(1800,1020,BufferedImage.TYPE_INT_RGB);Graphics2D contact=sheet.createGraphics();int panel=0;
  for(int tick=0;tick<=180;tick++){
   b.unloadingTicks=tick;b.deliveryRecorded=tick==180;g.visualTicks=tick;
   BufferedImage frame=new BufferedImage(1000,850,BufferedImage.TYPE_INT_RGB);Graphics2D gg=frame.createGraphics();gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
   Renderer.draw(g,new DesktopPainter(gg));gg.dispose();
   ImageIO.write(frame,"png",new File(dir,String.format("offload-%03d.png",tick)));
   if(tick==0 || tick==24 || tick==54 || tick==100 || tick==132 || tick==180){contact.drawImage(frame,(panel%3)*600,(panel/3)*510,600,510,null);panel++;}
  }
  contact.dispose();ImageIO.write(sheet,"png",new File(dir,"BargeBoss-cargo-offload-v1.png"));
  for(int kind=0;kind<2;kind++)for(int tick:new int[]{54,132}){
   Body other=new Body(101+kind,kind,crane.pickX,crane.pickY,g.voyage.heading(dock));other.destination=dock;other.lineAttached=true;other.unloadingTicks=tick;other.mooringTicks=Mooring.ALIGN_TICKS;other.tiedAngle=other.angle;g.cargo[0]=other;g.visualTicks=tick;
   BufferedImage frame=new BufferedImage(1000,850,BufferedImage.TYPE_INT_RGB);Graphics2D gg=frame.createGraphics();gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Renderer.draw(g,new DesktopPainter(gg));gg.dispose();ImageIO.write(frame,"png",new File(dir,(kind==0?"steel-":"containers-")+tick+".png"));
  }
  System.out.println("181 shared-renderer unloading frames, contact sheet and steel/container pickup/landing reviews exported");
 }
}
