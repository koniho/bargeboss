package com.bargeboss.game;

/** First survey pass: geographically placed surface tiles and rigid bridge silhouettes. */
public final class CalumetRiverView {
    private CalumetRiverView(){}
    static void prepare(){CalumetWaterGeometry.prepare();LocationBridges.prepare(LocationBridges.calumet());}
    static int count(Game g){return 0;}
    static float depth(Game g,int i){LandmarkBridge b=LocationBridges.calumet()[i];return g.layout.camera.screenY(b.x,b.y);}
    static void landmark(Game g,Painter p,int i){LocationBridges.calumet()[i].draw(g,p,g.passages.amount(i),g.passages.passable(i));}
    public static void draw(Game g,Painter p){
        p.rect(0,0,g.layout.width,g.layout.height,0xff151b25);
        WaveView.surface(g,p);ArcadeView.water(g,new RiverPainter(p,g.layout.camera));
        ShorelineView.water(g,p);LocationBridges.shadows(g,p);CalumetRiverTiles.walls(g,p);CalumetRiverTiles.ground(g,p);ShorelineView.walls(g,p);
        Renderer.river(g,new RiverPainter(p,g.layout.camera));
    }
    static void berth(Game g,Painter p,int kind){int d=g.voyage.destination(kind);float x=g.voyage.dockX(d),y=g.voyage.dockY(d),nx=g.voyage.normalX(d),ny=g.voyage.normalY(d),tx=-ny,ty=nx;
        float[] q=new float[8];for(int i=0;i<4;i++){float a=i<2?-34:34,b=i==0 || i==3?-57:57;q[i*2]=x+nx*a+tx*b;q[i*2+1]=y+ny*a+ty*b;}
        int color=SoftCue.animated(g,0x779ec8ae);for(int i=0;i<4;i++){int j=(i+1)%4;SoftCue.line(p,q[i*2],q[i*2+1],q[j*2],q[j*2+1],1,color);}
    }
}
