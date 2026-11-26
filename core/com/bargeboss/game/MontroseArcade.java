package com.bargeboss.game;

/** Montrose's shared arcade layer, on every job of the day:
 * - NEAR MISS: skim past a moving boat at speed without touching it for points; misses chained within a
 *   few seconds build a combo multiplier.
 * - QUICK JOB: each job has a par time; finishing under par pays seconds saved.
 * - JOB COMPLETE banner with the job's stars and score breakdown at every hand-off.
 * Fixed-step and deterministic; drawing never changes state. */
final class MontroseArcade {
    static final float SKIM=22,MIN_SPEED=55;
    static final int COMBO_WINDOW=240,BANNER=210,NEAR_POINTS=50,PAR_POINTS=15;
    private static final int TRACKED=32;
    private final Body[] near=new Body[TRACKED];private final boolean[] touched=new boolean[TRACKED];private final float[] closest=new float[TRACKED];
    int combo,comboTicks,nearMisses;
    int bannerTicks,bannerStars,bannerScore,bannerBonus;String bannerTitle="";
    int jobStart;
    private final float[] popX=new float[6],popY=new float[6];private final int[] popAge=new int[6];private final String[] popText=new String[6];private int popNext;

    /** Par seconds for each Montrose job (finishing faster pays PAR_POINTS a second). */
    static int par(int id){
        switch(id){
            case Challenges.HAULOUT:return 150;
            case Challenges.RAFT_RESCUE:return 300;
            case Challenges.MONTROSE:return 180;
            case Challenges.SWIMMER_RESCUE:return 420;
            case Challenges.WAKE_PATROL:return 120;
            case Challenges.LOOSE_BOATS:return 240;
            case Challenges.WATER_TAXI:return 120;
            case Challenges.SANDBAR:return 200;
            case Challenges.REGATTA:return 60;
            default:return 240;
        }
    }
    void step(Game g){
        if(comboTicks>0 && --comboTicks==0)combo=0;
        for(int i=0;i<popAge.length;i++)if(popAge[i]>0 && ++popAge[i]>60)popAge[i]=0;
        if(bannerTicks>0)bannerTicks--;
        if(g.mode!=Game.PLAY || g.conversation())return;
        for(HarborTraffic.Craft c:g.harborTraffic.craft)watch(g,c.body);
        if(g.job!=null)for(Body b:g.job.bodies())if(b!=null)watch(g,b);
    }
    private void watch(Game g,Body b){
        int slot=-1,free=-1;
        for(int i=0;i<TRACKED;i++){if(near[i]==b){slot=i;break;}if(near[i]==null && free<0)free=i;}
        float gap=Physics.length(b.x-g.tug.x,b.y-g.tug.y)-(b.half+b.radius+g.tug.half+g.tug.radius)*.75f;
        boolean close=b.active && b.sinkTicks<=0 && !b.awaitingPickup && gap<SKIM && Math.max(g.tug.speed(),b.speed())>MIN_SPEED;
        if(slot<0){if(!close || free<0)return;slot=free;near[slot]=b;touched[slot]=false;closest[slot]=gap;}
        if(Physics.touching(g.tug,b,1))touched[slot]=true;
        closest[slot]=Math.min(closest[slot],gap);
        if(gap<SKIM*2.2f && b.active)return;
        // Cleared the boat: a skim without contact pays out.
        if(!touched[slot] && b.active){
            combo++;comboTicks=COMBO_WINDOW;nearMisses++;
            int points=NEAR_POINTS*combo;g.score+=points;g.haptic(Game.HAPTIC_TICK);
            String label=combo>1?"NEAR MISS x"+combo+" +"+points:"NEAR MISS +"+points;
            int i=popNext++%popAge.length;popX[i]=b.x;popY[i]=b.y;popText[i]=label;popAge[i]=1;
        }
        near[slot]=null;
    }
    /** Called as a Montrose job finishes: quick-job bonus and the banner. Returns the bonus paid. */
    int complete(Game g,int id,int stars,int jobScore){
        int seconds=(g.ticks-jobStart)/60,bonus=Math.max(0,par(id)-seconds)*PAR_POINTS;
        g.score+=bonus;
        bannerTicks=BANNER;bannerStars=stars;bannerScore=jobScore;bannerBonus=bonus;bannerTitle=Challenges.title(id);
        jobStart=g.ticks;
        return bonus;
    }
    /** Near-miss pops in world space, over the vessels. */
    void overlay(Game g,Painter p){
        RiverCamera c=g.layout.camera;
        for(int i=0;i<popAge.length;i++){
            if(popAge[i]<=0)continue;
            int age=popAge[i];float u=age/60f,size=Physics.clamp(14*c.scale,14,22)*(age<6?1.4f-age*.067f:1);
            float x=c.screenX(popX[i],popY[i]),y=c.screenY(popX[i],popY[i])-(24+u*30)*c.scale,a=1-Physics.clamp((age-40)/20f,0,1);
            Lettering.center(p,popText[i],x+1.2f,y+1.2f,size,SoftCue.alpha(0xff0a1016,.8f*a));
            Lettering.center(p,popText[i],x,y,size,SoftCue.alpha(0xff6dffb0,a));
        }
    }
    void draw(Game g,Painter p){
        if(combo>1 && comboTicks>0){
            float x=g.layout.width-84,y=104,a=Physics.clamp(comboTicks/40f,0,1);
            Lettering.center(p,"COMBO x"+combo,x,y,18,SoftCue.alpha(0xffff6fd8,a));
        }
        if(bannerTicks<=0)return;
        int age=BANNER-bannerTicks;float in=Physics.clamp(age/12f,0,1),out=Physics.clamp(bannerTicks/24f,0,1),a=Math.min(in,out);
        float pop=age<12?1.3f-.3f*in:1,cx=g.layout.width*.5f,cy=g.layout.height*.36f;
        p.rect(cx-230,cy-46,460,118,SoftCue.alpha(0xd0081420,a));
        p.rect(cx-230,cy-46,460,3,SoftCue.alpha(0xffffd166,a));
        Lettering.center(p,"JOB COMPLETE!",cx,cy-30,30*pop,SoftCue.alpha(0xffffe27a,a));
        Lettering.center(p,bannerTitle,cx,cy+6,15,SoftCue.alpha(0xffcfeefa,a));
        // Stars land one after another.
        int shown=Math.min(3,Math.max(0,(age-14)/10));
        for(int i=0;i<3;i++){
            boolean lit=i<bannerStars && i<shown;float sx=cx-40+i*40,size=lit && (age-14-i*10)<8?15:11;
            ScreenView.star(p,sx,cy+30,size,SoftCue.alpha(lit?0xffffd166:0x55ffffff,a));
        }
        String line="+"+bannerScore+(bannerBonus>0?"   QUICK JOB +"+bannerBonus:"");
        Lettering.center(p,line,cx,cy+52,14,SoftCue.alpha(0xffffffff,a));
    }
}
