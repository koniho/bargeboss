package com.bargeboss.game;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.*;import javax.imageio.ImageIO;
/** Independent production PNG coverage versus actual hull pixels at every heading. */
public final class IslandShorePixelTest {
 public static void main(String[] args)throws Exception{
  Game g=new Game();g.selectChallenge(Challenges.ISLAND_TAG);g.reset();int probes=0,wet=0;
  float[] island=ArenaCourse.Islands.ISLES[4];
  for(int bank=0;bank<4;bank++)for(int kind=0;kind<3;kind++)for(int heading=0;heading<32;heading++){
   double angle=bank*StrictMath.PI/2;float x=island[0]+(float)StrictMath.cos(angle)*(island[2]+5),y=island[1]+(float)StrictMath.sin(angle)*(island[2]+5);
   Body b=new Body(kind==0?0:1,-1,x,y,heading*(float)Math.PI/16,false,kind>0);if(kind>0)b.hullKind=kind==2?PoliceBoatModel.KIND:1;
   b.floatingInitialized=true;b.heave=(heading%3-1)*1.5f;float scale=heading%2==0?1.8f:2.7f;
   RiverCamera base=new RiverCamera(0,0,scale),c=new RiverCamera(192-base.screenX(x,y),240-base.screenY(x,y),scale);g.layout=Layout.renderTile(384,384,c);
   BufferedImage land=new BufferedImage(384,384,BufferedImage.TYPE_INT_ARGB),hull=new BufferedImage(384,384,BufferedImage.TYPE_INT_ARGB);
   Graphics2D lg=land.createGraphics();Painter lp=new DesktopPainter(lg);float size=island[2]*2*384/360;
   lp.sprite(SpriteAtlas.ISLAND_GROUND,0,c.screenX(island[0],island[1]),c.screenY(island[0],island[1])-5*scale,size*scale,size*2/3*scale,0,1);lg.dispose();
   Graphics2D hg=hull.createGraphics();Painter hp=new DesktopPainter(hg);hp.beginFrame();VesselView.draw(g,new RiverPainter(hp,c),b,kind==0,false);hp.rect(2,2,4,4,0xffff00ff);hg.dispose();
   if(hull.getRGB(3,3)!=0xffff00ff)throw new AssertionError("Clip not restored");
   for(int py=16;py<368;py+=3)for(int px=16;px<368;px+=3){
    boolean solid=true;for(int dy=-8;dy<=8;dy+=4)for(int dx=-8;dx<=8;dx+=4)if((land.getRGB(px+dx,py+dy)>>>24)<230)solid=false;
    if(solid && py>c.screenY(x,y)-IslandArt.TOP*scale+3){probes++;if((hull.getRGB(px,py)>>>24)>0)throw new AssertionError("Hull through painted foreground land: bank="+bank+" kind="+kind+" heading="+heading+" pixel="+px+","+py);}
    else if((land.getRGB(px,py)>>>24)<10 && (hull.getRGB(px,py)>>>24)>40)wet++;
   }
  }
  if(probes<10000 || wet<1000)throw new AssertionError("Insufficient shoreline coverage: "+probes+"/"+wet);
  long bytes=0;
  for(int atlas:new int[]{SpriteAtlas.ISLAND_WOODLAND,SpriteAtlas.ISLAND_GROUND})for(int f=0;f<SpriteAtlas.count(atlas);f++){
   BufferedImage b=ImageIO.read(new File("assets/sprites/"+SpriteAtlas.file(atlas,f)));bytes+=b.getWidth()*b.getHeight()*4L;
   if(b.getWidth()>384 || b.getHeight()>256)throw new AssertionError("Oversize runtime scenery frame");
   if(b.getRGB(0,0)>>>24!=0 || b.getRGB(b.getWidth()-1,b.getHeight()-1)>>>24!=0)throw new AssertionError("No transparent gutter");
  }
  if(bytes>3*1048576L)throw new AssertionError("Scenery exceeds 3 MiB source-pixel budget");
  System.out.println(probes+" painted-island hull-occlusion probes, "+wet+" visible water pixels; source pixels="+bytes+" bytes");
 }
}
