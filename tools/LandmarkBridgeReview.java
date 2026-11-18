package com.bargeboss.game;
import java.awt.*;import java.awt.image.*;import javax.imageio.ImageIO;import java.io.File;
/** Production rigid models, registered closed/half/open landmark review. */
public final class LandmarkBridgeReview {
 public static void main(String[] args)throws Exception{
  new File("out/landmark-bridges").mkdirs();Game g=new Game();
  for(int location=0;location<2;location++){
   if(location==1){g.challengeId=Challenges.INDIANA;g.reset();LocationBridges.prepare(LocationBridges.indiana());}
   LandmarkBridge[] bridges=LocationBridges.forGame(g);
   for(int i=0;i<bridges.length;i++){
    LandmarkBridge b=bridges[i];BufferedImage contact=new BufferedImage(1800,720,BufferedImage.TYPE_INT_RGB);Graphics2D cg=contact.createGraphics();
    for(int k=0;k<3;k++){
     float scale=Math.min(1.8f,300/(b.left+b.right+(b.movable()?110:850)));
     g.layout=Layout.focused(600,720,b.x,b.y,scale,300,460);g.visualTicks=180;
     BufferedImage frame=new BufferedImage(600,720,BufferedImage.TYPE_INT_RGB);Graphics2D gg=frame.createGraphics();gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
     Painter p=new DesktopPainter(gg);p.rect(0,0,600,720,0xff182734);b.draw(g,p,k*.5f,k==2 || !b.movable());
     Lettering.center(p,b.name,300,35,10,VisualStyle.CREAM);Lettering.center(p,!b.movable()?"FIXED HIGH SPAN":k==0?"CLOSED":k==1?"OPENING":"OPEN",300,660,12,VisualStyle.GOLD);
     gg.dispose();cg.drawImage(frame,k*600,0,null);
    }
    cg.dispose();ImageIO.write(contact,"png",new File("out/landmark-bridges/"+(location==0?"calumet-":"indiana-")+b.id+".png"));System.out.println(b.id+" "+b.left+"/"+b.right);
    if(i==0 || b.type==LandmarkBridge.SKYWAY || b.id.equals("106th") || b.id.equals("dickey")){
     g.mode=Game.PLAY;for(int j=0;j<g.passages.position.length;j++)g.passages.position[j]=BridgePassage.OPEN_TICKS;
     g.tug.x=b.x;g.tug.y=b.y;g.tug.angle=(float)StrictMath.atan2(b.dx,-b.dy);
     g.layout=Layout.focused(1200,800,b.x,b.y,b.type==LandmarkBridge.SKYWAY?.65f:1.6f,600,520);
     BufferedImage scene=new BufferedImage(1200,800,BufferedImage.TYPE_INT_RGB);Graphics2D gg=scene.createGraphics();gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
     Renderer.draw(g,new DesktopPainter(gg));gg.dispose();ImageIO.write(scene,"png",new File("out/landmark-bridges/scene-"+b.id+".png"));
    }
   }
  }
 }
}
