package com.bargeboss.game;

/** Radio teaching/waiting keeps hull kinematics and water emissions in agreement. */
public final class InstructorIdleWakeTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(){
        for(boolean teaching:new boolean[]{true,false}){
            Game root=new Game();root.configureCanvas(1333,600);ReplayInput.startPractice(root);
            Game g=root.activeGame();g.tutorial.lesson=Tutorial.MOVE;g.tutorial.setup(g);
            g.tutorial.entering=false;g.dialogue.close(g);g.cancelInputs();
            Body teacher=g.teacherBoat();teacher.x=-1770;teacher.y=510;
            teacher.vx=70;teacher.vy=20;teacher.engineLoad=1;
            if(teaching)g.tutorial.instructorRadio.introduce(g);
            else g.tutorial.instructorRadio.phase=1;
            float x=teacher.x,y=teacher.y;
            for(int i=0;i<180;i++)g.step();
            check(teacher.speed()<.1f && teacher.engineLoad<.01f,"radio observer coasts to rest and idles engine: teach="+teaching+" speed="+teacher.speed()+" load="+teacher.engineLoad+" phase="+g.tutorial.instructorRadio.phase+" teach="+g.tutorial.instructorRadio.teach);
            check(Physics.length(teacher.x-x,teacher.y-y)>0 && Physics.length(teacher.x-x,teacher.y-y)<12,"radio hold integrates a short coast instead of freezing velocity or continuing pursuit");
            int wakes=g.water.emitted,foam=g.water.foam.emitted,prop=g.water.foam.propEmitted;
            for(int i=0;i<600;i++)g.step();
            check(g.water.emitted==wakes && g.water.foam.emitted==foam && g.water.foam.propEmitted==prop,"idle radio wait cannot accumulate traveling wake or prop wash");
            for(int age:g.water.age)check(age>=WaterSurface.LIFE,"existing wakes expire while radio waits");
            check(g.tutorial.lesson==Tutorial.MOVE,"waiting does not advance lesson");
            check(teaching?g.tutorial.instructorRadio.teach:g.dialogue.active,"radio remains available for the player");
            teacher.vx=60;teacher.engineLoad=1;
            g.tutorial.instructorRadio.teach=false;g.tutorial.instructorRadio.phase=0;g.dialogue.close(g);
            g.water.floatBoats(g);
            for(int i=0;i<16;i++){g.visualTicks++;g.water.step(g);}
            check(g.water.emitted>wakes,"real movement still generates wake after radio");
        }
        System.out.println("Instructor radio hold, coasting, wake expiry and resumed motion checks passed");
    }
    public static void main(String[] args){run();}
}
