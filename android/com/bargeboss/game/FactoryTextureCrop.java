package com.bargeboss.game;

import android.graphics.Bitmap;
import android.graphics.Rect;

/** Lossless empty-margin crop, prepared during atlas decoding, never per draw. */
final class FactoryTextureCrop {
    static Rect bounds(Bitmap image,Rect source){
        int left=source.right,top=source.bottom,right=source.left,bottom=source.top;
        int[] row=new int[source.width()];
        for(int y=source.top;y<source.bottom;y++){
            image.getPixels(row,0,row.length,source.left,y,row.length,1);
            for(int x=0;x<row.length;x++)if((row[x]>>>24)!=0){
                left=Math.min(left,x+source.left);right=Math.max(right,x+source.left+1);
                top=Math.min(top,y);bottom=Math.max(bottom,y+1);
            }
        }
        if(right<=left || bottom<=top)return new Rect(source);
        // Retain transparent texels for edge sampling; never discard faint alpha.
        return new Rect(Math.max(source.left,left-2),Math.max(source.top,top-2),Math.min(source.right,right+2),Math.min(source.bottom,bottom+2));
    }
}
