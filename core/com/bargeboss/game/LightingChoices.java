package com.bargeboss.game;

/** Next-entry preset selection; never recolors a live voyage. */
final class LightingChoices {
    private LightingChoices(){}
    static Layout.Box box(Game g,int index){
        boolean title=g.mode==Game.TITLE;
        float x=title?g.layout.width-960+632:g.layout.offsetX+232;
        float y=(title?294:467)+g.layout.offsetY,w=title?69:124;
        return new Layout.Box(x+index*w,y,w-4,30);
    }
    static boolean tap(Game g,float x,float y){
        if(g.mode!=Game.TITLE && g.mode!=Game.CHALLENGES)return false;
        for(int i=0;i<4;i++){Layout.Box b=box(g,i);if(b.contains(x,y) && b.contains(g.downX,g.downY)){g.selectLighting(i);g.haptic(Game.HAPTIC_TICK);return true;}}
        return false;
    }
    static void draw(Game g,Painter p){
        for(int i=0;i<4;i++){
            Layout.Box b=box(g,i);boolean selected=g.selectedLighting()==i;
            PilotGlass.panel(p,b.x,b.y,b.w,b.h,selected?0xe039545b:0xb00b1c29);
            if(selected)p.line(b.x+7,b.y+b.h-2,b.x+b.w-7,b.y+b.h-2,1,VisualStyle.GOLD);
            Lettering.center(p,LightingProfile.preset(i).name,b.centerX(),b.y+10,g.mode==Game.TITLE?7:10,selected?VisualStyle.GOLD:VisualStyle.CREAM);
        }
    }
}
