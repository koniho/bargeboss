package com.bargeboss.game;

/** Real taps, explicit skip replies, first-throttle teaching and movable instructor contact. */
public final class InstructorRadioTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void ticks(Game root,int n){for(int i=0;i<n;i++)root.step();}
    private static void frame(Game g){Body b=g.teacherBoat();g.layout=Layout.focused(g.layout.width,600,b.x,b.y,1.2f,g.layout.width*.45f,300);}
    private static void call(Game root){Game g=root.activeGame();frame(g);InstructorRadio radio=g.tutorial.instructorRadio;ReplayInput.tap(root,radio.x(g),radio.y(g));check(radio.phase==1,"boat tap initiates player radio call");ticks(root,75);check(g.dialogue.ready(),"instructor responds with reply choices");}
    private static void reply(Game root,int choice){Game g=root.activeGame();ReplayInput.tap(root,g.dialogue.choiceBox(g,choice));ticks(root,26);}
    public static void run(){
        for(int width:new int[]{960,1333}){
            Game root=new Game();root.configureCanvas(width,600);ReplayInput.startPractice(root);Game g=root.activeGame();InstructorRadio radio=g.tutorial.instructorRadio;
            frame(g);float x=radio.x(g),y=radio.y(g);
            root.pointerDown(61,x,y);root.pointerCancel(61);root.pointerUp(61,x,y);check(radio.phase==0,"cancelled instructor tap cannot call");
            root.pointerDown(61,x,y);root.pointerMove(61,x+100,y);root.pointerUp(61,x,y);check(radio.phase==0,"dragging across instructor does not call");
            call(root);check(radio.taught && g.tutorial.helm.phase==0,"experienced player can call during first throttle exercise");
            reply(root,0);check(g.tutorial.lesson==Tutorial.MOVE && g.tutorial.helm.phase==0,"keep practicing preserves current exercise");
            call(root);int tick=radio.ticks;g.pause();ticks(root,60);check(radio.ticks==tick,"pause freezes instructor conversation");g.pause();
            reply(root,1);check(g.tutorial.lesson==Tutorial.CONNECT,"skip driving goes directly to towing");
            check(g.passages.passable(0) && g.cargo[0]!=null,"towing starts with open canal and cargo ready");
            call(root);reply(root,1);check(g.tutorial.lesson==Tutorial.SHOW,"skip towing prepares abilities");
            check(g.tow.loadCount(g.tug)==0 && g.cargo[0].ability==BargeAbilities.FIREWORKS && !g.tutorial.fireworks.launched,"abilities starts with released fireworks barges ready to activate");
            call(root);reply(root,1);check(g.tutorial.lesson==Tutorial.CHALLENGE && g.dialogue.active,"skip abilities opens the towing challenge briefing");
            ticks(root,20);reply(root,0);check(!g.dialogue.active && g.tutorial.challenge.phase==TowingChallenge.APPROACH,"briefing reply hands over to the basin approach");
            call(root);reply(root,1);check(g.tutorial.lesson==Tutorial.DONE && !g.dialogue.active,"skip towing challenge finishes training");
            for(int lesson=Tutorial.MOVE;lesson<Tutorial.DONE;lesson++){
                g.tutorial.lesson=lesson;g.tutorial.setup(g);
                if(lesson==Tutorial.CHALLENGE){ticks(root,20);reply(root,0);}
                int expected=lesson<Tutorial.CONNECT?Tutorial.CONNECT:lesson<Tutorial.SHOW?Tutorial.SHOW:lesson<Tutorial.CHALLENGE?Tutorial.CHALLENGE:Tutorial.DONE;
                call(root);check(g.dialogue.content().title.contains(g.tutorial.phaseName()),"confirmation names the entire phase");reply(root,1);
                check(g.tutorial.lesson==expected,"skip from every sublesson reaches next phase boundary");
                check(!g.tutorial.feedback.active(),"skipping does not claim earned lesson praise");
                if(expected==Tutorial.CHALLENGE){check(g.dialogue.active,"challenge skip target briefs the timed basin");ticks(root,20);reply(root,0);}
                if(expected>=Tutorial.CHALLENGE){ticks(root,360);check(!g.dialogue.active && g.tutorial.fireworks.heat==0 && g.tutorial.fireworks.sizzle==0,"skipped fire cannot restart radio or ambience");}
            }
            root=new Game();root.configureCanvas(width,600);ReplayInput.startPractice(root);g=root.activeGame();
            for(int i=0;i<1000 && !g.tutorial.instructorRadio.teach;i++){ReplayInput.tutorial(root);root.step();}
            check(g.tutorial.instructorRadio.teach && g.tutorial.helm.phase==HelmPractice.LEFT,"radio teaching follows first forward throttle, before the first turn");
            ticks(root,90);check(g.tutorial.helm.phase==HelmPractice.LEFT,"teaching waits for the player's call");
            call(root);reply(root,0);check(!g.tutorial.instructorRadio.teach && g.tutorial.helm.phase==HelmPractice.LEFT,"practice reply resumes the pending turn");
        }
        Game root=new Game();ReplayInput.startPractice(root);Game g=root.activeGame();Body b=g.teacherBoat();
        b.x=-1550;b.y=500;b.vx=b.vy=0;b.angle=0;g.tug.x=-1595;g.tug.y=500;g.tug.angle=0;g.tug.vx=70;g.tug.vy=0;
        float before=b.x;g.step();check(b.x>before && b.vx>0 && g.tutorial.arrival.pushedTicks>0,"instructor receives displacement and momentum from the player");
        float pushed=b.x;ticks(root,12);check(b.x>pushed+1,"instructor drifts after the impact instead of snapping back");
        check(g.inWater(b.x,b.y),"pushed instructor stays inside navigable water");
        System.out.println(checks+" instructor tap, skip, teaching, reset and physical bump assertions passed");
    }
    public static void main(String[] args){run();}
}
