package com.bargeboss.game;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import java.io.File;
import java.io.FileWriter;

/** Native concurrency, retention, pixel and cancellation review. Opt-in debug build only. */
final class AndroidRotationReview {
    static void run(AssetManager assets,File directory)throws Exception {
        CanvasPainter prepared=new CanvasPainter(assets,false),reference=new CanvasPainter(assets,false);
        Bitmap a=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888),b=Bitmap.createBitmap(96,96,Bitmap.Config.ARGB_8888);
        try {
            require(prepared.profileFrameBudget()==160L*1048576,"Production budget");
            // Production budget, including every registered derivative family.
            prepared.profileRotationCache(160,true);reference.profileRotationCache(24,false);
            prepared.bind(new Canvas(a));reference.bind(new Canvas(b));
            CanvasPainter.Profile stats=prepared.enableProfile();
            prepared.prepareRotations();prepared.prepareRotations(); // idempotent
            // Visible requests overlap preparation; publication must not double-account cells.
            for(int f=0;f<32;f++)prepared.profileFrameImage(SpriteAtlas.TUG,f);
            prepared.profileAwaitRotations(30000);
            require(prepared.profileRotationsReady(),"Preparation failed/timed out: "+prepared.profileRotationFailure());
            int total=0;long sum=0,pixels=0;int[] pa=new int[96*96],pb=new int[pa.length];
            for(int id:SpriteAtlas.rotationAtlases()) {
                reference.profileResetTextures();
                require(SpriteAtlas.rotationAtlas(id),"Preparation/pinning metadata disagree");
                for(int f=0;f<SpriteAtlas.count(id);f++) {
                    total++;prepared.beginFrame();
                    Bitmap frame=prepared.profileFrameImage(id,f);
                    require(frame==prepared.profileFrameImage(id,f),"Unstable frame publication");
                    if(!SpriteAtlas.framePack(id))sum+=frame.getAllocationByteCount();
                    a.eraseColor(0xff19374d);b.eraseColor(0xff19374d);
                    draw(prepared,id,f);draw(reference,id,f);
                    require(stats.frameBuilds==0 && stats.frameEvictions==0,"Rotation not retained");
                    a.getPixels(pa,0,96,0,0,96,96);b.getPixels(pb,0,96,0,0,96,96);
                    for(int i=0;i<pa.length;i++)require(pa[i]==pb[i],"Prepared pixel differs at atlas "+id+" frame "+f);
                    pixels+=pa.length;
                }
            }
            require(prepared.profileRotationFrames()==total,"Incomplete/duplicate preparation");
            require(sum==prepared.profileFrameBytes(),"Race double-counted frame storage");
            long rotationBytes=sum;int evictions=0;
            for(int id:new int[]{8,9,10,12,13,15,16,17,18,20,26,27,28,29,30,31}) {
                for(int f=0;f<SpriteAtlas.count(id);f++) {
                    prepared.beginFrame();prepared.profileFrameImage(id,f);evictions+=stats.frameEvictions;
                    require(prepared.profileFrameBytes()<=prepared.profileFrameBudget(),"Cache exceeded budget");
                }
            }
            // Frame packs reduce crop residency; record observed pressure rather than require churn.
            prepared.beginFrame();
            for(int id:SpriteAtlas.rotationAtlases())for(int f=0;f<SpriteAtlas.count(id);f++)prepared.profileFrameImage(id,f);
            require(stats.frameBuilds==0,"Unrelated scenery evicted a pinned rotation");
            long prepareNs=prepared.profileRotationNs();
            prepared.close();reference.close();
            // Cancel an in-progress atlas decode; no crops/sources may publish after close.
            CanvasPainter cancelled=new CanvasPainter(assets,false);
            cancelled.prepareRotations();cancelled.close();cancelled.profileAwaitRotations(30000);
            require(!cancelled.profileRotationsReady() && !cancelled.profileRotationWorking(),"Cancellation did not finish");
            require(cancelled.profileFrameBytes()==0 && cancelled.profileSourceBytes()==0,"Late publication after close");
            try(FileWriter report=new FileWriter(new File(directory,"rotation-review.txt"))) {
                report.write("frames="+total+" bytes="+rotationBytes+" prepare_ms="+(prepareNs/1e6)+" pixels="+pixels+" changed=0 pressure_evictions="+evictions+" rotation_rebuilds=0 cancellation=passed\n");
            }
            reviewLevels(assets,directory);
            android.util.Log.i("BargeBossProfile","ROTATIONS_REVIEW passed frames="+total+" bytes="+rotationBytes+" pixels="+pixels);
        } finally {prepared.close();reference.close();a.recycle();b.recycle();}
    }
    private static void reviewLevels(AssetManager assets,File directory)throws Exception {
        CanvasPainter scoped=new CanvasPainter(assets,false);
        try {
            scoped.profileRotationCache(160,true);
            int[] tutorial=LevelAssets.tutorial().rotationAtlases();
            scoped.prepareRotations(tutorial);scoped.profileAwaitRotations(30000);
            require(scoped.profileRotationsReady() && scoped.profileRotationFrames()==LevelAssets.tutorial().rotationFrames(),"Tutorial manifest incomplete");
            require(!scoped.profileHasAtlas(SpriteAtlas.STEEL) && !scoped.profileHasAtlas(SpriteAtlas.CONTAINER)
                && !scoped.profileHasAtlas(SurfaceLightArt.forSource(SpriteAtlas.STEEL))
                && !scoped.profileHasAtlas(SurfaceLightArt.forSource(SpriteAtlas.CONTAINER)),"Unused cargo was prefetched");
            long tutorialBytes=scoped.profileFrameBytes();
            scoped.prepareRotations(LevelAssets.challenge(0).rotationAtlases());scoped.profileAwaitRotations(30000);
            require(scoped.profileRotationsReady() && scoped.profileRotationFrames()==LevelAssets.challenge(0).rotationFrames(),"Next level's containers missing");
            require(scoped.profileHasAtlas(SpriteAtlas.STEEL),"Contiguous Calumet jobs need steel");
            require(!scoped.profileHasAtlas(SpriteAtlas.FIREWORKS) && !scoped.profileHasAtlas(SpriteAtlas.FIRE_BARGE),"Previous tutorial-only equipment stayed resident");
            scoped.prepareRotations(LevelAssets.challenge(1).rotationAtlases());scoped.profileAwaitRotations(30000);
            require(scoped.profileRotationsReady() && scoped.profileRotationFrames()==LevelAssets.challenge(1).rotationFrames(),"Later steel delivery missing");
            // Rapid selection replaces pending work rather than building a queue for each tap.
            scoped.prepareRotations(tutorial);
            scoped.prepareRotations(LevelAssets.challenge(0).rotationAtlases());
            scoped.prepareRotations(tutorial);scoped.profileAwaitRotations(30000);
            require(scoped.profileRotationsReady() && scoped.profileRotationFrames()==LevelAssets.tutorial().rotationFrames(),"Obsolete manifest overwrote current readiness");
            CanvasPainter.Profile stats=scoped.enableProfile();int evictions=0;
            for(int id:new int[]{8,9,10,12,13,15,16,17,18,20,26,27,28,29,30,31})for(int f=0;f<SpriteAtlas.count(id);f++) {
                scoped.beginFrame();scoped.profileFrameImage(id,f);evictions+=stats.frameEvictions;
                require(scoped.profileFrameBytes()<=scoped.profileFrameBudget(),"Scoped cache exceeded budget");
            }
            require(!scoped.profileHasAtlas(SpriteAtlas.STEEL) && !scoped.profileHasAtlas(SpriteAtlas.CONTAINER),"Previous level decoded cargo stayed resident");
            scoped.beginFrame();
            for(int id:tutorial)for(int f=0;f<SpriteAtlas.count(id);f++)scoped.profileFrameImage(id,f);
            require(stats.frameBuilds==0,"Current level frames were evicted");
            // A level's complete preparation list includes evictable scenery as well
            // as reserved rotations. Montrose previously overflowed and froze arrival.
            LevelAssets.Plan marina=LevelAssets.challenge(Challenges.MONTROSE);
            scoped.prepareRotations(marina.preparationAtlases());scoped.profileAwaitRotations(30000);
            require(scoped.profileRotationsReady(),"Full Montrose scenery/rescue manifest must finish");
            require(scoped.profileFrameBytes()<=scoped.profileFrameBudget(),"Montrose exceeds bounded cache");
            scoped.beginFrame();
            for(int id:marina.rotationAtlases())for(int f=0;f<SpriteAtlas.count(id);f++)scoped.profileFrameImage(id,f);
            require(stats.frameBuilds==0,"Scenery preparation evicted Montrose gameplay rotations");
            try(FileWriter report=new FileWriter(new File(directory,"rotation-levels.txt"))) {
                report.write("tutorial_frames="+LevelAssets.tutorial().rotationFrames()+" tutorial_bytes="+tutorialBytes+" calumet_frames="+LevelAssets.challenge(0).rotationFrames()+" unused_cargo=not_loaded rapid_switch=passed previous_level_released=passed current_level_rebuilds=0\n");
            }
        } finally {scoped.close();scoped.profileAwaitRotations(30000);}
    }
    private static void draw(CanvasPainter painter,int atlas,int frame) {
        painter.sprite(atlas,frame,24.25f,31.75f,35.5f,32.25f,.13f,.73f);
        painter.spriteAffine(atlas,frame,70.25f,32.75f,29.5f,-8.25f,11.5f,25.25f,.69f);
        painter.spriteBlend(atlas,frame,(frame+1)%SpriteAtlas.count(atlas),48.25f,77.75f,42.5f,32.5f,-.09f,.42f,.61f);
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
