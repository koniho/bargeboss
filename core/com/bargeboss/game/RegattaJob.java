package com.bargeboss.game;

/** REGATTA RUN: the regatta needs its course proven before the racers go. Thread ten gates in order:
 * down Montrose Beach past the swimmers, round the breakwater head, and back down the lakefront to the
 * harbor mouth. The clock starts at gate one; each gate adds time and the leftover seconds pay out. */
final class RegattaJob extends MontroseJob {
    /** Gate centres in course order (all on verified traffic lanes) and their half-width. */
    static final float[] GATES={0,-3760,500,-3360,1000,-3010,1520,-2730,2150,-2520,2250,-3150,2550,-3980,3150,-3250,3120,-1700,2350,450};
    static final float HALF=70;
    static final int START_SECONDS=45,GATE_SECONDS=9;
    int next;

    void setup(Game g){
        limit=START_SECONDS*60;
        start(g,-200,-4000,(float)StrictMath.atan2(GATES[1]-(-4000),GATES[0]-(-200)));
    }
    int gates(){return GATES.length/2;}
    void objective(Game g,Painter canvas){if(next<gates())arrow(g,canvas,GATES[next*2],GATES[next*2+1],"GATE "+(next+1));}
    String status(Game g){return next>=gates()?"COURSE PROVEN!":next==0?"HEAD TO GATE 1 / THE CLOCK STARTS THERE":"GATE "+(next+1)+" OF "+gates()+" / THREAD THE BUOYS";}
    boolean failed(Game g){return next<gates() && ticks>=limit;}
    void step(Game g){
        super.step(g);
        if(next==0)ticks=0;
        if(next>=gates() || ticks>=limit)return;
        float x=GATES[next*2],y=GATES[next*2+1];
        if(Physics.length(g.tug.x-x,g.tug.y-y)>HALF)return;
        next++;limit+=GATE_SECONDS*60;
        if(next<gates()){score(g,x,y,200,"GATE "+next+" +"+GATE_SECONDS+"s");return;}
        int bonus=secondsLeft()*50;
        score(g,x,y,500+bonus,"FINISH! TIME BONUS +"+bonus);
        pop(x,y+40,"FINISH!");
    }
    void water(Game g,Painter p){
        RiverCamera c=g.layout.camera;
        for(int i=next;i<Math.min(gates(),next+3);i++){
            float x=GATES[i*2],y=GATES[i*2+1];
            // Gate axis: across the course direction, between two buoys.
            int from=Math.max(0,i-1),to=Math.min(gates()-1,i+1);
            float dx=GATES[to*2]-GATES[from*2],dy=GATES[to*2+1]-GATES[from*2+1],d=Math.max(1,Physics.length(dx,dy)),nx=-dy/d*HALF,ny=dx/d*HALF;
            boolean live=i==next;float strength=live?1:.35f;int color=live?0xffffd166:0xff8fd0ff;
            float ax=c.screenX(x+nx,y+ny),ay=c.screenY(x+nx,y+ny),bx=c.screenX(x-nx,y-ny),by=c.screenY(x-nx,y-ny);
            if(Math.max(ax,bx)<-60 || Math.min(ax,bx)>g.layout.width+60 || Math.max(ay,by)<-60 || Math.min(ay,by)>g.layout.height+60)continue;
            float shimmer=.5f+.5f*(float)StrictMath.sin(g.visualTicks*.2f+i);
            p.line(ax,ay,bx,by,(live?3:1.5f)*c.scale,SoftCue.alpha(color,(.35f+.35f*shimmer)*strength));
            for(int side=0;side<2;side++){
                float sx=side==0?ax:bx,sy=side==0?ay:by;
                p.material(sx,sy,16*c.scale,0,0,14*c.scale,EffectMaterial.GLOW,0,1,SoftCue.alpha(color,.8f*strength));
                p.material(sx,sy-6*c.scale,6*c.scale,0,0,10*c.scale,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffffffff,.9f*strength));
            }
            if(live)Lettering.center(p,""+(i+1),(ax+bx)*.5f,(ay+by)*.5f-14*c.scale,Physics.clamp(12*c.scale,13,18),0xffffe27a);
        }
    }
}
