package com.bargeboss.game;

/** Goal-based lock passage. Only entry/exit windows are timed; preparation is free. */
public final class Lock {
    public static final float ENTRY_Y=330,EXIT_Y=0,GATE_HALF=6,SAFE_GAP=3;
    public static final int REQUIRED=2,ENTRY_TICKS=45*60,EXIT_TICKS=35*60,CYCLE_TICKS=8*60,GATE_TICKS=5*60;
    public static final int READY=0,ENTRY=1,CLOSING=2,FILLING=3,OPENING=4,EXIT=5,COMPLETE=6,FAILED=7;
    public int phase,ticks;
    private final int[] targets=new int[REQUIRED];
    private String failure="";
    private float failedLevel,failedEntry,failedExit;
    public void reset(){phase=READY;ticks=0;failure="";failedLevel=failedEntry=failedExit=0;for(int i=0;i<targets.length;i++)targets[i]=0;}
    public boolean complete(){return phase==COMPLETE;}
    public boolean failed(){return phase==FAILED;}
    public String failure(){return failure;}
    public float waterLevel(){return failed()?failedLevel:phase<FILLING?0:phase==FILLING?Physics.clamp(ticks/(float)CYCLE_TICKS,0,1):1;}
    public float entryOpening(){return failed()?failedEntry:phase==ENTRY?gateProgress():phase==CLOSING?1-gateProgress():0;}
    public float exitOpening(){return failed()?failedExit:phase==OPENING?gateProgress():phase==EXIT || phase==COMPLETE?1:0;}
    private float gateProgress(){float t=Physics.clamp(ticks/(float)GATE_TICKS,0,1);return t*t*(3-2*t);}
    /** Shared water velocity field, in world units per motion-second. No random/render state.
     * Open-gate reaches oppose upstream travel; filling sluices stir the sealed chamber. */
    private float gateFlow(float y){
        if(phase==ENTRY)return entryOpening()*Physics.clamp(1-Math.abs(y-ENTRY_Y)/180,0,1);
        if(phase==OPENING || phase==EXIT)return exitOpening()*Physics.clamp(1-Math.abs(y-EXIT_Y)/180,0,1);
        return 0;
    }
    private float fillingFlow(float x,float y){
        if(phase!=FILLING || x<=Physics.LEFT || x>=Physics.RIGHT || y<=EXIT_Y || y>=ENTRY_Y)return 0;
        float ends=Physics.clamp(Math.min(y-EXIT_Y,ENTRY_Y-y)/45,0,1);
        return ends*(float)StrictMath.sin(Math.PI*waterLevel());
    }
    public float currentX(float x,float y){
        if(x<=Physics.LEFT || x>=Physics.RIGHT)return 0;
        float bank=(float)StrictMath.sin(Math.PI*(x-Physics.LEFT)/(Physics.RIGHT-Physics.LEFT));
        return bank*(6*gateFlow(y)*(float)StrictMath.sin(ticks*.018)
                +30*fillingFlow(x,y)*(float)StrictMath.sin(y*.025+ticks*.018));
    }
    public float currentY(float x,float y){
        if(x<=Physics.LEFT || x>=Physics.RIGHT)return 0;
        return 26*gateFlow(y)+16*fillingFlow(x,y)*(float)StrictMath.sin(x*.025+ticks*.014);
    }
    /** Relative-water drag preserves hull orientation and tow mass response. */
    public void integrate(Body b){Physics.integrate(b,currentX(b.x,b.y),currentY(b.x,b.y));}
    public boolean entryPassable(){return phase==ENTRY && ticks>=GATE_TICKS;}
    public boolean exitPassable(){return phase==EXIT || phase==COMPLETE;}
    public int secondsRemaining(){return phase==ENTRY?Math.max(0,(ENTRY_TICKS-ticks+59)/60):phase==EXIT?Math.max(0,(EXIT_TICKS-ticks+59)/60):0;}
    public int targetId(int index){return targets[index];}
    private Body target(Game g,int index){for(Body b:g.cargo)if(b!=null && b.active && b.id==targets[index])return b;return null;}
    public static float front(Body b){float y=Float.MAX_VALUE;for(int i=0;i<3;i++)y=Math.min(y,b.discY(i)-b.radius);return y;}
    public static float back(Body b){float y=-Float.MAX_VALUE;for(int i=0;i<3;i++)y=Math.max(y,b.discY(i)+b.radius);return y;}
    public static boolean inside(Body b){return b.active && front(b)>=EXIT_Y+GATE_HALF+SAFE_GAP && back(b)<=ENTRY_Y-GATE_HALF-SAFE_GAP;}
    public static boolean cleared(Body b){return b.active && back(b)<EXIT_Y-GATE_HALF-SAFE_GAP;}
    public int insideCount(Game g){int n=0;for(int i=0;i<targets.length;i++){Body b=target(g,i);if(b!=null && inside(b))n++;}return n;}
    public int clearedCount(Game g){int n=0;for(int i=0;i<targets.length;i++){Body b=target(g,i);if(b!=null && cleared(b))n++;}return n;}
    private boolean acquire(Game g){int n=0;for(Body b:g.cargo)if(b!=null && b.active && b.id>0 && !b.deliveryRecorded){targets[n++]=b.id;if(n==REQUIRED)return true;}return false;}
    private void change(Game g,int next,String words){phase=next;ticks=0;g.tell(words);g.haptic(Game.HAPTIC_TICK);}
    private void fail(Game g,String reason){failedLevel=waterLevel();failedEntry=entryOpening();failedExit=exitOpening();failure=reason;change(g,FAILED,reason+" / RETRY THE LOCK");}
    public void step(Game g){
        if(phase==COMPLETE || phase==FAILED)return;
        if(phase==READY){
            // Do not spend the entry window while the feeders are still arriving.
            if(g.tug.y<=ENTRY_Y+140 && g.tug.y>=ENTRY_Y && acquire(g))change(g,ENTRY,"ENTRY OPENING / WAIT FOR GREEN");
            return;
        }
        ticks++;
        for(int i=0;i<targets.length;i++)if(target(g,i)==null){fail(g,"LOCK CARGO MISSING");return;}
        switch(phase){
            case ENTRY:
                if(ticks==GATE_TICKS)g.tell("ENTRY OPEN / STEER AGAINST THE CURRENT");
                if(entryPassable() && inside(g.tug) && insideCount(g)==REQUIRED)change(g,CLOSING,"ALL ABOARD / KEEP CLEAR OF THE GATES");
                else if(ticks>=ENTRY_TICKS)fail(g,"ENTRY WINDOW MISSED");
                break;
            case CLOSING:
                if(ticks>=GATE_TICKS)change(g,FILLING,"LOCK FILLING / STEER TO HOLD POSITION");
                break;
            case FILLING:
                if(ticks>=CYCLE_TICKS)change(g,OPENING,"WATER LEVEL MATCHED / EXIT OPENING");
                break;
            case OPENING:
                if(ticks>=GATE_TICKS)change(g,EXIT,"EXIT OPEN / CURRENT AHEAD / CLEAR BOTH BARGES");
                break;
            case EXIT:
                if(clearedCount(g)==REQUIRED)change(g,COMPLETE,"BOTH BARGES THROUGH / LOCK COMPLETE");
                else if(ticks>=EXIT_TICKS)fail(g,"EXIT WINDOW MISSED");
                break;
            default:break;
        }
    }
    /** A closed gate also blocks a new towline across its water-level face. */
    public boolean blocksLine(float x,float y,float xx,float yy){
        return !entryPassable() && crosses(x,y,xx,yy,ENTRY_Y)
                || !exitPassable() && crosses(x,y,xx,yy,EXIT_Y);
    }
    private static boolean crosses(float x,float y,float xx,float yy,float gate){
        if(Math.min(y,yy)>gate || Math.max(y,yy)<gate)return false;
        if(y==yy)return Math.max(x,xx)>=Physics.LEFT && Math.min(x,xx)<=Physics.RIGHT;
        float at=x+(xx-x)*(gate-y)/(yy-y);return at>=Physics.LEFT && at<=Physics.RIGHT;
    }
    /** Full oriented hull discs stop against each closed gate; no position-center shortcut. */
    public float collide(Body b){
        if(!b.active || failed())return 0;
        float impact=0;
        if(!entryPassable())impact=Math.max(impact,gateCollision(b,ENTRY_Y));
        if(!exitPassable())impact=Math.max(impact,gateCollision(b,EXIT_Y));
        return impact;
    }
    private static float gateCollision(Body b,float y){
        float impact=0;
        for(int i=0;i<3;i++){
            if(b.discX(i)+b.radius<Physics.LEFT || b.discX(i)-b.radius>Physics.RIGHT)continue;
            float delta=b.discY(i)-y,reach=b.radius+GATE_HALF;
            if(Math.abs(delta)>=reach)continue;
            float side=b.y>=y?1:-1;b.y+=side*reach-delta;
            if(b.vy*side<0){impact=Math.max(impact,Math.abs(b.vy));b.vy=-b.vy*.08f;}
            b.spin*=.6f;
        }
        return impact;
    }
    public String signal(){
        if(phase==READY)return "APPROACH WITH TWO BARGES";
        if(phase==ENTRY)return (entryPassable()?"ENTER / CURRENT / ":"ENTRY OPENING / ")+secondsRemaining()+"S";
        if(phase==CLOSING)return "GATES CLOSING";
        if(phase==FILLING)return "FILLING / "+((CYCLE_TICKS-ticks+59)/60)+"S";
        if(phase==OPENING)return "EXIT OPENING";
        if(phase==EXIT)return "EXIT / "+secondsRemaining()+"S";
        return phase==COMPLETE?"LOCK COMPLETE":failure;
    }
}
