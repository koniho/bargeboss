package com.bargeboss.game;

/** Timed practice finale: tow ten barges across the challenge basin and tie them off on the far quay.
 * Geometry is part of the permanent training water; this class owns only the clock, the hulls and berths. */
public final class TowingChallenge {
    public static final int BARGES=10,TICKS=60*60,RESULT_DELAY=45;
    public static final int IDLE=0,INTRO=1,APPROACH=2,RUN=3,RESULT=4;
    /** Ten hulls wait along the west quay; ten berths line the east quay, one row per hull. */
    public static final float SPAWN_X=TrainingChannel.CHALLENGE_LEFT+30,BERTH_X=TrainingChannel.CHALLENGE_RIGHT-60;
    public static final float FIRST_ROW=TrainingChannel.CHALLENGE_TOP+80,ROW_SPACING=110,HALF_WIDTH=55;
    public static final float START_X=(TrainingChannel.CHALLENGE_MOUTH_LEFT+TrainingChannel.CHALLENGE_MOUTH_RIGHT)*.5f;
    public static final float START_Y=TrainingChannel.CHALLENGE_BOTTOM,ZOOM=1.4f;
    private static final String[] CLOCK=clockLabels(),COUNT=countLabels(),BEST=bestLabels();
    final Body[] barges=new Body[BARGES];
    private final int[] berth=new int[BARGES];
    public int phase=IDLE,remaining=TICKS,tied,best,attempts,resultTicks,celebrateTicks;
    private boolean newBest;
    private String resultTitle="",resultDetail="";
    private int introReply=-1;

    public static float rowY(int row){return FIRST_ROW+row*ROW_SPACING;}
    public static float bollardX(){return TrainingChannel.CHALLENGE_RIGHT+50+RiverCamera.riseX(GroundArt.LAND_RISE);}
    public static float bollardY(int row){return rowY(row)+RiverCamera.riseY(GroundArt.LAND_RISE);}

    /** True only for the ten challenge hulls; earlier lesson barges keep their own berth rules. */
    public boolean owns(Body b){return index(b)>=0;}
    int index(Body b){if(b==null)return -1;for(int i=0;i<BARGES;i++)if(barges[i]==b)return i;return -1;}
    public boolean active(){return phase!=IDLE;}
    public boolean running(){return phase==RUN;}
    /** Assigned berth, otherwise the nearest berth no tied hull has claimed. */
    public int berthFor(Body b){
        int i=index(b);if(i<0)return -1;
        if(berth[i]>=0)return berth[i];
        int best=-1;float distance=Float.MAX_VALUE;
        for(int row=0;row<BARGES;row++){
            if(claimed(row))continue;
            float d=Math.abs(b.y-rowY(row));
            if(d<distance){distance=d;best=row;}
        }
        return best<0?0:best;
    }
    boolean claimed(int row){for(int i=0;i<BARGES;i++)if(berth[i]==row)return true;return false;}
    public float dockX(Body b){return BERTH_X;}
    public float dockY(Body b){return rowY(berthFor(b));}
    public float bollardX(Body b){return bollardX();}
    public float bollardY(Body b){return bollardY(berthFor(b));}
    /** A tied neighbor occupying this berth blocks a second catch. */
    public Body blocker(Game g,Body incoming){
        int row=berthFor(incoming);
        for(int i=0;i<BARGES;i++)if(barges[i]!=null && barges[i]!=incoming && berth[i]==row)return barges[i];
        return null;
    }
    public boolean tieable(Body b){return phase==RUN && owns(b);}

    /** Explicit setup or retry: fresh hulls at the west quay and the tug at the basin mouth. */
    void setup(Game g,boolean retry){
        spawn(g);
        g.tug=new Body(0,-1,START_X,START_Y+110,-(float)Math.PI/2);
        phase=retry?APPROACH:INTRO;introReply=-1;
        if(!retry)intro(g);
    }
    /** Natural progression keeps the tug, water and camera; only the waiting hulls are added. */
    void begin(Game g){
        g.tow.release(g);spawn(g);phase=INTRO;introReply=-1;intro(g);
    }
    private void intro(Game g){
        g.dialogue.show(g,"INSTRUCTOR / GREEN TUG","ONE LAST JOB: THE BIG BASIN.",
                "TEN BARGES, SIXTY SECONDS. TIE EACH ONE ON THE FAR QUAY.",
                "START THE CLOCK.","TRAINS COUNT TOO?");
    }
    private void spawn(Game g){
        for(int i=0;i<BARGES;i++){
            Body old=barges[i];
            if(old!=null){g.tow.detachBody(g,old);for(int slot=0;slot<g.cargo.length;slot++)if(g.cargo[slot]==old)g.cargo[slot]=null;}
            barges[i]=null;berth[i]=-1;
        }
        int slot=0;
        for(int i=0;i<BARGES;i++){
            while(slot<g.cargo.length && g.cargo[slot]!=null && g.cargo[slot].active)slot++;
            if(slot>=g.cargo.length)throw new IllegalStateException("No cargo slot for challenge barge");
            Body b=new Body(g.nextId++,2,SPAWN_X,rowY(i),(i%2==0?1:-1)*(float)Math.PI/2);
            b.awaitingPickup=true;b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;
            g.cargo[slot]=b;barges[i]=b;
        }
        remaining=TICKS;tied=0;resultTicks=celebrateTicks=0;newBest=false;
        best=g.settings.towingChallengeBest();
    }
    /** Returns true once the player chooses to finish training from the result. */
    boolean step(Game g){
        if(celebrateTicks>0)celebrateTicks--;
        if(phase==INTRO){
            if(!g.dialogue.active){phase=APPROACH;return false;}
            if(g.dialogue.result()>=0 && ++resultTicks>=24){
                introReply=g.dialogue.result();g.dialogue.close(g);resultTicks=0;phase=APPROACH;g.radio.transmit(RadioAudio.TEACHER);
            }
            return false;
        }
        if(phase==APPROACH){
            if(g.tug.y<START_Y && g.tug.x>TrainingChannel.CHALLENGE_LEFT && g.tug.x<TrainingChannel.CHALLENGE_RIGHT){
                phase=RUN;remaining=TICKS;attempts++;g.haptic(Game.HAPTIC_SUCCESS);g.radio.transmit(RadioAudio.TEACHER);
                g.tell("GO! TIE OFF ON THE FAR QUAY");
            }
            return false;
        }
        if(phase==RUN){
            int count=0;
            for(int i=0;i<BARGES;i++){
                Body b=barges[i];if(b==null || !b.lineAttached)continue;
                if(berth[i]<0)berth[i]=berthFor(b);
                count++;
            }
            if(count>tied){celebrateTicks=60;g.sounds.complete();g.haptic(Game.HAPTIC_SUCCESS);}
            tied=count;
            if(remaining>0)remaining--;
            if(remaining==0 || tied==BARGES)finish(g);
            return false;
        }
        if(phase==RESULT){
            if(!g.dialogue.active){
                if(++resultTicks<RESULT_DELAY)return false;
                resultTicks=0;
                g.dialogue.show(g,"INSTRUCTOR / GREEN TUG",resultTitle,resultDetail,"FINISH TRAINING.","TRY AGAIN.");
                return false;
            }
            if(g.dialogue.result()<0)return false;
            if(++resultTicks<24)return false;
            int choice=g.dialogue.result();g.dialogue.close(g);resultTicks=0;g.radio.transmit(RadioAudio.TEACHER);
            if(choice==1){retry(g);return false;}
            return true;
        }
        return false;
    }
    private void finish(Game g){
        phase=RESULT;resultTicks=0;g.cancelInputs();
        newBest=tied>best;
        if(newBest){best=tied;g.settings.recordTowingChallenge(tied);}
        resultTitle=tied==BARGES?"ALL TEN TIED! WITH TIME TO SPARE.":"TIME! "+tied+(tied==1?" BARGE":" BARGES")+" TIED.";
        resultDetail=(newBest?"NEW BEST: ":"BEST SO FAR: ")+best+" / "+BARGES+". "+(tied>=7?"THAT IS HARBOR PACE.":tied>=4?"TRY A TRAIN OF TWO OR THREE.":"TOW SEVERAL AT ONCE TO GO FASTER.");
        g.sounds.complete();g.haptic(Game.HAPTIC_SUCCESS);
    }
    void retry(Game g){
        g.cancelInputs();g.tow=new Towline();g.ropePhase=g.ropeTicks=0;g.ropeBody=null;g.ropeTowTarget=g.towAimBody=null;g.ropeToTug=false;
        setup(g,true);g.fitTutorialCamera();
    }
    void reset(){
        for(int i=0;i<BARGES;i++){barges[i]=null;berth[i]=-1;}
        phase=IDLE;remaining=TICKS;tied=resultTicks=celebrateTicks=0;newBest=false;introReply=-1;
    }
    /** Winch alongside the east quay, matching the shared tutorial berth alignment. */
    void moor(Game g,Body b){
        int i=index(b);if(berth[i]<0)berth[i]=berthFor(b);
        if(b.mooringTicks<Mooring.ALIGN_TICKS)b.mooringTicks++;
        b.dockTime=1.2f;
        float t=b.mooringTicks/(float)Mooring.ALIGN_TICKS,eased=t*t*(3-2*t);
        float heading=(float)(StrictMath.sin(b.mooringAngle)>=0?Math.PI/2:-Math.PI/2);
        b.angle=Physics.angle(b.mooringAngle+Physics.angle(heading-b.mooringAngle)*eased);
        b.x=b.mooringX+(TrainingChannel.CHALLENGE_RIGHT-b.radius-10-b.mooringX)*eased;
        b.y=b.mooringY+(rowY(berth[i])-b.mooringY)*eased;
        b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;b.vx=b.vy=b.spin=0;
    }
    /** Nearest waiting hull (or free berth while towing) for the shared offscreen objective arrow. */
    Body nearestWaiting(Game g){
        Body result=null;float distance=Float.MAX_VALUE;
        for(Body b:barges)if(b!=null && !b.lineAttached && g.tow.incoming(b)==null){
            float d=Physics.length(b.x-g.tug.x,b.y-g.tug.y);if(d<distance){distance=d;result=b;}
        }
        return result;
    }
    int nearestFreeRow(float y){
        int best=-1;float distance=Float.MAX_VALUE;
        for(int row=0;row<BARGES;row++){if(claimed(row))continue;float d=Math.abs(y-rowY(row));if(d<distance){distance=d;best=row;}}
        return best;
    }
    String clockLabel(){return CLOCK[(remaining+59)/60];}
    String countLabel(){return COUNT[tied];}
    String bestLabel(){return BEST[Math.max(0,Math.min(BARGES,best))];}
    boolean newBest(){return newBest;}
    int introReply(){return introReply;}
    private InstructionPanel.Content approach;
    private int approachKey=-1;
    InstructionPanel.Content content(Game g){
        if(phase!=APPROACH)return null; // The scoreboard replaces the field guide while the clock runs.
        int key=attempts*2+(introReply==1?1:0);
        if(approach==null || approachKey!=key){
            approachKey=key;
            approach=new InstructionPanel.Content("challenge-approach-"+key,"TOWING CHALLENGE",
                    introReply==1?"YES: EVERY BARGE IN A TRAIN COUNTS.":"DRIVE NORTH INTO THE BIG BASIN.",
                    "THE CLOCK STARTS AT THE BASIN MOUTH.",Tutorial.CHALLENGE,0);
        }
        return approach;
    }
    private static String[] clockLabels(){
        String[] labels=new String[TICKS/60+1];
        for(int s=0;s<labels.length;s++)labels[s]=(s/60)+":"+(s%60<10?"0":"")+(s%60);
        return labels;
    }
    private static String[] bestLabels(){
        String[] labels=new String[BARGES+1];
        for(int n=0;n<=BARGES;n++)labels[n]="BEST "+n;
        return labels;
    }
    private static String[] countLabels(){
        String[] labels=new String[BARGES+1];
        for(int n=0;n<=BARGES;n++)labels[n]=n+" / "+BARGES;
        return labels;
    }
}
