package com.bargeboss.game;

/** Location grouping must not reinterpret the append-only BB1 career slots. */
public final class LocationCatalogTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){run();}
    public static void run(){
        Career legacy=new Career();check(legacy.restore("BB1|2|3,1700|2,2000|1,900"),"three-job legacy save restores");
        check(legacy.bestStars(0)==3 && legacy.bestStars(1)==2 && legacy.bestStars(2)==1 && legacy.bestStars(3)==0,"old stars retain exact stable job identities");
        check(Challenges.stageEntry(legacy.selectedChallenge())==0,"old transit selection enters Calumet location");
        check(legacy.locationStars(0)==6 && legacy.locationScore(0)==4600 && legacy.locationRating(0)==0,"partial location reports earned job stars without claiming location completion");
        legacy.record(3,2,1100);check(legacy.locationRating(0)==1 && legacy.locationScore(0)==5700,"location rating uses its weakest completed job");
        Career restored=new Career();check(restored.restore(legacy.save()) && restored.save().equals(legacy.save()),"grouped presentation keeps BB1 roundtrip exact");
        int[] seen=new int[Challenges.COUNT];
        for(int location=0;location<Challenges.LOCATION_COUNT;location++){
            int id=Challenges.menuId(location);check(Challenges.valid(id),"location entry exists");
            for(int job:Challenges.locationJobs(id)){seen[job]++;check(Challenges.stageEntry(job)==id,"every job maps to one geographic entry");}
        }
        for(int count:seen)check(count==1,"all legacy rewards appear in exactly one location");
        Career marina=new Career();marina.record(4,3,1200);marina.record(6,2,2400);marina.record(7,3,2000);
        check(marina.locationStars(4)==8 && marina.locationRating(6)==0 && marina.locationScore(7)==5600,"Legacy Montrose rewards survive with the new haulout still unearned");
        marina.record(Challenges.HAULOUT,3,1200);check(marina.locationRating(4)==2 && marina.locationStars(4)==11,"Haulout adds its own reward slot");
        check(Challenges.nextStage(3)==4 && Challenges.nextStage(7)==5 && Challenges.nextStage(5)==Challenges.CHICAGO && Challenges.nextStage(Challenges.CHICAGO)==-1,"next-location flow does not expose a standalone job");
        System.out.println(checks+" location grouping and legacy career compatibility assertions passed");
    }
}
