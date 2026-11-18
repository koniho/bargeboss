package com.bargeboss.game;

import android.graphics.RenderNode;
import java.util.LinkedHashMap;
import java.util.Map;

/** Surface-only display lists: no bitmap raster cache, independently ordered chunks. */
@android.annotation.TargetApi(29)
final class RetainedSurfaces {
    private static final int MAX_ENTRIES=96;
    private static final long MAX_BYTES=8L*1024*1024;
    private final LinkedHashMap<Integer,RenderNode> nodes=new LinkedHashMap<>(32,.75f,true);
    private long bytes;
    long bytes(){return bytes;}
    RenderNode get(int key){
        RenderNode node=nodes.get(key);
        if(node!=null && !node.hasDisplayList()){nodes.remove(key);recount();return null;}
        return node;
    }
    void put(int key,RenderNode node){
        nodes.put(key,node);recount();
        while(nodes.size()>MAX_ENTRIES || bytes>MAX_BYTES){
            Map.Entry<Integer,RenderNode> oldest=nodes.entrySet().iterator().next();
            // Do not discard a node referenced by this frame's recorded parent.
            nodes.remove(oldest.getKey());recount();
        }
    }
    private void recount(){bytes=0;for(RenderNode n:nodes.values())bytes+=n.computeApproximateMemoryUsage();}
    void clear(){for(RenderNode n:nodes.values())n.discardDisplayList();nodes.clear();bytes=0;}
}
