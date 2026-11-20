package com.bargeboss.game;

public final class BuildingFadeAnimationTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void run() {
        Game root=new Game();root.startTutorial();Game g=root.activeGame();
        g.tutorial.setup(g);g.tug.x=170;g.tug.y=150;g.tug.angle=0;
        g.tug.floatingInitialized=true;g.tug.heave=0;
        g.layout=Layout.focused(1333,600,g.tug.x,g.tug.y,1.7f,666,360);
        // Small set-back workshops deliberately do not cover the navigation water.
        // Test the production interpolation independently of a dense-factory fixture.
        float previous=1;
        for(int i=0;i<18;i++){
            float alpha=BuildingFades.approach(previous,.28f);
            check(alpha<=previous && previous-alpha<=.041f,"entry opacity changes continuously");previous=alpha;
        }
        check(previous<.3f,"held overlap reaches its target after 18 ticks");
        for(int i=0;i<28;i++){
            float alpha=BuildingFades.approach(previous,1);
            check(alpha>=previous && alpha-previous<=.027f,"clearing a hull restores opacity gradually");previous=alpha;
        }
        check(previous==1,"restoration reaches full opacity");
        for(int i=0;i<30;i++)g.buildingFades.step(g);
        for(int i=0;i<TrainingChannelView.FACTORY_PLACEMENTS;i++)
            check(g.buildingFades.opacity(SpriteAtlas.FACTORY,TrainingChannelView.factoryCell(i),TrainingChannelView.factoryX(i),TrainingChannelView.factoryY(i))==1,
                    "distant set-back tutorial workshops stay opaque");
        int frame=Parity.frame(g);check(frame==Parity.frame(g),"drawing is pure");
        System.out.println(checks+" timed building fade assertions passed");
    }
}
