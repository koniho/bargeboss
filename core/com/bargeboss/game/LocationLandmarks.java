package com.bargeboss.game;
/** Shared depth ordering for fixed geographic landmarks. */
final class LocationLandmarks {
    static int[] ordered(Game g){
        if(!g.calumetStage() && !g.indianaStage())return new int[0];
        int base=base(g);int[] workers=DockWorkers.ordered(g),ids=new int[base+workers.length];
        for(int i=0;i<base;i++)ids[i]=i;
        for(int i=0;i<workers.length;i++)ids[base+i]=base+workers[i];
        for(int i=1;i<ids.length;i++){int id=ids[i],j=i;float d=depth(g,id);
            while(j>0 && depth(g,ids[j-1])>d){ids[j]=ids[j-1];j--;}ids[j]=id;
        }return ids;
    }
    static int base(Game g){return g.calumetStage()?CalumetRiverView.count(g):IndianaView.count(g);}
    static int count(Game g){return base(g)+(g.calumetStage() || g.indianaStage()?6:0);}
    static float depth(Game g,int i){if(i>=base(g))return DockWorkers.depth(g,i-base(g));return g.calumetStage()?CalumetRiverView.depth(g,i):IndianaView.depth(g,i);}
    static void draw(Game g,Painter p,int i){if(i>=base(g)){int id=i-base(g);if(g.berthDisplayEnabled(id/2))DockWorkers.draw(g,p,id);return;}if(g.calumetStage())CalumetRiverView.landmark(g,p,i);else IndianaView.landmark(g,p,i);}
}
