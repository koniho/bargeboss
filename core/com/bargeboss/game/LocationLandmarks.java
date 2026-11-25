package com.bargeboss.game;
/** Shared depth ordering for fixed geographic landmarks: survey-level landmarks, dock line
 * crews at survey berths, and Montrose's beach scenery and line crew. */
final class LocationLandmarks {
    static int[] ordered(Game g){
        SurveyLevel level=g.level();
        if(level!=null && !level.dockWorkers())return level.orderedLandmarks(g);
        if(level==null && !g.montroseStage())return new int[0];
        int base=base(g);int[] workers=g.montroseStage()?new int[0]:DockWorkers.ordered(g),ids=new int[base+workers.length];
        for(int i=0;i<base;i++)ids[i]=i;
        for(int i=0;i<workers.length;i++)ids[base+i]=base+workers[i];
        for(int i=1;i<ids.length;i++){int id=ids[i],j=i;float d=depth(g,id);
            while(j>0 && depth(g,ids[j-1])>d){ids[j]=ids[j-1];j--;}ids[j]=id;
        }return ids;
    }
    static int base(Game g){SurveyLevel level=g.level();return level!=null?level.landmarkCount(g):g.montroseStage()?MontroseBeach.COUNT+4:0;}
    static int count(Game g){SurveyLevel level=g.level();return base(g)+(level!=null && level.dockWorkers()?6:0);}
    static float depth(Game g,int i){SurveyLevel level=g.level();
        if(g.montroseStage())return i>=MontroseBeach.COUNT?DockLineCrew.depth(g,i-MontroseBeach.COUNT):MontroseBeach.depth(g,i);
        if(i>=base(g))return DockWorkers.depth(g,i-base(g));
        return level.landmarkDepth(g,i);}
    /** Screen rectangle for landmarks that publish one; false means always treat as overlapping. */
    static boolean screenBox(Game g,int i,float[] out){SurveyLevel level=g.level();return level!=null && i<level.landmarkCount(g) && level.landmarkBox(g,i,out);}
    static void draw(Game g,Painter p,int i){SurveyLevel level=g.level();
        if(g.montroseStage()){if(i>=MontroseBeach.COUNT)DockLineCrew.draw(g,p,i-MontroseBeach.COUNT);else MontroseBeach.landmark(g,p,i);return;}
        if(i>=base(g)){int id=i-base(g);if(g.berthDisplayEnabled(id/2))DockWorkers.draw(g,p,id);return;}
        level.drawLandmark(g,p,i);}
}
