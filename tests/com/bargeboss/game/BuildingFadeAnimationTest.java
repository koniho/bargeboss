package com.bargeboss.game;

public final class BuildingFadeAnimationTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void run() {
        Game root=new Game();root.startTutorial();Game g=root.activeGame();
        g.tug.x=-1610;g.tug.y=TrainingChannel.BASIN_BOTTOM-16;g.tug.angle=0;
        g.tug.floatingInitialized=true;g.tug.heave=0;
        g.layout=Layout.focused(1333,600,g.tug.x,g.tug.y,1.7f,666,360);
        SceneryOcclusion scene=SceneryOcclusion.snapshot(g);int slot=-1;
        for(int i=0;i<TrainingChannelView.FACTORY_PLACEMENTS;i++) {
            int frame=TrainingChannelView.factoryCell(i);
            if(scene.buildingTarget(g,SpriteAtlas.FACTORY,frame,TrainingChannelView.factoryX(i),TrainingChannelView.factoryY(i),GroundArt.LAND_RISE,FactoryArt.size(frame))<.3f){slot=i;break;}
        }
        check(slot>=0,"fixture has a real foreground factory silhouette obscuring the tug");
        int frame=TrainingChannelView.factoryCell(slot);float x=TrainingChannelView.factoryX(slot),y=TrainingChannelView.factoryY(slot);
        float previous=1;
        for(int i=0;i<18;i++) {
            g.buildingFades.step(g);float alpha=g.buildingFades.opacity(SpriteAtlas.FACTORY,frame,x,y);
            check(alpha<=previous && previous-alpha<=.041f,"entry changes opacity continuously rather than snapping");previous=alpha;
        }
        check(previous<.3f,"held overlap reaches its transparent target after the transition");
        int rendered=Parity.frame(g);check(rendered==Parity.frame(g) && previous==g.buildingFades.opacity(SpriteAtlas.FACTORY,frame,x,y),"rendering cannot advance fades");
        g.background();g.advance(.25);check(previous==g.buildingFades.opacity(SpriteAtlas.FACTORY,frame,x,y),"pause freezes the fade clock");
        g.tug.x-=400;g.tug.y-=300;
        for(int i=0;i<28;i++) {
            g.buildingFades.step(g);float alpha=g.buildingFades.opacity(SpriteAtlas.FACTORY,frame,x,y);
            check(alpha>=previous && alpha-previous<=.027f,"clearing the hull gradually restores building opacity");previous=alpha;
        }
        check(previous==1,"building returns fully opaque without lingering transparency");
        g.reset();check(g.buildingFades.opacity(SpriteAtlas.FACTORY,frame,x,y)==1,"restart clears fade history");
        System.out.println(checks+" timed building fade assertions passed");
    }
}
