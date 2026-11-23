package com.bargeboss.game;
public final class MooredBoatsTest {
    static int checks;
    static void check(boolean yes,String text){checks++;if(!yes)throw new AssertionError(text);}
    public static void main(String[] args){boolean[] kinds=new boolean[6];float previous=-Float.MAX_VALUE;
        check(MooredBoats.BERTHS.length>150,"populated harbor");
        // Reference layout: Dock I fingers face southwest, away from the seawall.
        float dx=-152,dy=-94,len=Physics.length(dx,dy);dx/=len;dy/=len;
        for(int at=30;at<170;at+=30){
            float x=1718+dx*at,y=287+dy*at;
            if(at>=60)check(MontroseHarbor.contains(MontroseHarbor.mapX(x-dy*20),MontroseHarbor.mapY(y+dx*20)),"Dock I seawall side is open water");
            check(!MontroseHarbor.contains(MontroseHarbor.mapX(x+dy*20),MontroseHarbor.mapY(y-dx*20)),"Dock I harbor-side fingers remain solid");
        }
        int dockIBoats=0;for(MooredBoats.Berth b:MooredBoats.BERTHS)if(b.row==14){dockIBoats++;check(b.side==-1,"No boats or mooring lines on Dock I seawall side");}
        check(dockIBoats>0,"Dock I harbor-side slips stay populated");
        for(MooredBoats.Berth b:MooredBoats.BERTHS){kinds[b.kind]=true;check(b.y-b.x>=previous,"depth order");previous=b.y-b.x;
            check(b.frame>=0 && b.frame<SpriteAtlas.count(SpriteAtlas.MOORED_BOATS),"atlas range");
            for(int i=0;i<8;i+=2)check(MontroseHarbor.contains(b.hull[i],b.hull[i+1]),"dry hull corner");
            check(b.mooring>=0 || !MontroseHarbor.contains(b.ax,b.ay),"stern line must attach to dock");
            check(b.mooring>=0 || !MontroseHarbor.contains(b.bx,b.by),"bow line must attach to dock");
            check(Physics.length(b.x-MontroseHarbor.SLIP_X,b.y-MontroseHarbor.SLIP_Y)>50,"mission bay reserved");
        }
        for(boolean present:kinds)check(present,"each boat design placed");
        boolean[][] palettes=new boolean[6][BiminiArt.COLORS];
        for(MooredBoats.Berth b:MooredBoats.BERTHS){
            check(b.bimini>=0 && b.bimini<BiminiArt.COLORS,"valid canvas palette");palettes[b.kind][b.bimini]=true;
            for(MooredBoats.Berth next:MooredBoats.BERTHS)if(b.row>=0 && next.row==b.row && next.side==b.side && next.station==b.station+30)
                check(b.bimini!=next.bimini,"neighboring slips have different canvas");
        }
        for(boolean[] colors:palettes){int count=0;for(boolean present:colors)if(present)count++;check(count>=4,"canvas colors vary independently of hull design");}
        boolean prepared=false;for(int atlas:LevelAssets.challenge(Challenges.MONTROSE).preparationAtlases())if(atlas==SpriteAtlas.MOORED_BIMINI)prepared=true;
        check(prepared,"canvas frames prepared before scene drawing");
        Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();
        Game replay=new Game();replay.selectChallenge(Challenges.MONTROSE);replay.reset();
        float moved=0,turned=0;
        for(int tick=0;tick<600;tick++){g.visualTicks=replay.visualTicks=tick;g.mooredFleet.step(g);replay.mooredFleet.step(replay);
            for(int i=0;i<g.mooredFleet.boats.length;i++){MooredFleet.FloatBoat b=g.mooredFleet.boats[i],same=replay.mooredFleet.boats[i];
                check(Physics.length(b.x-b.berth.x,b.y-b.berth.y)<=MooredFleet.REACH+.001f,"finite mooring reach");
                check(Math.abs(b.yaw)<=MooredFleet.YAW+.001f && b.clear(),"yaw bounded and hull on water");
                check(b.x==same.x && b.y==same.y && b.yaw==same.yaw,"deterministic tether simulation");
                moved=Math.max(moved,Physics.length(b.x-b.berth.x,b.y-b.berth.y));turned=Math.max(turned,Math.abs(b.yaw));
            }
        }
        for(MooredFleet.FloatBoat b:g.mooredFleet.boats)if(SeaWaves.exposure(b.berth.x,b.berth.y)==0)
            check(Math.abs(b.heave)<.001f && Math.abs(b.yaw)<.001f,"sheltered moorings remain calm without passing wakes");
        MooredFleet.FloatBoat forced=g.mooredFleet.boats[0];forced.x+=100;forced.y+=100;forced.yaw=2;forced.constrain();
        check(forced.clear() && Physics.length(forced.x-forced.berth.x,forced.y-forced.berth.y)<=MooredFleet.REACH+.001f,"large impulse remains tethered");
        check(DockTopLight.heightFactor(3)==0 && DockTopLight.heightFactor(20)==1,"dock tops receive only lights above them");
        float[] red=new float[3];MooredBoatLight.add(red,0,0,0,0,50,1,0xffff0000,0,-1);
        check(red[0]>0 && red[1]==0 && red[2]==0,"receiver preserves red source color");
        float[] unlit=new float[3];MooredBoatLight.add(unlit,100,100,0,0,20,1,0xffffffff,0,-1);
        check(unlit[0]==0 && unlit[1]==0 && unlit[2]==0,"no light outside source reach");
        check(VesselShoreLight.falloff(0,50)==1 && VesselShoreLight.falloff(2500,50)==0,"finite light reach");
        check(VesselShoreLight.sector(0,VesselNavigationLights.PORT,0,-1)==1,"red port receiver");
        check(VesselShoreLight.sector(0,VesselNavigationLights.PORT,0,1)==0,"red excludes starboard");
        check(VesselShoreLight.sector(0,VesselNavigationLights.STARBOARD,0,1)==1,"green starboard receiver");
        System.out.println(checks+" moored boat placement, tether and colored shore-light assertions passed; "+MooredBoats.BERTHS.length+" boats");
    }
}
