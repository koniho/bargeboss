package com.bargeboss.game;
public final class TutorialEquipmentTimingTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void run(){
        Game root=new Game();root.configureCanvas(1243,600);ReplayInput.startPractice(root);Game g=root.activeGame();
        for(int lesson=Tutorial.DOUBLE;lesson<=Tutorial.RELEASE;lesson++){
            g.tutorial.lesson=lesson;g.tutorial.setup(g);g.abilities.reveal(g,g.cargo[0]);
            InstructionPanel.Content content=TutorialView.instructionContent(g);
            check(!content.key.startsWith("ability-") && !content.second.contains("SHORT TAP FOR ABILITIES"),"tow instructions stay primary before fireworks: "+lesson);
        }
        g.tutorial.lesson=Tutorial.SHOW;g.tutorial.setup(g);
        check(TutorialView.instructionContent(g).first.contains("TAP THE FIREWORKS BARGE"),"fireworks instruction appears in celebration basin");
        g.tutorial.lesson=Tutorial.DOUBLE;g.tutorial.setup(g);
        g.layout=Layout.focused(1243,600,300,100,1.4f,621,300);
        g.lock.passive=false;g.lock.phase=Lock.READY;g.lock.ticks=0;int closed=Parity.frame(g);
        g.lock.phase=Lock.EXIT;g.lock.ticks=Lock.GATE_TICKS;int opened=Parity.frame(g);
        check(closed==opened,"tutorial render contains no legacy lock doors or water effects");
        g.bridge.phase=Bridge.CLOSED;int bridgeClosed=Parity.frame(g);
        g.bridge.phase=Bridge.OPEN;check(bridgeClosed==Parity.frame(g),"tutorial has no obsolete bridge between tow bridges");
        check(g.passages.bridges.length>=3,"real tutorial bridges remain");
        System.out.println("Tutorial tow coaching, delayed equipment prompt and absent lock rendering passed");
    }
    public static void main(String[]args){run();}
}
