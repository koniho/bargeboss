package com.bargeboss.game;
/** Ain't Broke: 56x14 offshore racing hull, +X bow, waterline-centered32-pose bake. */
final class RacingBoatArt {
 // Rigid hull-local effect metadata. Exhaust is submerged: no stack or tire emitters.
 static final float[][] SMOKESTACKS={},TIRES={},FENDERS={};
 static final String[] CLEAT_IDS={"stern-port","stern-starboard","bow-port","bow-starboard"};
 static final float[][] CLEATS={{-23,-3.2f,5.4f},{-23,3.2f,5.4f},{23,-3.2f,5.4f},{23,3.2f,5.4f}};
 static final String[] LIGHT_IDS={"port","starboard","stern","anchor"};
 static final float[][] LIGHTS={{4,-5.2f,5.8f},{4,5.2f,5.8f},{-25,0,5.4f},{-20,0,9.6f}};
 static final float[][] TOW_EYES={{-25,0,3},{25,0,3}};
 static void shadow(Game g,Painter p,Body b,RiverCamera c){
  float heading=b.angle*32/(2*(float)Math.PI),whole=(float)StrictMath.floor(heading),mix=heading-whole;
  int a=Math.floorMod((int)whole,32),next=(a+1)%32;float x=c.screenX(b.x,b.y),y=c.screenY(b.x,b.y),size=96*c.scale;
  int atlas=b.hullKind==PoliceBoatModel.KIND?SpriteAtlas.POLICE_SHADOW:SpriteAtlas.RACING_SHADOW;
  for(int style=0;style<2;style++){float alpha=(style==0?.24f:.13f)*g.lighting().shadowStrength;
   p.sprite(atlas,style*32+a,x,y,size,size,0,alpha*(1-mix));
   if(mix>.001f)p.sprite(atlas,style*32+next,x,y,size,size,0,alpha*mix);
  }
 }
 private RacingBoatArt(){}
}
