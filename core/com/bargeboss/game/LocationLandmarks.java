package com.bargeboss.game;
/** Shared depth ordering for fixed geographic landmarks. */
final class LocationLandmarks {
    static int[] ordered(Game g){
        if(!g.calumetStage() && !g.indianaStage() && !g.montroseStage())return new int[0];
        int base=base(g);int[] workers=g.montroseStage()?new int[0]:DockWorkers.ordered(g),ids=new int[base+workers.length];
        for(int i=0;i<base;i++)ids[i]=i;
        for(int i=0;i<workers.length;i++)ids[base+i]=base+workers[i];
        for(int i=1;i<ids.length;i++){int id=ids[i],j=i;float d=depth(g,id);
            while(j>0 && depth(g,ids[j-1])>d){ids[j]=ids[j-1];j--;}ids[j]=id;
        }return ids;
    }
    static int base(Game g){return g.montroseStage()?MontroseBeach.COUNT+4:g.calumetStage()?CalumetRiverView.count(g):IndianaView.count(g);}
    static int count(Game g){return base(g)+(g.calumetStage() || g.indianaStage()?6:0);}
    static float depth(Game g,int i){if(g.montroseStage() && i>=MontroseBeach.COUNT)return DockLineCrew.depth(g,i-MontroseBeach.COUNT);if(i>=base(g))return DockWorkers.depth(g,i-base(g));return g.montroseStage()?MontroseBeach.depth(g,i):g.calumetStage()?CalumetRiverView.depth(g,i):IndianaView.depth(g,i);}
    static void draw(Game g,Painter p,int i){if(g.montroseStage() && i>=MontroseBeach.COUNT){DockLineCrew.draw(g,p,i-MontroseBeach.COUNT);return;}if(i>=base(g)){int id=i-base(g);if(g.berthDisplayEnabled(id/2))DockWorkers.draw(g,p,id);return;}if(g.montroseStage())MontroseBeach.landmark(g,p,i);else if(g.calumetStage())CalumetRiverView.landmark(g,p,i);else IndianaView.landmark(g,p,i);}
}
