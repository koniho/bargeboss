package com.bargeboss.game;

/** Instructor leads through the existing orthogonal route and waits beyond each milestone. */
final class CanalEscort {
    int gate,radioTicks,serial;
    boolean waiting,leading;
    private int approach;
    private final float[] target=new float[2];
    void reset(){gate=radioTicks=serial=approach=0;waiting=leading=false;}
    void step(Game g,Body boat){
        if(radioTicks>0)radioTicks--;
        boolean open=g.tutorial.canal.authorized && g.passages.passable(0);
        if(!open){move(g,boat,TrainingChannel.MIN-220,TrainingChannel.Y[0]-95,85);return;}
        if(!leading){leading=true;radioTicks=210;serial++;g.radio.transmit(RadioAudio.TEACHER);}
        // Reach the mouth from the side of the waiting basin before entering single-file.
        if(approach==0 && boat.x<TrainingChannel.MIN-65){
            move(g,boat,TrainingChannel.MIN-65,TrainingChannel.Y[0]-70,100);
            if(boat.x>=TrainingChannel.MIN-75)approach=1;
            return;
        }
        approach=1;
        if(waiting){
            boolean reached=gate==0?g.tug.x>=TrainingChannel.X[0]-23:g.tutorial.channelGate>gate;
            if(reached && gate<TrainingChannel.X.length-1){gate++;waiting=false;radioTicks=150;serial++;g.radio.transmit(RadioAudio.TEACHER);}
        }
        float tx=TrainingChannel.X[gate]+44,ty=TrainingChannel.Y[gate];
        if(waiting){move(g,boat,tx,ty,35);return;}
        if(boat.x>=TrainingChannel.X[gate]-8 && Math.abs(boat.y-ty)<28){target[0]=tx;target[1]=ty;}
        else TrainingChannel.guide(boat.x,boat.y,Math.max(1,gate),target);
        move(g,boat,target[0],target[1],115);
        if(Physics.length(boat.x-tx,boat.y-ty)<9){waiting=true;radioTicks=180;serial++;g.radio.transmit(RadioAudio.TEACHER);}
    }
    private static void move(Game g,Body b,float x,float y,float limit){
        float dx=x-b.x,dy=y-b.y,d=Physics.length(dx,dy),wanted=(float)StrictMath.atan2(dy,dx);
        if(d>3)b.angle=Physics.angle(b.angle+Physics.clamp(Physics.angle(wanted-b.angle),-.055f,.055f));
        float speed=Math.min(limit,d*2.2f);
        // Only the instructor moves itself; a waiting player never gets displaced intentionally.
        if(Physics.touching(b,g.tug,4) && dx*(g.tug.x-b.x)+dy*(g.tug.y-b.y)>0)speed=0;
        b.vx+=(dx/Math.max(1,d)*speed-b.vx)*.16f;b.vy+=(dy/Math.max(1,d)*speed-b.vy)*.16f;
        b.engineLoad+=(Math.min(.85f,speed/140)-b.engineLoad)*.08f;
        Physics.integrate(b);CalumetMap.collide(b,g.splashes);g.passages.collide(b,g.splashes);
    }
    InstructionPanel.Content content(Game g){
        String title=!leading?"LET ME GO FIRST":waiting?"I'LL WAIT JUST PAST THE LINE":"FOLLOW ME";
        String first=waiting?"CROSS THE MILESTONE LINE BEHIND ME.":gate==0?"FOLLOW MY GREEN TUG INTO THE CANAL.":"STAY BEHIND ME THROUGH THIS REACH.";
        String second=waiting?"KEEP A LITTLE SPACE BETWEEN OUR TUGS.":gate==0?"LET ME ENTER FIRST. KEEP YOUR SPEED LOW.":"SLOW FOR EACH TURN. LIFT TO COAST.";
        return new InstructionPanel.Content("escort-"+serial+":"+waiting,title,first,second,Tutorial.CHANNEL,
                (g.tutorial.channelGate-1)/(TrainingChannel.X.length-1f),"INSTRUCTOR / GREEN TUG","RADIO / CH 08");
    }
}
