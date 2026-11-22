package com.bargeboss.game;
/** Hull gestures win over floating actions, with forgiving initial tow direction. */
public final class TowPriorityTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Game fixture(){Game g=new Game();g.configureCanvas(1333,600);g.resetPracticeYard();
        g.tug=new Body(0,-1,300,570,0);g.cargo[0]=new Body(1,2,300,690,0);
        g.layout=Layout.focused(1333,600,300,630,1.8f,600,350);return g;}
    public static void run(){
        for(int lesson:new int[]{Tutorial.CONNECT,Tutorial.HOLD,Tutorial.STEER,Tutorial.DOCK,Tutorial.TIE}){
            Game g=fixture();g.tutorial=new Tutorial();g.tutorial.lesson=lesson;Body b=g.cargo[0];
            check(g.canTow(b),"towing is available during every cargo lesson");
            ReplayInput.down(g,b.x,b.y);ReplayInput.move(g,b.x+24,b.y-22);
            check(g.towAiming && !g.pushing,"imperfect first diagonal arms tow rather than prematurely committing push");
            ReplayInput.move(g,g.tug.x,g.tug.y);ReplayInput.up(g,g.tug.x,g.tug.y);
            check(g.ropePhase==1 && g.ropeToTug && g.ropeTowTarget==g.tug,"only release launches the aimed towline");
        }
        Game g=fixture();Body first=g.cargo[0];first.ability=BargeAbilities.FIREWORKS;g.abilities.reveal(g,first);
        float x=g.abilities.buttonX(g,0),y=g.abilities.buttonY(g);
        Body spare=new Body(2,2,g.layout.camera.worldX(x,y),g.layout.camera.worldY(x,y),0);g.cargo[1]=spare;
        check(Physics.length(g.abilities.buttonX(g,0)-x,g.abilities.buttonY(g)-y)>25,"ability button moves aside instead of covering a hull");
        g.pointerDown(44,x,y);check(g.heldCargo()==spare && g.abilities.owner==-1,"barge under an ability icon owns its drag");g.pointerCancel(44);
        g=fixture();g.tow.attach(g,g.cargo[0]);Towline line=g.tow.incoming(g.cargo[0]);
        x=TowRelease.point(g,line,.5f,true);y=TowRelease.point(g,line,.5f,false);
        spare=new Body(2,2,g.layout.camera.worldX(x,y),g.layout.camera.worldY(x,y),0);g.cargo[1]=spare;
        g.pointerDown(45,x,y);check(g.heldCargo()==spare && !g.towRelease.owns(45),"barge beneath crossing towline owns its drag");g.pointerCancel(45);
        System.out.println("Tow availability, diagonal aim, release-only throw and hull input priority passed");
    }
    public static void main(String[] args){run();}
}
