package com.bargeboss.game;

/** Guards profiler accounting so cached calls cannot hide cold offscreen work. */
public final class LayerCounterTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static StaticLayer layer(final int key,final float size) {
        return new StaticLayer() {
            public int key(){return key;}public float width(){return size;}public float height(){return size;}
            public void paint(Painter p){for(int i=0;i<3;i++)p.rect(i*2,i*2,2,2,0xff123456);}
        };
    }
    public static void main(String[] args) {
        StaticLayer a=layer(0x7e000001,16),b=layer(0x7e000002,16);
        ScenePerformanceBenchmark.Counter c=new ScenePerformanceBenchmark.Counter(960,600,true);
        c.beginFrame();c.layer(a,10,10,1,1);c.layer(b,30,30,1,1);
        check(c.layerCalls==2 && c.cacheBuilds==1 && c.cacheFallbacks==1,"cold frame honors one native raster build and expands remaining layer");
        check(c.layerBlits==1 && c.buildPrimitives==3 && c.calls()==4,"offscreen build primitives are separate from target blit and fallback submissions");
        c.clear();c.beginFrame();c.layer(b,30,30,1,1);c.layer(a,10,10,1,.28f);
        check(c.cacheBuilds==1 && c.cacheHits==1 && c.cacheFallbacks==0 && c.calls()==2,"next frame fills cache and changed group alpha reuses immutable content");
        c.clear();c.beginFrame();c.layer(a,10,10,1,.61f);c.layer(b,30,30,1,1);
        check(c.cacheHits==2 && c.cacheBuilds==0 && c.buildPrimitives==0 && c.calls()==2,"warmed cache counts two blits without replaying layer commands");
        c.clear();c.beginFrame();c.layer(a,10,10,2,1);
        check(c.cacheBuilds==1 && c.cacheHits==0,"host density change uses a separate raster key");
        long retained=c.cacheBytes;c.clear();c.beginFrame();c.layer(a,-1000,-1000,1,1);
        check(c.layerCalls==0 && c.cacheBytes==retained,"offscreen layer does not pollute native cache or counters");
        ScenePerformanceBenchmark.Counter lru=new ScenePerformanceBenchmark.Counter(960,600,true);
        for(int i=0;i<4;i++){lru.clear();lru.beginFrame();lru.layer(layer(0x7e001000+i,1024),0,0,1,1);check(lru.cacheBytes<=StaticLayer.cacheBudgetBytes(),"LRU respects native byte cap");}
        lru.clear();lru.beginFrame();lru.layer(layer(0x7e001000,1024),0,0,1,1);
        check(lru.cacheBuilds==1 && lru.cacheHits==0,"oldest raster is rebuilt after byte-budget eviction");
        ScenePerformanceBenchmark.Counter oversized=new ScenePerformanceBenchmark.Counter(960,600,true);
        oversized.beginFrame();oversized.layer(layer(0x7e002000,4096),0,0,1,1);
        check(oversized.cacheBuilds==0 && oversized.cacheFallbacks==1 && oversized.cacheBytes==0 && oversized.calls()==3,"oversized layer stays uncached and replay work remains visible");
        System.out.println(checks+" native-layer profiling accounting assertions passed");
    }
}
