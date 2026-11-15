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
    private final boolean trace;
    private final File directory;
    private final double[][] rows;
    private static final String HEADER="frame,phase,lesson,tick,zoom,advance_ms,draw_ms,interval_ms,layer_build_ms,decode_ms,cache_hits,cache_builds,cache_fallbacks,cache_evictions,cache_mib,sprites,blends,affines,circles,polys,lines,clips,draw_start_ns,draw_end_ns\n";
    private int frame;
    private boolean configured,finished,tracing;
    private long drawStart,previousDraw,advanceNs;
    public AndroidProfileSession(Activity activity,CanvasPainter painter) {
        Intent intent=activity.getIntent();this.painter=painter;stats=painter.enableProfile();
        scene=intent.getStringExtra("bb_scene");
        String requested=intent.getStringExtra("bb_mode");mode=requested==null?"live":requested;
        if(!mode.equals("live") && !mode.equals("freeze") && !mode.equals("replay"))throw new IllegalArgumentException("bb_mode must be live, freeze or replay");
        boolean valid=false;for(String n:ProfileScenario.NAMES)if(n.equals(scene))valid=true;
        if(!valid)throw new IllegalArgumentException("Unknown bb_scene");
        warmup=Math.max(0,Math.min(1800,intent.getIntExtra("bb_warmup",120)));
        samples=Math.max(30,Math.min(3600,intent.getIntExtra("bb_frames",360)));
        trace=intent.getBooleanExtra("bb_trace",false);
        directory=new File(activity.getExternalFilesDir(null),"profiles");
        if(!directory.isDirectory() && !directory.mkdirs())throw new IllegalStateException("Cannot create profile directory");
        rows=new double[warmup+samples][22];
    }
    public Game game(){return root;}
    public void configure(float width,float height) {
        if(configured)return;
        ProfileScenario.prepare(root,scene,width,height);configured=true;
        root.hapticsEnabled=false;root.activeGame().hapticsEnabled=false;
        // Live mode advances the world without the replay's held pointer. Replay keeps its exact gesture.
        if(!mode.equals("replay"))root.cancelInputs();
        Log.i("BargeBossProfile","READY scene="+scene+" mode="+mode+" width="+width+" height="+height+" warmup="+warmup+" samples="+samples);
    }
    public void advance(double seconds) {
        if(!configured || finished)return;
        long start=System.nanoTime();
        if(mode.equals("replay"))ProfileScenario.replayTick(root);
        else if(mode.equals("live"))root.advance(seconds);
        advanceNs+=System.nanoTime()-start;
    }
    public void beforeDraw() {
        if(!configured || finished)return;
        if(trace && frame==warmup){
            Debug.startMethodTracingSampling(new File(directory,scene+"-"+mode+".trace").getAbsolutePath(),32*1024*1024,1000);tracing=true;
        }
        drawStart=System.nanoTime();
    }
    public void afterDraw() {
        if(!configured || finished)return;
        long end=System.nanoTime(),drawNs=end-drawStart,interval=previousDraw==0?0:drawStart-previousDraw;previousDraw=drawStart;
        Game g=root.activeGame();
        double[] r=rows[frame];
        r[0]=g.tutorial.lesson;r[1]=g.visualTicks;r[2]=g.layout.camera.scale;
        r[3]=advanceNs/1e6;r[4]=drawNs/1e6;r[5]=interval/1e6;r[6]=stats.buildNs/1e6;r[7]=stats.decodeNs/1e6;
        r[8]=stats.hits;r[9]=stats.builds;r[10]=stats.fallbacks;r[11]=stats.evictions;r[12]=painter.profileCacheBytes()/1048576.0;
        r[13]=stats.sprites;r[14]=stats.blends;r[15]=stats.affines;r[16]=stats.circles;r[17]=stats.polys;r[18]=stats.lines;r[19]=stats.clips;
        r[20]=drawStart;r[21]=end;
        advanceNs=0;frame++;
        if(frame==warmup)Log.i("BargeBossProfile","SAMPLING "+scene+" "+mode);
        if(frame>=warmup+samples)finish("complete");
    }
    public boolean running(){return !finished;}
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
