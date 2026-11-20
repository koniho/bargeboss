package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual continuous practice, driven using shared pointer gestures. */
public final class TrainReview {
    private static void capture(Game root,String name)throws Exception {
        BufferedImage image=new BufferedImage(1333,600,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();
        p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(root,new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File("out/"+name+".png"));
    }
    public static void main(String[] args)throws Exception {
        Game root=new Game();root.configureCanvas(1333,600);root.startTutorial();Game g=root.activeGame();
        g.tutorial.lesson=Tutorial.CONNECT;g.tutorial.setup(g);boolean aim=false,pull=false,release=false,bridge=false;
        int trainTicks=0;
        for(int tick=0;tick<16000 && g.tutorial.lesson!=Tutorial.DONE;tick++) {
            if(g.tutorial.lesson==Tutorial.LINK && !aim && g.ropePhase==0 && g.tow.incoming(g.cargo[2])==null) {
                Body b=g.cargo[2],parent=g.cargo[0];ReplayInput.down(root,b.x,b.y);ReplayInput.move(root,parent.x,parent.y);
                capture(root,"train-link-aim");ReplayInput.up(root,parent.x,parent.y);aim=true;
            } else ReplayInput.tutorial(root);
            root.step();
            if(g.tutorial.lesson==Tutorial.TRAIN && !pull && g.cargo[0].y<-240){capture(root,"train-pulling");pull=true;}
            if(g.tutorial.lesson==Tutorial.BRIDGE && !bridge && g.bridge.passable() && g.tug.y<Bridge.Y && g.cargo[2].y>Bridge.Y){capture(root,"train-double-bridge");bridge=true;}
            if(g.tutorial.lesson==Tutorial.RELEASE && !release){capture(root,"train-uncouple");release=true;}
        }
        if(!aim || !pull || !release || !bridge || g.tutorial.lesson!=Tutorial.DONE)throw new AssertionError("Train tutorial review did not complete: "+g.tutorial.lesson);
        capture(root,"train-tutorial-complete");System.out.println("Barge train linking, loaded pull and uncoupling exported to out/");
    }
}
