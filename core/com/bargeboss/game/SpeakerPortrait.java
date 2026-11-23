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
    static int atlas(Game g,String header){
        if(g.montroseStage() && header.startsWith("ALEX /"))return SpriteAtlas.ALEX_PORTRAIT;
        if(g.montroseStage() && (g.dialogue.crewConversation() || header.startsWith("CHAS /")))return SpriteAtlas.CHAS_PORTRAIT;
        if(g.mode==Game.PLAY && g.montroseStage() && g.dialogue.dockmasterConversation())return SpriteAtlas.GAVIN_PORTRAIT;
        if(g.mode!=Game.PLAY || g.tutorial==null)return -1;
        if(g.tutorial.entering && g.tutorial.entranceTicks<TutorialArrival.APPROACH)return -1;
        boolean bridge=g.dialogue.active?g.dialogue.bridgeConversation():header.startsWith("BRIDGE") || header.startsWith("TOW BRIDGE");
        if(bridge){
            int gate=g.tutorial.towBridges.current(g);
            if(gate==0)gate=g.tutorial.towBridges.calling;
            return gate==TutorialTowBridges.ENTRY?SpriteAtlas.BRIDGE_OPERATOR_NIGHT:SpriteAtlas.BRIDGE_OPERATOR_STEEL;
        }
        boolean teacher=g.dialogue.active?g.dialogue.instructorConversation():header.startsWith("INSTRUCTOR");
        return teacher && g.teacherBoat()!=null?SpriteAtlas.INSTRUCTOR_PORTRAIT:-1;
    }
    static boolean visible(Game g,String header){return atlas(g,header)>=0;}
    static float facing(float center,float screenWidth){return center>screenWidth*.5f?-1:1;}
    static void draw(Game g,Painter p,float panelX,float panelY,float panelOpen,String header){
        if(!visible(g,header) || panelOpen<=0)return;
        int portrait=atlas(g,header);
        boolean talking=portrait==SpriteAtlas.INSTRUCTOR_PORTRAIT?
                g.radio.transmitter()==RadioAudio.TEACHER && g.radio.transmitting(g,g.teacherBoat()):
                g.radio.transmitter()==((portrait==SpriteAtlas.CHAS_PORTRAIT || portrait==SpriteAtlas.ALEX_PORTRAIT)?RadioAudio.CREW:portrait==SpriteAtlas.GAVIN_PORTRAIT?RadioAudio.DOCKMASTER:RadioAudio.BRIDGE) && g.radio.progress()<1;
        float progress=g.radio.progress();
        float size=100,cx=panelX+488,cy=panelY+62;
        Painter ink=new OpacityPainter(p,panelOpen);
        ink.line(panelX+432,panelY+18,panelX+432,panelY+106,1,0x447e939f);
        ink.spriteAffine(portrait,talking?frame(Math.round(progress*54)):0,cx,cy-4,
                -size,0,0,size,1);
        if(talking)for(int i=0;i<7;i++){
            float h=2+5*(.5f+.5f*(float)StrictMath.sin(progress*38+i*1.8));
            ink.rect(cx-22+i*7,cy+49-h,4,h,PilotGlass.AMBER);
        }
    }
    private SpeakerPortrait(){}
}
