package com.bargeboss.game;

/** Six-fire red wash and smoke columns: bounded, scaled by heat, steam while cooled, gone when out. */
public final class FireWashTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static final class Capture implements Painter {
        int glows,soot,steam;float glowAmount,sootAlpha;
        public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){
            if(kind==EffectMaterial.GLOW){glows++;glowAmount+=amount*(color>>>24)/255f;check(((color>>16)&255)==255 && ((color>>8)&255)<120,"wash stays warm red");}}
        public void smokePuff(float x,float y,float radius,float phase,int color){soot++;sootAlpha+=(color>>>24)/255f;check((color&0xffffff)==SmokePuff.SOOT,"burning plume is dark soot");}
        public void layer(StaticLayer layer,float x,float y,float scale,float alpha){if(layer==SteamPuff.LAYER)steam++;}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int n,float x,float y,float w,float h,float r,float m,float alpha){}
        public void rect(float x,float y,float w,float h,int c){} public void circle(float x,float y,float r,int c){} public void poly(float[] xy,int c){} public void line(float x,float y,float xx,float yy,float w,int c){}
    }
    private static Capture draw(Game g){Capture p=new Capture();g.tutorial.fireworks.wash(g,p);g.tutorial.fireworks.plumes(g,p);return p;}
    /** Wide camera that keeps all six fires and their plumes on screen. */
    private static void overview(Game g){g.layout=Layout.focused(1333,600,130,-1340,.55f,666,300);}
    public static void run(){
        Game root=new Game();root.configureCanvas(1333,600);ReplayInput.startPractice(root);Game g=root.activeGame();
        g.tutorial.lesson=Tutorial.EXTINGUISH;g.tutorial.setup(g);FireworksLesson f=g.tutorial.fireworks;
        for(int i=0;i<300;i++)f.step(g);overview(g);
        Capture full=draw(g);
        check(full.glows==FireworksLesson.GROUPS,"one wide wash quad per burning quay group, no more");
        check(full.soot>FireworksLesson.PILES*FireworksLesson.SMOKE_PUFFS/2 && full.soot<=FireworksLesson.PILES*FireworksLesson.SMOKE_PUFFS,"dense but bounded soot columns from every fire");
        check(full.steam==0,"dry fires make no steam");
        for(int pile=0;pile<FireworksLesson.PILES;pile++)check(f.intensity(g,pile)>.7f,"full heat burns at high intensity");
        // Cool pile 0 halfway: its wash and plume thin while its neighbours stay full.
        float before=f.intensity(g,0);
        for(int i=0;i<FireworksLesson.PILE_HEAT/2;i++)f.waterHit(FireworksLesson.x(0)+FireworksLesson.FOOT_OFFSET,FireworksLesson.y(0));
        check(f.intensity(g,0)<before*.7f && f.intensity(g,1)>.7f,"tint scales with each fire's own heat");
        // Water landing turns newly born puffs to steam.
        for(int i=0;i<FireworksLesson.SMOKE_LIFE;i++){f.waterHit(FireworksLesson.x(0)+FireworksLesson.FOOT_OFFSET,FireworksLesson.y(0));f.step(g);if(f.heat(0)==0)break;
            if(i==60){Capture wet=draw(g);check(wet.steam>0 && wet.soot<full.soot,"cooled fire's plume turns to steam");}}
        while(f.heat(0)>0)f.waterHit(FireworksLesson.x(0)+FireworksLesson.FOOT_OFFSET,FireworksLesson.y(0));
        check(f.remaining()==5,"one fire out");
        Capture one=draw(g);check(one.glows==FireworksLesson.GROUPS && one.glowAmount<full.glowAmount,"the extinguished fire's wash fades through its steam tail");
        for(int i=0;i<FireworksLesson.STEAM_TICKS+1;i++)f.step(g);
        Capture after=draw(g);check(after.glows==FireworksLesson.GROUPS && after.glowAmount<one.glowAmount,"the out fire stops contributing once its steam tail ends");
        for(int i=0;i<FireworksLesson.PILE_HEAT;i++)f.waterHit(FireworksLesson.x(1)+FireworksLesson.FOOT_OFFSET,FireworksLesson.y(1));
        for(int i=0;i<FireworksLesson.STEAM_TICKS+1;i++)f.step(g);
        check(draw(g).glows==FireworksLesson.GROUPS-1,"a fully cooled quay group stops its wash");
        check(f.intensity(g,0)==0,"out fire has no intensity");
        for(int pile=2;pile<FireworksLesson.PILES;pile++)while(f.heat(pile)>0)f.waterHit(FireworksLesson.x(pile)+FireworksLesson.FOOT_OFFSET,FireworksLesson.y(pile));
        for(int i=0;i<FireworksLesson.STEAM_TICKS+1;i++)f.step(g);
        Capture out=draw(g);check(f.done && out.glows==0 && out.soot==0 && out.steam==0,"no tint and no smoke once every fire is out");
        System.out.println("Six-fire wash/smoke bounds, heat scaling, steam conversion and shutdown passed");
    }
    public static void main(String[] args){run();}
}
