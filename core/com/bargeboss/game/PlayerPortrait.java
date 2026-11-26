package com.bargeboss.game;

/**
 * The player's captain: chosen character, short radio acknowledgements and the always-visible
 * top-left HUD portrait with talking, mad, wincing and idle (blink/breathing) faces.
 *
 * <p>Simulation-side state only advances in fixed {@link Game#step()} ticks and never feeds back
 * into gameplay; drawing reads it without mutating anything and allocates nothing per frame.
 */
public final class PlayerPortrait {
    public static final int WHITNEY=0,WESLEY=1,DOT=2,COUNT=3;
    /** Face states in priority order: an impact reaction always shows over speech. */
    public static final int IDLE=0,TALK=1,WINCE=2,MAD=3;
    /** Atlas frames: neutral, small speech, open speech, blink, mad, wince. */
    static final int NEUTRAL=0,SMALL=1,OPEN=2,BLINK=3,ANGRY=4,FLINCH=5;
    static final int MAD_TICKS=54,WINCE_TICKS=40,ACK_TICKS=78,ACK_GAP=7*60,BLINK_PERIOD=233,BLINK_TICKS=7;
    static final String[] NAMES={"WHITNEY","WESLEY","DOT"};
    static final String[] TAGLINES={"THE WAVER","THE CLOSER","THE LUNCH LADY"};
    static final String[][] STORIES={
        {"Whitney spent ten years crossing Chicago's bridges, waving at every tug below. Not one ever waved back.",
         "So she is getting her own tug and will wave at everyone. Sunglasses stay on, day or night: she says the river looks cooler that way."},
        {"Wesley sold yachts for twenty years and could talk anyone into teak decking. Secretly, he always wanted the scrappy little tug that pushed them around.",
         "He has a grin for every situation, a life vest for any weather, and believes every barge is a friend he has not towed yet."},
        {"Dot ran the busiest school cafeteria in Calumet for forty years, then retired and found the quiet unbearable.",
         "Barges, she says, are easier to herd than third graders, and none of them complain about the meatloaf. She knits on her coffee breaks."}};
    static final String[][] ACKS={
        {"COPY THAT!","ON IT, BOSS.","EASY PEASY.","WAVING AS I GO!"},
        {"ROGER, ROGER.","CONSIDER IT DONE.","SMOOTH AS SILK.","YOU GOT IT, CHIEF."},
        {"YES, DEAR.","RIGHT AWAY, HONEY.","WELL, ALRIGHT!","NO DAWDLING."}};
    // HUD geometry, logical canvas units. The plate sits in the top-left corner, clear of the
    // centered objective ribbon at the 960 minimum width, the joystick and the chart.
    static final float X=10,Y=8,W=104,H=112,NAME_H=15,SPRITE=114;
    /** Right and bottom edges of everything the portrait draws (excluding its speech bubble). */
    public static final float RIGHT=X+W,BOTTOM=Y+H;
    static final float BUBBLE_X=RIGHT+10,BUBBLE_Y=64,BUBBLE_H=26;

    int madTicks,winceTicks,ackTicks,ackLine=-1,ackCount,sinceAck=ACK_GAP;
    private int seenSequence;
    private boolean ackPending;

    static int atlas(int captain){return SpriteAtlas.WHITNEY_PORTRAIT+valid(captain);}
    static int valid(int captain){return captain<0 || captain>=COUNT?WHITNEY:captain;}
    public static String name(int captain){return NAMES[valid(captain)];}

    void reset(){madTicks=winceTicks=ackTicks=0;ackLine=-1;ackPending=false;sinceAck=ACK_GAP;}
    /** Own tug took hull damage. */
    void mad(){madTicks=MAD_TICKS;}
    /** The player damaged someone else's vessel (called from HullDamage). */
    public static void wince(Game g){g.portrait.winceTicks=WINCE_TICKS;}

    /** Fixed-step update: decay reactions and answer finished incoming radio calls. */
    void step(Game g){
        if(madTicks>0)madTicks--;
        if(winceTicks>0)winceTicks--;
        if(ackTicks>0)ackTicks--;
        if(sinceAck<ACK_GAP)sinceAck++;
        RadioAudio radio=g.radio;
        if(radio.transmission()!=seenSequence){
            seenSequence=radio.transmission();
            ackPending=radio.transmitter()!=RadioAudio.REPLY;
            if(!ackPending)ackTicks=0;
        }
        if(ackPending && radio.progress()>=1 && !g.dialogue.active && ackTicks==0){
            ackPending=false;
            if(sinceAck>=ACK_GAP){ackLine=ackCount++%ACKS[0].length;ackTicks=ACK_TICKS;sinceAck=0;}
        }
    }
    /** The player is speaking: a radio reply in flight or a short acknowledgement. */
    boolean talking(Game g){
        return ackTicks>0 || g.radio.transmitter()==RadioAudio.REPLY && g.radio.progress()<1;
    }
    int state(Game g){
        if(madTicks>0)return MAD;
        if(winceTicks>0)return WINCE;
        return talking(g)?TALK:IDLE;
    }
    private static final int[] MOUTH={SMALL,OPEN,SMALL,NEUTRAL,OPEN,SMALL,OPEN,NEUTRAL};
    int frame(Game g){
        switch(state(g)){
            case MAD:return ANGRY;
            case WINCE:return FLINCH;
            case TALK:return MOUTH[(g.visualTicks/5)%MOUTH.length];
            default:{
                int t=g.visualTicks%BLINK_PERIOD;
                boolean doubleBlink=(g.visualTicks/BLINK_PERIOD)%3==1 && t>=12 && t<12+BLINK_TICKS-2;
                return t<BLINK_TICKS || doubleBlink?BLINK:NEUTRAL;
            }
        }
    }
    String line(Game g){return ackTicks>0 && ackLine>=0?ACKS[valid(g.settings.captain())][ackLine]:null;}

    /** HUD draw: one plate, one atlas sprite, a cached name label and an optional bubble. */
    static void draw(Game g,Painter p){
        PlayerPortrait self=g.portrait;int captain=valid(g.settings.captain());
        int state=self.state(g);
        float shake=state==MAD?(float)StrictMath.sin(self.madTicks*1.9)*Math.min(1,self.madTicks/18f)*2.2f
                :state==WINCE?(float)StrictMath.sin(self.winceTicks*2.6)*Math.min(1,self.winceTicks/14f)*1.4f:0;
        PilotGlass.panel(p,X,Y,W,H,0xe8101c30);
        if(state==MAD)p.rect(X,Y,W,H,(Math.round(110*Math.min(1,self.madTicks/(float)MAD_TICKS))<<24)|0x9a1f22);
        // Breathing: a sub-pixel rise of the bust, never a resample of the face. It runs through
        // every state so a swap from idle to talk/mad/wince never drops the bust by up to .7 px.
        float breathe=(float)StrictMath.sin(g.visualTicks*.045)*.7f;
        // Registered shoulder base (row 182 of the 192 px cell) rests on the plate's bottom edge.
        float cx=X+W*.5f+shake,cy=Y+H-SPRITE*(182.5f/192-.5f)+breathe;
        p.sprite(atlas(captain),self.frame(g),cx,cy,SPRITE,SPRITE,0,1);
        p.rect(X,Y+H-NAME_H,W,NAME_H,0xd8101c30);
        p.rect(X,Y+H-NAME_H,3,NAME_H,state==MAD?0xffd8463c:PilotGlass.AMBER);
        Lettering.center(p,NAMES[captain],X+W*.5f+1,Y+H-NAME_H+3,9,PilotGlass.TEXT);
        String line=self.line(g);
        if(line!=null)bubble(p,line);
    }
    // Reused tail geometry; the bubble is drawn at constant colors so its text layer stays cached.
    private static final float[] TAIL={BUBBLE_X-9,BUBBLE_Y+BUBBLE_H*.5f,BUBBLE_X+1,BUBBLE_Y+6,BUBBLE_X+1,BUBBLE_Y+BUBBLE_H-6};
    private static void bubble(Painter p,String line){
        float w=Lettering.width(line,11)+24;
        p.poly(TAIL,0xf0f3ead2);
        p.rect(BUBBLE_X,BUBBLE_Y,w,BUBBLE_H,0xf0f3ead2);
        p.rect(BUBBLE_X,BUBBLE_Y+BUBBLE_H-2,w,2,0xffd9a441);
        Lettering.draw(p,line,BUBBLE_X+12,BUBBLE_Y+7,11,VisualStyle.NAVY);
    }
    PlayerPortrait(){}
}
