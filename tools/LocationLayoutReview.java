package com.bargeboss.game;
import java.awt.*;import java.awt.image.*;import javax.imageio.*;import java.io.*;
/** Production isometric layout plus a legible source-layout plan; no replacement runtime art. */
public final class LocationLayoutReview {
 static final int W=1800,H=1250;
 public static void main(String[] args)throws Exception{
  new File("out/location-layout").mkdirs();
  for(int stage=0;stage<2;stage++){
   Game g=new Game();if(stage==1){g.challengeId=Challenges.INDIANA;g.reset();}
   float[] coast=stage==0?CalumetRiver.COAST:IndianaHarbor.COAST;RiverCamera base=new RiverCamera(0,0,1);
   float l=Float.MAX_VALUE,t=l,r=-l,b=-l;
   for(int i=0;i<coast.length;i+=2){float x=base.screenX(coast[i],coast[i+1]),y=base.screenY(coast[i],coast[i+1]);l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);}
   float scale=Math.min((W-250)/(r-l),(H-260)/(b-t));RiverCamera c=new RiverCamera((W-(r-l)*scale)*.5f-l*scale,(H-(b-t)*scale)*.5f-t*scale,scale);
   g.layout=Layout.renderTile(W,H,c);capture(g,stage==0?"calumet-overview":"indiana-overview",stage==0?"CALUMET RIVER / LAKE MICHIGAN TO BASIN 5":"INDIANA HARBOR / CANAL FORKS");
   if(stage==0){for(int i=0;i<3;i++){float[][] center={{CalumetRiver.ROUTE[3][0],CalumetRiver.ROUTE[3][1]},{CalumetRiver.JOB_SITES[2][0],CalumetRiver.JOB_SITES[2][1]},{CalumetRiver.ROUTE[CalumetRiver.ROUTE.length-1][0],CalumetRiver.ROUTE[CalumetRiver.ROUTE.length-1][1]}};
      g.layout=Layout.focused(W,H,center[i][0],center[i][1],.85f,W*.5f,H*.5f);capture(g,"calumet-detail-"+i,"CALUMET / SURVEY LAYOUT PASS");}}
  }
 }
 static void capture(Game g,String name,String title)throws Exception{
  BufferedImage im=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=im.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
  Painter p=new DesktopPainter(graphics);p.beginFrame();SurveyLevelView.draw(g,p,g.level());
  p.rect(0,0,W,80,0xe30c161e);Lettering.center(p,title,W*.5f,35,20,VisualStyle.CREAM);
  p.rect(0,H-76,W,76,0xe30c161e);Lettering.center(p,"LAYOUT FIRST / GRASS / CONCRETE / WORK YARDS / RAIL / BRIDGES",W*.5f,H-46,13,VisualStyle.CREAM);
  float x=W-120,y=140;p.line(x,y,x+48,y-28,3,0xffd2b968);p.line(x+48,y-28,x+32,y-25,3,0xffd2b968);p.line(x+48,y-28,x+39,y-15,3,0xffd2b968);Lettering.center(p,"N",x+54,y-44,16,0xffd2b968);
  graphics.dispose();ImageIO.write(im,"png",new File("out/location-layout/"+name+".png"));System.out.println(name);
 }
}
