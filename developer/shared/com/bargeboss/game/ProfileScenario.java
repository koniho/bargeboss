package com.bargeboss.game;

/** Debug-only fixtures reached through real tutorial input; no alternate gameplay implementation. */
public final class ProfileScenario {
    public static final String[] NAMES={"basin-entry","canal-entry","canal-middle","barge-arrival","barge-push","harbor-close","barge-tow","barge-bridge"};
    private ProfileScenario(){}
    public static void prepare(Game root,String name,float width,float height) {
        boolean valid=false;for(String n:NAMES)if(n.equals(name))valid=true;
        if(!valid)throw new IllegalArgumentException("Unknown profile scene: "+name);
        root.configureCanvas(width,height);root.startTutorial();Game g=root.activeGame();
        int channelTicks=0;
        for(int tick=0;tick<24000;tick++) {
            int lesson=g.tutorial.lesson;
            if(lesson==Tutorial.CHANNEL)channelTicks++;
            if(name.equals("basin-entry")
                    || name.equals("canal-entry") && channelTicks==1
                    || name.equals("canal-middle") && channelTicks==700
                    || name.equals("barge-arrival") && lesson==Tutorial.HOLD
                    || name.equals("barge-push") && lesson==Tutorial.HOLD && g.pushing && g.manualSteering && Physics.touching(g.tug,g.cargo[0],5)
                    || name.equals("harbor-close") && lesson==Tutorial.STEER
                    || name.equals("barge-tow") && lesson==Tutorial.PULL
                    || name.equals("barge-bridge") && lesson==Tutorial.BRIDGE)return;
            ReplayInput.tutorial(root);root.step();
        }
        throw new IllegalStateException("Profile fixture not reached: "+name);
    }
    public static void replayTick(Game root) {
        Game g=root.activeGame();
        if(g.tutorial!=null && g.tutorial.lesson<Tutorial.DONE)ReplayInput.tutorial(root);
        root.step();
    }
}
