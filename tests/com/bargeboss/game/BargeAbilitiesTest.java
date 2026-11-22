package com.bargeboss.game;

/** Public-pointer regressions: selection, activation, towing and cancellation. */
public final class BargeAbilitiesTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void steps(Game g,int n){for(int i=0;i<n;i++)g.step();}
    public static Game fixture(){Game root=new Game();root.configureCanvas(1333,600);ReplayInput.startPractice(root);Game g=root.activeGame();g.tutorial.lesson=Tutorial.DOUBLE;g.tutorial.setup(g);g.tutorial.towBridges.authorized[1]=true;g.passages.position[1]=BridgePassage.OPEN_TICKS;g.tug.x=300;g.tug.y=110;g.cargo[3].x=365;g.cargo[3].y=140;g.cargo[3].tiedX=365;g.cargo[3].tiedY=140;g.fitTutorialCamera();return g;}
    public static void run(){
        Game g=fixture();Body b=g.cargo[0];check(b.ability==BargeAbilities.FIREWORKS && g.cargo[2].ability==BargeAbilities.FIREWORKS && g.cargo[3].ability==BargeAbilities.FIRE,"specialized pair stays in next basin");
        check(TutorialTowBridges.ENTRY_Y-TutorialTowBridges.EXIT_Y>=650,"collection basin has room to approach and assemble a tow");
        ReplayInput.down(g,b.x,b.y);steps(g,24);ReplayInput.up(g,b.x,b.y);
        check(g.abilities.selected==null && b.abilityTicks==0,"holding a hull for push cannot reveal or fire equipment");
        g.cancelInputs();g.tow.attach(g,b);ReplayInput.down(g,b.x,b.y);ReplayInput.up(g,b.x,b.y);
        check(g.abilities.selected==b && g.tow.incoming(b)!=null && b.abilityTicks==0,"first tap reveals equipment without firing or disconnecting");
        ReplayInput.down(g,b.x,b.y);steps(g,15);ReplayInput.up(g,b.x,b.y);
        check(g.tow.incoming(b)!=null,"medium-length equipped-hull press cannot fall through to legacy cast-off");
        float x=g.abilities.buttonX(g,0),y=g.abilities.buttonY(g);ReplayInput.tap(g,x,y);
        check(b.abilityTicks==180 && g.abilities.launches==1 && g.tow.incoming(b)!=null,"second icon tap launches while preserving tow");
        ReplayInput.tap(g,x,y);check(g.abilities.launches==1,"cooldown blocks repeated activation");
        ReplayInput.releaseTow(g,b);check(g.tow.incoming(b)==null,"rope tap retains train reconfiguration");
        Body fire=g.cargo[3];ReplayInput.down(g,fire.x,fire.y);ReplayInput.up(g,fire.x,fire.y);
        check(g.abilities.selected==fire,"fire equipment selects independently: selected="+(g.abilities.selected==null?-1:g.abilities.selected.id)+" fire="+fire.id+" screen="+g.layout.camera.screenX(fire.x,fire.y)+","+g.layout.camera.screenY(fire.x,fire.y)+" tug="+g.tug.x+","+g.tug.y);x=g.abilities.buttonX(g,0);y=g.abilities.buttonY(g);
        g.pointerDown(77,x,y);steps(g,14);check(g.abilities.spraying && !g.pointer,"hold owns equipment without starting vessel drag");
        float tx=g.layout.camera.screenX(fire.x-150,fire.y+30),ty=g.layout.camera.screenY(fire.x-150,fire.y+30);g.pointerMove(77,tx,ty);g.step();
        check(Math.abs(Physics.angle(fire.abilityAim-(float)StrictMath.atan2(30,-150)))<.05f,"screen drag aims in world space");
        g.pointerCancel(77);check(!g.abilities.spraying && g.abilities.owner==-1,"cancel immediately stops spray");
        x=g.abilities.buttonX(g,0);y=g.abilities.buttonY(g);g.pointerDown(78,x,y);steps(g,14);g.background();check(!g.abilities.spraying && g.abilities.owner==-1,"background cancels held nozzle");
        for(int atlas:new int[]{SpriteAtlas.FIREWORKS,SpriteAtlas.FIRE_BARGE}){check(SpriteAtlas.count(atlas)==64,"equipment has 64 real source poses");for(int i=0;i<64;i++){check(SpriteAtlas.sourceBottom(atlas,i)<=1 && SpriteAtlas.sourceTop(atlas,i)>=0,"all source rectangles fit");check(SpriteAtlas.pivotY(atlas,i)==.72f,"waterline datum never moves");}}
        System.out.println("Barge ability selection, activation, cooldown, directional spraying, release and lifecycle passed");
    }
    public static void main(String[] args){run();}
}
