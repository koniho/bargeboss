package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Production views plus a world-space site plan for reviewing connected yard layout. */
public final class TutorialYardReview {
 public static void main(String[] args)throws Exception{
  File out=new File("out/tutorial-yard");out.mkdirs();
  BufferedImage map=new BufferedImage(1357,948,BufferedImage.TYPE_INT_RGB);Graphics2D m=map.createGraphics();
  m.setColor(new Color(0x293139));m.fillRect(0,0,1357,948);m.scale(.4,.4);m.translate(-TrainingScenery.MIN_X,-TrainingScenery.MIN_Y);
  m.setColor(new Color(0x123740));for(float[] w:TrainingChannel.WATER_RECTANGLES)m.fillRect((int)w[0],(int)w[1],(int)(w[2]-w[0]),(int)(w[3]-w[1]));
  m.setColor(new Color(0x777865));for(int row=IndustrialRoadView.MIN_ROW;row<IndustrialRoadView.MAX_ROW;row++)for(int col=IndustrialRoadView.MIN_COL;col<IndustrialRoadView.MAX_COL;col++)if(IndustrialRoadView.roadClear(col,row))m.fillRect(col*64,row*64,64,64);
  m.setColor(new Color(0x90b1bd));for(int i=0;i<TrainingChannelView.FACTORY_PLACEMENTS;i++)m.fillRect((int)TrainingChannelView.factoryX(i)-29,(int)TrainingChannelView.factoryY(i)-29,58,58);
  float[] at=new float[3];m.setColor(Color.ORANGE);for(int i=0;i<YardActivity.count();i++){YardActivity.position(i,180,at);m.fillOval((int)at[0]-10,(int)at[1]-10,20,20);}
  m.setColor(Color.GREEN);for(int i=0;i<TrainingYardCrew.count();i++){TrainingYardCrew.position(i,at);m.fillOval((int)at[0]-8,(int)at[1]-8,16,16);}
  m.dispose();ImageIO.write(map,"png",new File(out,"site-plan.png"));
  for(String scene:new String[]{"tutorial-yard","basin-entry","canal-middle","harbor-close"})for(int tick:new int[]{180,360}){
   Game root=new Game();ProfileScenario.prepare(root,scene,1333,600);Game g=root.activeGame();g.visualTicks=tick;
   BufferedImage img=new BufferedImage(1333,600,BufferedImage.TYPE_INT_RGB);Graphics2D p=img.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);root.render(new DesktopPainter(p));p.dispose();ImageIO.write(img,"png",new File(out,scene+"-"+tick+".png"));
  }
  System.out.println("workshops="+TrainingChannelView.FACTORY_PLACEMENTS+" vehicle routes="+YardActivity.count()+" work sites="+TrainingYardCrew.count());
 }
}
