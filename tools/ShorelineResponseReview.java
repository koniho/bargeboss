package com.bargeboss.game;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Actual shared-renderer shoreline close-ups; phase and wake are controlled simulation inputs. */
public final class ShorelineResponseReview {
 public static void main(String[] args)throws Exception {
  new File("out/shoreline-response").mkdirs();
  for(int scene=0;scene<2;scene++)for(int frame=0;frame<3;frame++) {
   Game g=new Game();g.challengeId=scene==0?Challenges.TWO_BANKS:Challenges.MONTROSE;g.reset();g.mode=Game.PLAY;g.configureCanvas(1333,750);g.visualTicks=90+frame*30;
   float x=460,y=450;
   if(scene==1){float[] e=MontroseShoreArt.DOCK_EDGES[MontroseShoreArt.DOCK_EDGES.length/2];x=(e[0]+e[2])*.5f;y=(e[1]+e[3])*.5f;}
   g.layout=Layout.focused(1333,750,x,y,2.6f,666,375);
   if(frame==2){g.tug.x=x;g.tug.y=y;g.tug.vx=50;for(int step=0;step<8;step++)g.water.step(g);
    for(int i=0;i<WaterSurface.CAPACITY;i++)if(g.water.age[i]<WaterSurface.LIFE){g.water.x[i]=x;g.water.y[i]=y;g.water.strength[i]=1.8f;g.water.age[i]=40;}}
   BufferedImage image=new BufferedImage(1333,750,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
   g.render(new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File("out/shoreline-response/"+(scene==0?"calumet":"marina")+"-"+frame+".png"));
  }
 }
}
