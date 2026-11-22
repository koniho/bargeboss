package com.bargeboss.game;

/** Small authored silhouettes: bold functional shapes with cream/gold UI ink. */
final class ArcadeIcons {
    static final int FIREWORK=0,WATER=1,UNLINK=2,RADIO=3;
    static void draw(Painter p,int kind,float x,float y,float s,int ink){
        if(kind==RADIO){
            p.rect(x-8*s,y-5*s,16*s,13*s,ink);
            p.rect(x-5*s,y-2*s,7*s,3*s,PilotGlass.TRACK);
            p.line(x+5*s,y-5*s,x+9*s,y-14*s,2*s,ink);
            for(int i=0;i<3;i++)p.line(x-5*s,y+(3+i*2)*s,x+5*s,y+(3+i*2)*s,s,PilotGlass.TRACK);
        }else if(kind==UNLINK){
            // Two broken chain ends, with a clear separation and release flash.
            for(int i=-1;i<=1;i+=2){
                float cx=x+i*8*s;
                p.line(cx-4*s,y-5*s,cx+4*s,y-5*s,3*s,ink);
                p.line(cx-4*s,y+5*s,cx+4*s,y+5*s,3*s,ink);
                p.line(cx+i*4*s,y-5*s,cx+i*4*s,y+5*s,3*s,ink);
            }
            p.line(x-2*s,y-9*s,x+2*s,y-13*s,2*s,ink);
            p.line(x-2*s,y+13*s,x+2*s,y+9*s,2*s,ink);
        }else if(kind==WATER){
            p.line(x-9*s,y+10*s,x+5*s,y-4*s,7*s,ink);
            p.line(x+1*s,y-8*s,x+9*s,y,3*s,ink);
            for(int i=0;i<3;i++){
                float xx=x+(9+i*4)*s,yy=y+(-13+i*4)*s;
                p.line(x+8*s,y-7*s,xx,yy,2*s,ink);
                p.circle(xx,yy,1.8f*s,ink);
            }
        }else{
            p.line(x-5*s,y+13*s,x+1*s,y+3*s,2*s,ink);
            p.poly(new float[]{x-5*s,y+1*s,x+1*s,y-13*s,x+7*s,y+1*s,x+1*s,y+6*s},ink);
            p.line(x-11*s,y-8*s,x-14*s,y-12*s,2*s,ink);
            p.line(x+12*s,y-7*s,x+16*s,y-10*s,2*s,ink);
            p.line(x+11*s,y+7*s,x+15*s,y+10*s,2*s,ink);
        }
    }
    private ArcadeIcons(){}
}
