package com.bargeboss.game;

/** Fixed-step CRT show/hide and two-pass left-to-right content wipe. */
final class InstructionPanel {
    static final int OPEN_TICKS=18,CLOSE_TICKS=8,WIPE_TICKS=12;
    private static final int OPEN=0,STEADY=1,CLEAR=2,REVEAL=3,CLOSE=4,HIDDEN=5;
    private int phase=OPEN,ticks;
    private float closeFrom=1;
    private Content content,pending;
    static final class Content {
        final String key,title,first,second;final int lesson;final float progress;
        Content(String key,String title,String first,String second,int lesson,float progress){
            this.key=key;this.title=title;this.first=first;this.second=second;this.lesson=lesson;this.progress=progress;
        }
        void draw(Painter p){
            p.line(18,7,414,7,.8f,0x9de7bc73);
            Lettering.draw(p,"PRACTICE",18,18,10,PilotGlass.AMBER);
            String count=lesson<0?"ARRIVAL":(lesson+1)+" / "+Tutorial.DONE;
            Lettering.draw(p,count,414-Lettering.width(count,10),18,10,PilotGlass.MUTED);
            Lettering.draw(p,title,18,39,19,PilotGlass.TEXT);
            Lettering.draw(p,first,18,70,12,PilotGlass.TEXT);
            Lettering.draw(p,second,18,89,11,PilotGlass.MUTED);
            if(lesson==Tutorial.CHANNEL){p.rect(18,112,400,4,PilotGlass.TRACK);p.rect(18,112,400*progress,4,ControlView.GREEN);}
            else if(lesson>=0)for(int i=0;i<Tutorial.DONE;i++)p.rect(18+i*(400f/Tutorial.DONE),112,400f/Tutorial.DONE-8,4,i<lesson?ControlView.GREEN:i==lesson?PilotGlass.AMBER:PilotGlass.TRACK);
        }
    }
    void reset(){phase=OPEN;ticks=0;content=pending=null;}
    void step(Content next){
        pending=next;
        if(next==null && phase!=CLOSE && phase!=HIDDEN){closeFrom=open();phase=CLOSE;ticks=0;}
        if(content==null && next!=null)content=next;
        if(phase==STEADY && next!=null){
            if(!next.key.equals(content.key)){phase=CLEAR;ticks=0;}
            else content=next; // Live counters don't repeatedly wipe the instructions.
        }
        if(phase==OPEN && next!=null)content=next;
        ticks++;
        if(phase==OPEN && ticks>=OPEN_TICKS){phase=STEADY;ticks=0;}
        else if(phase==CLEAR && ticks>=WIPE_TICKS){content=pending;phase=REVEAL;ticks=0;}
        else if(phase==REVEAL && ticks>=WIPE_TICKS){phase=STEADY;ticks=0;}
        else if(phase==CLOSE && ticks>=CLOSE_TICKS){phase=HIDDEN;ticks=0;}
    }
    boolean hidden(){return phase==HIDDEN;}
    float open(){
        if(phase==HIDDEN)return 0;
        if(phase!=OPEN && phase!=CLOSE)return 1;
        float t=phase==CLOSE?1-ticks/(float)CLOSE_TICKS:ticks/(float)OPEN_TICKS;
        t=Physics.clamp(t,0,1);return t*t*(3-2*t)*(phase==CLOSE?closeFrom:1);
    }
    float textAlpha(){return phase==CLOSE?0:Physics.clamp((open()-.65f)/.35f,0,1);}
    float wipe(){return ticks/(float)WIPE_TICKS;}
    boolean clearing(){return phase==CLEAR;}
    boolean revealing(){return phase==REVEAL;}
    void draw(Game g,Painter p){
        float x=g.layout.width-450,y=68;
        paint(p,x,y);
        Content shown=content==null?TutorialView.instructionContent(g):content;
        if(shown==null || textAlpha()==0)return;
        boolean wipe=phase==CLEAR || phase==REVEAL;
        if(wipe){
            float edge=x+18+396*wipe();
            float left=phase==CLEAR?x:edge,right=phase==CLEAR?edge:x+432;
            // Exclude only the wiped contents; frame was already drawn. Same winding, disjoint triangles.
            p.pushClipOutside(new float[]{left,y,right,y,right,y+124,left,y,right,y+124,left,y+124});
        }
        try{shown.draw(ScreenView.offset(new OpacityPainter(p,textAlpha()),x,y));}
        finally{if(wipe)p.popClip();}
        if(wipe){
            float edge=x+18+396*wipe();
            p.line(edge,y+15,edge,y+117,4,0x1895d7d1);
            p.line(edge,y+15,edge,y+117,.8f,0x8095d7d1);
        }
    }
    void paint(Painter p,float x,float y){
        if(hidden())return;
        float amount=open(),h=Math.max(1,124*amount),cy=y+62;
        PilotGlass.panel(p,x,cy-h*.5f,432,h,PilotGlass.FILL);
        if(amount<1){
            float light=1-amount,beamY=phase==CLOSE?cy:cy-h*.5f+h*amount;
            p.line(x+9,beamY,x+423,beamY,7,SoftCue.alpha(0x2095d7d1,light));
            p.line(x+9,beamY,x+423,beamY,3,SoftCue.alpha(0x5595d7d1,light));
            p.line(x+9,beamY,x+423,beamY,.8f,SoftCue.alpha(0xc0d9e7e2,light));
        }
    }
}
