package com.bargeboss.game;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import java.io.File;
import java.io.FileWriter;

/** Compare the actual graded crop cache against all original factory texels. */
final class AndroidFactoryReview {
    static void run(AssetManager assets,File directory)throws Exception{
        CanvasPainter painter=new CanvasPainter(assets,false);long originalBytes=0,croppedBytes=0,pixels=0;
        painter.profileKeepFactoryOriginals(true);
        try(FileWriter report=new FileWriter(new File(directory,"factory-crop-pixels.txt"))){
            for(int frame=0;frame<FactoryArt.COUNT;frame++){
                Bitmap source=painter.profileFactoryOriginal(frame),crop=painter.profileFrameImage(SpriteAtlas.FACTORY,frame);
                Rect r=painter.profileFactoryBounds(frame);
                if(crop.getWidth()!=r.width() || crop.getHeight()!=r.height())throw new AssertionError("Crop dimensions changed");
                int[] row=new int[source.getWidth()],trimmed=new int[crop.getWidth()];
                for(int y=0;y<source.getHeight();y++){
                    source.getPixels(row,0,row.length,0,y,row.length,1);
                    if(y>=r.top && y<r.bottom)crop.getPixels(trimmed,0,trimmed.length,0,y-r.top,trimmed.length,1);
                    for(int x=0;x<row.length;x++){
                        if(r.contains(x,y)){if(row[x]!=trimmed[x-r.left])throw new AssertionError("Factory pixel changed");}
                        else if((row[x]>>>24)!=0)throw new AssertionError("Factory alpha discarded");
                        pixels++;
                    }
                }
                originalBytes+=source.getAllocationByteCount();croppedBytes+=crop.getAllocationByteCount();
                report.write(frame+" "+r+" source="+source.getAllocationByteCount()+" cropped="+crop.getAllocationByteCount()+"\n");
            }
            report.write("PASS pixels="+pixels+" source_bytes="+originalBytes+" cropped_bytes="+croppedBytes+"\n");
        }finally{painter.close();}
        android.util.Log.i("BargeBossProfile","FACTORY_REVIEW lossless pixels="+pixels+" source_bytes="+originalBytes+" cropped_bytes="+croppedBytes);
    }
}
