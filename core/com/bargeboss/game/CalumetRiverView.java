package com.bargeboss.game;

/** Calumet berth cues. Water, shore, ground and bridges draw through SurveyLevelView. */
public final class CalumetRiverView {
    private CalumetRiverView(){}
    static void berth(Game g,Painter p,int kind){int d=g.voyage.destination(kind);float x=g.voyage.dockX(d),y=g.voyage.dockY(d),nx=g.voyage.normalX(d),ny=g.voyage.normalY(d),tx=-ny,ty=nx;
        float[] q=new float[8];for(int i=0;i<4;i++){float a=i<2?-34:34,b=i==0 || i==3?-57:57;q[i*2]=x+nx*a+tx*b;q[i*2+1]=y+ny*a+ty*b;}
        int color=SoftCue.animated(g,0x779ec8ae);for(int i=0;i<4;i++){int j=(i+1)%4;SoftCue.line(p,q[i*2],q[i*2+1],q[j*2],q[j*2+1],1,color);}
    }
}
