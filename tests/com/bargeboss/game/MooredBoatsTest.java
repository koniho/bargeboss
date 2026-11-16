package com.bargeboss.game;
public final class MooredBoatsTest {
    static int checks;
    static void check(boolean yes,String text){checks++;if(!yes)throw new AssertionError(text);}
    public static void main(String[] args){boolean[] kinds=new boolean[6];float previous=-Float.MAX_VALUE;
        check(MooredBoats.BERTHS.length>150,"populated harbor");
        for(MooredBoats.Berth b:MooredBoats.BERTHS){kinds[b.kind]=true;check(b.y-b.x>=previous,"depth order");previous=b.y-b.x;
            check(b.frame>=0 && b.frame<SpriteAtlas.count(SpriteAtlas.MOORED_BOATS),"atlas range");
            for(int i=0;i<8;i+=2)check(MontroseHarbor.contains(b.hull[i],b.hull[i+1]),"dry hull corner");
            check(!MontroseHarbor.contains(b.ax,b.ay),"stern line must attach to dock");
            check(!MontroseHarbor.contains(b.bx,b.by),"bow line must attach to dock");
            check(Physics.length(b.x-MontroseHarbor.SLIP_X,b.y-MontroseHarbor.SLIP_Y)>50,"mission bay reserved");
        }
        for(boolean present:kinds)check(present,"each boat design placed");
        for(int tick=0;tick<3600;tick++)check(Math.abs(MooredBoats.bob(tick,37))<=.35001f,"tethered heave bound");
        check(VesselShoreLight.falloff(0,50)==1 && VesselShoreLight.falloff(2500,50)==0,"finite light reach");
        check(VesselShoreLight.sector(0,VesselNavigationLights.PORT,0,-1)==1,"red port receiver");
        check(VesselShoreLight.sector(0,VesselNavigationLights.PORT,0,1)==0,"red excludes starboard");
        check(VesselShoreLight.sector(0,VesselNavigationLights.STARBOARD,0,1)==1,"green starboard receiver");
        System.out.println(checks+" moored boat placement, tether and colored shore-light assertions passed; "+MooredBoats.BERTHS.length+" boats");
    }
}
