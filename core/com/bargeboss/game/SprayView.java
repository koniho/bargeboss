package com.bargeboss.game;

/** One depth merge for collision spray and detached bow spray; height never changes depth. */
final class SprayView {
    static float depth(Game g,int i){return i>=0?g.splashes.depth(g,i):g.water.foam.depth(g,-i-1);}
    static void draw(Game g,Painter p,int i){if(i>=0)g.splashes.draw(g,p,i);else g.water.foam.drawAir(g,p,-i-1);}
    static int[] ordered(Game g){
        int[] impacts=g.splashes.ordered(g),work=new int[impacts.length+VesselFoam.CAPACITY];
        System.arraycopy(impacts,0,work,0,impacts.length);int n=impacts.length;
        for(int i=0;i<VesselFoam.CAPACITY;i++)if(g.water.foam.airborne(i)){
            int at=n++;float d=g.water.foam.depth(g,i);
            while(at>0 && depth(g,work[at-1])>d){work[at]=work[at-1];at--;}
            work[at]=-i-1;
        }
        int[] result=new int[n];System.arraycopy(work,0,result,0,n);return result;
    }
}
