package com.bargeboss.game;

/** Screen-space radio portrait. Source art faces right; shared affine flip faces center. */
final class SpeakerPortrait {
    // Authored GIF delays, converted to 60-Hz ticks (23,12,15,13,18,20 centiseconds).
    private static final int[] END={14,21,30,38,49,61};
    static int frame(int tick){
        int t=Math.max(0,tick)%61;
        for(int i=0;i<END.length;i++)if(t<END[i])return i;
        return 0;
    }
    static boolean visible(Game g){
        return g.mode==Game.PLAY && g.teacherBoat()!=null
                && g.radio.transmitter()==RadioAudio.TEACHER && g.radio.transmitting(g,g.teacherBoat());
    }
    static float facing(float center,float screenWidth){return center>screenWidth*.5f?-1:1;}
    static void draw(Game g,Painter p,float panelX,float panelY,float panelOpen){
        if(!visible(g))return;
        float progress=g.radio.progress(),fade=Math.min(1,Math.min(progress*10,(1-progress)*8))*panelOpen;
        if(fade<=0)return;
        // Reserve the outer right edge below the fixed instruction card.
        // Never place the speaker between the card and the screen center.
        float size=92,cx=g.layout.width-68,cy=panelY+124+16+56;
        Painter ink=new OpacityPainter(p,fade);
        PilotGlass.panel(ink,cx-50,cy-56,100,112,PilotGlass.FILL);
        ink.spriteAffine(SpriteAtlas.INSTRUCTOR_PORTRAIT,frame(Math.round(progress*54)),cx,cy-4,
                size*facing(cx,g.layout.width),0,0,size,1);
        // A short transmission meter animates with the same clock as the mouth.
        for(int i=0;i<7;i++){
            float h=2+5*(.5f+.5f*(float)StrictMath.sin(progress*38+i*1.8));
            ink.rect(cx-22+i*7,cy+49-h,4,h,PilotGlass.AMBER);
        }
    }
    private SpeakerPortrait(){}
}
