package com.bargeboss.game;

/** Artwork budgets, manifest isolation and continuously rigid lift travel, including reversal. */
final class IslandSceneryTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void run(){
        for(int id=0;id<Challenges.COUNT;id++){
            boolean islands=Challenges.kind(id)==Challenges.ISLAND_TAG,trees=false,ground=false;
            for(int a:LevelAssets.challenge(id).preparationAtlases()){trees|=a==SpriteAtlas.ISLAND_WOODLAND;ground|=a==SpriteAtlas.ISLAND_GROUND;}
            check(trees==islands && ground==islands,"Only the islands retain woodland/ground textures");
        }
        ArenaCourse course=new ArenaCourse.Islands();
        for(int k=0;k<course.gates();k++){
            IslandFootbridge bridge=new IslandFootbridge(course,k);check(bridge.faces()<400,"Bounded bridge topology");
            float[] old=bridge.spanPoint(0);float x=old[0],y=old[1];
            for(int tick=1;tick<=120;tick++){
                float phase=tick<=60?tick/60f:(120-tick)/60f;bridge.update(phase);
                float[] p=bridge.spanPoint(0),q=bridge.spanPoint(37);
                check(Math.abs(p[0]-x)<.001f && Math.abs(p[1]-y)<.001f,"Pure vertical lift keeps its shore alignment");
                check(Math.abs(Physics.length(q[0]-p[0],q[1]-p[1])-37)<.001f && Math.abs(q[2]-p[2])<.001f,"Rigid deck length through the full lift");
                check(tick<=60?p[2]>old[2]:p[2]<old[2],"No held/sampled travel ticks, including reversal");old=p;
            }
            bridge.update(.42317f);float z=bridge.spanPoint(0)[2];bridge.update(.42318f);
            check(bridge.spanPoint(0)[2]>z,"Arbitrary fractions remain continuous");
            check(bridge.faces()<400,"Repeated updates do not accumulate topology");
        }
        Game g=new Game();g.selectChallenge(Challenges.ISLAND_TAG);g.reset();
        float[] a=ArenaCourse.Islands.ISLES[4];RiverCamera c=RiverCamera.DEFAULT;
        float[] full=IslandArt.shore(c,a[0]-180,a[1]-180,a[0]+180,a[1]+180,Float.NEGATIVE_INFINITY);
        check(ShoreOcclusion.contains(full,c.screenX(a[0],a[1]),c.screenY(a[0],a[1])-5*c.scale),"Painted island center is solid");
        check(!ShoreOcclusion.contains(full,c.screenX(a[0]+200,a[1]),c.screenY(a[0]+200,a[1])),"Navigation channel remains clear");
        check(full.length<=64*6,"Single island shoreline mask is bounded");
        for(float v:full)check(Float.isFinite(v),"Finite registered shoreline");
        System.out.println(checks+" island manifest, shoreline budget and continuous lift checks passed");
    }
    public static void main(String[] args){run();}
}
