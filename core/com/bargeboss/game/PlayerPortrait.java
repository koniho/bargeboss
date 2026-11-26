package com.bargeboss.game;

/**
 * The player's captain: chosen character, short radio acknowledgements and the player's own
 * transmission panel. It mirrors the incoming-radio panel (same height on screen, portrait on the
 * left instead of the right) and is only open while the captain is transmitting or has something to
 * say: a radio reply, an acknowledgement, or a reaction line when the tug is hit (mad) or hits
 * someone else (wince). Faces: talking, mad, wincing and idle (blink/breathing).
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
    // Panel geometry, logical canvas units: the incoming-radio panel's top and height (InstructionPanel
    // draws at y=68, 124 tall), mirrored to the left edge. At the 960 minimum width it stays clear of
    // the right-hand radio panel, the centered objective ribbon above, and the joystick below.
    static final float X=18,Y=68,W=372,H=124,SPRITE=108,SPLIT=112;
    /** Right and bottom edges of the open panel. */
    public static final float RIGHT=X+W,BOTTOM=Y+H;
    static final int OPEN_TICKS=10,REACT_TICKS=96,REPLY_HOLD=150;
    static final String[] MAD_LINES={"ARGH! MY PAINT!","OOF! THAT'LL LEAVE A MARK","WHO PUT THAT THERE?!","EASY, EASY!"};
    static final String[] WINCE_LINES={"OOPS, SORRY!","MY BAD!","THAT WASN'T ME...","WHOOPS! INSURED, RIGHT?"};
    int openTicks,reactTicks,reactCount,replyTicks;String reaction,reply;

    int madTicks,winceTicks,ackTicks,ackLine=-1,ackCount,sinceAck=ACK_GAP;
    private int seenSequence;
    private boolean ackPending;

    static int atlas(int captain){return SpriteAtlas.WHITNEY_PORTRAIT+valid(captain);}
    static int valid(int captain){return captain<0 || captain>=COUNT?WHITNEY:captain;}
    public static String name(int captain){return NAMES[valid(captain)];}

    void reset(){madTicks=winceTicks=ackTicks=openTicks=reactTicks=0;ackLine=-1;ackPending=false;sinceAck=ACK_GAP;reaction=reply=null;}
    /** Own tug took hull damage. */
    void mad(){madTicks=MAD_TICKS;reaction=MAD_LINES[reactCount++%MAD_LINES.length];reactTicks=REACT_TICKS;}
    /** The player damaged someone else's vessel (called from HullDamage). */
    public static void wince(Game g){PlayerPortrait self=g.portrait;self.winceTicks=WINCE_TICKS;
        if(self.reactTicks<REACT_TICKS/2){self.reaction=WINCE_LINES[self.reactCount++%WINCE_LINES.length];self.reactTicks=REACT_TICKS;}}

    /** Fixed-step update: decay reactions and answer finished incoming radio calls. */
    void step(Game g){
        if(madTicks>0)madTicks--;
        if(winceTicks>0)winceTicks--;
        if(reactTicks>0)reactTicks--;
        if(replyTicks>0)replyTicks--;
        // The panel opens while there is something to say, and folds away when there is not.
        boolean show=g.mode==Game.PLAY && (talking(g) || reactTicks>0 || replyTicks>0);
        openTicks=Math.max(0,Math.min(OPEN_TICKS,openTicks+(show?1:-1)));
        if(ackTicks>0)ackTicks--;
        if(sinceAck<ACK_GAP)sinceAck++;
        RadioAudio radio=g.radio;
        if(radio.transmission()!=seenSequence){
            seenSequence=radio.transmission();
            ackPending=radio.transmitter()!=RadioAudio.REPLY;
            if(radio.transmitter()==RadioAudio.REPLY){reply=g.dialogue.chosen();if(reply!=null)replyTicks=REPLY_HOLD;}
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
    /** What the panel says: the radio reply in flight, an acknowledgement, or a reaction line. */
    String text(Game g){
        if(replyTicks>0 && reply!=null)return reply;
        String ack=line(g);if(ack!=null)return ack;
        return reactTicks>0?reaction:null;
    }
    float open(){float t=openTicks/(float)OPEN_TICKS;return t*t*(3-2*t);}

    /** HUD draw: the mirrored transmission panel, hidden when the captain has nothing to say. */
    static void draw(Game g,Painter p){
        PlayerPortrait self=g.portrait;float amount=self.open();
        if(amount<=0)return;
        int captain=valid(g.settings.captain()),state=self.state(g);
        float shake=state==MAD?(float)StrictMath.sin(self.madTicks*1.9)*Math.min(1,self.madTicks/18f)*2.2f
                :state==WINCE?(float)StrictMath.sin(self.winceTicks*2.6)*Math.min(1,self.winceTicks/14f)*1.4f:0;
        // CRT open/close like the incoming panel: the plate grows from its vertical centre.
        float h=Math.max(1,H*amount),cy=Y+H*.5f;
        PilotGlass.panel(p,X,cy-h*.5f,W,h,PilotGlass.FILL);
        if(state==MAD)p.rect(X,cy-h*.5f,W,h,(Math.round(90*Math.min(1,self.madTicks/(float)MAD_TICKS))<<24)|0x9a1f22);
        if(amount<1){float beam=cy-h*.5f+h*amount;p.line(X+9,beam,X+W-9,beam,3,SoftCue.alpha(0x55ffc52f,1-amount));}
        if(amount<.6f)return;
        // Contents fade in once the plate is mostly open, and out as it folds.
        if(amount<1)p=new OpacityPainter(p,(amount-.6f)/.4f);
        // Portrait on the LEFT (the incoming panel keeps its speaker on the right), facing the text.
        float breathe=(float)StrictMath.sin(g.visualTicks*.045)*.7f;
        float px=X+SPLIT*.5f+shake,py=Y+H-6-SPRITE*(182.5f/192-.5f)+breathe;
        p.sprite(atlas(captain),self.frame(g),px,py,SPRITE,SPRITE,0,1);
        p.line(X+SPLIT,Y+18,X+SPLIT,Y+106,1,0x447e939f);
        float tx=X+SPLIT+14,tw=W-SPLIT-28;
        ArcadeIcons.draw(p,ArcadeIcons.RADIO,tx+6,Y+22,.65f,state==MAD?0xffd8463c:PilotGlass.AMBER);
        String header=NAMES[captain]+" / YOUR TUG";
        Lettering.draw(p,header,tx+20,Y+18,Math.min(9,(tw-80)/Math.max(1,Lettering.width(header,1))),PilotGlass.AMBER);
        String badge=self.talking(g)?"RADIO / TX":state==MAD?"HULL HIT":"OOPS";
        float bw=Lettering.width(badge,9)+16;p.rect(X+W-14-bw,Y+12,bw,22,0xff26334a);Lettering.draw(p,badge,X+W-6-bw,Y+18,9,PilotGlass.MUTED);
        String text=self.text(g);
        if(text!=null)Lettering.heading(p,text,tx,Y+46,Math.min(17,17*tw/Math.max(1,Lettering.headingWidth(text,17))),PilotGlass.TEXT);
        String note=state==MAD?"HULL DAMAGED / EASY ON THE THROTTLE":state==WINCE?"SOMEONE ELSE'S PROPERTY / THAT GOES ON THE RECORD":self.talking(g)?"TRANSMITTING":"SENT";
        Lettering.draw(p,note,tx,Y+80,Math.min(10,tw/Math.max(1,Lettering.width(note,1))),PilotGlass.MUTED);
        if(self.talking(g))for(int i=0;i<7;i++){
            float bh=2+5*(.5f+.5f*(float)StrictMath.sin(g.visualTicks*.6f+i*1.8f));
            p.rect(tx+i*7,Y+106-bh,4,bh,PilotGlass.AMBER);
        }
    }
    PlayerPortrait(){}
}
