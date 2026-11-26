package com.bargeboss.game;
/** Last practice lesson: both fireworks barges towed to the woodpile basin's mooring ball and tied on. */
public final class PracticeMooringTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void run(){
        check(Tutorial.RELEASE<Tutorial.CHALLENGE && Tutorial.CHALLENGE<Tutorial.SHOW && Tutorial.EXTINGUISH<Tutorial.MOOR && Tutorial.MOOR<Tutorial.DONE,"order: towing, challenge, celebrate, fire, moor");
        Game root=new Game();root.configureCanvas(1243,600);ReplayInput.startPractice(root);Game g=root.activeGame();
        g.tutorial.lesson=Tutorial.SHOW;g.tutorial.setup(g);
        Body a=PracticeMooring.hull(g,0),b=PracticeMooring.hull(g,1);
        check(a!=null && b!=null && a!=b && a.ability==BargeAbilities.FIREWORKS && b.ability==BargeAbilities.FIREWORKS,"both fireworks barges are found by ability");
        check(!g.tutorial.mooring.owns(g,a),"the ball claims no barge before the mooring lesson");
        check(g.inWater(PracticeMooring.X,PracticeMooring.Y),"the ball floats in open water");
        g.tutorial.lesson=Tutorial.MOOR;g.tutorial.setup(g);a=PracticeMooring.hull(g,0);b=PracticeMooring.hull(g,1);
        check(g.tutorial.mooring.owns(g,a) && g.tutorial.mooring.owns(g,b) && !a.lineAttached && !b.lineAttached,"mooring lesson starts with both barges loose near the ball");
        check(g.bollardX(a)==g.tutorial.mooring.x(g)+RiverCamera.riseX(PracticeMooring.TOP),"the tie point is the ball's fitting");
        for(int i=0;i<4000 && g.tutorial.lesson!=Tutorial.DONE;i++){ReplayInput.tutorial(root);root.step();}
        check(g.tutorial.lesson==Tutorial.DONE && a.lineAttached && b.lineAttached,"public lasso gestures tie both barges to the ball and finish training");
        for(int i=0;i<600;i++)root.step();
        float bx=g.tutorial.mooring.x(g),by=g.tutorial.mooring.y(g);
        for(Body h:new Body[]{a,b})check(Physics.length(h.x-bx,h.y-by)<=PracticeMooring.LINE+6,"the winch draws each barge in to a short riding line");
        check(Physics.length(a.x-b.x,a.y-b.y)>a.radius+b.radius,"moored barges do not overlap");
        check(g.tow.incoming(a)==null && g.tow.incoming(b)==null,"tied barges are off the tug");
        System.out.println(checks+" practice mooring ball, order, tie-off and winch assertions passed");
    }
    public static void main(String[] a){run();}
}
