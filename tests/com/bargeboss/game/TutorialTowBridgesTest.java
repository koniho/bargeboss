package com.bargeboss.game;

/** Basin inventory and explicit radio authorization through the public touch router. */
public final class TutorialTowBridgesTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void steps(Game g,int n){for(int i=0;i<n;i++)g.step();}
    private static Game fixture(int lesson,float width){Game root=new Game();root.configureCanvas(width,600);ReplayInput.startPractice(root);Game g=root.activeGame();g.tutorial.lesson=lesson;g.tutorial.setup(g);return g;}
    private static void call(Game g,int choice){
        TutorialTowBridges r=g.tutorial.towBridges;int i=r.current(g);
        check(r.contactable(g),"active bridge house is visible and contactable at width "+g.layout.width);
        ReplayInput.tap(g,r.houseX(g,i),r.houseY(g,i));steps(g,61);
        check(g.dialogue.active,"bridge press produces a real radio question");
        while(!g.dialogue.ready())g.step();Layout.Box answer=g.dialogue.choiceBox(g,choice);ReplayInput.tap(g,answer.centerX(),answer.centerY());steps(g,25);
    }
    public static void run(){
        Game g=fixture(Tutorial.HOLD,960);int first=0,next=0;
        for(Body b:g.cargo)if(b!=null){if(b.y>TutorialTowBridges.ENTRY_Y)first++;else if(b.y>TutorialTowBridges.EXIT_Y)next++;}
        check(first==1 && next==2,"one barge in first basin and exactly two in the gated basin");
        steps(g,600);check(g.passages.position[1]==0 && g.passages.position[2]==0,"proximity and waiting cannot auto-open towing bridges");
        check(!g.tutorial.towBridges.eligible(g,1) && !g.tutorial.towBridges.eligible(g,2),"calls unavailable before first tow delivery");
        g=fixture(Tutorial.CONNECT,960);Body firstLoad=g.cargo[0];
        check(!firstLoad.deliveryRecorded && !firstLoad.lineAttached && firstLoad.x>Physics.RIGHT,"loaded first hull starts on far side of the basin");
        check(firstLoad.x==599 && firstLoad.y==300 && Math.abs(firstLoad.angle-(float)Math.PI/2)<.001f,"first load occupies the annotated narrow arm, aligned along its length");
        for(int i=0;i<1800 && g.tutorial.lesson<Tutorial.CASTOFF;i++){ReplayInput.tutorial(g);g.step();}
        check(g.tutorial.lesson==Tutorial.CASTOFF && g.tow.incoming(firstLoad)!=null,"tow reaches open water before release lesson");
        check(!g.tutorial.towBridges.eligible(g,1),"entry bridge remains locked until pushed delivery");
        steps(g,120);check(g.tutorial.lesson==Tutorial.CASTOFF,"waiting with line attached does not earn cast-off");
        ReplayInput.releaseTow(g);g.step();
        check(g.tutorial.lesson==Tutorial.HOLD && g.cargo[0]==firstLoad && !g.tutorial.firstTowParked,"explicit release starts pushing with the same loaded hull");
        for(int i=0;i<2200 && g.tutorial.lesson<Tutorial.DOUBLE;i++){ReplayInput.tutorial(g);g.step();}
        check(g.tutorial.lesson==Tutorial.DOUBLE && g.tutorial.firstTowParked && g.cargo[1]==firstLoad && firstLoad.lineAttached && firstLoad.deliveryRecorded,"pushed and unloaded first hull remains tied alongside before next basin opens");
        g=fixture(Tutorial.CASTOFF,960);g.tow.release(g);g.tutorial.update(g);
        check(g.tutorial.lesson==Tutorial.CONNECT,"automatic line loss cannot substitute for player cast-off");
        for(float width:new float[]{960,1333,1680}){
            g=fixture(Tutorial.DOUBLE,width);TutorialTowBridges r=g.tutorial.towBridges;
            check(r.current(g)==1 && !r.eligible(g,2),"only the entry bridge is offered initially");
            float x=r.houseX(g,1),y=r.houseY(g,1);g.pointerDown(29,x,y);g.pointerCancel(29);steps(g,65);
            check(!g.dialogue.active && !r.authorized[1],"cancelled radio tap does not call or authorize");
            call(g,1);check(!r.authorized[1],"standby reply leaves entry closed");steps(g,121);call(g,0);
            check(r.authorized[1] && !g.passages.passable(1) && g.passages.position[2]==0,"confirmed entry call starts a gradual lift only for that bridge");
            steps(g,BridgePassage.WAIT_TICKS+BridgePassage.OPEN_TICKS);
            check(g.passages.passable(1) && !r.eligible(g,2),"opening first bridge does not unlock second call");
            g=fixture(Tutorial.BRIDGE,width);r=g.tutorial.towBridges;
            check(Tutorial.doubleTow(g) && r.current(g)==2 && r.eligible(g,2),"two direct tow links unlock second call");
            g.releaseTow(g.cargo[2]);check(!r.eligible(g,2) && !r.contactable(g),"losing either tow removes call availability before authorization");g.tow.attach(g,g.cargo[2]);
            call(g,0);check(r.authorized[2],"second bridge authorizes with both barges under tow");steps(g,BridgePassage.WAIT_TICKS+BridgePassage.OPEN_TICKS);
            check(g.passages.passable(2),"second bridge completes its actual opening");
            g.tutorial.setup(g);check(!g.tutorial.towBridges.authorized[2] && !g.passages.passable(2),"reset bridge lesson requires a fresh call");
        }
        System.out.println("Two-basin cargo inventory, first-load cast-off, bridge radio choices and convoy authorization passed");
    }
    public static void main(String[]a){run();}
}
