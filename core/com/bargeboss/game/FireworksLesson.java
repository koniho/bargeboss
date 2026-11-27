package com.bargeboss.game;

/** Deterministic tutorial mishap: six burning woodpiles in three quay groups around the
 * celebration basin and its west canal. Only simulated nozzle droplets can extinguish them;
 * render code reads this state and never advances it. */
final class FireworksLesson {
    static final float X=448,Y=-1160;
    /** Timber footprint around (x+FOOT_OFFSET, y): the droplet stop box and the cooling area agree,
     * so a pile is cooled from whichever side the jet arrives. */
    static final float FOOT_OFFSET=15,FOOT_X=27,FOOT_Y=38;
    static boolean onPile(int pile,float x,float y){return Math.abs(x-x(pile)-FOOT_OFFSET)<=FOOT_X && Math.abs(y-y(pile))<=FOOT_Y;}
    static final float TOW_X=330,TOW_Y=-1230;
    static final float BERTH_X=-350,BERTH_Y=-1050;
    // Centerline runs south from the celebration basin through two generous bends.
    private static final float[] ROUTE_X={330,300,-50,-50,-240};
    private static final float[] ROUTE_Y={-1120,-1350,-1350,-1050,-1050};
    float target(Game g,boolean x){
        if(g.tutorial.lesson==Tutorial.EXTINGUISH){int group=targetGroup();
            float[] rx=group==1?NORTH_ROUTE_X:WEST_ROUTE_X,ry=group==1?NORTH_ROUTE_Y:WEST_ROUTE_Y;
            return TowRouteGuide.target(g.tug.x,g.tug.y,rx,ry,false,x);}
        return TowRouteGuide.target(g.tug.x,g.tug.y,ROUTE_X,ROUTE_Y,Tutorial.direct(g,g.cargo[3]),x);
    }
    /** Two piles per quay group, ordered east quay (reachable from the marked water), north
     * quay, then the west canal's far shore and north-west corner. Every pile sits on dry
     * raised quay within monitor reach (40..210) of its group's station, never of the
     * previous one, so the barge must be towed between groups. */
    static final int PILES=6,GROUPS=3,PILE_HEAT=240,HEAT=PILES*PILE_HEAT,IGNITION=140,STEAM_TICKS=180;
    private static final float[] PX={448,448,225,385,-195,-60},PY={-1160,-1280,-1550,-1550,-1300,-1472};
    /** Barge stations: where the fire barge comfortably reaches both piles of a group. */
    static final float[] STATION_X={TOW_X,350,-50},STATION_Y={TOW_Y,-1450,-1340};
    static final String[] GROUP_NAME={"EAST QUAY","NORTH QUAY","WEST CANAL"};
    static final float REACH=195;
    // Up the east side, then west along the north wall so the trailing barge swings in beneath
    // the north quay; then back south-west round into the west canal, heading south so the
    // barge trails north toward the far-shore and corner piles.
    private static final float[] NORTH_ROUTE_X={330,405,405,215},NORTH_ROUTE_Y={-1230,-1300,-1440,-1465};
    private static final float[] WEST_ROUTE_X={215,250,-45,-45},WEST_ROUTE_Y={-1465,-1370,-1370,-1230};
    static float x(int pile){return PX[pile];}
    static float y(int pile){return PY[pile];}
    static int group(int pile){return pile/2;}
    private final int[] pileHeat=new int[PILES],pileSteam=new int[PILES],pileSizzle=new int[PILES];
    /** Ring of "water was landing" flags per pile, indexed by smoke tick, so each smoke puff
     * keeps the colour it was born with: soot while dry, steam while being cooled. */
    static final int SMOKE_LIFE=FireArt.SMOKE_LIFE,SMOKE_PUFFS=FireArt.SMOKE_PUFFS;
    private final boolean[][] wetAt=new boolean[PILES][SMOKE_LIFE];
    int smokeTicks;
    int heat(int pile){return heat==0?0:pileHeat[pile];}
    int steam(int pile){return pileSteam[pile];}
    int sizzle(int pile){return pileSizzle[pile];}
    boolean burning(int group){return heat(group*2)>0 || heat(group*2+1)>0;}
    /** First quay group, in route order, that still has a burning pile. */
    int targetGroup(){for(int i=0;i<GROUPS;i++)if(burning(i))return i;return GROUPS-1;}
    /** Nearest burning pile to the fire barge, preferring the current group. */
    int targetPile(){int group=targetGroup();return heat(group*2)>0?group*2:group*2+1;}
    int targetPile(Body barge){int best=targetPile();float nearest=Float.MAX_VALUE;
        for(int i=0;i<PILES;i++)if(heat(i)>0){float d=Physics.length(barge.x-x(i),barge.y-y(i));if(d<nearest){nearest=d;best=i;}}
        return best;}
    boolean inReach(Body barge,int pile){return Physics.length(barge.x-x(pile),barge.y-y(pile))<=REACH;}
    /** The barge reaches every burning pile of the current group. */
    boolean onStation(Body barge){int group=targetGroup();
        for(int i=group*2;i<group*2+2;i++)if(heat(i)>0 && !inReach(barge,i))return false;return true;}
    /** Camera framing: burning piles near the fire barge, so distant groups never zoom out the view. */
    boolean framed(Body barge,int pile){return heat(pile)>0 && Physics.length(barge.x-x(pile),barge.y-y(pile))<=300;}
    int remaining(){int n=0;for(int h:pileHeat)if(h>0)n++;return n;}
    void ignite(){ignited=true;heat=HEAT;for(int i=0;i<PILES;i++)pileHeat[i]=PILE_HEAT;}
    final FireSteam contactSteam=new FireSteam();
    final FireRescueRadio rescueRadio=new FireRescueRadio();
    int showTicks,heat,steam,sizzle;
    boolean launched,ignited,done;
    float launchX,launchY;
    void reset(){contactSteam.reset();for(int i=0;i<PILES;i++){pileHeat[i]=pileSteam[i]=pileSizzle[i]=0;for(int j=0;j<SMOKE_LIFE;j++)wetAt[i][j]=false;}rescueRadio.reset();showTicks=heat=steam=sizzle=smokeTicks=0;launched=ignited=done=false;}
    /** Explicit setup after the fire lesson: the piles are already out and the relief call is over. */
    void extinguished(){reset();launched=ignited=done=true;rescueRadio.conclude();}
    void launched(Game g,Body b){
        if(g.tutorial.lesson!=Tutorial.SHOW || (b!=g.cargo[0] && b!=g.cargo[2]) || !g.passages.passable(TutorialTowBridges.ENTRY) || launched)return;
        launched=true;launchX=b.x;launchY=b.y;
    }
    void step(Game g){
        contactSteam.step();smokeTicks++;
        steam=sizzle=0;for(int i=0;i<PILES;i++){if(pileSteam[i]>0)pileSteam[i]--;if(pileSizzle[i]>0)pileSizzle[i]--;steam=Math.max(steam,pileSteam[i]);sizzle=Math.max(sizzle,pileSizzle[i]);
            wetAt[i][smokeTicks&(SMOKE_LIFE-1)]=pileSizzle[i]>0;}
        rescueRadio.step(g);
        if(launched && !ignited && ++showTicks>=IGNITION){ignite();g.radio.transmit(RadioAudio.TEACHER);}
        if(ignited && heat>0)FlameArt.step(g);
    }
    boolean waterHit(float x,float y){
        if(!ignited || done || heat==0)return false;
        for(int i=0;i<PILES;i++)if(pileHeat[i]>0 && onPile(i,x,y)){
            contactSteam.hit(i,x,y);pileSizzle[i]=sizzle=24;pileHeat[i]--;heat--;
            if(pileHeat[i]==0)pileSteam[i]=steam=STEAM_TICKS;
            if(heat==0)done=true;
            return true;
        }
        return false;
    }
    boolean visible(){return launched && !ignited || heat>0 || steam>0;}
    float depth(Game g,int pile){return g.layout.camera.screenY(x(pile),y(pile));}
    /** Flickering intensity in 0..1: full heat burns brightest, a cooled pile fades through
     * its steam tail to zero. Shared by the red wash and the smoke column density. */
    float intensity(Game g,int pile){return FireArt.intensity(g,heat(pile)/(float)PILE_HEAT,pileSteam[pile]/(float)STEAM_TICKS,pile);}
    InstructionPanel.Content content(Game g){
        boolean fire=g.tutorial.lesson>=Tutorial.FIRE_TOW;
        boolean collect=g.tutorial.lesson==Tutorial.FIRE_TOW;
        int left=remaining();
        String title=fire?"THAT WAS NOT THE TINY BUTTON.":launched?"PERHAPS A LITTLE TOO FESTIVE...":"YOU PASSED! CUE THE FIREWORKS.";
        String first=fire?(collect?"SMALL UPDATE: SIX WOODPILES ARE BURNING.":(left==1?"ONE FIRE LEFT":left+" FIRES LEFT")+" / "+GROUP_NAME[targetGroup()]):launched?"I SAID CONFETTI. THAT IS A SMALL SUN.":"TAP THE FIREWORKS BARGE, THEN TAP LAUNCH.";
        String second=collect?(Tutorial.direct(g,g.cargo[3])?"TOW THE FIRE BARGE INTO THE MARKED WATER.":"FETCH THE FIRE BARGE THROUGH THE SOUTH CANAL."):fire?(onStation(g.cargo[3])?"USE THE AIM PAD. SPRAY EVERY WOODPILE.":"TOW THE FIRE BARGE TO THE "+GROUP_NAME[targetGroup()]+"."):launched?"WHO PARKED ALL THAT FIREWOOD THERE?":"A TINY VICTORY DISPLAY. YOU HAVE EARNED IT.";
        InstructionPanel.Content radio=rescueRadio.content(g,second);if(radio!=null)return radio;
        return new InstructionPanel.Content("fireworks-lesson-"+g.tutorial.lesson+":"+launched+(fire && !collect?":"+left:""),title,first,second,-1,fire?1-heat/(float)HEAT:0,"INSTRUCTOR / CHANNEL 08",fire?"FIRE RESPONSE":"EQUIPMENT PRACTICE");
    }
    /** Wide warm-red wash for the burning fires, drawn on the ground/water layer beneath vessels,
     * buildings and HUD so several fires tint most of the screen while hulls stay readable.
     * The two piles of a quay group stand far closer together than the wash is wide, so they share
     * ONE large GLOW quad, centred on their intensity-weighted midpoint with their summed
     * intensity: at most three screen-sized quads of fill, and quads entirely off screen are skipped. */
    void wash(Game g,Painter p){
        if(!ignited)return;
        for(int group=0;group<GROUPS;group++){
            int a=group*2,b=a+1;float ea=intensity(g,a),eb=intensity(g,b),e=ea+eb;if(e<=.003f)continue;
            FireArt.wash(g,p,(x(a)*ea+x(b)*eb)/e,(y(a)*ea+y(b)*eb)/e,GroundArt.LAND_RISE,e,FireArt.washColor(g,ea>=eb?a:b));
        }
    }
    /** Analytic smoke column: a fixed set of puffs per fire, each reborn every SMOKE_LIFE ticks,
     * drifting downwind as it rises. No particles are stored; dark soot while dry, pale steam
     * for puffs born while water was landing, and nothing once the steam tail ends. */
    /** Smoke columns draw on the world layer beneath vessels and buildings, so a plume drifting
     * across the basin never hides the tug or fire barge; flames and steam stay depth-sorted. */
    void plumes(Game g,Painter p){
        if(!ignited)return;
        for(int pile=0;pile<PILES;pile++)smoke(g,p,pile);
    }
    void smoke(Game g,Painter p,int pile){
        FireArt.plume(g,p,x(pile),y(pile),GroundArt.LAND_RISE,intensity(g,pile),heat(pile)==0,pileSteam[pile]/(float)STEAM_TICKS,smokeTicks,wetAt[pile],pile);
    }
    void draw(Game g,Painter p,int pile){
        float X=x(pile),Y=y(pile);int heat=heat(pile),steam=pileSteam[pile];
        RiverCamera c=g.layout.camera;float scale=c.scale;
        float sx=c.screenX(X,Y),sy=c.screenY(X,Y)-GroundArt.LAND_RISE*scale;
        boolean onScreen=sx>=-180*scale && sx<=g.layout.width+180*scale && sy>=-180*scale && sy<=g.layout.height+180*scale;
        if(onScreen)FirewoodPile.draw(g,p,pile);
        if(launched && !ignited){
            float t=Physics.clamp((showTicks-65)/75f,0,1);
            if(t>0){float x=launchX+(X-launchX)*t,y=launchY+(Y-launchY)*t;
                float h=GroundArt.LAND_RISE+150*(1-t)+90*4*t*(1-t);
                float emberX=c.screenX(x,y),emberY=c.screenY(x,y)-h*scale;
                p.material(emberX,emberY,30*scale,0,0,30*scale,EffectMaterial.GLOW,0,1,0xffff7f2d);
                p.material(emberX,emberY,10*scale,0,0,10*scale,EffectMaterial.DOT,0,1,0xffffed9e);
            }return;
        }
        if(!onScreen)return;
        FireArt.burn(g,p,X,Y,GroundArt.LAND_RISE,heat/(float)PILE_HEAT,pile);
        contactSteam.draw(g,p,pile);
        if(heat==0 && steam>0)FireArt.steamTail(g,p,X,Y,GroundArt.LAND_RISE,1-steam/(float)STEAM_TICKS);
    }
}
