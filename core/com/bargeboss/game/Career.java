package com.bargeboss.game;

/** Bounded, versioned progress data. Persistence transport belongs to the host. */
public final class Career {
    private static final int MAX_SCORE=999999999;
    private static final int[] RANK_STARS={0,1,3,6,9};
    private static final String[] RANK_TITLES={"DECK DUCK","LINE TAMER","WAKE WRANGLER","HARBOR HERO","CAPTAIN QUACK"};
    private final int[] stars=new int[Challenges.COUNT],scores=new int[Challenges.COUNT];
    private int selected,version;
    public int selectedChallenge() { return selected; }
    public int version() { return version; }
    public int bestStars(int id) { return Challenges.valid(id)?stars[id]:0; }
    public int bestScore(int id) { return Challenges.valid(id)?scores[id]:0; }
    /** Geographic cards summarize their stable per-job reward slots. */
    public int locationRating(int id){int rating=3;for(int job:Challenges.locationJobs(id))rating=Math.min(rating,bestStars(job));return rating;}
    public int locationScore(int id){int total=0;for(int job:Challenges.locationJobs(id))total+=bestScore(job);return total;}
    public int locationStars(int id){int total=0;for(int job:Challenges.locationJobs(id))total+=bestStars(job);return total;}
    public boolean unlocked(int id) {
        if(!Challenges.valid(id)) return false;
        if(Challenges.standalone(id))return true;
        for(int i=0;i<id;i++) if(!Challenges.standalone(i) && stars[i]==0) return false;
        return true;
    }
    public int totalStars() { int total=0; for(int value:stars) total+=value; return total; }
    public int rank() {
        int total=totalStars(),rank=0;
        for(int i=1;i<RANK_STARS.length;i++) if(total>=RANK_STARS[i]) rank=i;
        return rank;
    }
    public int stripes() { return rank(); }
    public String rankTitle() { return RANK_TITLES[rank()]; }
    public boolean select(int id) {
        if(!unlocked(id)) return false;
        if(selected!=id) { selected=id; version++; }
        return true;
    }
    /** Called once by Game when a level goal is completed. */
    boolean record(int id,int earnedStars,int score) {
        if(!unlocked(id) || earnedStars<1 || earnedStars>3) return false;
        int boundedScore=Math.max(0,Math.min(MAX_SCORE,score));
        int bestStars=Math.max(stars[id],earnedStars),bestScore=Math.max(scores[id],boundedScore);
        if(bestStars==stars[id] && bestScore==scores[id]) return false;
        stars[id]=bestStars; scores[id]=bestScore; version++; return true;
    }
    public String save() {
        StringBuilder value=new StringBuilder("BB1|").append(selected);
        for(int i=0;i<Challenges.COUNT;i++) value.append('|').append(stars[i]).append(',').append(scores[i]);
        return value.toString();
    }
    /** Invalid or unsupported input is ignored atomically; no partial unlocks survive corruption. */
    public boolean restore(String value) {
        if(value==null || value.length()>32+Challenges.COUNT*24) return false;
        String[] parts=value.split("\\|",-1);
        // Historical BB1 saves contain three levels. Appending catalog entries adds
        // empty slots without discarding earned stripes or reinterpreting stable IDs.
        int stored=parts.length-2;
        if(stored<3 || stored>Challenges.COUNT || !parts[0].equals("BB1")) return false;
        int nextSelected=number(parts[1],stored-1);
        if(nextSelected<0) return false;
        int[] nextStars=new int[Challenges.COUNT],nextScores=new int[Challenges.COUNT];
        boolean predecessorComplete=true;
        for(int i=0;i<stored;i++) {
            String[] pair=parts[i+2].split(",",-1);
            if(pair.length!=2) return false;
            nextStars[i]=number(pair[0],3); nextScores[i]=number(pair[1],MAX_SCORE);
            if(nextStars[i]<0 || nextScores[i]<0 || (!Challenges.standalone(i) && !predecessorComplete && nextStars[i]>0)
                    || (nextStars[i]==0 && nextScores[i]!=0)) return false;
            if(!Challenges.standalone(i))predecessorComplete=nextStars[i]>0;
        }
        for(int i=0;!Challenges.standalone(nextSelected) && i<nextSelected;i++) if(!Challenges.standalone(i) && nextStars[i]==0) return false;
        for(int i=0;i<Challenges.COUNT;i++) { stars[i]=nextStars[i]; scores[i]=nextScores[i]; }
        selected=nextSelected; version=0; return true;
    }
    private static int number(String value,int max) {
        if(value.length()==0 || value.length()>9) return -1;
        int result=0;
        for(int i=0;i<value.length();i++) {
            char digit=value.charAt(i);
            if(digit<'0' || digit>'9') return -1;
            result=result*10+digit-'0'; if(result>max) return -1;
        }
        return result;
    }
}
