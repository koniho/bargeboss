package com.bargeboss.game;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import java.io.File;
import java.io.FileWriter;

/** Opt-in pixel comparison of native bitmap storage strategies, never shipped in normal APKs. */
final class AndroidTextureReview {
    static void run(AssetManager assets,File directory) throws java.io.IOException {
        CanvasPainter full=new CanvasPainter(assets),cells=new CanvasPainter(assets);
        full.profileStrategies(true,false);cells.profileStrategies(true,true);
        full.profileAsyncLayers(false);cells.profileAsyncLayers(false);
        Bitmap a=Bitmap.createBitmap(512,384,Bitmap.Config.ARGB_8888),b=Bitmap.createBitmap(512,384,Bitmap.Config.ARGB_8888);
        Canvas ca=new Canvas(a),cb=new Canvas(b);full.bind(ca);cells.bind(cb);
        int[] pa=new int[512*384],pb=new int[pa.length];long pixels=0,different=0;int cases=0,maxError=0;
        try(FileWriter report=new FileWriter(new File(directory,"texture-pixels.txt"))) {
            // All production atlases, including 32 headings, glow/shadow/shade layers,
            // irregular source crops, title strips, factories, foam and markers.
            for(int atlas=0;atlas<42;atlas++){
                full.profileResetTextures();cells.profileResetTextures();
                for(int frame=0;frame<SpriteAtlas.count(atlas);frame++) {
                ca.drawColor(0xff19374d);cb.drawColor(0xff19374d);
                for(int i=0;i<2;i++) {
                    CanvasPainter p=i==0?full:cells;
                    p.sprite(atlas,frame,128.25f,170.75f,170.5f,150.25f,.13f,.73f);
                    p.spriteAffine(atlas,frame,360.25f,190.75f,190.5f,-31.25f,67.5f,124.25f,.69f);
                    p.spriteBlend(atlas,frame,(frame+1)%SpriteAtlas.count(atlas),255.25f,310.75f,135.5f,100.5f,-.09f,.42f,.61f);
                }
                a.getPixels(pa,0,512,0,0,512,384);b.getPixels(pb,0,512,0,0,512,384);
                long changed=0;int caseError=0;
                for(int i=0;i<pa.length;i++)if(pa[i]!=pb[i]) {
                    changed++;
                    for(int shift=0;shift<32;shift+=8)caseError=Math.max(caseError,Math.abs(((pa[i]>>>shift)&255)-((pb[i]>>>shift)&255)));
                }
                maxError=Math.max(maxError,caseError);
                if(changed>0)report.write("atlas="+atlas+" frame="+frame+" changed="+changed+" max_error="+caseError+"\n");
                if(frame==0 && changed>0){save(a,directory,"texture-full-"+atlas+".png");save(b,directory,"texture-cells-"+atlas+".png");}
                different+=changed;pixels+=pa.length;cases++;
                }
            }
            report.write("cases="+cases+" pixels="+pixels+" changed="+different+" max_channel_error="+maxError+"\n");
        }
        full.profileResetTextures();cells.profileResetTextures();
        cells.profileRotationCache(24,false);
        CanvasPainter.Profile stress=cells.enableProfile();long maximum=0;int evictions=0;
        for(int atlas:new int[]{0,1,2,3,4,21,22,23,31,36,37,38,39,40})for(int f=0;f<SpriteAtlas.count(atlas);f++) {
            cells.beginFrame();cells.sprite(atlas,f,256,200,170,150,0,1);
            maximum=Math.max(maximum,cells.profileFrameBytes());evictions+=stress.frameEvictions;
            if(cells.profileFrameBytes()>cells.profileFrameBudget())throw new AssertionError("Frame texture cache exceeded budget");
        }
        if(evictions==0)throw new AssertionError("Frame texture stress did not exercise eviction");
        // The first tug heading has been evicted; reloading must still draw its source pixels.
        ca.drawColor(0xff19374d);cb.drawColor(0xff19374d);
        full.profileStrategies(true,true);
        full.sprite(0,0,256,200,170,150,0,1);cells.sprite(0,0,256,200,170,150,0,1);
        a.getPixels(pa,0,512,0,0,512,384);b.getPixels(pb,0,512,0,0,512,384);
        int reloadDifferences=0;for(int i=0;i<pa.length;i++)if(pa[i]!=pb[i])reloadDifferences++;
        if(reloadDifferences!=0)throw new AssertionError("Reloaded tug pixels differ: "+reloadDifferences);
        try(FileWriter report=new FileWriter(new File(directory,"texture-cache.txt"))){report.write("max_bytes="+maximum+" evictions="+evictions+" reloaded_tug_differences="+reloadDifferences+"\n");}
        full.profileStrategies(true,false);
        a.recycle();b.recycle();
        full.profileResetTextures();cells.profileResetTextures();
        Bitmap sceneA=Bitmap.createBitmap(1320,600,Bitmap.Config.ARGB_8888),sceneB=Bitmap.createBitmap(1320,600,Bitmap.Config.ARGB_8888);
        full.bind(new Canvas(sceneA));cells.bind(new Canvas(sceneB));
        int[] scenePa=new int[1320*600],scenePb=new int[scenePa.length];
        try(FileWriter report=new FileWriter(new File(directory,"texture-scenes.txt"))) {
            for(String scene:new String[]{"barge-arrival","harbor-close","barge-bridge","canal-middle","title"}) {
                Game g=new Game();if(scene.equals("title"))g.configureCanvas(1320,600);else ProfileScenario.prepare(g,scene,1320,600);g.cancelInputs();
                // Warm the same bounded terrain/UI caches for each painter.
                for(int warm=0;warm<90;warm++){g.render(full);g.render(cells);}
                sceneA.getPixels(scenePa,0,1320,0,0,1320,600);sceneB.getPixels(scenePb,0,1320,0,0,1320,600);
                int changed=0,error=0;
                for(int i=0;i<scenePa.length;i++)if(scenePa[i]!=scenePb[i]){
                    changed++;for(int shift=0;shift<32;shift+=8)error=Math.max(error,Math.abs(((scenePa[i]>>>shift)&255)-((scenePb[i]>>>shift)&255)));
                }
                report.write(scene+" pixels="+scenePa.length+" changed="+changed+" max_error="+error+"\n");
                save(sceneA,directory,scene+"-texture-full.png");save(sceneB,directory,scene+"-texture-cells.png");
            }
        }
        sceneA.recycle();sceneB.recycle();
        android.util.Log.i("BargeBossProfile","TEXTURE_PIXELS cases="+cases+" pixels="+pixels+" changed="+different+" max_channel_error="+maxError);
    }
    private static void save(Bitmap image,File directory,String name)throws java.io.IOException {
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new File(directory,name))){image.compress(Bitmap.CompressFormat.PNG,100,out);}
    }
}
