package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual shared UI composition, including small-screen controls and selected replies. */
public final class UiStyleReview {
    private static final File OUT=new File("out/ui-type-v3");
    private static void capture(Game root,String name)throws Exception{
        Game g=root.activeGame();
        BufferedImage image=new BufferedImage((int)g.layout.width,(int)g.layout.height,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(root,new DesktopPainter(graphics));graphics.dispose();
        ImageIO.write(image,"png",new File(OUT,name+"-"+(int)g.layout.width+".png"));
    }
    public static void main(String[] args)throws Exception{
        OUT.mkdirs();
        for(int width:new int[]{960,1333}){
            Game g=new Game();g.configureCanvas(width,600);capture(g,"title");
            g.openSettings();capture(g,"settings");
            g=new Game();g.configureCanvas(width,600);g.mode=Game.CHALLENGES;capture(g,"locations");
            ProfileScenario.prepare(g,"calumet-inlet",width,600);capture(g,"driving");
            g.settings.setJoystickSide(GameSettings.RIGHT);capture(g,"driving-right");
            g.pause();capture(g,"pause");
            g.mode=Game.HELP;capture(g,"help");
            g.mode=Game.FINISH;g.earnedStars=2;capture(g,"result");
            Game root=new Game();ProfileScenario.prepare(root,"instructor-teach",width,600);capture(root,"lesson");
            g=root.activeGame();g.dialogue.show(g,"HARBOR CONTROL","CHOOSE YOUR NEXT ASSIGNMENT","ALL CREWS ARE STANDING BY.",
                "TOW TO THE NORTH BERTH.","SHIFT THE COAL BARGE.","WAIT FOR THE BRIDGE.","REQUEST MORE INSTRUCTIONS.");
            for(int i=0;i<160;i++)root.step();capture(root,"radio");
            Layout.Box b=g.dialogue.choiceBox(g,0);root.pointerDown(75,b.centerX(),b.centerY());capture(root,"radio-selected");
            root=new Game();ProfileScenario.prepare(root,"barge-abilities",width,600);capture(root,"ability");
        }
        System.out.println("UI review frames saved to "+OUT);
    }
}
