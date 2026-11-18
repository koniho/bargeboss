package com.bargeboss.game;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import java.util.LinkedHashMap;
import java.util.Iterator;

/** One worker, four reservations, no live painter/Game access. Results publish under the monitor. */
final class TerrainRasterizer implements AutoCloseable {
    private static final int LIMIT=4;
    private static final class Job {
        final long key,bytes;final StaticLayer layer;final float density;
        boolean visible,active,demanded;int requestedFrame;Bitmap image;RuntimeException failure;
        Job(long key,StaticLayer layer,float density,long bytes,boolean visible){this.key=key;this.layer=layer;this.density=density;this.bytes=bytes;this.visible=visible;this.demanded=visible;}
    }
    private final AssetManager assets;
    private final LinkedHashMap<Long,Job> jobs=new LinkedHashMap<Long,Job>(8,.75f,true);
    private Thread thread;private boolean closed;private int frame;
    private long reserved,completedNs;private int completed;
    private volatile long sourceBytes;
    TerrainRasterizer(AssetManager assets){this.assets=assets;}
    synchronized void beginFrame(){frame++;for(Job job:jobs.values())job.visible=false;}
    synchronized void trimTo(long allowance) {
        Iterator<Job> entries=jobs.values().iterator();
        while(reserved>allowance && entries.hasNext()){
            Job old=entries.next();if(evictable(old)){entries.remove();reserved-=old.bytes;}
        }
    }
    // A completed image may be drawn late in the next frame. Do not let an
    // earlier request evict it just because beginFrame cleared its visible bit.
    // Two frames of grace stay bounded by LIMIT; abandoned camera work can age out.
    private boolean evictable(Job job){return !job.active && !job.visible
            && (!job.demanded || job.image==null && job.failure==null || frame-job.requestedFrame>2);}
    synchronized long reservedBytes(){return reserved;}
    long sourceBytes(){return sourceBytes;}
    synchronized void telemetry(CanvasPainter.Profile profile) {
        if(profile!=null){profile.backgroundNs+=completedNs;profile.backgroundBuilds+=completed;}
        completedNs=0;completed=0;
    }
    synchronized Bitmap take(long key) {
        Job job=jobs.get(key);if(job==null)return null;
        if(job.failure!=null){jobs.remove(key);reserved-=job.bytes;throw job.failure;}
        if(job.image==null)return null;
        jobs.remove(key);reserved-=job.bytes;return job.image;
    }
    synchronized boolean request(long key,StaticLayer layer,float density,long bytes,long allowance,boolean visible) {
        if(closed || bytes<=0 || bytes>allowance)return false;
        Job existing=jobs.get(key);
        if(existing!=null){existing.visible|=visible;if(visible || !existing.demanded)existing.requestedFrame=frame;existing.demanded|=visible;return true;}
        while(jobs.size()>=LIMIT || reserved+bytes>allowance) {
            boolean removed=false;
            Iterator<Job> entries=jobs.values().iterator();
            while(entries.hasNext()){
                Job old=entries.next();
                if(evictable(old)){entries.remove();reserved-=old.bytes;removed=true;break;}
            }
            if(!removed)return false;
        }
        Job added=new Job(key,layer,density,bytes,visible);added.requestedFrame=frame;
        jobs.put(key,added);reserved+=bytes;
        if(thread==null){thread=new Thread(this::work,"bargeboss-terrain");thread.setDaemon(true);thread.start();}
        notifyAll();return true;
    }
    private void work() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        CanvasPainter painter=new CanvasPainter(assets,false);
        try {
            while(true) {
                Job job;
                synchronized(this) {
                    while((job=next())==null && !closed)try{wait();}catch(InterruptedException ignored){closed=true;}
                    if(closed)return;
                    job.active=true;
                }
                Bitmap image=null;RuntimeException failure=null;long start=System.nanoTime();
                try{image=painter.rasterLayer(job.layer,job.density);}catch(RuntimeException error){failure=error;}
                long elapsed=System.nanoTime()-start;sourceBytes=painter.profileSourceBytes();
                synchronized(this) {
                    job.active=false;
                    if(!closed){job.image=image;job.failure=failure;completedNs+=elapsed;completed++;}
                }
            }
        } finally {painter.close();}
    }
    // Monitor held; visible misses take precedence over speculative requests.
    private Job next(){Job first=null;for(Job job:jobs.values())if(!job.active && job.image==null && job.failure==null){if(job.visible)return job;if(first==null)first=job;}return first;}
    public synchronized void close(){closed=true;jobs.clear();reserved=0;notifyAll();}
}
