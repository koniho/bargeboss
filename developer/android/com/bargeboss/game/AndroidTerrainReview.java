package com.bargeboss.game;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Native pixel, reservation, publication and shutdown checks. Run separately from timings. */
final class AndroidTerrainReview {
    static void run(AssetManager assets,File directory)throws Exception {
        queueChecks(assets);completedHandoff(assets);cachePressure(assets);
        try(FileWriter report=new FileWriter(new File(directory,"terrain-pixels.txt"))) {
            for(String scene:new String[]{"barge-arrival","harbor-close","barge-bridge","canal-middle"}) {
                CanvasPainter before=new CanvasPainter(assets),after=new CanvasPainter(assets);
                before.profileAsyncLayers(false);after.profileAsyncLayers(true);
                CanvasPainter.Profile stats=after.enableProfile(),beforeStats=before.enableProfile();
                Bitmap a=Bitmap.createBitmap(1320,600,Bitmap.Config.ARGB_8888),b=Bitmap.createBitmap(1320,600,Bitmap.Config.ARGB_8888);
                before.bind(new Canvas(a));after.bind(new Canvas(b));
                try {
                    Game g=new Game();ProfileScenario.prepare(g,scene,1320,600);g.cancelInputs();
                    int quiet=0;
                    for(int warm=0;warm<160;warm++) {
                        g.render(before);g.render(after);
                        if(after.profileCacheBytes()+after.profilePendingBytes()>StaticLayer.cacheBudgetBytes())throw new AssertionError("Raster cache plus reservations exceeded budget");
                        if(stats.asyncMisses==0 && stats.fallbacks==0 && beforeStats.fallbacks==0 && stats.builds==0 && beforeStats.builds==0)quiet++;else quiet=0;
                        if(warm>=90 && quiet>=5)break;
                        Thread.sleep(3);
                    }
                    if(quiet<5)throw new AssertionError("Visible terrain did not converge: "+scene);
                    int[] pa=new int[1320*600],pb=new int[pa.length];a.getPixels(pa,0,1320,0,0,1320,600);b.getPixels(pb,0,1320,0,0,1320,600);
                    int changed=0,max=0;
                    for(int i=0;i<pa.length;i++)if(pa[i]!=pb[i]){changed++;for(int shift=0;shift<32;shift+=8)max=Math.max(max,Math.abs(((pa[i]>>>shift)&255)-((pb[i]>>>shift)&255)));}
                    report.write(scene+" pixels="+pa.length+" changed="+changed+" max_error="+max+" cache_bytes="+after.profileCacheBytes()+" pending_bytes="+after.profilePendingBytes()+"\n");report.flush();
                    save(a,directory,scene+"-terrain-before.png");save(b,directory,scene+"-terrain-after.png");
                    if(changed!=0)throw new AssertionError("Background terrain changed warm pixels: "+scene+" "+changed);
                } finally {before.close();after.close();a.recycle();b.recycle();}
            }
            report.write("queue bounds, duplicate requests, visible priority, full-cache progress, nonblocking close and unpublished-result shutdown passed\n");
        }
        android.util.Log.i("BargeBossProfile","TERRAIN_REVIEW passed");
    }
    private static StaticLayer solid(final int key){return new StaticLayer(){
        public int key(){return key;}public float width(){return 256;}public float height(){return 256;}
        public boolean backgroundSafe(){return true;}
        public void paint(Painter p){p.rect(0,0,256,256,0xff123456);}
    };}
    private static void cachePressure(AssetManager assets)throws Exception {
        CanvasPainter painter=new CanvasPainter(assets,false);Bitmap target=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);
        try {
            painter.bind(new Canvas(target));painter.profileAsyncLayers(false);CanvasPainter.Profile stats=painter.enableProfile();
            for(int i=0;i<21;i++){painter.beginFrame();painter.layer(solid(1000+i),0,0,2,1);}
            if(painter.profileCacheBytes()!=21L*768*768*4)throw new AssertionError("Pressure fixture did not fill retained cache");
            painter.profileAsyncLayers(true);boolean ready=false;int evictions=0;
            for(int i=0;i<100;i++) {
                painter.beginFrame();painter.layer(solid(2000),0,0,2,1);evictions+=stats.evictions;
                if(painter.profileCacheBytes()+painter.profilePendingBytes()>StaticLayer.cacheBudgetBytes())throw new AssertionError("Reservations escaped cache budget");
                if(stats.asyncMisses==0){ready=true;break;}Thread.sleep(3);
            }
            if(!ready || evictions==0 || target.getPixel(20,20)!=0xff123456)throw new AssertionError("Visible miss failed to make progress under pressure");
        } finally {painter.close();target.recycle();}
    }
    private static StaticLayer block(final int key,final CountDownLatch started,final CountDownLatch release) {
        return new StaticLayer(){
            public int key(){return key;}public float width(){return 16;}public float height(){return 16;}
            public boolean backgroundSafe(){return true;}
            public void paint(Painter p){
                if(started!=null)started.countDown();
                if(release!=null)try{if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("test gate timed out");}catch(InterruptedException e){throw new IllegalStateException(e);}
                p.rect(0,0,16,16,0xff123456);
            }
        };
    }
    private static void queueChecks(AssetManager assets)throws Exception {
        TerrainRasterizer queue=new TerrainRasterizer(assets);
        CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);
        try {
            StaticLayer first=block(1,started,release);
            if(!queue.request(1,first,1,1024,4096,true) || !started.await(5,TimeUnit.SECONDS))throw new AssertionError("Worker did not start");
            for(int i=2;i<=4;i++)if(!queue.request(i,block(i,null,null),1,1024,4096,false))throw new AssertionError("Queue admission failed");
            queue.request(1,first,1,1024,4096,true);
            if(queue.reservedBytes()!=4096)throw new AssertionError("Duplicate request changed reservation");
            if(!queue.request(5,block(5,null,null),1,1024,4096,true) || queue.reservedBytes()!=4096)throw new AssertionError("Visible request did not replace speculation");
            if(queue.request(6,block(6,null,null),1,5000,4096,false))throw new AssertionError("Oversized reservation accepted");
            if(queue.take(1)!=null)throw new AssertionError("Unfinished raster published");
            queue.close();
            if(queue.reservedBytes()!=0 || queue.request(7,first,1,1024,4096,true))throw new AssertionError("Closed worker accepted work");
            release.countDown();
            if(queue.take(1)!=null)throw new AssertionError("Closed worker published an image");
        } finally {release.countDown();queue.close();}
    }
    private static void completedHandoff(AssetManager assets)throws Exception {
        for(boolean visible:new boolean[]{false,true}) {
            TerrainRasterizer queue=new TerrainRasterizer(assets);
            try {
                for(int i=0;i<4;i++)queue.request(i,block(i,null,null),1,1024,4096,visible);
                CanvasPainter.Profile stats=new CanvasPainter.Profile();
                for(int i=0;i<1000 && stats.backgroundBuilds<4;i++){Thread.sleep(2);queue.telemetry(stats);}
                if(stats.backgroundBuilds!=4)throw new AssertionError("Handoff fixture did not finish");
                queue.beginFrame();
                boolean accepted=queue.request(9,block(9,null,null),1,1024,4096,true);
                if(accepted==visible)throw new AssertionError("Completed visible result lost or speculation blocked demand");
                if(visible){
                    Bitmap result=queue.take(0);if(result==null || result.getPixel(0,0)!=0xff123456)throw new AssertionError("Completed result lost before late draw");result.recycle();
                    queue.beginFrame();queue.beginFrame();queue.trimTo(0);
                    if(queue.reservedBytes()!=0)throw new AssertionError("Abandoned results did not expire");
                }
            } finally {queue.close();}
        }
    }
    private static void save(Bitmap b,File directory,String name)throws java.io.IOException {
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new File(directory,name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}
    }
}
