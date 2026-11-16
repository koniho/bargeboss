package com.bargeboss.game;

import android.app.Activity;
import android.content.Intent;
import android.os.Debug;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;

/** Debug-only measurement adapter. Fixtures and replay inputs stay in platform-free Java. */
public final class AndroidProfileSession implements DeveloperHooks.Session {
    private final Game root=new Game();
    private final CanvasPainter painter;
    private final CanvasPainter.Profile stats;
    private final String scene,mode;
    private final int warmup,samples;
    private final boolean trace,systemTrace,compoundPaths,frameTextures,asyncLayers;
    private final File directory;
    private final double[][] rows;
    private static final String HEADER="frame,phase,lesson,tick,zoom,advance_ms,draw_ms,interval_ms,layer_build_ms,decode_ms,cache_hits,cache_builds,cache_fallbacks,cache_evictions,cache_mib,sprites,blends,affines,circles,polys,lines,clips,draw_start_ns,draw_end_ns,texture_mib,source_mib,frame_mib,frame_build_ms,frame_builds,frame_evictions,pending_mib,worker_source_mib,background_build_ms,background_builds,async_misses,layer_key,layer_width,layer_height,rotation_frames,rotation_ready,rotation_prepare_ms\n";
    private int frame;
    private boolean configured,finished,tracing,reviewStarted,reviewDone;
    private final Activity activity;
    private final boolean pixelReview,terrainReview,waterReview,waterGrouping,rotationReview;
    private long drawStart,previousDraw,advanceNs;
    public AndroidProfileSession(Activity activity,CanvasPainter painter) {
        this.activity=activity;Intent intent=activity.getIntent();this.painter=painter;stats=painter.enableProfile();
        scene=intent.getStringExtra("bb_scene");
        String requested=intent.getStringExtra("bb_mode");mode=requested==null?"live":requested;
        if(!mode.equals("live") && !mode.equals("freeze") && !mode.equals("replay"))throw new IllegalArgumentException("bb_mode must be live, freeze or replay");
        boolean valid=false;for(String n:ProfileScenario.NAMES)if(n.equals(scene))valid=true;
        if(!valid)throw new IllegalArgumentException("Unknown bb_scene");
        warmup=Math.max(0,Math.min(1800,intent.getIntExtra("bb_warmup",120)));
        samples=Math.max(30,Math.min(3600,intent.getIntExtra("bb_frames",360)));
        trace=intent.getBooleanExtra("bb_trace",false);
        systemTrace=intent.getBooleanExtra("bb_system_trace",false);
        rotationReview=intent.getBooleanExtra("bb_rotation_review",false);
        painter.profileRotationCache(intent.getIntExtra("bb_frame_cache_mib",160),intent.getBooleanExtra("bb_rotation_prefetch",true));
        pixelReview=intent.getBooleanExtra("bb_texture_review",false);
        waterReview=intent.getBooleanExtra("bb_water_review",false);
        waterGrouping=intent.getBooleanExtra("bb_water_grouping",false);
        painter.profileWaterGrouping(waterGrouping);
        terrainReview=intent.getBooleanExtra("bb_terrain_review",false);
        compoundPaths=intent.getBooleanExtra("bb_compound_paths",false);
        frameTextures=intent.getBooleanExtra("bb_frame_textures",true);
        painter.profileStrategies(compoundPaths,frameTextures);
        asyncLayers=intent.getBooleanExtra("bb_async_layers",true);painter.profileAsyncLayers(asyncLayers);
        directory=new File(activity.getExternalFilesDir(null),"profiles");
        if(!directory.isDirectory() && !directory.mkdirs())throw new IllegalStateException("Cannot create profile directory");
        rows=new double[warmup+samples][39];
    }
    public Game game(){return root;}
    public void configure(float width,float height) {
        // Activity startup can briefly report the previous portrait window.
        // Do not prepare or time a landscape fixture against that transient layout.
        if(configured || width<height)return;
        if((pixelReview || terrainReview || waterReview || rotationReview) && !reviewDone) {
            if(!reviewStarted){
                reviewStarted=true;
                new Thread(()->{
                    try{if(rotationReview)AndroidRotationReview.run(activity.getAssets(),directory);if(waterReview)AndroidWaterReview.run(activity.getAssets(),directory);if(pixelReview)AndroidTextureReview.run(activity.getAssets(),directory);if(terrainReview)AndroidTerrainReview.run(activity.getAssets(),directory);}
                    catch(Exception e){throw new IllegalStateException(e);}
                    activity.runOnUiThread(()->{reviewDone=true;configure(width,height);});
                },"bargeboss-texture-review").start();
            }
            return;
        }
        ProfileScenario.prepare(root,scene,width,height);configured=true;
        root.hapticsEnabled=false;root.activeGame().hapticsEnabled=false;
        // Live mode advances the world without the replay's held pointer. Replay keeps its exact gesture.
        if(!mode.equals("replay"))root.cancelInputs();
        Log.i("BargeBossProfile","READY scene="+scene+" mode="+mode+" width="+width+" height="+height+" warmup="+warmup+" samples="+samples+" compound_paths="+compoundPaths+" frame_textures="+frameTextures+" async_layers="+asyncLayers+" water_grouping="+waterGrouping+" frame_cache_mib="+(painter.profileFrameBudget()/1048576)+" rotation_prefetch="+activity.getIntent().getBooleanExtra("bb_rotation_prefetch",true));
    }
    public void advance(double seconds) {
        if(!configured || finished)return;
        long start=System.nanoTime();
        if(mode.equals("live"))root.advance(seconds);
        advanceNs+=System.nanoTime()-start;
    }
    public void beforeDraw() {
        if(!configured || finished)return;
        if(trace && frame==warmup){
            Debug.startMethodTracingSampling(new File(directory,scene+"-"+mode+".trace").getAbsolutePath(),32*1024*1024,1000);tracing=true;
        }
        // A Choreographer callback need not produce a draw. Advance replay here
        // so every recorded row has exactly one tick regardless of render speed.
        if(mode.equals("replay")){long start=System.nanoTime();ProfileScenario.replayTick(root);advanceNs+=System.nanoTime()-start;}
        if(systemTrace)android.os.Trace.beginSection("BB draw "+frame);
        drawStart=System.nanoTime();
    }
    public void afterDraw() {
        if(!configured || finished)return;
        if(systemTrace)android.os.Trace.endSection();
        long end=System.nanoTime(),drawNs=end-drawStart,interval=previousDraw==0?0:drawStart-previousDraw;previousDraw=drawStart;
        Game g=root.activeGame();
        double[] r=rows[frame];
        r[0]=g.tutorial.lesson;r[1]=g.visualTicks;r[2]=g.layout.camera.scale;
        r[3]=advanceNs/1e6;r[4]=drawNs/1e6;r[5]=interval/1e6;r[6]=stats.buildNs/1e6;r[7]=stats.decodeNs/1e6;
        r[8]=stats.hits;r[9]=stats.builds;r[10]=stats.fallbacks;r[11]=stats.evictions;r[12]=painter.profileCacheBytes()/1048576.0;
        r[13]=stats.sprites;r[14]=stats.blends;r[15]=stats.affines;r[16]=stats.circles;r[17]=stats.polys;r[18]=stats.lines;r[19]=stats.clips;
        r[20]=drawStart;r[21]=end;
        r[22]=stats.textureBytes/1048576.0;r[23]=painter.profileSourceBytes()/1048576.0;r[24]=painter.profileFrameBytes()/1048576.0;
        r[25]=stats.frameBuildNs/1e6;r[26]=stats.frameBuilds;r[27]=stats.frameEvictions;
        r[28]=painter.profilePendingBytes()/1048576.0;r[29]=painter.profileWorkerSourceBytes()/1048576.0;
        r[30]=stats.backgroundNs/1e6;r[31]=stats.backgroundBuilds;r[32]=stats.asyncMisses;r[33]=stats.layerKey;r[34]=stats.layerWidth;r[35]=stats.layerHeight;
        r[36]=painter.profileRotationFrames();r[37]=painter.profileRotationsReady()?1:0;r[38]=painter.profileRotationNs()/1e6;
        advanceNs=0;frame++;
        if(frame==warmup)Log.i("BargeBossProfile","SAMPLING "+scene+" "+mode);
        if(frame>=warmup+samples)finish("complete");
    }
    public boolean running(){return configured && !finished;}
    public void stop(){if(configured && !finished)finish("interrupted");}
    private void finish(String reason) {
        finished=true;if(tracing){Debug.stopMethodTracing();tracing=false;}
        final int count=frame;final File output=new File(directory,scene+"-"+mode+".csv");
        new Thread(()->{
            File temporary=new File(directory,scene+"-"+mode+".csv.tmp");
            try {
                try(FileWriter writer=new FileWriter(temporary)){
                    writer.write(HEADER);
                    for(int i=0;i<count;i++){
                        StringBuilder line=new StringBuilder(256);line.append(i).append(',').append(i<warmup?"warmup":"sample");
                        for(double value:rows[i])line.append(',').append(value);
                        writer.write(line.append('\n').toString());
                    }
                }
                if(!temporary.renameTo(output))throw new java.io.IOException("Cannot publish profile CSV");
                Log.i("BargeBossProfile","SAVED "+reason+" "+output+" frames="+count);
            }
            catch(java.io.IOException error){Log.e("BargeBossProfile","Profile export failed",error);}
        },"bargeboss-profile-export").start();
    }
}
