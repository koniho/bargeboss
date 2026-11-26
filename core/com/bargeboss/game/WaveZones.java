package com.bargeboss.game;

/** Per-level wave exposure: where storm waves can build (open water) and where water stays calmer
 * (rivers, canals, inner harbours). 0 = fully sheltered, 1 = fully exposed. Every stage defines its map
 * here or through {@link SurveyLevel#waveZones()}; Montrose keeps its authored lake map in SeaWaves.
 *
 * A zone is {x, y, seawardX, seawardY, inside, outside, peak}: exposure ramps from 0 at `inside` units
 * on the harbour side of (x,y) to `peak` at `outside` units seaward. A level's exposure is its calm
 * base plus the strongest zone. */
final class WaveZones {
    private WaveZones(){}
    /** Calm base for sheltered reaches when a level does not say otherwise. */
    static final float CALM=.18f;

    static float exposure(float[][] zones,float calm,float x,float y){
        float best=0;
        for(float[] z:zones){
            float along=(x-z[0])*z[2]+(y-z[1])*z[3];
            best=Math.max(best,z[6]*SeaWaves.smooth(-z[4],z[5],along));
        }
        return calm+(1-calm)*best;
    }
    /** Practice yard and training stages: the big towing basin is open water, the working basins are
     * moderate, the winding canal and slips are sheltered. */
    static float training(float x,float y){
        if(x>=TrainingChannel.CHALLENGE_LEFT-60 && x<=TrainingChannel.CHALLENGE_RIGHT+60 && y<=TrainingChannel.CHALLENGE_BOTTOM+60){
            // Grows from the basin mouth toward the far (north) wall.
            return .55f+.45f*SeaWaves.smooth(TrainingChannel.CHALLENGE_BOTTOM,TrainingChannel.CHALLENGE_BOTTOM-500,y);
        }
        if(x>=TrainingChannel.MIN-40 && x<=TrainingChannel.MAX+40 && y>=TrainingChannel.BASIN_TOP-40 && y<=TrainingChannel.BASIN_BOTTOM+40)return .15f;
        if(x<TrainingChannel.BASIN_LEFT+40)return .2f;
        return .42f;
    }
    /** The active stage's exposure at a world point. */
    static float at(Game g,float x,float y){
        if(g.montroseStage())return SeaWaves.exposure(x,y);
        SurveyLevel level=g.level();
        if(level!=null)return level.waveExposure(x,y);
        if(g.lockStage())return .3f;
        return training(x,y);
    }
}
