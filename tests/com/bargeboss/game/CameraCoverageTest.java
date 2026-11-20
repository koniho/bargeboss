package com.bargeboss.game;

/** Scenery limits hold at wide aspect ratios, during pinch, and after resize. */
public final class CameraCoverageTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void covered(Layout l){
        RiverCamera c=l.camera;
        for(float rise:new float[]{0,GroundArt.LAND_RISE})for(int i=0;i<4;i++){
            float sx=i%2*l.width,sy=i/2*l.height+rise*c.scale;
            float x=c.worldX(sx,sy),y=c.worldY(sx,sy);
            check(x>=TrainingScenery.MIN_X+1.9f && x<=TrainingScenery.MAX_X-1.9f &&
                    y>=TrainingScenery.MIN_Y+1.9f && y<=TrainingScenery.MAX_Y-1.9f,"all viewport corners stay inside scenery, including raised land");
        }
    }
    public static void run(){
        for(float width:new float[]{960,1333,1800,2400})for(float zoom:new float[]{.2f,.65f,1.4f,2.4f,4.5f})
            for(float x:new float[]{TrainingScenery.MIN_X-400,-1500,300,TrainingScenery.MAX_X+400})
                for(float y:new float[]{TrainingScenery.MIN_Y-400,300,TrainingScenery.MAX_Y+400}){
                    Layout l=TrainingScenery.constrain(Layout.focused(width,600,x,y,zoom,width*.5f,300));covered(l);
                    Layout twice=TrainingScenery.constrain(l);
                    check(Math.abs(l.camera.worldTop-twice.camera.worldTop)<.002f && Math.abs(l.camera.worldRight-twice.camera.worldRight)<.002f,"coverage projection is stable");
                }
        for(float width:new float[]{960,1333,1800})for(float[] r:TrainingChannel.WATER_RECTANGLES){
            float x=(r[0]+r[2])*.5f,y=(r[1]+r[3])*.5f;
            Layout l=TrainingScenery.constrain(Layout.focused(width,600,x,y,.65f,width*.5f,300),x,y);
            covered(l);float sx=l.camera.screenX(x,y),sy=l.camera.screenY(x,y);
            check(sx>=63.99f && sx<=width-63.99f && sy>=111.99f && sy<=480.01f,"boundary panning keeps the tug clear of the HUD");
        }
        Layout interior=Layout.focused(960,600,-800,0,2.4f,480,300);
        check(TrainingScenery.constrain(interior)==interior,"interior camera unchanged");
        Game root=new Game();root.startTutorial();Game g=root.activeGame();covered(g.layout);
        g.configureCanvas(1800,600);covered(g.layout);
        g.changeCamera(Layout.focused(1800,600,TrainingChannel.ENTRY_LEFT,650,.1f,900,300));covered(g.layout);
        float px=620,py=320,dx=590,dy=340;
        g.tutorial.entering=false;g.dialogue.active=false;
        g.down(g.layout.camera.worldX(px,py),g.layout.camera.worldY(px,py));
        g.pointerX=g.layout.camera.worldX(px,py);g.pointerY=g.layout.camera.worldY(px,py);
        g.downX=g.layout.camera.worldX(dx,dy);g.downY=g.layout.camera.worldY(dx,dy);
        g.changeCamera(Layout.focused(1800,600,600,-1000,.65f,900,300));covered(g.layout);
        check(Math.abs(g.layout.camera.screenX(g.pointerX,g.pointerY)-px)<.01f && Math.abs(g.layout.camera.screenY(g.downX,g.downY)-dy)<.01f,"bounded camera preserves screen drag anchors");
        for(int i=0;i<TrainingChannelView.FACTORY_PLACEMENTS;i++){
            int f=TrainingChannelView.factoryCell(i);float x=TrainingChannelView.factoryX(i),y=TrainingChannelView.factoryY(i);
            check(TrainingScenery.nearWater(x-FactoryArt.footprintX(f),y-FactoryArt.footprintY(f),2*FactoryArt.footprintX(f),2*FactoryArt.footprintY(f)),"no factory in distant filler");
        }
        System.out.println(checks+" finite scenery and camera coverage assertions passed");
    }
}
