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
        RenderNode oversized=new RenderNode("mesh-budget-review");oversized.setPosition(0,0,8,8);
        oversized.beginRecording().drawColor(0xff123456);oversized.endRecording();
        cache.put(500,oversized,9L*1024*1024);
        if(cache.get(500)!=null || cache.bytes()!=0 || !oversized.hasDisplayList())throw new AssertionError("Mesh storage was not bounded safely");
        oversized.discardDisplayList();
        android.util.Log.i("BargeBossProfile","SURFACE_REVIEW bounds/eviction/reload/invalidation/close passed");
    }
}
