package com.bargeboss.game;
public final class AbilityPanelTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void run(){
        for(int side:new int[]{GameSettings.LEFT,GameSettings.RIGHT}){
            Game g=BargeAbilitiesTest.fixture();g.settings.setJoystickSide(side);Body b=g.cargo[3];
            g.abilities.reveal(g,b);Layout.Box box=OverviewMap.bounds(g);
            check(box.contains(g.abilities.buttonX(g,0),g.abilities.buttonY(g)),"control occupies chart side");
            check(!OverviewMap.sliderHit(g,box.centerX(),g.layout.height-24),"hidden chart cannot zoom");
            float last=0;
            for(int i=0;i<10;i++){g.abilities.step(g);check(g.abilities.opacity()>last,"fade rises every tick");last=g.abilities.opacity();}
            check(last==1,"quick fade completes");
            Parity p=new Parity();g.abilities.drawUI(g,p);check(g.abilities.opacity()==last,"render cannot advance fade");
            ReplayInput.throttleDown(g);check(g.joystick.active,"drive still available");
            g.pointerDown(66,g.abilities.buttonX(g,0),g.abilities.buttonY(g));ReplayInput.hoseAim(g,66,b.x+100,b.y-40);
            for(int i=0;i<14;i++)g.abilities.step(g);
            check(g.abilities.spraying && g.joystick.active,"aim pad works with a second driving finger");
            g.pointerCancel(66);check(!g.abilities.spraying && g.joystick.active,"release hose leaves driving intact");
            g.cancelInputs();
            g.abilities.reveal(g,b);check(g.abilities.selected==null && g.abilities.opacity()==1,"deselect begins smooth map return");
            for(int i=0;i<4;i++)g.abilities.step(g);
            float partial=g.abilities.opacity();check(partial>0 && partial<1,"fade-out retains control pixels");
            g.abilities.reveal(g,b);check(g.abilities.opacity()==partial,"reselect reverses without opacity jump");
            g.abilities.step(g);check(g.abilities.opacity()>partial,"fade reverses");
            b.active=false;for(int i=0;i<11;i++)g.abilities.step(g);
            check(!g.abilities.replacesMap() && g.abilities.selected==null,"removed vessel returns chart");
        }
        System.out.println("Ability HUD: both sides, chart ownership, fades, reversal, render purity and simultaneous drive/aim passed");
    }
    public static void main(String[]args){run();}
}
