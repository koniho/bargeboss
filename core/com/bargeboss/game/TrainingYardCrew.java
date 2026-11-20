package com.bargeboss.game;

import java.util.ArrayList;

/** Workshop crews occupy reserved dry service plots, never traffic lanes or buildings. */
final class TrainingYardCrew {
    private static final float[][] SITES=sites();
    private TrainingYardCrew(){}
    private static float[][] sites(){
        ArrayList<float[]> sites=new ArrayList<float[]>();
        int stride=Math.max(1,(TrainingChannelView.FACTORY_PLACEMENTS+23)/24);
        for(int f=0;f<TrainingChannelView.FACTORY_PLACEMENTS;f+=stride){
            float x=TrainingChannelView.factoryX(f),y=TrainingChannelView.factoryY(f);
            for(int side=0;side<4;side++){
                float xx=x+(side==0?64:side==1?-64:0),yy=y+(side==2?64:side==3?-64:0);
                if(!IndustrialRoadView.clearance(xx-22,yy-22,44,44,48)
                        || !TrainingScenery.nearWater(xx-22,yy-22,44,44)
                        || xx-22<TrainingScenery.MIN_X || xx+22>TrainingScenery.MAX_X
                        || yy-22<TrainingScenery.MIN_Y || yy+22>TrainingScenery.MAX_Y
                        || IndustrialRoadView.roadClear((int)StrictMath.floor(xx/64),(int)StrictMath.floor(yy/64))
                        || !CalumetArt.clearFactory(xx-22,yy-22,44,44))continue;
                boolean clear=true;
                for(int j=0;j<TrainingChannelView.FACTORY_PLACEMENTS;j++){
                    int cell=TrainingChannelView.factoryCell(j);
                    if(Math.abs(xx-TrainingChannelView.factoryX(j))<FactoryArt.footprintX(cell)+24
                            && Math.abs(yy-TrainingChannelView.factoryY(j))<FactoryArt.footprintY(cell)+24)clear=false;
                }
                for(float[] other:sites)if(Physics.length(xx-other[0],yy-other[1])<100)clear=false;
                if(clear){sites.add(new float[]{xx,yy});break;}
            }
        }
        return sites.toArray(new float[sites.size()][]);
    }
    static int count(){return SITES.length;}
    static void position(int id,float[] out){out[0]=SITES[id][0];out[1]=SITES[id][1];}
    static float motion(int id,int ticks){
        float phase=Math.floorMod(ticks+id*97,360)/360f;
        return phase<.7f?.55f+.3f*(float)StrictMath.sin(phase*StrictMath.PI*16):.15f;
    }
    static void draw(Game g,Painter p,int id){
        float[] at=SITES[id];float x=at[0],y=at[1];RiverCamera c=g.layout.camera;
        // Pallet sorting and bench maintenance alternate along the workshops.
        YardActivity.box(p,c,x,y-12,22,12,0,3,0xff6b5a41,0xff443d32);
        YardActivity.box(p,c,x,y-12,18,10,3,id%2==0?7:11,0xff66716c,0xff3c4849);
        if(id%2==0)YardActivity.box(p,c,x-6,y-12,5,8,10,4,0xffa17e4c,0xff655036);
        DockWorkers.drawAt(g,p,x,y,motion(id,g.visualTicks));
        float z=13+3*(float)StrictMath.sin(g.visualTicks*.09+id);
        p.line(c.screenX(x-3,y-2),c.screenY(x-3,y-2)-(GroundArt.LAND_RISE+z)*c.scale,
                c.screenX(x-3,y-10),c.screenY(x-3,y-10)-(GroundArt.LAND_RISE+13)*c.scale,1.2f*c.scale,0xffb7b6a3);
    }
}
