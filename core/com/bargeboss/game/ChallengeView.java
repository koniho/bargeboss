package com.bargeboss.game;

/** Towing-challenge cues: start line, free far-quay berths and a top-center scoreboard.
 * Labels are cached by TowingChallenge; drawing builds no strings while the clock runs. */
final class ChallengeView {
    static final float WIDTH=330,HEIGHT=56,TOP=62;
    private static final int PAPER=PilotGlass.TEXT,MUTED=PilotGlass.MUTED,URGENT=0xffff6b5a;
    private ChallengeView(){}
    /** Screen box shared by drawing and tests; centered below the HUD ribbon, clear of the top-left corner. */
    static float left(Game g){return (g.layout.width-WIDTH)*.5f;}
    static void guidance(Game g,Painter p){
        TowingChallenge c=g.tutorial.challenge;
        if(g.dialogue.active)return;
        Painter river=new RiverPainter(p,g.layout.camera);
        if(c.phase==TowingChallenge.APPROACH){
            int ink=SoftCue.animated(g,ControlView.GREEN);
            SoftCue.line(river,TrainingChannel.CHALLENGE_MOUTH_LEFT+4,TowingChallenge.START_Y,TrainingChannel.CHALLENGE_MOUTH_RIGHT-4,TowingChallenge.START_Y,2,ink);
            SoftCue.caption(river,"START",TowingChallenge.START_X,TowingChallenge.START_Y-22,9,ControlView.GREEN);
            return;
        }
        if(c.phase!=TowingChallenge.RUN)return;
        RiverCamera camera=g.layout.camera;
        int ink=SoftCue.animated(g,0xcc28e7ff);
        for(int row=0;row<TowingChallenge.BARGES;row++){
            if(c.claimed(row))continue;
            float y=TowingChallenge.rowY(row),x=TrainingChannel.CHALLENGE_RIGHT-4;
            float sx=camera.screenX(x,y),sy=camera.screenY(x,y);
            if(sx<-40 || sx>g.layout.width+40 || sy<-40 || sy>g.layout.height+40)continue;
            // A free berth: two soft fender marks on the quay face, open in the middle.
            SoftCue.line(river,x,y-TowingChallenge.HALF_WIDTH+6,x,y-18,2,ink);
            SoftCue.line(river,x,y+18,x,y+TowingChallenge.HALF_WIDTH-6,2,ink);
        }
        if(g.tow.loadCount(g.tug)==0 && !g.towAiming && !g.ropeDragging){
            Body next=c.nearestWaiting(g);
            if(next!=null && Physics.length(next.x-g.tug.x,next.y-g.tug.y)<260)
                ControlView.ring(g,river,next.x,next.y,44+.8f*(float)StrictMath.sin(g.visualTicks*.045),1,VisualStyle.GOLD,2);
        }
    }
    static void scoreboard(Game g,Painter p){
        TowingChallenge c=g.tutorial.challenge;
        if(c.phase!=TowingChallenge.RUN && c.phase!=TowingChallenge.RESULT)return;
        float x=left(g),y=TOP;
        PilotGlass.panel(p,x,y,WIDTH,HEIGHT,PilotGlass.FILL);
        boolean urgent=c.running() && c.remaining<=10*60;
        int clock=urgent && (g.visualTicks/15)%2==0?URGENT:PilotGlass.AMBER;
        Lettering.draw(p,"TIME",x+20,y+9,9,MUTED);
        Lettering.heading(p,c.clockLabel(),x+20,y+23,22,clock);
        p.line(x+112,y+10,x+112,y+HEIGHT-10,1,PilotGlass.LINE);
        Lettering.draw(p,"BARGES TIED:",x+128,y+9,9,MUTED);
        float pop=c.celebrateTicks>0?3*(float)StrictMath.sin(Math.min(1,(60-c.celebrateTicks)/12f)*Math.PI):0;
        Lettering.heading(p,c.countLabel(),x+128,y+23-pop*.5f,22+pop,c.celebrateTicks>0?ControlView.GREEN:PAPER);
        String best=c.bestLabel();
        Lettering.draw(p,best,x+WIDTH-16-Lettering.width(best,9),y+9,9,MUTED);
        if(c.running() && c.remaining>TowingChallenge.TICKS-60){
            float t=(TowingChallenge.TICKS-c.remaining)/60f,alpha=1-t*t;
            Lettering.centerHeading(new OpacityPainter(p,alpha),"GO!",g.layout.width*.5f,g.layout.height*.36f,40+12*t,PilotGlass.AMBER);
        }
    }
}
