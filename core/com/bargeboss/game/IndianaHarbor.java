package com.bargeboss.game;

/** Compressed playable Indiana Harbor: outer basin, ship canal and the two forks.
 * Survey references and intentional simplifications live in docs/maps/indiana-harbor.md. */
public final class IndianaHarbor {
    public static final float DOCK_X=325,DOCK_Y=1500;
    public static final float[][] WATER={{-700,-1400,1400,-960},{400,-960,800,-820},{100,-820,1150,150},{250,150,500,850},{-500,850,500,1100},{250,1100,500,1800}};
    public static final float[] COAST={-700,-1400,1400,-1400,1400,-960,800,-960,800,-820,1150,-820,1150,150,500,150,500,1800,250,1800,250,1100,-500,1100,-500,850,250,850,250,150,100,150,100,-820,400,-820,400,-960,-700,-960};
    public static final float[][] LAND=WaterEffectsPainter.partition(new float[][]{{-900,-1500,1600,2000}},WATER);
    private IndianaHarbor(){}
    public static boolean contains(float x,float y){return PolygonWater.contains(COAST,x,y);}
    public static float collide(Body b,CollisionSplashes splashes){return PolygonWater.collide(b,COAST,true,splashes);}
    public static void setup(Game g){
        IndianaView.prepare();
        g.tug.x=600;g.tug.y=-680;g.tug.angle=(float)Math.PI/2;
        g.cargo[0]=new Body(g.nextId++,0,600,-785,(float)Math.PI/2);
        g.layout=Layout.focused(g.layout.width,g.layout.height,600,-700,Game.TRANSIT_ZOOM,g.layout.width*.5f,g.layout.height*.52f);
    }
    public static boolean clear(float x,float y,float xx,float yy){int count=Math.max(1,(int)(Physics.length(xx-x,yy-y)/3));
        for(int i=0;i<=count;i++)if(!contains(x+(xx-x)*i/count,y+(yy-y)*i/count))return false;return true;}
}
