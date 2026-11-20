package com.bargeboss.game;

/** Escort route safety, physical contact warnings and speaker-specific radio effects. */
public final class TutorialEscortTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void run(){
        Game root=new Game();root.startTutorial();Game g=root.activeGame();Body teacher=g.teacherBoat();
        float travel=0;boolean inCanal=false,inHarbor=false;
        for(int tick=0;tick<18000 && g.tutorial.lesson!=Tutorial.DONE;tick++){
            float x=teacher.x,y=teacher.y;ReplayInput.tutorial(root);root.step();
            float step=Physics.length(teacher.x-x,teacher.y-y);travel+=step;
            check(step<8,"escort never teleports: "+step);
            check(g.inWater(teacher.x,teacher.y),"escort stays in connected water");
            if(teacher.x>-1150 && teacher.x<80)inCanal=true;
            if(teacher.x>170)inHarbor=true;
        }
        check(g.tutorial.lesson==Tutorial.DONE,"escort cannot block the complete public tutorial: lesson "+g.tutorial.lesson);
        check(inCanal && inHarbor && travel>2500,"teacher follows from basin through canal into working harbor: "+teacher.x+","+teacher.y+" travel "+travel);
        // While the student maneuvers, the observer remains at its station and faces them.
        root=new Game();root.startTutorial();g=root.activeGame();
        for(int i=0;i<600 && g.tutorial.entering;i++){ReplayInput.tutorial(root);root.step();}
        g.tutorial.lesson=Tutorial.FIGURE_EIGHT;g.tug.x=HelmPractice.MEET_X;g.tug.y=HelmPractice.MEET_Y;
        for(int i=0;i<500;i++)g.step();teacher=g.teacherBoat();float stationX=teacher.x,stationY=teacher.y;
        g.tug.x=-1460;g.tug.y=400;g.tug.vx=g.tug.vy=0;
        for(int i=0;i<150;i++)g.step();
        check(Physics.length(teacher.x-stationX,teacher.y-stationY)<1,"teacher does not follow local maneuvers");
        float facing=(float)StrictMath.atan2(g.tug.y-teacher.y,g.tug.x-teacher.x);
        check(Math.abs(Physics.angle(facing-teacher.angle))<.04f,"stationary teacher faces the player");
        g.tug.x=-2150;g.tug.y=650;g.tug.vx=g.tug.vy=0;
        for(int i=0;i<2;i++)g.step();
        check(TutorialView.instructionContent(g).title.equals("COME BACK, CAPTAIN!"),"straying triggers a return call");
        // Actual closing player contact, not a scripted warning call.
        root=new Game();root.startTutorial();g=root.activeGame();
        for(int i=0;i<600 && g.tutorial.entering;i++){ReplayInput.tutorial(root);root.step();}
        teacher=g.teacherBoat();teacher.x=-1550;teacher.y=500;teacher.vx=teacher.vy=0;
        g.tug.x=teacher.x-45;g.tug.y=teacher.y;g.tug.angle=0;g.tug.vx=70;g.tug.vy=0;
        int sequence=RadioAudio.sequence(g);g.step();
        check(g.tutorial.arrival.warningTicks==0 && RadioAudio.sequence(g)==sequence,"teacher takes time to react");
        for(int i=0;i<31;i++)g.step();
        check(g.tutorial.arrival.warningTicks>0 && RadioAudio.sequence(g)==sequence+1,"real player collision radios warning once");
        check(!g.dialogue.active && !g.conversation(),"collision warning leaves driving controls available");
        check(TutorialView.instructionContent(g).title.equals("EASY, CAPTAIN!"),"warning uses instruction panel");
        check(g.radio.transmitting(g,teacher) && !g.radio.transmitting(g,g.tug),"teacher warning animates only teacher");
        int warning=g.tutorial.arrival.warningTicks;float phase=g.radio.progress();g.pause();for(int i=0;i<60;i++)g.step();
        check(warning==g.tutorial.arrival.warningTicks && phase==g.radio.progress(),"pause freezes warning and radio effect");g.pause();
        g.tutorial.arrival.warn(g);check(RadioAudio.sequence(g)==sequence+1,"sustained contact cannot spam radio");
        for(int i=0;i<185;i++)g.step();check(g.tutorial.arrival.warningTicks==0,"warning restores current lesson automatically");
        g.radio.transmit(RadioAudio.REPLY);g.step();
        check(g.radio.transmitting(g,g.tug) && !g.radio.transmitting(g,teacher),"reply animates only the player's mast");
        int before=Parity.frame(g);check(Parity.frame(g)==before,"radio rendering is pure");
        for(int i=0;i<60;i++)g.step();check(!g.radio.transmitting(g,g.tug),"transmission effect expires");
        System.out.println("Teacher escort, canal route, physical warning, cooldown and radio source assertions passed");
    }
    public static void main(String[] args){run();}
}
