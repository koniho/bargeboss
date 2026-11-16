package com.bargeboss.game;
/** Continuous geometry invariants, independent of material fallback pixels. */
public final class MontroseShoreTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static boolean joins(float[] a,float[] b){
        for(int i=0;i<a.length;i+=2)if(PolygonWater.contains(b,a[i],a[i+1]))return true;
        for(int i=0;i<b.length;i+=2)if(PolygonWater.contains(a,b[i],b[i+1]))return true;
        for(int i=0;i<a.length;i+=2)for(int j=0;j<b.length;j+=2){int ii=(i+2)%a.length,jj=(j+2)%b.length;
            float ux=a[ii]-a[i],uy=a[ii+1]-a[i+1],vx=b[jj]-b[j],vy=b[jj+1]-b[j+1],den=ux*vy-uy*vx;
            if(Math.abs(den)<.00001f)continue;float t=((b[j]-a[i])*vy-(b[j+1]-a[i+1])*vx)/den,s=((b[j]-a[i])*uy-(b[j+1]-a[i+1])*ux)/den;
            if(t>=0 && t<=1 && s>=0 && s<=1)return true;}
        return false;
    }
    public static void run(){
        float[][] docks=MontroseHarbor.DOCKS;boolean[] linked=new boolean[docks.length];
        for(int i=0;i<docks.length;i++)linked[i]=joins(docks[i],MontroseHarbor.LAND_POLYGON);
        boolean changed=true;while(changed){changed=false;for(int i=0;i<docks.length;i++)if(!linked[i])for(int j=0;j<docks.length;j++)if(linked[j] && joins(docks[i],docks[j])){linked[i]=true;changed=true;break;}}
        int[] starArms=new int[MontroseHarbor.STAR_CENTERS.length];
        for(int i=0;i<linked.length;i++){float[] line=MontroseHarbor.DOCK_LINES[i];int star=-1;for(int j=0;j<starArms.length;j++){float[] center=MontroseHarbor.STAR_CENTERS[j];if(line[0]==center[0] && line[1]==center[1])star=j;}
            if(star>=0){starArms[star]++;check(!linked[i],"Star float must have no land connection: "+star);}else check(linked[i],"Shore-connected dock segment disconnected: "+i);}
        for(int i=0;i<starArms.length;i++)check(starArms[i]==8,"Original eight arms retained for isolated star "+i);
        float[][] path=MontroseShoreArt.PROMENADE;for(int i=1;i<path.length;i++){check(path[i-1][2]==path[i][0] && path[i-1][3]==path[i][1],"Walkway outer join "+i);check(path[i-1][4]==path[i][6] && path[i-1][5]==path[i][7],"Walkway inner join "+i);}
        check(MontroseShoreArt.gateCount()==19,"15 main docks, 2 service and 2 inlet gates; no star gates");
        for(int i=0;i<MontroseShoreArt.ACCESS.length;i++){float[] a=MontroseShoreArt.ACCESS[i];check(!MontroseHarbor.openWater(a[0],a[1]),"Gate root must be on shore: "+i);boolean onPath=false;for(float[] q:path)onPath|=PolygonWater.contains(q,a[0],a[1]);check(onPath,"Gate joins promenade: "+i);}
        String names="ABCDEFGHIJKLMNO";int[] gates={6,7,8,9,10,11,12,13,14,5,4,3,2,1,0};
        boolean[] used=new boolean[19];
        for(int i=0;i<names.length();i++){char letter=names.charAt(i);int gate=MontroseShoreArt.gateForLetter(letter);check(gate==gates[i],"Authoritative dock map: "+letter);check(!used[gate],"Unique letter "+letter);used[gate]=true;check(MontroseShoreArt.gateLetter(gate)==letter,"Letter roundtrip "+letter);}
        for(int gate=15;gate<19;gate++)check(MontroseShoreArt.gateLetter(gate)==' ',"Service/inlet gate stays unlettered");
        for(int i=0;i<MontroseHarbor.STAR_LETTERS.length();i++){char letter=MontroseHarbor.STAR_LETTERS.charAt(i);float[] anchor=MontroseShoreArt.dockLabelAnchor(letter),center=MontroseHarbor.STAR_CENTERS[i];check(anchor[0]==center[0] && anchor[1]==center[1],"Floating letter stays at hub "+letter);boolean noGate=false;try{MontroseShoreArt.gateForLetter(letter);}catch(IllegalArgumentException expected){noGate=true;}check(noGate,"Floating star has no shore gate "+letter);}
        boolean skippedQ=false;try{MontroseShoreArt.gateForLetter('Q');}catch(IllegalArgumentException expected){skippedQ=true;}check(skippedQ,"Reference deliberately skips Q");
        for(float[] s:MontroseShoreArt.STEPS){float x=(s[0]+s[2]+s[4]+s[6])/4,y=(s[1]+s[3]+s[5]+s[7])/4;check(!MontroseHarbor.openWater(x,y),"Steps retain original shoreline footprint");check(s[8]>0 && s[8]<=MontroseHarbor.LAND_RISE,"Bounded step rise");}
        System.out.println(checks+" Montrose connected docks, gates, promenade and terrace checks passed");
    }
    public static void main(String[] args){run();}
}
