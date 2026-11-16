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
        check(moved>.4f && turned>.015f,"boats drift and turn with water");
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
        Game wakes=new Game();wakes.selectChallenge(Challenges.MONTROSE);wakes.reset();
        for(int t=0;t<400;t++){wakes.tug.vx=35;wakes.tug.vy=4;wakes.visualTicks=t;wakes.water.step(wakes);}
        float[] optimized=new float[2],reference=new float[2];
        for(int ix=-15;ix<=15;ix++)for(int iy=-15;iy<=15;iy++){float x=wakes.tug.x+ix*17,y=wakes.tug.y+iy*17;
            wakes.water.buoyForce(wakes,x,y,optimized);referenceForce(wakes,x,y,reference);
            check(Float.floatToIntBits(optimized[0])==Float.floatToIntBits(reference[0]) && Float.floatToIntBits(optimized[1])==Float.floatToIntBits(reference[1]),"wake force rejection preserves exact response");
        }
        System.out.println(checks+" moored boat placement, tether and colored shore-light assertions passed; "+MooredBoats.BERTHS.length+" boats");
    }
    static void referenceForce(Game g,float wx,float wy,float[] out){WaterSurface w=g.water;
        out[0]=.65f*(float)StrictMath.sin(g.visualTicks*.048+wx*.017+wy*.009);out[1]=.5f*(float)StrictMath.sin(g.visualTicks*.031-wx*.013+wy*.021);
        for(int i=0;i<WaterSurface.CAPACITY;i++)if(w.age[i]<WaterSurface.LIFE){float dx=wx-w.x[i],dy=wy-w.y[i],distance=Physics.length(dx,dy),offset=distance-(9+w.age[i]*.55f);
            if(Math.abs(offset)>=26 || distance<.001f)continue;
            float angle=Physics.angle((float)StrictMath.atan2(dy,dx)-w.heading[i]-(float)StrictMath.PI);if(Math.abs(angle)>(float)StrictMath.PI*.4f)continue;
            boolean clear=true;int samples=Math.max(1,(int)(distance/8));for(int k=0;k<=samples;k++)if(!WaterEffectsPainter.contains(g,w.x[i]+dx*k/samples,w.y[i]+dy*k/samples)){clear=false;break;}if(!clear)continue;
            float envelope=1-Math.abs(offset)/26,push=22*w.strength[i]*(1-w.age[i]/(float)WaterSurface.LIFE)*envelope*(float)StrictMath.sin(offset*StrictMath.PI/26);out[0]+=push*dx/distance;out[1]+=push*dy/distance;
        }out[0]=Physics.clamp(out[0],-35,35);out[1]=Physics.clamp(out[1],-35,35);
    }
}
