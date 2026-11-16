package com.bargeboss.game;

/** Immutable upcoming-level preparation manifests. These are cache hints, not draw rules.
 * Include later spawns and all gameplay phases; never infer requirements only from visibility. */
public final class LevelAssets {
    public static final class Plan {
        private final int[] rotations;
        private Plan(int... sources){rotations=SpriteAtlas.rotationAtlases(sources);}
        public int[] rotationAtlases(){return rotations.clone();}
        public int rotationFrames(){int count=0;for(int atlas:rotations)count+=SpriteAtlas.count(atlas);return count;}
    }
    // The continuous tutorial uses coal loads throughout, including its later tow-train steps.
    private static final Plan TUTORIAL=new Plan(SpriteAtlas.TUG,SpriteAtlas.BARGE,SpriteAtlas.BULK);
    private static final Plan[] CHALLENGES=new Plan[Challenges.COUNT];
    static {
        for(int id=0;id<CHALLENGES.length;id++) {
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
