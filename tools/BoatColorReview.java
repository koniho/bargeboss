package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Matched projected hull length, heading and ambient profile; no nearby lamps. */
public final class BoatColorReview {
 public static void main(String[] args)throws Exception{
  String name=args.length>0?args[0]:"boat-color-review";
  BufferedImage board=new BufferedImage(1280,850,BufferedImage.TYPE_INT_RGB);Graphics2D out=board.createGraphics();
  out.setColor(new Color(0x091b25));out.fillRect(0,0,1280,850);out.setFont(new Font("SansSerif",Font.BOLD,21));out.setColor(new Color(0xffecd1));out.drawString("SPRITE / LIVE 3D — COLOR COMPARISON",28,36);
  String[] labels={"Painted tug sprite","Painted 80 ft yacht","Live motor cruiser","Live sailboat"};
  for(int c=0;c<4;c++)out.drawString(labels[c],28+c*320,75);
  Game g=ReplayInput.montroseJob(Challenges.HAULOUT);g.dialogue.close(g);HarborBoatModel.prepare();
  for(int r=0;r<3;r++){
   g.changeLighting(r==0?1:2);float angle=r==2?(float)Math.PI*.75f:0;float scale=4;
   out.setFont(new Font("SansSerif",Font.PLAIN,17));out.setColor(new Color(0x9fb4bb));out.drawString(r==0?"DAY / bow NE":r==1?"DUSK / bow NE":"DUSK / opposite quarter",28,105+r*245);
   for(int c=0;c<4;c++){
    Graphics2D cell=(Graphics2D)out.create(c*320,115+r*245,320,220);cell.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);DesktopPainter p=new DesktopPainter(cell);p.lighting(g.lighting());
    g.layout=Layout.focused(320,220,0,0,scale,160,150);
    if(c<2){int atlas=c==0?SpriteAtlas.TUG:SpriteAtlas.YACHT;Body body=c==0?g.tug:new Body(10,2,0,0,angle,true,false);body.x=body.y=0;body.angle=angle;
     float size=160/(c==0?TugArt.HULL_FRACTION:YachtArt.HULL_FRACTION)/scale;
     Painter river=new RiverPainter(p,g.layout.camera);SpriteAtlas.draw(river,atlas,angle,0,0,size,size,1);SurfaceLighting.directional(g,river,atlas,angle,0,0,size,1,body);
    }else{int kind=c==2?0:3;g.layout=Layout.focused(320,220,0,0,160/MooredBoats.length(kind),160,150);MooredBoats.drawHull(g,p,kind,0,0,0,0,angle,0,0,1,new MooredBoatLight.Lights());}
    cell.dispose();
   }
  }
  out.dispose();ImageIO.write(board,"png",new File("out/"+name+".png"));
 }
}
