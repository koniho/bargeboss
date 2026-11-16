package com.bargeboss.game;

/** Mooring contact must resolve against marina shores, never the narrow river corridor. */
public final class MontroseBuoyContactTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Game marina(){Game g=new Game();check(g.selectChallenge(Challenges.MONTROSE),"Montrose selectable");g.reset();for(Body b:g.cargo)if(b!=null)b.active=false;return g;}
    private static boolean clearHull(Body b){
        for(int disc=0;disc<3;disc++)for(int sample=0;sample<16;sample++){
            double angle=sample*StrictMath.PI/8;
            if(!MontroseHarbor.contains(b.discX(disc)+(b.radius+.1f)*(float)StrictMath.cos(angle),b.discY(disc)+(b.radius+.1f)*(float)StrictMath.sin(angle)))return false;
        }
        return true;
    }
    public static void run(){
        Game inventory=marina();int approaches=0,outsideRiver=0,fullSteps=0;
        for(int mark=2;mark<NavigationLights.count(inventory);mark++){
            int clearApproaches=0;
            for(int heading=0;heading<16;heading++){
                Game g=marina();Buoys.Buoy buoy=g.buoys.get(g,mark);check(buoy!=null,"mooring ball has a physical body");
                Body tug=g.tug;tug.angle=heading*(float)StrictMath.PI/8;
                float c=(float)StrictMath.cos(tug.angle),s=(float)StrictMath.sin(tug.angle);
                float distance=tug.half+tug.radius+buoy.radius-1;
                tug.x=buoy.x-c*distance;tug.y=buoy.y-s*distance;tug.vx=c*30;tug.vy=s*30;tug.spin=0;
                if(!clearHull(tug))continue;
                approaches++;clearApproaches++;if(tug.x<Physics.LEFT || tug.x>Physics.RIGHT)outsideRiver++;
                float x=tug.x,y=tug.y;int tick=g.ticks;
                Buoys.contact(g,buoy,tug,false);
                check(Physics.length(tug.x-x,tug.y-y)<3,"a one-unit overlap causes only local contact correction, never a river-bank teleport");
                check(Float.isFinite(tug.x+tug.y+tug.vx+tug.vy+tug.spin),"buoy impulse remains finite");
                check(MontroseHarbor.contains(tug.x,tug.y),"contact retains the hull in Montrose water");
                check(Physics.length(buoy.x-buoy.anchorX,buoy.y-buoy.anchorY)<=buoy.reach+.001f,"contact preserves the mooring tether");
                check(g.ticks==tick,"contact solver does not advance simulation");
                x=tug.x;y=tug.y;g.buoys.step(g);fullSteps++;
                check(Physics.length(tug.x-x,tug.y-y)<3,"full buoy update uses the same local marina collision route");
            }
            check(clearApproaches>0,"every actual marina mooring is exercised from open water");
        }
        check(approaches>=100 && fullSteps==approaches,"test covers many actual buoy contacts through both entry points");
        check(outsideRiver>=50,"regression includes hulls outside legacy river X limits");
        System.out.println(checks+" Montrose buoy contact locality and shore-routing assertions passed across "+approaches+" approaches");
    }
    public static void main(String[] args){run();}
}
