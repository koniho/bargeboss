package com.bargeboss.game;
/** Revetment geometry stays within existing opaque receivers and navigation solids. */
public final class MontroseEntranceShoreTest {
    private static int checks;private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void run(){check(MontroseEntranceShore.rockCount()>20 && MontroseEntranceShore.rockCount()<180,"Bounded reference-scale rock field");check(MontroseEntranceShore.geometryWithinShore(),"Rocks stay inside original dry shore and receiver height: no hull leaks");
        float[][] steps=MontroseEntranceShore.stepGeometry();float[] dock=MontroseHarbor.DOCKS[MontroseHarbor.DOCKS.length-2];
        for(float[] s:steps)for(int i=0;i<8;i+=2){boolean dry=!MontroseHarbor.openWater(s[i],s[i+1]);if(!dry){float x=(s[i]+s[(i+2)%8])*.5f,y=(s[i+1]+s[(i+3)%8])*.5f;check(PolygonWater.contains(dock,x,y) || !MontroseHarbor.openWater(x,y),"Step footprint cannot extend into navigable inlet: "+x+","+y);}check(s[8]>=MontroseHarbor.DOCK_RISE && s[8]<=MontroseHarbor.LAND_RISE,"Steps stay under conservative shore/gangway receivers");}
        check(MontroseEntranceShore.walkwayInset(235,1247)==24,"Rock band has room beside shifted walkway");check(MontroseEntranceShore.walkwayInset(823,225)==0,"Northern docks unaffected");
        System.out.println(checks+" entrance revetment footprint and height checks passed ("+MontroseEntranceShore.rockCount()+" precomputed blocks)");
    }
    public static void main(String[] args){run();}
}
