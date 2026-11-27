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
        public int[] sceneryAtlases(){return SpotlightArt.prepare(baseSceneryAtlases());}
        private int[] baseSceneryAtlases(){if(instructor)return new int[]{SpriteAtlas.TUTORIAL_SCENERY,SpriteAtlas.STATIC_HARBOR,SpriteAtlas.CHIMNEYS,SpriteAtlas.LAND_MARKER,SpriteAtlas.SHORE_BOLLARDS,SpriteAtlas.SHORE_LOADING,SpriteAtlas.SHORE_TRUCKS,SpriteAtlas.FACTORY,SpriteAtlas.INSTRUCTOR_PORTRAIT,SpriteAtlas.BRIDGE_OPERATOR_STEEL,SpriteAtlas.BRIDGE_OPERATOR_NIGHT};return geometryScenery?new int[0]:marina?new int[]{SpriteAtlas.POLICE_PORTRAIT,SpriteAtlas.POLICE_SHADOW,SpriteAtlas.RACING_SHADOW,SpriteAtlas.CHAS_PORTRAIT,SpriteAtlas.ALEX_PORTRAIT,SpriteAtlas.GAVIN_PORTRAIT,SpriteAtlas.MARINA,SpriteAtlas.MOORED_BIMINI,SpriteAtlas.MOORED_BOATS,SpriteAtlas.MOORED_NORMALS,SpriteAtlas.STATIC_HARBOR}:new int[]{SpriteAtlas.SHORE_BOLLARDS,SpriteAtlas.SHORE_LOADING,SpriteAtlas.SHORE_TRUCKS,SpriteAtlas.FACTORY};}
        public int[] preparationAtlases(){int[] scenery=sceneryAtlases(),all=new int[rotations.length+scenery.length+4+PlayerPortrait.COUNT];System.arraycopy(rotations,0,all,0,rotations.length);System.arraycopy(scenery,0,all,rotations.length,scenery.length);for(int i=0;i<PlayerPortrait.COUNT;i++)all[rotations.length+scenery.length+i]=PlayerPortrait.atlas(i);all[all.length-4]=SpriteAtlas.FLAME;all[all.length-3]=SpriteAtlas.FOAM;all[all.length-2]=SpriteAtlas.SPLASH;all[all.length-1]=SpriteAtlas.RIPPLES;return all;}
        public StaticLayer[] residentLayers(){return instructor?new StaticLayer[]{SteamPuff.LAYER}:marina?MarinaPedestals.residentLayers():new StaticLayer[0];}
        public int[] rotationAtlases(){return rotations.clone();}
        public int rotationFrames(){int count=0;for(int atlas:rotations)count+=SpriteAtlas.count(atlas);return count;}
    }
    // The continuous tutorial uses coal loads throughout, including its later tow-train steps.
    private static final Plan MENU=new Plan(true,SpriteAtlas.TUG,SpriteAtlas.BARGE,SpriteAtlas.STEEL,SpriteAtlas.BULK,SpriteAtlas.CONTAINER);
    public static Plan menu(){return MENU;}
    private static final Plan TUTORIAL=new Plan(SpriteAtlas.FIREWORKS,SpriteAtlas.FIRE_BARGE,SpriteAtlas.TEACHER_TUG,SpriteAtlas.TUG,SpriteAtlas.BARGE,SpriteAtlas.BULK);
    private static final Plan MONTROSE=new Plan(2,false,SpriteAtlas.TUG,SpriteAtlas.YACHT,SpriteAtlas.BARGE,SpriteAtlas.FIREWORKS);
    private static final Plan[] CHALLENGES=new Plan[Challenges.COUNT];
    static {
        for(int id=0;id<CHALLENGES.length;id++) {
            if(id==Challenges.INDIANA){CHALLENGES[id]=new Plan(true,SpriteAtlas.TUG,SpriteAtlas.BARGE,SpriteAtlas.STEEL);continue;}
            // Engine 2 fire barge: hull, deck monitors and the cargo family its berth selects.
            if(id==Challenges.CHICAGO){CHALLENGES[id]=new Plan(true,SpriteAtlas.FIRE_BARGE,SpriteAtlas.TUG,SpriteAtlas.BARGE,SpriteAtlas.BULK);continue;}
            if(Challenges.kind(id)==Challenges.MONTROSE){CHALLENGES[id]=MONTROSE;continue;}
            // Tag circuit: tug plus the marina boat sprites its neon NPCs are drawn with.
            if(Challenges.kind(id)==Challenges.BOAT_TAG || Challenges.kind(id)==Challenges.ISLAND_TAG){CHALLENGES[id]=new Plan(1,false,SpriteAtlas.TUG);continue;}
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
    /** Menus retain a stable title convoy; only departure prepares a location manifest. */
    public static Plan upcoming(Game root){Game game=root.activeGame();return game.mode==Game.TITLE || game.mode==Game.CHALLENGES || game.mode==Game.CAPTAINS || game.mode==Game.SETTINGS && game.tutorial==null?MENU:game.tutorial!=null?TUTORIAL:challenge(game.challengeId);}
}
