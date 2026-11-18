package com.bargeboss.game;
/** Survey coverage inventory and production tile receiver clipping. */
public final class IndianaShoreTilesTest {
 public static void main(String[] args){
  int checks=0;boolean[] kinds=new boolean[7];
  for(String row:IndianaSurvey.COVER){if(row.length()!=IndianaSurvey.COLS)throw new AssertionError("ragged cover");for(int i=0;i<row.length();i++){int kind=row.charAt(i)-'0';if(kind<0||kind>6)throw new AssertionError("invalid cover");kinds[kind]=true;checks++;}}
  for(int kind:new int[]{0,1,2,3,4,5})if(!kinds[kind])throw new AssertionError("missing shore material "+kind);
  Game g=new Game();g.challengeId=Challenges.INDIANA;g.reset();
  for(float[] center:IndianaHarbor.ROUTE){g.layout=Layout.focused(960,600,center[0],center[1],1,480,300);Receiver p=new Receiver(g);IndianaShoreTiles.ground(g,p);checks+=p.count;}
  System.out.println(checks+" Indiana survey tile/shore receiver assertions passed");
 }
 static final class Receiver implements Painter {
  final Game g;int count;Receiver(Game g){this.g=g;}
  public void poly(float[] q,int color){float x=0,y=0;for(int i=0;i<q.length;i+=2){x+=q[i];y+=q[i+1];}x/=q.length/2;y=y/(q.length/2)+GroundArt.LAND_RISE*g.layout.camera.scale;
   float wx=g.layout.camera.worldX(x,y),wy=g.layout.camera.worldY(x,y);if(IndianaHarbor.contains(wx,wy))throw new AssertionError("ground covers water "+wx+","+wy);count++;}
  public void surfacePolygon(float[] xy,float[] uv,int material,int color,float wet){poly(xy,color);}
  public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}public void line(float x,float y,float xx,float yy,float w,int c){}
  public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
 }
}
