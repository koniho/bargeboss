package com.bargeboss.game;

/** Registered 64-pose deck machinery. Local XYZ anchors also own live effects. */
public final class BargeEquipment {
    private BargeEquipment(){}
    public static boolean art(int atlas){return atlas==62 || atlas==63;}
    public static boolean family(int atlas){return atlas>=62 && atlas<=67;}
    public static String file(int atlas){String name=atlas%2==0?"fireworks":"fire";return atlas<64?name+"-deck-v1-64.png":atlas<66?"shadow-"+name+"-deck-v1.png":"surface-shade-"+name+"-v2.png";}
    public static void draw(Game g,Painter p,Body b,float x,float y,float size){
        int atlas=b.ability==BargeAbilities.FIRE?SpriteAtlas.FIRE_BARGE:SpriteAtlas.FIREWORKS;
        SpriteAtlas.draw(p,atlas,b.angle,x,y,size,size,1);
        SurfaceLighting.directional(g,p,atlas,b.angle,x,y,size,1,b);
        if(b.ability==BargeAbilities.FIRE && p instanceof RiverPainter){
            RiverPainter river=(RiverPainter)p;float angle=g.abilities.selected==b && g.abilities.spraying?b.abilityAim:b.angle;
            for(int side:new int[]{-1,1})monitor(g,river.canvas(),b,side,angle);
        }
    }
    private static void monitor(Game g,Painter p,Body b,int side,float angle){
        RiverCamera c=g.layout.camera;float x=anchorX(b,side),y=anchorY(b,side),co=(float)StrictMath.cos(angle),si=(float)StrictMath.sin(angle);
        float bob=WaterSurface.lift(g,b);
        for(int i=0;i<8;i++){
            double a=i*StrictMath.PI/4,d=(i+1)*StrictMath.PI/4;
            float u=(float)StrictMath.cos(a)*1.5f,v=(float)StrictMath.sin(a)*1.5f,uu=(float)StrictMath.cos(d)*1.5f,vv=(float)StrictMath.sin(d)*1.5f;
            float[] q=new float[8];float[] along={0,9,9,0},across={u,u,uu,uu},up={v,v,vv,vv};
            for(int j=0;j<4;j++){float wx=x+co*along[j]-si*across[j],wy=y+si*along[j]+co*across[j];q[j*2]=c.screenX(wx,wy);q[j*2+1]=c.screenY(wx,wy)-(22+up[j])*ArticulatedModel.Z_SCALE*c.scale-bob*c.scale;}
            p.poly(q,i<4?0xffa8aaa0:0xff666f70);
        }
        float lx=x+co*9,ly=y+si*9;SoftCue.dot(p,c.screenX(lx,ly),c.screenY(lx,ly)-(22*ArticulatedModel.Z_SCALE+bob)*c.scale,1.1f*c.scale,0xff253138);
    }
    /** Stable physical fitting: nozzle.mast.port/starboard; no frame-space offsets. */
    public static float anchorX(Body b,int side){return b.x+18*(float)StrictMath.cos(b.angle)-side*7*(float)StrictMath.sin(b.angle);}
    public static float anchorY(Body b,int side){return b.y+18*(float)StrictMath.sin(b.angle)+side*7*(float)StrictMath.cos(b.angle);}
}
