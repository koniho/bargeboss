package com.bargeboss.game;

import android.graphics.RenderNode;

/** Command-cache bounds, eviction, native invalidation and lifecycle checks. */
final class AndroidSurfaceReview {
    static void run(){
        RetainedSurfaces cache=new RetainedSurfaces();RenderNode first=null;
        for(int i=0;i<120;i++){
            RenderNode node=new RenderNode("surface-review");node.setPosition(0,0,256,256);
            node.beginRecording().drawColor(0xff123456);node.endRecording();
            if(i==0)first=node;cache.put(i,node);
            if(cache.bytes()>8L*1024*1024)throw new AssertionError("Surface command budget exceeded");
        }
        if(cache.get(0)!=null || cache.get(119)==null || !first.hasDisplayList())throw new AssertionError("Eviction invalidated recorded references");
        RenderNode last=cache.get(119);last.discardDisplayList();
        if(cache.get(119)!=null)throw new AssertionError("Discarded command list was reused");
        cache.put(0,first);if(cache.get(0)!=first)throw new AssertionError("Evicted chunk failed to reload");
        cache.clear();if(cache.bytes()!=0 || first.hasDisplayList())throw new AssertionError("Command cache failed to close");
        android.util.Log.i("BargeBossProfile","SURFACE_REVIEW bounds/eviction/reload/invalidation/close passed");
    }
}
