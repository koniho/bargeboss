package com.bargeboss.game;

/** Fixed-step CRT show/hide and two-pass left-to-right content wipe. */
final class InstructionPanel {
    static final int OPEN_TICKS=18,CLOSE_TICKS=8,WIPE_TICKS=12,DROP_TICKS=8,RETURN_TICKS=12,RISE_TICKS=8,SHIMMER_TICKS=24;
    private static final int OPEN=0,STEADY=1,CLEAR=2,REVEAL=3,CLOSE=4,HIDDEN=5,DROP=6,RETURN=7,RISE=8;
    private int phase=OPEN,ticks,shimmerTicks;
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
    void reset(){phase=OPEN;ticks=shimmerTicks=0;content=pending=null;}
    void step(Content next){
        pending=next;
        if(shimmerTicks>0)shimmerTicks--;
        if(next==null && phase!=CLOSE && phase!=HIDDEN){closeFrom=open();phase=CLOSE;ticks=shimmerTicks=0;}
        if(content==null && next!=null)content=next;
        if(phase==STEADY && next!=null){
            if(!next.key.equals(content.key)){phase=CLEAR;ticks=shimmerTicks=0;}
            else content=next; // Live counters don't repeatedly wipe the instructions.
        }
        if(phase==OPEN && next!=null)content=next;
        ticks++;
        if(phase==OPEN && ticks>=OPEN_TICKS){phase=STEADY;ticks=0;shimmerTicks=SHIMMER_TICKS;}
        else if(phase==CLEAR && ticks>=WIPE_TICKS){phase=DROP;ticks=0;}
        else if(phase==DROP && ticks>=DROP_TICKS){phase=RETURN;ticks=0;}
        else if(phase==RETURN && ticks>=RETURN_TICKS){phase=RISE;ticks=0;}
        else if(phase==RISE && ticks>=RISE_TICKS){content=pending;phase=REVEAL;ticks=0;}
        else if(phase==REVEAL && ticks>=WIPE_TICKS){phase=STEADY;ticks=0;shimmerTicks=SHIMMER_TICKS;}
        else if(phase==CLOSE && ticks>=CLOSE_TICKS){phase=HIDDEN;ticks=0;}
    }
    boolean hidden(){return phase==HIDDEN;}
    float open(){
        if(phase==HIDDEN)return 0;
        if(phase!=OPEN && phase!=CLOSE)return 1;
        float t=phase==CLOSE?1-ticks/(float)CLOSE_TICKS:ticks/(float)OPEN_TICKS;
        t=Physics.clamp(t,0,1);return t*t*(3-2*t)*(phase==CLOSE?closeFrom:1);
    }
    float textAlpha(){return phase==CLOSE || phase==DROP || phase==RETURN || phase==RISE?0:Physics.clamp((open()-.65f)/.35f,0,1);}
    float wipe(){return ticks/(float)WIPE_TICKS;}
    boolean clearing(){return phase==CLEAR;}
    boolean revealing(){return phase==REVEAL;}
    boolean dropping(){return phase==DROP;}
    boolean returning(){return phase==RETURN;}
    boolean rising(){return phase==RISE;}
    boolean shimmering(){return shimmerTicks>0;}
    private static float ease(float t){t=Physics.clamp(t,0,1);return t*t*(3-2*t);}
    float starX(){
        if(phase==CLEAR || phase==REVEAL)return 18+396*wipe();
        if(phase==RETURN)return 414-396*ease(ticks/(float)RETURN_TICKS);
        if(phase==RISE)return 18;
        return 414;
    }
    float starY(){
        if(phase==DROP)return 39+65*ease(ticks/(float)DROP_TICKS);
        if(phase==RETURN)return 104;
        if(phase==RISE)return 104-65*ease(ticks/(float)RISE_TICKS);
        return 39;
    }
    float lineHeight(){
        return phase==CLEAR || phase==REVEAL || phase==DROP || phase==RISE?104-starY():0;
    }
    private void wipeLine(Painter p,float x,float y){
        float height=lineHeight();if(height<=0)return;
        float xx=x+starX(),top=y+starY(),bottom=y+104;
        float pulse=.8f+.2f*(float)StrictMath.sin(ticks*.5f);
        p.line(xx,top,xx,bottom,6,SoftCue.alpha(0x1895d7d1,pulse));
        p.line(xx,top,xx,bottom,2.5f,SoftCue.alpha(0x4095d7d1,pulse));
        p.line(xx,top,xx,bottom,.8f,SoftCue.alpha(0xa0c4e9e5,pulse));
        float glint=(.5f+.5f*(float)StrictMath.sin(ticks*.55f))*height;
        p.line(xx,Math.max(top,top+glint-5),xx,Math.min(bottom,top+glint+5),1,0x99edf8ea);
    }
    private void sparkle(Painter p,float x,float y){
        boolean moving=phase==CLEAR || phase==DROP || phase==RETURN || phase==RISE || phase==REVEAL;
        float fade=moving?1:shimmerTicks>SHIMMER_TICKS-6?(shimmerTicks-(SHIMMER_TICKS-6))/6f:0;
        if(fade<=0)return;
        float radius=4+.65f*(float)StrictMath.sin(ticks*.65f);
        float angle=.18f*(float)StrictMath.sin(ticks*.4f),cx=x+starX(),cy=y+starY();
        p.circle(cx,cy,8,SoftCue.alpha(0x1395d7d1,fade));
        p.circle(cx,cy,4,SoftCue.alpha(0x3095d7d1,fade));
        for(int i=0;i<2;i++){
            float a=angle+i*(float)StrictMath.PI/2,dx=radius*(float)StrictMath.cos(a),dy=radius*(float)StrictMath.sin(a);
            p.line(cx-dx,cy-dy,cx+dx,cy+dy,2.8f,SoftCue.alpha(0x4095d7d1,fade));
            p.line(cx-dx,cy-dy,cx+dx,cy+dy,.8f,SoftCue.alpha(0xe0edf8ea,fade));
        }
    }
    private void shimmer(Painter p,float x,float y){
        if(shimmerTicks<=0)return;
        float t=1-shimmerTicks/(float)SHIMMER_TICKS,center=9+414*t;
        float alpha=(float)StrictMath.sin(t*StrictMath.PI);
        for(int i=2;i>=0;i--){
            float half=4+i*6;
            p.poly(new float[]{x+Physics.clamp(center-half+16,9,423),y+10,x+Physics.clamp(center+half+16,9,423),y+10,
                    x+Physics.clamp(center+half-16,9,423),y+114,x+Physics.clamp(center-half-16,9,423),y+114},SoftCue.alpha(0x0795d7d1,alpha));
        }
    }
    void draw(Game g,Painter p){
        float x=g.layout.width-450,y=68;
        paint(p,x,y);
        Content shown=content==null?TutorialView.instructionContent(g):content;
        if(shown==null || hidden())return;
        boolean wipe=phase==CLEAR || phase==REVEAL;
        if(wipe){
            float edge=x+18+396*wipe();
            float left=phase==CLEAR?x:edge,right=phase==CLEAR?edge:x+432;
            // Exclude only the wiped contents; frame was already drawn. Same winding, disjoint triangles.
            p.pushClipOutside(new float[]{left,y,right,y,right,y+124,left,y,right,y+124,left,y+124});
        }
        try{if(textAlpha()>0)shown.draw(ScreenView.offset(new OpacityPainter(p,textAlpha()),x,y));}
        finally{if(wipe)p.popClip();}
        wipeLine(p,x,y);
        shimmer(p,x,y);
        sparkle(p,x,y);
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
