package com.bargeboss.game;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import java.io.File;
import java.io.FileWriter;

/** Native wave raster guard; run separately from timing captures. */
final class AndroidWaterReview {
    static void run(AssetManager assets,File directory)throws Exception {
        try(FileWriter report=new FileWriter(new File(directory,"water-pixels.txt"))) {
            long total=0;
            for(int width:new int[]{960,1320,1920}) {
                Bitmap a=Bitmap.createBitmap(width,600,Bitmap.Config.ARGB_8888),b=Bitmap.createBitmap(width,600,Bitmap.Config.ARGB_8888);
                CanvasPainter before=new CanvasPainter(assets),after=new CanvasPainter(assets);
                before.profileWaterGrouping(true);after.profileWaterGrouping(false);
                before.bind(new Canvas(a));after.bind(new Canvas(b));
                int[] pa=new int[width*600],pb=new int[pa.length];
                try {
                    for(String scene:ProfileScenario.NAMES) {
                        Game root=new Game();ProfileScenario.prepare(root,scene,width,600);root.cancelInputs();Game g=root.activeGame();
                        int tick=g.visualTicks;
                        for(int offset:new int[]{0,29,107}) {
                            g.visualTicks=tick+offset;
                            WaveView.draw(g,new SceneryOcclusion(new ViewportPainter(before,width,600)));
                            WaveView.draw(g,new SceneryOcclusion(new ViewportPainter(after,width,600)));
                            a.getPixels(pa,0,width,0,0,width,600);b.getPixels(pb,0,width,0,0,width,600);
                            int changed=0,max=0;
                            for(int i=0;i<pa.length;i++)if(pa[i]!=pb[i]) {
                                changed++;
                                for(int shift=0;shift<32;shift+=8)max=Math.max(max,Math.abs(((pa[i]>>>shift)&255)-((pb[i]>>>shift)&255)));
                            }
                            total+=pa.length;
                            report.write(scene+" width="+width+" offset="+offset+" pixels="+pa.length+" changed="+changed+" max_error="+max+"\n");report.flush();
                            if(changed!=0)throw new AssertionError("Ungrouped water changed pixels: "+scene+" "+changed);
                        }
                    }
                } finally {before.close();after.close();a.recycle();b.recycle();}
            }
            report.write("PASS total_pixels="+total+"\n");
        }
        android.util.Log.i("BargeBossProfile","WATER_REVIEW passed");
    }
}
