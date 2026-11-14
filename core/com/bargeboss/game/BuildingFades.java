package com.bargeboss.game;

import java.util.ArrayList;
import java.util.HashMap;

/** World-keyed presentation state, advanced at 60 Hz rather than during rendering. */
public final class BuildingFades {
    private static final float OUT_STEP=.72f/18,IN_STEP=.72f/27;
    private static final class Fade {
        final int bucket;final long key;float alpha=1,target=1;
        Fade(int bucket,long key){this.bucket=bucket;this.key=key;}
    }
    @SuppressWarnings("unchecked")
    private final HashMap<Long,Fade>[] values=new HashMap[21*32];
    private final ArrayList<Fade> active=new ArrayList<Fade>();
    private static long key(float x,float y){return ((long)Float.floatToIntBits(x)<<32)|(Float.floatToIntBits(y)&0xffffffffL);}
    public float opacity(int atlas,int frame,float x,float y) {
        HashMap<Long,Fade> map=values[atlas*32+frame];Fade f=map==null?null:map.get(key(x,y));return f==null?1:f.alpha;
    }
    private void target(int atlas,int frame,float x,float y,float target) {
        if(target>=1)return;
        int bucket=atlas*32+frame;long key=key(x,y);
        HashMap<Long,Fade> map=values[bucket];if(map==null)values[bucket]=map=new HashMap<Long,Fade>();
        Fade f=map.get(key);if(f==null){f=new Fade(bucket,key);map.put(key,f);active.add(f);}f.target=target;
    }
    public void step(Game g) {
        for(Fade f:active)f.target=1;
        SceneryOcclusion scene=SceneryOcclusion.snapshot(g);
        if(g.tutorial!=null) {
            for(int i=0;i<TrainingChannelView.FACTORY_PLACEMENTS;i++) {
                int frame=TrainingChannelView.factoryCell(i);float x=TrainingChannelView.factoryX(i),y=TrainingChannelView.factoryY(i);
                target(SpriteAtlas.FACTORY,frame,x,y,scene.buildingTarget(g,SpriteAtlas.FACTORY,frame,x,y,GroundArt.LAND_RISE,FactoryArt.size(frame)));
            }
        } else {
            // Only the foreground shed is a fading building in the ordinary stages.
            for(int segment=g.bridgeStage()?-2:0;segment<=0;segment++) {
                float x=Physics.LEFT-82,y=218+segment*688;
                if(g.bridgeStage() && Math.abs(y-Bridge.Y)<95)continue;
                target(SpriteAtlas.TUTORIAL_SCENERY,TutorialSceneryArt.SHED,x,y,
                        scene.buildingTarget(g,SpriteAtlas.TUTORIAL_SCENERY,TutorialSceneryArt.SHED,x,y,0,TutorialSceneryArt.size(TutorialSceneryArt.SHED)));
            }
        }
        for(int i=active.size()-1;i>=0;i--) {
            Fade f=active.get(i);float delta=f.target-f.alpha;
            f.alpha+=Physics.clamp(delta,-OUT_STEP,IN_STEP);
            if(f.target==1 && f.alpha>=.9999f){values[f.bucket].remove(f.key);active.remove(i);}
        }
    }
}
