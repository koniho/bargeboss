package com.bargeboss.game;

/** Preparation covers future deliveries/effects without requesting unrelated cargo. */
final class LevelAssetsTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static boolean includes(LevelAssets.Plan plan,int atlas){for(int id:plan.rotationAtlases())if(id==atlas)return true;return false;}
    static void run() {
        LevelAssets.Plan tutorial=LevelAssets.tutorial();
        check(tutorial.rotationFrames()==1632 && includes(tutorial,SpriteAtlas.TEACHER_TUG),"Tutorial prepares cargo, teacher livery and both complete equipment families");
        check(!includes(tutorial,SpriteAtlas.STEEL) && !includes(tutorial,SpriteAtlas.CONTAINER),"Tutorial excludes unused cargo");
        check(!includes(tutorial,SurfaceLightArt.forSource(SpriteAtlas.STEEL)),"Unused cargo effects excluded too");
        for(int id=0;id<Challenges.COUNT;id++) {
            LevelAssets.Plan plan=LevelAssets.challenge(id);
            int[] scenery=plan.sceneryAtlases(),preparation=plan.preparationAtlases();
            check(scenery.length==(Challenges.kind(id)==Challenges.MONTROSE?5:0),"Level prepares its permanent scenery family");
            check(preparation.length==plan.rotationAtlases().length+scenery.length+(Challenges.kind(id)==Challenges.MONTROSE?4:3),"Complete preparation includes scenery, rotations, cyclic foam, splashes and ripples");
            check(preparation[preparation.length-3]==SpriteAtlas.FOAM,"Cyclic foam is prepared before active draws");
            for(int atlas:scenery)check(Challenges.kind(id)==Challenges.MONTROSE?(atlas==SpriteAtlas.MOORED_BIMINI||atlas==SpriteAtlas.MARINA||atlas==SpriteAtlas.MOORED_BOATS||atlas==SpriteAtlas.MOORED_NORMALS||atlas==SpriteAtlas.STATIC_HARBOR):atlas==SpriteAtlas.SHORE_BOLLARDS||atlas==SpriteAtlas.SHORE_LOADING||atlas==SpriteAtlas.SHORE_TRUCKS||atlas==SpriteAtlas.STEELWORKS||atlas==SpriteAtlas.FACTORY,"Scenery stays level-specific");
            Game game=new Game();game.challengeId=id;game.reset();
            check(LevelAssets.upcoming(game)==LevelAssets.challenge(Challenges.stageEntry(id)),"Gameplay manifest matches selected level");
            game.mode=Game.CHALLENGES;
            check(LevelAssets.upcoming(game)==LevelAssets.challenge(Challenges.stageEntry(id)),"Menu prepares upcoming selected level");
            if(id==Challenges.RAFT_RESCUE || id==Challenges.SWIMMER_RESCUE){
                check(includes(plan,SpriteAtlas.TUG) && includes(plan,SpriteAtlas.YACHT),"Rescue retains yacht rotations from earlier job");
                if(id==Challenges.SWIMMER_RESCUE)check(preparation[preparation.length-4]==SpriteAtlas.SWIMMERS,"Swimmer atlas warmed before active draws");
                continue;
            }
            if(Challenges.kind(id)==Challenges.MONTROSE){
                check(plan.rotationFrames()==544,"Montrose prepares complete tug and yacht families");
                check(includes(plan,SpriteAtlas.YACHT) && includes(plan,SpriteAtlas.YACHT_GOLD) && includes(plan,SpriteAtlas.YACHT_MINT) && includes(plan,ShadowArt.forSource(SpriteAtlas.YACHT)) && includes(plan,SurfaceLightArt.forSource(SpriteAtlas.YACHT)),"Yacht hull and every effect prepared");
                check(!includes(plan,SpriteAtlas.BARGE) && !includes(plan,SpriteAtlas.BULK),"Marina excludes industrial cargo");continue;
            }
            check(includes(plan,SpriteAtlas.TUG) && includes(plan,SpriteAtlas.BARGE),"Player, feeder and empty hulls prepared");
            check(includes(plan,SpriteAtlas.TUG_GOLD) && includes(plan,SpriteAtlas.BARGE_GOLD) && includes(plan,SpriteAtlas.BARGE_MINT),"Glow states prepared");
            check(includes(plan,ShadowArt.forSource(SpriteAtlas.TUG)) && includes(plan,ShadowArt.forSource(SpriteAtlas.BARGE)),"Both shadow styles prepared");
            for(int delivery=0;delivery<Challenges.quota(id);delivery++) {
                int berth=Challenges.feedBerth(id,delivery),atlas=berth==0?SpriteAtlas.STEEL:berth==2?SpriteAtlas.BULK:SpriteAtlas.CONTAINER;
                check(includes(plan,atlas) && includes(plan,SurfaceLightArt.forSource(atlas)),"Future cargo and every lighting direction included");
            }
            for(int a:plan.rotationAtlases()) {
                check(SpriteAtlas.rotationAtlas(a),"Preparation contains only directional families");
                check(SpriteAtlas.count(a)%32==0,"Complete 32-heading sets, not adjacent headings");
            }
            game.startTutorial();check(LevelAssets.upcoming(game)==tutorial,"Practice has its own manifest");
            game.activeGame().tutorial.lesson=Tutorial.TRAIN;
            check(LevelAssets.upcoming(game)==tutorial,"Later train lesson retains the complete tutorial manifest");
        }
        check(LevelAssets.challenge(0).rotationFrames()==1024 && LevelAssets.challenge(2).rotationFrames()==1024,"Calumet prepares cargo for every contiguous job");
        check(LevelAssets.challenge(1).rotationFrames()==1024 && LevelAssets.challenge(3).rotationFrames()==1024,"Ordering levels include all three cargo kinds");
        int[] copy=tutorial.rotationAtlases();copy[0]=999;
        check(!includes(tutorial,999),"Callers cannot mutate manifest");
        check(SpriteAtlas.rotationAtlases(SpriteAtlas.TUG,SpriteAtlas.TUG).length==4,"Duplicate sources expand once");
        check(SpriteAtlas.rotationAtlases(new int[0]).length==0,"Empty plan supported");
        StaticLayer[] resident=LevelAssets.challenge(Challenges.MONTROSE).residentLayers();
        check(resident.length==2,"Marina declares its pool and halo kernels");
        long bytes=0;
        for(StaticLayer layer:resident){check(layer.backgroundSafe(),"Resident kernels are immutable worker inputs");for(int density:new int[]{2,3})bytes+=(long)layer.width()*(long)layer.height()*density*density*4;}
        check(bytes==402688,"Both kernel density families remain below 400 KiB");
        resident[0]=null;
        check(LevelAssets.challenge(Challenges.MONTROSE).residentLayers()[0]!=null,"Resident manifest is defensively copied");
        check(LevelAssets.tutorial().residentLayers().length==0,"Non-marina stages release marina residency");
        System.out.println(checks+" upcoming-level asset, future cargo, rotation/effect and manifest isolation assertions passed");
    }
}
