package com.bargeboss.game;

/** Making the run between the marina and the beach a ride, not a commute:
 *  - Open water: outside the marina's no-wake zone a free-running tug (nothing on the towline) runs
 *    30% faster, throwing bow spray.
 *  - Boost lane: a line of marker buoys from the harbor mouth, up the outer breakwater and round its
 *    head to the beach. Pass close at speed for a short boost; back-to-back markers build a combo.
 *    Markers re-arm, and they work in either direction.
 *  - Travel radio: Gavin chats while you're out in open water (nonblocking, like the police radio):
 *    reactions to the lane and landmarks, hints about the job at hand, gossip about the last one.
 * Fixed-step and deterministic; drawing never changes state. */
final class MontroseTransit {
    /** Marker buoys along the route, harbor mouth to beach (all open water outside the no-wake zone). */
    static final float[] BUOYS={250,860,670,876,1089,892,1507,868,1922,803,2301,666,2609,380,2822,33,2955,-365,3041,-775,
        3104,-1190,3130,-1609,3144,-2028,3142,-2448,3128,-2868,3064,-3279,2938,-3680,2621,-3893,2225,-3969,1837,-3807,1462,-3620};
    static final float OPEN_WATER=1.3f,BOOST=1.35f,REACH=48,MIN_SPEED=45;
    static final int BOOST_TICKS=100,COMBO_TICKS=420,REARM_TICKS=720,CHATTER_TICKS=1200,SHOW_TICKS=300;
    final int[] spent=new int[BUOYS.length/2];
    int boostTicks,combo,comboTicks,flash,flashBuoy=-1;
    boolean open;
    private int job=-1,openTicks,lineIndex;
    private boolean greeted,rounded,lineCheered,flyingCheered,smokeWarned;
    /** Last debrief, so the travel chatter can tease about it. */
    int lastJob=-1,lastTier=-1;
    private final InstructionPanel panel=new InstructionPanel();
    private InstructionPanel.Content content;
    private int visible,serial;
    private String[] pending;

    void reset(){java.util.Arrays.fill(spent,0);boostTicks=combo=comboTicks=flash=0;flashBuoy=-1;open=false;job=-1;openTicks=0;greeted=rounded=lineCheered=flyingCheered=smokeWarned=false;lastJob=lastTier=-1;content=null;visible=0;pending=null;}
    static int count(){return BUOYS.length/2;}
    private static boolean active(Game g){return g.montroseStage() && g.tutorial==null && g.mode==Game.PLAY && !g.dying() && !g.montroseArrival.active();}
    /** Drive multiplier for the tug right now. */
    float speedFactor(Game g){
        // Free running only: a loaded towline keeps its careful pace.
        if(!active(g) || g.tow.loadCount(g.tug)>0)return 1;
        return (open?OPEN_WATER:1)*(boostTicks>0?BOOST:1);
    }
    void step(Game g){
        if(!active(g)){open=false;return;}
        if(g.challengeId!=job){job=g.challengeId;greeted=rounded=false;openTicks=0;}
        boolean was=open;open=!HarborTraffic.noWake(g.tug.x,g.tug.y);
        if(open && !was && g.tug.speed()>20){g.tell("OPEN WATER! FULL THROTTLE");if(!greeted){greeted=true;say(g,greeting(g));}}
        if(!open && was)say(g,new String[]{"WELCOME BACK INSIDE","NO-WAKE ZONE. EASE OFF THE THROTTLE, CAPTAIN.","MONTROSE HARBOR / DOCKMASTER"});
        if(boostTicks>0)boostTicks--;if(flash>0)flash--;
        if(comboTicks>0 && --comboTicks==0){combo=0;}
        for(int i=0;i<spent.length;i++)if(spent[i]>0)spent[i]--;
        // Markers: pass close enough at speed.
        if(g.tug.speed()>=MIN_SPEED)for(int i=0;i<spent.length;i++){
            if(spent[i]>0 || Physics.length(g.tug.x-BUOYS[i*2],g.tug.y-BUOYS[i*2+1])>REACH)continue;
            spent[i]=REARM_TICKS;boostTicks=BOOST_TICKS;combo=comboTicks>0?combo+1:1;comboTicks=COMBO_TICKS;
            int points=25*combo;g.score+=points;flash=40;flashBuoy=i;
            g.haptic(Game.HAPTIC_TICK);g.sounds.splash(.35f);
            g.tell(combo>1?"BOOST! x"+combo+" +"+points:"BOOST! +"+points);
            if(combo>=4 && !lineCheered){lineCheered=true;say(g,new String[]{"NICE LINE ON THOSE MARKERS!","YOU'RE THREADING THEM LIKE A REGATTA PRO.","MONTROSE HARBOR / DOCKMASTER"});}
            else if(combo>=8 && !flyingCheered){flyingCheered=true;say(g,new String[]{"YOU'RE FLYING, CAPTAIN!","I CAN SEE YOUR ROOSTER TAIL FROM THE OFFICE.","MONTROSE HARBOR / DOCKMASTER"});}
        }
        // Landmarks.
        if(!rounded && g.tug.y<-3450 && g.tug.x>2300){rounded=true;say(g,new String[]{"ROUNDING THE BREAKWATER HEAD","THAT'S MONTROSE BEACH OFF YOUR BOW. MIND THE SWIMMERS INSIDE.","MONTROSE HARBOR / DOCKMASTER"});}
        if(!smokeWarned && open && g.hull<40){smokeWarned=true;say(g,new String[]{"YOU'RE TRAILING SMOKE","TAKE IT EASY OUT THERE. THAT HULL HAS HAD A DAY.","MONTROSE HARBOR / DOCKMASTER"});}
        // Idle chatter on long open-water runs.
        if(open && ++openTicks%CHATTER_TICKS==0)say(g,chatter(g));
        radio(g);
    }
    private void say(Game g,String[] line){if(line!=null && pending==null)pending=line;}
    private void radio(Game g){
        if(g.dialogue.active)return;
        if(visible>0 && --visible==0)content=null;
        if(visible==0 && pending!=null && g.radio.progress()>=1){
            panel.reset();content=new InstructionPanel.Content("transit-"+(++serial),pending[0],pending[1],"",-1,0,"GAVIN / "+pending[2],"RADIO / RX");
            pending=null;visible=SHOW_TICKS;g.radio.receiveDockmaster();
        }
        panel.step(content);
    }
    private String[] greeting(Game g){
        String who="MONTROSE HARBOR / DOCKMASTER";
        switch(g.challengeId){
            case Challenges.WATER_TAXI:return new String[]{"OPEN WATER. OPEN HER UP","FOLLOW THE MARKERS UP THE BREAKWATER. THE BEACH CROWD IS WAITING.",who};
            case Challenges.SANDBAR:return new String[]{"THE YACHT ISN'T GOING ANYWHERE","BUT THE OWNER IS PACING THE BEACH. HIT THE MARKERS FOR A BOOST.",who};
            case Challenges.REGATTA:return new String[]{"GATES FIRST, SIGHTSEEING LATER","THE COMMITTEE IS WATCHING THE CLOCK. SO AM I.",who};
            case Challenges.FIREWORKS_NIGHT:return new String[]{"THE LAKEFRONT IS FILLING UP","EVERYONE'S WAITING ON THAT BARGE. NO PRESSURE.",who};
            case Challenges.DEEP_FREEZE:return new String[]{"YOU SHOULDN'T BE OUT HERE","THE ICE IS COMING. GET THOSE BOATS TO THE LIFT.",who};
            default:return new String[]{"OPEN WATER. NO-WAKE RULES DON'T APPLY","OPEN HER UP. THE MARKER BUOYS GIVE YOU A KICK IF YOU PASS CLOSE.",who};
        }
    }
    /** Rotating chatter: a tease about the last job, a hint for this one, then local color. */
    private String[] chatter(Game g){
        String who="MONTROSE HARBOR / DOCKMASTER";
        int pick=lineIndex++%4;
        if(pick==0 && lastJob>=0){
            if(lastTier==0)return new String[]{"STILL TALKING ABOUT THAT LAST JOB","THE GUYS AT THE FUEL DOCK WANT YOUR AUTOGRAPH.",who};
            if(lastTier==2)return new String[]{"ABOUT THAT LAST JOB...","I'VE STOPPED ANSWERING THE OWNERS' CALLS. LET'S MAKE THIS ONE CLEAN.",who};
            return new String[]{"SOLID WORK BACK THERE","KEEP THAT UP AND I'LL HAVE TO GIVE YOU A RAISE. OR A SANDWICH.",who};
        }
        if(pick==1)switch(g.challengeId){
            case Challenges.WATER_TAXI:return new String[]{"SHUTTLE TIP","SLOW RIGHT DOWN AT THE WATER'S EDGE OR THEY WON'T JUMP ABOARD.",who};
            case Challenges.SANDBAR:return new String[]{"SANDBAR TIP","KEEP THE LINE TIGHT AND PULL STRAIGHT AWAY. SHE'LL ROCK LOOSE.",who};
            case Challenges.REGATTA:return new String[]{"REGATTA TIP","CUT THE GATES CLOSE. EVERY SECOND LEFT PAYS AT THE FINISH.",who};
            case Challenges.WAKE_PATROL:return new String[]{"PATROL TIP","BUMP THEM FIRST. A TICKETED SPEEDER STOPS DEAD.",who};
            case Challenges.LOOSE_BOATS:return new String[]{"ROUNDUP TIP","THE WIND KEEPS PUSHING THEM. GRAB THE FARTHEST ONE FIRST.",who};
            default:return new String[]{"HOW'S SHE HANDLING?","THAT TUG IS OLDER THAN ME AND STILL FASTER.",who};
        }
        String[][] color={
            {"LIGHTHOUSE KEEPER SAYS HI","HE WAVES AT EVERY BOAT. NOBODY HAS THE HEART TO TELL HIM HE'S RETIRED.",who},
            {"WATCH FOR KITE SURFERS","THEY COME OUT WHEN THE WIND PICKS UP. THEY DO NOT LOOK WHERE THEY'RE GOING.",who},
            {"FISH REPORT","THE PERCH ARE BITING OFF THE BREAKWATER. NOT THAT YOU HAVE TIME.",who},
            {"SKYLINE'S CLEAR TODAY","YOU CAN SEE ALL THE WAY DOWN TO NAVY PIER. ENJOY IT.",who}};
        return color[(lineIndex/4)%color.length];
    }

    /** Water layer: marker buoys and the tug's bow spray at speed. */
    void water(Game g,Painter p){
        if(!g.montroseStage() || g.tutorial!=null)return;
        RiverCamera c=g.layout.camera;float s=c.scale;
        for(int i=0;i<spent.length;i++){
            float x=BUOYS[i*2],y=BUOYS[i*2+1],sx=c.screenX(x,y),sy=c.screenY(x,y);
            if(sx<-80 || sx>g.layout.width+80 || sy<-80 || sy>g.layout.height+80)continue;
            boolean ready=spent[i]==0;float bob=(float)StrictMath.sin(g.visualTicks*.08f+i)*1.5f*s;
            // Lane direction toward the beach, for the chevron.
            int n=Math.min(spent.length-1,i+1),m=Math.max(0,i-1);float dx=BUOYS[n*2]-BUOYS[m*2],dy=BUOYS[n*2+1]-BUOYS[m*2+1],d=Math.max(1,Physics.length(dx,dy));dx/=d;dy/=d;
            float pulse=.75f+.25f*(float)StrictMath.sin(g.visualTicks*.15f+i*.7f);
            int col=ready?0xff2fe6ff:0xff4a6670;
            if(ready)p.material(sx,sy,REACH*2.2f*s,0,0,REACH*2.2f*s*RiverCamera.DEPTH,EffectMaterial.RING,.05f,1,SoftCue.alpha(col,.35f*pulse));
            p.material(sx,sy-6*s+bob,26*s,0,0,26*s,EffectMaterial.GLOW,0,1,SoftCue.alpha(col,ready?.55f*pulse:.15f));
            p.circle(sx,sy-4*s+bob,5*s,ready?0xffff8a2b:0xff6a4a3a);p.circle(sx,sy-10*s+bob,2.4f*s,ready?0xffffffff:0xff8a8a8a);
            if(ready)for(int k=0;k<2;k++){
                float along=(14+k*9)*s+((g.visualTicks*.6f)%9)*s,cx=c.screenX(x+dx*along/s,y+dy*along/s),cy=c.screenY(x+dx*along/s,y+dy*along/s);
                float ux=c.screenX(x+dx,y+dy)-sx,uy=c.screenY(x+dx,y+dy)-sy,ul=Math.max(.001f,Physics.length(ux,uy));ux/=ul;uy/=ul;
                float w=5*s;p.line(cx-ux*w+uy*w,cy-uy*w-ux*w,cx,cy,2*s,SoftCue.alpha(col,.7f));p.line(cx-ux*w-uy*w,cy-uy*w+ux*w,cx,cy,2*s,SoftCue.alpha(col,.7f));
            }
            if(flash>0 && flashBuoy==i){float u=1-flash/40f,r=(REACH*.5f+u*55)*s;p.material(sx,sy,r*2,0,0,r*2*RiverCamera.DEPTH,EffectMaterial.RING,.035f,1,SoftCue.alpha(0xff9ff4ff,(1-u)*(1-u)*.7f));}
        }
        // Bow spray in open water at speed; boosting throws a bigger plume.
        Body t=g.tug;float speed=t.speed();
        if(open && speed>55){
            float k=Math.min(1,(speed-55)/50f)*(boostTicks>0?1.5f:1),ca=(float)StrictMath.cos(t.angle),sa=(float)StrictMath.sin(t.angle);
            for(int j=0;j<6;j++){
                int age=(g.visualTicks*2+j*7)%24;float u=age/24f,side=(j%2==0?1:-1);
                float bx=t.x+ca*(t.half+4)-sa*side*(6+u*18),by=t.y+sa*(t.half+4)+ca*side*(6+u*18);
                float px=c.screenX(bx-ca*u*20,by-sa*u*20),py=c.screenY(bx-ca*u*20,by-sa*u*20)-(4+u*10)*s*k,size=(4+u*7)*s*k;
                p.spriteAffine(SpriteAtlas.SPLASH,j%4,px,py,size*1.4f,0,0,size,(1-u)*.75f*Math.min(1,k));
            }
        }
    }
    /** Screen layer: boost bar and combo, and the travel radio. */
    void hud(Game g,Painter p){
        if(!active(g))return;
        if(boostTicks>0 || combo>1){
            float x=g.layout.width*.5f,y=g.layout.height-70,w=160,f=boostTicks/(float)BOOST_TICKS;
            p.rect(x-w*.5f-2,y-2,w+4,10,0xd0050a10);p.rect(x-w*.5f,y,w*f,6,0xff2fe6ff);
            Lettering.center(p,combo>1?"BOOST x"+combo:"BOOST",x,y-20,15,0xff2fe6ff);
        }
        if(serial>0 && !g.dialogue.active)panel.draw(g,p);
    }
}
