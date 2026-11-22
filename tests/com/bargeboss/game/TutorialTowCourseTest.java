package com.bargeboss.game;

/** A whole tow must clear each obstacle, with actual player-owned reconfiguration. */
public final class TutorialTowCourseTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static Game exercise(int lesson,float width){Game g=new Game();g.configureCanvas(width,600);g.startTutorial();g=g.activeGame();g.tutorial.lesson=lesson;g.tutorial.setup(g);return g;}
    public static void run(){
        closePair();
        gates();
        bridgeBounds();
        joystickCourse();
        for(float width:new float[]{960,1333,1680}){
            Game g=exercise(Tutorial.CONNECT,width);Body a=g.cargo[0],b=g.cargo[2],pair=g.cargo[1],tug=g.tug;int seen=0,releaseAtBridge=-1;
            for(int tick=0;tick<8000 && g.tutorial.lesson!=Tutorial.DONE;tick++){
                int lesson=g.tutorial.lesson;seen|=1<<lesson;float ax=a.x,ay=a.y,bx=b.x,by=b.y;
                ReplayInput.tutorial(g);g.step();
                check((g.cargo[0]==a || g.cargo[1]==a) && (g.cargo[0]==pair || g.cargo[1]==pair) && g.cargo[2]==b && g.tug==tug,"all towing lessons preserve the same vessels");
                check(Physics.length(a.x-ax,a.y-ay)<8 && Physics.length(b.x-bx,b.y-by)<8,"course never teleports cargo at lesson changes");
                if(g.tutorial.lesson!=lesson){
                    if(lesson==Tutorial.PULL)check(Physics.length(a.x-Tutorial.TOW_BERTH_X,a.y-Tutorial.TOW_BERTH_Y)<=Tutorial.TOW_BERTH_RADIUS,"single tow reaches the berth before multi-tow lessons");
                    if(lesson==Tutorial.DOUBLE)check(Tutorial.doubleTow(g),"bridge exercise starts with two independent tug connections");
                    if(lesson==Tutorial.BRIDGE){check(Tutorial.doubleTow(g) && Tutorial.allClear(g,Tutorial.BRIDGE_CLEAR_Y) && g.tutorial.bridgeCrossed==3,"both whole hulls physically clear bridge with direct tug connections");releaseAtBridge=g.towReleases;}
                    if(lesson==Tutorial.LINK)check(Tutorial.chain(g) && g.towReleases>releaseAtBridge,"player actually releases and re-rigs one of the same two loads into a train");
                    if(lesson==Tutorial.TRAIN)check(Tutorial.chain(g) && Tutorial.allClear(g,Tutorial.TRAIN_CLEAR_Y) && g.tutorial.trainCrossed==3,"channel completion waits for tug and both complete hulls");
                }
            }
            check(g.tutorial.lesson==Tutorial.DONE && seen==((1<<Tutorial.DONE)-(1<<Tutorial.CONNECT)),"public touch gestures complete every tow lesson at width "+width+"; lesson="+g.tutorial.lesson);
            check(g.tow.count()==1 && Tutorial.direct(g,g.cargo[3]) && g.delivered==1,"final fire response preserves first delivery and leaves only the separate fire barge connected");
        }
        System.out.println(checks+" berth tow, double bridge passage, reconfiguration and narrow train assertions passed");
    }
    private static void bridgeBounds(){
        Game g=exercise(Tutorial.BRIDGE,960);g.passages.position[TutorialTowBridges.EXIT]=0;
        Body distant=new Body(0,-1,Tutorial.START_X,405,-(float)Math.PI/2);distant.vy=-50;
        check(g.passages.collide(distant,null)==0 && distant.x==Tutorial.START_X && distant.y==405 && distant.vy==-50,"tutorial bridge cannot create an invisible wall in the remote opening basin");
        Body under=new Body(0,-1,300,-295,-(float)Math.PI/2);under.vy=-50;
        check(g.passages.collide(under,null)>0 && under.y>-295,"closed physical bridge still blocks its actual span");
        check(Towline.clear(g,Tutorial.START_X,395,Tutorial.START_X,430),"remote basin line can cross the bridge's y coordinate without intersecting its span");
        check(!Towline.clear(g,300,-320,300,-260),"closed bridge blocks a towline crossing its real span");
    }
    private static void joystickCourse(){
        Game g=exercise(Tutorial.CONNECT,1333);g.settings.setJoystickSide(GameSettings.RIGHT);
        for(int tick=0;tick<8000 && g.tutorial.lesson!=Tutorial.DONE;tick++){
            int lesson=g.tutorial.lesson;
            if(ReplayInput.towRadio(g)){g.step();continue;}
            if(lesson==Tutorial.PULL || lesson==Tutorial.BRIDGE || lesson==Tutorial.TRAIN){
                float x=Joystick.centerX(g),y=Joystick.centerY(g);
                if(!g.joystick.active){g.cancelInputs();g.pointerDown(11,x,y);}
                float dx=g.tutorial.towTargetX()-g.tug.x,dy=g.tutorial.towTargetY(g)-g.tug.y;
                float sx=RiverCamera.Q*(dx+dy),sy=RiverCamera.Q*RiverCamera.DEPTH*(dy-dx),len=Math.max(1,Physics.length(sx,sy));
                if(Physics.length(dx,dy)<25)g.pointerMove(11,x,y);
                else g.pointerMove(11,x+Joystick.TRAVEL*sx/len,y+Joystick.TRAVEL*sy/len);
            } else ReplayInput.tutorial(g);
            g.step();
        }
        check(g.tutorial.lesson==Tutorial.DONE,"joystick can tow to berth, through bridge and narrow channel using screen-space target feedback");
    }
    private static void closePair(){
        Game g=exercise(Tutorial.LINK,1333);Body a=g.cargo[0],b=g.cargo[2];
        a.x=283;a.y=-470;b.x=318;b.y=-470;g.tug.x=300;g.tug.y=-590;g.fitTutorialCamera();
        ReplayInput.releaseTow(g,b);
        check(g.tow.incoming(b)==null && Tutorial.direct(g,a),"tap releases selected close-packed barge only");
        ReplayInput.down(g,b.x,b.y);check(g.heldCargo()==b,"free hull center wins over the linked neighbor's padded hit region");
        ReplayInput.move(g,a.x,a.y);check(g.towAiming && g.towAimBody==a,"close pair arms a barge-to-barge throw");ReplayInput.up(g,a.x,a.y);
        for(int i=0;i<Game.ROPE_FLIGHT_TICKS;i++)g.step();
        check(Tutorial.chain(g) && g.tutorial.lesson==Tutorial.TRAIN,"close-packed unlink and re-lasso enters train lesson immediately");
    }
    private static void gates(){
        Game wrong=exercise(Tutorial.DOUBLE,960);wrong.tow.attach(wrong,wrong.cargo[2],wrong.cargo[0]);wrong.tutorial.update(wrong);
        check(wrong.tutorial.lesson==Tutorial.DOUBLE,"a chain cannot substitute for the two direct bridge connections");
        Game g=exercise(Tutorial.BRIDGE,960);g.passages.position[TutorialTowBridges.EXIT]=BridgePassage.OPEN_TICKS;
        for(int i=0;i<120;i++)g.tutorial.update(g);
        check(g.tutorial.lesson==Tutorial.BRIDGE,"waiting does not clear bridge");
        g.tug.y=Tutorial.BRIDGE_CLEAR_Y-200;g.cargo[0].y=Tutorial.BRIDGE_CLEAR_Y-100;g.tutorial.update(g);
        check(g.tutorial.lesson==Tutorial.BRIDGE && g.tutorial.bridgeCrossed==1,"tug and only one cleared load cannot advance");
        g.cargo[2].y=Tutorial.BRIDGE_CLEAR_Y-10;g.tutorial.update(g);
        check(g.tutorial.lesson==Tutorial.BRIDGE,"center crossing without stern clearance cannot advance");
        g.cargo[2].y=Tutorial.BRIDGE_CLEAR_Y-70;g.tutorial.update(g);
        check(g.tutorial.lesson==Tutorial.LINK,"second whole hull completes bridge");
        g.tow.detach(g,g.cargo[2]);g.tow.attach(g,g.cargo[2],g.cargo[0]);g.tutorial.update(g);
        check(g.tutorial.lesson==Tutorial.LINK,"programmatic rewiring without the player's release does not earn reconfiguration");
        g.releaseTow(g.cargo[2]);g.tow.attach(g,g.cargo[2],g.cargo[0]);g.tutorial.update(g);
        check(g.tutorial.lesson==Tutorial.TRAIN,"explicit release and chain connections complete reconfiguration");
        g.tug.y=-1350;g.cargo[0].y=-1200;g.cargo[2].y=TrainingChannel.NARROW_EXIT-5;g.tutorial.update(g);
        check(g.tutorial.lesson==Tutorial.TRAIN,"rear hull must leave the narrow channel fully");
        g.cargo[2].y=-1100;g.tutorial.update(g);
        check(g.tutorial.lesson==Tutorial.RELEASE,"full train clearance finishes narrow passage");
        g=exercise(Tutorial.BRIDGE,960);Body same=g.cargo[2];g.releaseTow(same);g.tutorial.update(g);
        check(g.tutorial.lesson==Tutorial.DOUBLE && g.cargo[2]==same,"lost bridge connection returns to rigging without resetting bodies");
        g=exercise(Tutorial.TRAIN,960);same=g.cargo[2];g.releaseTow(same);g.tutorial.update(g);
        check(g.tutorial.lesson==Tutorial.LINK && g.cargo[2]==same,"lost train connection returns to reconfiguration in place");
        g=exercise(Tutorial.RELEASE,960);same=g.cargo[2];g.tow.release(g);g.tutorial.update(g);
        check(g.tutorial.lesson==Tutorial.LINK && g.cargo[2]==same,"automatic loss of every line at final release returns to reconfiguration instead of trapping the tutorial");
        check(TrainingChannel.NARROW_RIGHT-TrainingChannel.NARROW_LEFT>same.radius*2 && TrainingChannel.NARROW_RIGHT-TrainingChannel.NARROW_LEFT<same.radius*4,"narrow reach fits one barge across, but not two abreast");
        for(int y=-950;y<800;y+=10)check(TrainingChannel.contains(300,y),"whole course is permanently connected water");
    }
    public static void main(String[] args){run();}
}
