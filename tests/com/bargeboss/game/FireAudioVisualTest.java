package com.bargeboss.game;
/** Real rocket/contact timing, bounded shared audio policy and deterministic fire lighting. */
public final class FireAudioVisualTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(){
        Game g=new Game();g.resetPracticeYard();g.mode=Game.PLAY;g.tutorial=new Tutorial();
        Body b=g.cargo[0]=new Body(1,2,300,700,0);b.ability=BargeAbilities.FIREWORKS;b.abilityTicks=24;
        g.abilities.step(g);
        check(SoundEffects.cue(g)==SoundEffects.FIREWORK_LAUNCH && g.abilities.screenFlashTicks()==0,"launch sounds without premature burst/flash");
        for(int i=0;i<53;i++){g.sounds.step();g.abilities.step(g);}
        check(SoundEffects.cue(g)==SoundEffects.FIREWORK_BURST && g.abilities.screenFlashTicks()==8,"burst sound and flash coincide with actual rocket detonation");
        for(int i=0;i<8;i++){g.sounds.step();g.abilities.step(g);}
        check(g.abilities.screenFlashTicks()==0 && !SoundEffects.audible(g),"flash decays and delayed burst events expire");
        for(int i=0;i<6;i++){g.sounds.fireworkBurst();check(SoundEffects.cue(g)==SoundEffects.FIREWORK_BURST+(i+1)%3,"burst variants cycle deterministically");}
        for(int incoming=0;incoming<SoundEffects.COUNT;incoming++)for(int playing=0;playing<SoundEffects.COUNT;playing++)
            check(SoundEffects.interrupts(incoming,playing)==(incoming==playing || incoming<9 || playing<9),"bounded shared overlap priority");
        FireworksLesson f=g.tutorial.fireworks;g.tug.x=FireworksLesson.X-100;g.tug.y=FireworksLesson.Y;
        check(EngineAudio.gain(g,EngineAudio.FIRE)==0 && EngineAudio.gain(g,EngineAudio.SIZZLE)==0,"no imaginary fire audio");
        f.ignite();
        float roar=EngineAudio.gain(g,EngineAudio.FIRE);check(roar>0,"burning timber has local roar");
        check(!f.waterHit(0,0) && EngineAudio.gain(g,EngineAudio.SIZZLE)==0,"missed spray cannot sizzle");
        check(f.waterHit(FireworksLesson.X,FireworksLesson.Y) && EngineAudio.gain(g,EngineAudio.SIZZLE)>0,"real water hit activates sizzle");
        g.radio.transmit(RadioAudio.TEACHER);check(EngineAudio.gain(g,EngineAudio.FIRE)<roar*.3f,"radio ducks fire");
        for(int i=0;i<24;i++)f.step(g);check(EngineAudio.gain(g,EngineAudio.SIZZLE)==0,"sizzle fades after water stops");
        int first=FireLight.color(g);boolean varied=false;
        for(int i=0;i<180;i++){g.visualTicks=i;int c=FireLight.color(g);varied|=c!=first;check((c>>16&255)==255 && (c>>8&255)>=82 && (c&255)>=23,"palette stays within warm fire colors");}
        check(varied,"fire light changes color");
        FireLight light=new FireLight(g);check(light.shade(0xff101010,0xff808080,FireworksLesson.X,FireworksLesson.Y,20,0,0,1)!=0xff101010,"timber receives real colored light");
        f.heat=0;check(FireLight.energy(g)==0 && EngineAudio.gain(g,EngineAudio.FIRE)==0,"extinguished fire has neither glow nor roar");
        g.mode=Game.PAUSE;for(int i=0;i<EngineAudio.LAYERS;i++)check(EngineAudio.gain(g,i)==0,"pause silences every loop");
        g.abilities.reset();check(g.abilities.screenFlashTicks()==0,"reset clears exposure pulse");
        System.out.println("Firework burst/flash timing, audio overlap, fire/sizzle lifecycle and colored receivers passed");
    }
    public static void main(String[] args){run();}
}
