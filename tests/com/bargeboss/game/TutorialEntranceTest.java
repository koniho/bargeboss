package com.bargeboss.game;

/** Public-input radio arrival, modal capture, pause, cancellation and continuous handoff. */
public final class TutorialEntranceTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(){for(float width:new float[]{960,1333,1600})for(int answer=0;answer<3;answer++){
        Game root=new Game();root.configureCanvas(width,600);root.startTutorial();Game g=root.activeGame();Body tug=g.tug,teacher=g.teacherBoat();
        check(g.tutorial.entering && tug.y==TutorialArrival.DOCK_Y,"starts beside seawall dock");
        check(!SpeakerPortrait.visible(g,"INSTRUCTOR"),"arrival starts without a portrait");
        check(teacher!=null && teacher.x<tug.x && teacher.y<tug.y,"teacher approaches alongside from clear water");
        ReplayInput.down(root,tug.x,tug.y);ReplayInput.move(root,tug.x-90,tug.y);
        check(!g.pointer && !g.joystick.active,"arrival blocks drive gestures");
        float start=teacher.x;g.pause();for(int i=0;i<90;i++)root.step();
        check(teacher.x==start && g.tutorial.entranceTicks==0,"pause freezes arrival");g.pause();
        for(int tick=0;tick<270;tick++){
            float x=teacher.x;root.step();
            check(SpeakerPortrait.visible(g,"INSTRUCTOR")==(g.tutorial.entranceTicks>=TutorialArrival.APPROACH),"portrait appears with first radio introduction");
            check(g.tug==tug && tug.x==TutorialArrival.DOCK_X && tug.speed()==0,"original player hull stays at dock");
            check(teacher.x>=x && teacher.x-x<2,"teacher approaches continuously");
            check(CalumetMap.clear(teacher.x-teacher.half,teacher.y,teacher.x+teacher.half,teacher.y,teacher.radius),"teacher hull stays in navigable water");
            check(g.tutorial.driveMarker==0 && g.tutorial.channelGate==1,"radio earns no driving goals");
        }
        check(g.dialogue.ready() && RadioAudio.sequence(g)==1,"arrival transmits one question");
        Layout.Box b=g.dialogue.choiceBox(g,answer);
        root.pointerDown(4,b.centerX(),b.centerY());root.pointerCancel(4);root.pointerUp(4,b.centerX(),b.centerY());
        check(g.dialogue.result()<0,"cancelled touch cannot answer");
        root.pointerDown(4,b.centerX(),b.centerY());root.pointerMove(4,0,0);root.pointerUp(4,b.centerX(),b.centerY());
        check(g.dialogue.result()<0,"drag away cancels choice");
        root.pointerDown(4,b.centerX(),b.centerY());root.pointerUp(5,b.centerX(),b.centerY());
        check(g.dialogue.result()<0,"another finger cannot commit");
        root.pointerUp(4,b.centerX(),b.centerY());
        check(g.dialogue.result()<0,"reply pulse precedes commit");ReplayInput.finishMenu(root);
        check(g.dialogue.result()==answer && RadioAudio.sequence(g)==2,"release selects one answer and transmits");
        for(int i=0;i<185;i++)root.step();
        check(!g.conversation() && g.tug==tug && g.teacherBoat()==teacher,"reply hands off same continuous scene");
        check(RadioAudio.sequence(g)==3 && !g.pointer && !g.navigating,"one teacher acknowledgement; no input leaks");
        ReplayInput.throttleDown(root);check(g.joystick.active,"player can take helm");root.cancelInputs();
        g.dialogue.show(g,"DISPATCH","PICK A BERTH","FOUR DESTINATIONS.","ONE","TWO","THREE","FOUR");
        for(int i=0;i<20;i++)root.step();
        Layout.Box fourth=g.dialogue.choiceBox(g,3);root.pointerDown(9,fourth.centerX(),fourth.centerY());g.background();
        root.pointerUp(9,fourth.centerX(),fourth.centerY());check(g.dialogue.result()<0,"background cancels pending reply");g.pause();
        root.pointerDown(9,fourth.centerX(),fourth.centerY());root.pointerUp(9,fourth.centerX(),fourth.centerY());ReplayInput.finishMenu(root);check(g.dialogue.result()==3,"fourth choice works outside arrival");
        for(int count=1;count<=2;count++){
            String[] choices=count==1?new String[]{"ROGER"}:new String[]{"ROGER","STAND BY"};
            g.dialogue.show(g,"DISPATCH","RADIO CHECK","HOW DO YOU COPY?",choices);
            for(int i=0;i<20;i++)root.step();
            Layout.Box choice=g.dialogue.choiceBox(g,count-1);root.pointerDown(10,choice.centerX(),choice.centerY());root.pointerUp(10,choice.centerX(),choice.centerY());ReplayInput.finishMenu(root);
            check(g.dialogue.result()==count-1,"one/two choices use the same release routing");
        }
        for(int frame=0;frame<32;frame++)check(SpriteAtlas.pivotX(SpriteAtlas.TEACHER_TUG,frame)==TugArt.pivotX(frame) && SpriteAtlas.pivotY(SpriteAtlas.TEACHER_TUG,frame)==TugArt.pivotY(frame),"teacher preserves all registered poses");
    }System.out.println("Radio arrival, 1-4 choice routing, lifecycle and continuous handoff passed");}
}
