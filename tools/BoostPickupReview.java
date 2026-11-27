package com.bargeboss.game;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** Actual scene states, including the uncollected powerup that the pickup fixture skips. */
public final class BoostPickupReview {
 public static void main(String[] args)throws Exception{
  File dir=new File(args.length>0?args[0]:"out/boost-powerup");dir.mkdirs();
  for(String state:new String[]{"ready","catch","spent","recharging"})for(float zoom:new float[]{.9f,2.1f}){
   Game g=new Game();ProfileScenario.prepare(g,"montrose-boost-review",1440,900);
   int i=3;float x=MontroseTransit.BUOYS[i*2],y=MontroseTransit.BUOYS[i*2+1];g.tug.x=x+90;g.tug.y=y+20;
   g.transit.flash=0;g.transit.spent[i]=state.equals("ready")?0:state.equals("recharging")?180:720;
   if(state.equals("catch")){g.transit.flash=34;g.transit.flashBuoy=i;}
   g.visualTicks=67;g.layout=Layout.focused(1440,900,x,y,zoom,650,540);
   BufferedImage im=new BufferedImage(1440,900,BufferedImage.TYPE_INT_RGB);Graphics2D p=im.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Renderer.draw(g,new DesktopPainter(p));p.dispose();
   ImageIO.write(im,"png",new File(dir,state+"-"+zoom+".png"));
  }
 }
}
