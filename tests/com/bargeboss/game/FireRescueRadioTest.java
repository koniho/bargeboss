package com.bargeboss.game;

public final class FireRescueRadioTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(){
        for(int answer=0;answer<3;answer++){
            Game root=new Game();ReplayInput.startPractice(root);Game g=root.activeGame();
            g.tutorial.lesson=Tutorial.EXTINGUISH;g.tutorial.setup(g);
            FireworksLesson f=g.tutorial.fireworks;
            g.navigating=true;
            for(int i=0;i<360;i++){g.radio.step();f.rescueRadio.step(g);}
            check(f.rescueRadio.content(g,"KEEP SPRAYING")!=null,"urgent calls appear after initial breathing room");
            check(!g.dialogue.active && g.navigating,"urgent chatter does not seize controls");
            int sequence=RadioAudio.sequence(g);
            for(int i=0;i<600;i++){g.radio.step();f.rescueRadio.step(g);}
            check(RadioAudio.sequence(g)==sequence,"urgent calls are spaced apart");
            f.heat=0;f.done=true;
            for(int i=0;i<250;i++){g.radio.step();g.dialogue.step();f.rescueRadio.step(g);}
            check(g.dialogue.ready() && g.dialogue.count()==3,"relief leads to three player replies");
            for(int i=0;i<600;i++)f.rescueRadio.step(g);
            check(!f.rescueRadio.complete(),"relief conversation waits for player reply");
            Layout.Box b=g.dialogue.choiceBox(g,answer);g.dialogue.down(g,93,b.centerX(),b.centerY());g.dialogue.up(g,93,b.centerX(),b.centerY());
            for(int i=0;i<45;i++)f.rescueRadio.step(g);
            check(!g.dialogue.active && !f.rescueRadio.complete(),"reply gets a final instructor transmission");
            check(f.rescueRadio.content(g,"")!=null,"final response is visible");
            for(int i=0;i<240;i++)f.rescueRadio.step(g);
            check(f.rescueRadio.complete(),"every reply allows completion");
            f.reset();check(!f.rescueRadio.complete(),"reset clears relief state");
        }
        System.out.println("Spaced nonblocking emergency radio and all three relief replies passed");
    }
    public static void main(String[] args){run();}
}
