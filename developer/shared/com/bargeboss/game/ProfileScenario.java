package com.bargeboss.game;

/** Debug-only fixtures reached through real tutorial input; no alternate gameplay implementation. */
public final class ProfileScenario {
    public static final String[] NAMES={"indiana-harbor","indiana-hick","splash-impact","basin-entry","canal-entry","canal-middle","barge-arrival","barge-push","harbor-close","barge-tow","barge-bridge","montrose-inlet","montrose-docks","montrose-overview","montrose-green-shore"};
    private ProfileScenario(){}
    public static void prepare(Game root,String name,float width,float height) {
        boolean valid=false;for(String n:NAMES)if(n.equals(name))valid=true;
        if(!valid)throw new IllegalArgumentException("Unknown profile scene: "+name);
        root.configureCanvas(width,height);
        if(name.startsWith("indiana-")){
            root.selectChallenge(Challenges.INDIANA);root.reset();
            if(name.equals("indiana-hick")){root.tug.x=375;root.tug.y=430;root.cargo[0].x=375;root.cargo[0].y=320;}
            root.layout=Layout.focused(width,height,root.tug.x,root.tug.y,1.4f,width*.5f,height*.52f);return;
        }
        if(name.equals("splash-impact")){
            root.selectChallenge(0);root.reset();for(Feeder f:root.feeders){f.phase=Feeder.IDLE;f.load=null;}
            for(int i=0;i<root.cargo.length;i++)root.cargo[i]=null;
            root.tug.x=270;root.tug.y=500;root.tug.angle=0;root.tug.vx=65;root.addCargo(0,330,500);root.cargo[0].angle=0;
            for(int tick=0;tick<16;tick++)root.step();
            root.layout=Layout.focused(width,height,300,500,3.2f,width*.5f,height*.58f);
            if(root.splashes.emitted==0)throw new AssertionError("Impact fixture missed contact");return;
        }
        if(name.startsWith("montrose-")){
            root.selectChallenge(Challenges.MONTROSE);root.reset();
            if(name.equals("montrose-docks")){
                for(int tick=0;tick<18000;tick++){
                    if(Physics.length(root.tug.x-MontroseHarbor.SLIP_X,root.tug.y-MontroseHarbor.SLIP_Y)<200)break;
                    ReplayInput.work(root);root.step();
                }
            }else for(int tick=0;tick<120;tick++)root.step();
            if(name.equals("montrose-overview"))root.layout=MontroseHarbor.overview(width,height);
            if(name.equals("montrose-green-shore"))root.layout=Layout.focused(width,height,MontroseHarbor.mapX(275),MontroseHarbor.mapY(1250),2.4f,width*.5f,height*.53f);
            return;
        }
        root.startTutorial();Game g=root.activeGame();
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
        else if(g.montroseStage())ReplayInput.work(g);
        root.step();
    }
}
