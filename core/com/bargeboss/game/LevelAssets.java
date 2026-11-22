package com.bargeboss.game;

/** Immutable upcoming-level preparation manifests. These are cache hints, not draw rules.
 * Include later spawns and all gameplay phases; never infer requirements only from visibility. */
public final class LevelAssets {
    public static final class Plan {
        private final int[] rotations;
        private final boolean marina,geometryScenery,swimmers,instructor;
        private Plan(int... sources){this(false,sources);}
        private Plan(boolean geometry,int... sources){this(0,geometry,sources);}
        private Plan(int rescue,boolean geometry,int... sources){swimmers=rescue==2;geometryScenery=geometry;rotations=SpriteAtlas.rotationAtlases(sources);boolean yacht=false,teacher=false;for(int source:sources){if(source==SpriteAtlas.YACHT)yacht=true;if(source==SpriteAtlas.TEACHER_TUG)teacher=true;}instructor=teacher;marina=yacht||rescue>0;}
        public int[] sceneryAtlases(){if(instructor)return new int[]{SpriteAtlas.SHORE_BOLLARDS,SpriteAtlas.SHORE_LOADING,SpriteAtlas.SHORE_TRUCKS,SpriteAtlas.FACTORY,SpriteAtlas.INSTRUCTOR_PORTRAIT,SpriteAtlas.BRIDGE_OPERATOR_STEEL,SpriteAtlas.BRIDGE_OPERATOR_NIGHT};return geometryScenery?new int[0]:marina?new int[]{SpriteAtlas.MARINA,SpriteAtlas.MOORED_BOATS,SpriteAtlas.MOORED_NORMALS,SpriteAtlas.STATIC_HARBOR}:new int[]{SpriteAtlas.SHORE_BOLLARDS,SpriteAtlas.SHORE_LOADING,SpriteAtlas.SHORE_TRUCKS,SpriteAtlas.FACTORY};}
        public int[] preparationAtlases(){int[] scenery=sceneryAtlases(),all=new int[rotations.length+scenery.length+(swimmers?4:3)];System.arraycopy(rotations,0,all,0,rotations.length);System.arraycopy(scenery,0,all,rotations.length,scenery.length);if(swimmers)all[all.length-4]=SpriteAtlas.SWIMMERS;all[all.length-3]=SpriteAtlas.FOAM;all[all.length-2]=SpriteAtlas.SPLASH;all[all.length-1]=SpriteAtlas.RIPPLES;return all;}
        public StaticLayer[] residentLayers(){return marina?MarinaPedestals.residentLayers():new StaticLayer[0];}
        public int[] rotationAtlases(){return rotations.clone();}
        public int rotationFrames(){int count=0;for(int atlas:rotations)count+=SpriteAtlas.count(atlas);return count;}
    }
    // The continuous tutorial uses coal loads throughout, including its later tow-train steps.
    private static final Plan TUTORIAL=new Plan(SpriteAtlas.FIREWORKS,SpriteAtlas.FIRE_BARGE,SpriteAtlas.TEACHER_TUG,SpriteAtlas.TUG,SpriteAtlas.BARGE,SpriteAtlas.BULK);
    private static final Plan MONTROSE=new Plan(2,false,SpriteAtlas.TUG,SpriteAtlas.YACHT);
    private static final Plan[] CHALLENGES=new Plan[Challenges.COUNT];
    static {
        for(int id=0;id<CHALLENGES.length;id++) {
            if(id==Challenges.INDIANA){CHALLENGES[id]=new Plan(true,SpriteAtlas.TUG,SpriteAtlas.BARGE,SpriteAtlas.STEEL);continue;}
            if(Challenges.kind(id)==Challenges.MONTROSE){CHALLENGES[id]=MONTROSE;continue;}
            if(Challenges.calumet(id)){CHALLENGES[id]=new Plan(true,SpriteAtlas.TUG,SpriteAtlas.BARGE,SpriteAtlas.STEEL,SpriteAtlas.BULK,SpriteAtlas.CONTAINER);continue;}
            int[] sources=new int[2+Challenges.quota(id)];sources[0]=SpriteAtlas.TUG;sources[1]=SpriteAtlas.BARGE;
            for(int load=0;load<Challenges.quota(id);load++) {
                int berth=Challenges.feedBerth(id,load);
                sources[load+2]=berth==0?SpriteAtlas.STEEL:berth==2?SpriteAtlas.BULK:SpriteAtlas.CONTAINER;
            }
            CHALLENGES[id]=new Plan(sources);
        }
    }
    private LevelAssets(){}
    public static Plan tutorial(){return TUTORIAL;}
    public static Plan challenge(int id){return CHALLENGES[Challenges.valid(id)?id:0];}
    /** Menu selection warms the upcoming challenge; practice gets its own complete manifest. */
    public static Plan upcoming(Game root){Game game=root.activeGame();return game.tutorial!=null?TUTORIAL:challenge(game.challengeId);}
}
