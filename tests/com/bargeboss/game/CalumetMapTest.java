package com.bargeboss.game;
/** Mission-independent coastline and source frame registration. */
final class CalumetMapTest {
 private static int checks;
 private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static void run(){
  for(int id=0;id<Challenges.COUNT;id++)if(!Challenges.standalone(id)){Game g=new Game();g.challengeId=id;g.resetPracticeYard();
   for(float y=-1050;y<900;y+=23)for(float x=-1900;x<950;x+=23)check(g.inWater(x,y)==CalumetMap.contains(x,y),"Practice workyard retains coastline");
   check(g.hasBridge(),"Practice bridge is retained");
  }
  check(CalumetMap.clear(599,660,599,220,20),"Full hull fits eastern berth approach");
  check(!CalumetMap.clear(300,480,599,480,0),"Eastern quay prevents cutting across dry land");
  for(int i=0;i<CalumetArt.count();i++){float[] p=CalumetArt.item(i);check(!CalumetMap.contains(p[2],p[3]),"Industrial prop anchored on land");}
  for(int atlas=SpriteAtlas.SHORE_BOLLARDS;atlas<=SpriteAtlas.STEELWORKS;atlas++)for(int f=0;f<SpriteAtlas.count(atlas);f++){
   float l=SpriteAtlas.sourceLeft(atlas,f),t=SpriteAtlas.sourceTop(atlas,f),r=SpriteAtlas.sourceRight(atlas,f),b=SpriteAtlas.sourceBottom(atlas,f);
   check(l>=0&&t>=0&&r<=1&&b<=1&&r>l&&b>t,"New sprite frame has valid crop");
   check(Math.abs((r-l)*SpriteAtlas.columns(atlas)-1)<.00001f&&Math.abs((b-t)*SpriteAtlas.rows(atlas)-1)<.00001f,"New sprite crops exactly one atlas cell");
  }
  System.out.println(checks+" shared Calumet coastline, shore anchor and sprite crop assertions passed");
 }
}
