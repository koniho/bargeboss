package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Exports actual shared-renderer animation, with user gestures and original real-time durations. */
public final class AnimationPreview {
    private static void frame(Game g,String sequence,int frame) throws Exception {
        File directory=new File("out/animation-frames/"+sequence); directory.mkdirs();
        BufferedImage image=new BufferedImage(600,960,BufferedImage.TYPE_INT_RGB);
        Graphics2D p=image.createGraphics(); p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.render(new DesktopPainter(p)); p.dispose();
        ImageIO.write(image,"png",new File(directory,String.format("%04d.png",frame)));
    }
    public static void main(String[] args) throws Exception {
        Game g=new Game();
        for(int tick=0;tick<180;tick++) { g.advance(1.0/60); if(tick%5==0) frame(g,"title",tick/5); }
        g.reset(); Body b=g.cargo[0]; ReplayInput.down(g,b.x,b.y);
        int phase=0,age=0,frame=0; float ropeX=0,ropeY=0;
        for(int tick=0;tick<900;tick++) {
            if(phase==0 && g.canTie(b)) { ReplayInput.up(g,g.pointerX,g.pointerY); phase=1; age=0; }
            if(phase==1 && age>=30) { ReplayInput.down(g,b.x,b.y); ropeX=b.x; ropeY=b.y; phase=2; age=0; }
            if(phase==2) {
                float t=Math.min(1,age/45f);
                ReplayInput.move(g,ropeX+(Game.TIE_X[2]-ropeX)*t,ropeY+(Game.DOCK_Y[2]-ropeY)*t);
                if(age==60) { ReplayInput.up(g,Game.TIE_X[2],Game.DOCK_Y[2]); phase=3; age=0; }
            }
            g.step(); age++;
            if(tick%5==0) frame(g,"mooring",frame++);
            if(phase==3 && age>360) break;
        }
        if(g.delivered!=1) throw new IllegalStateException("Animated sequence failed to unload");
        g.mode=Game.FINISH;
        for(int tick=0;tick<120;tick++) { g.advance(1.0/60); if(tick%5==0) frame(g,"results",tick/5); }
        System.out.println("12 fps animation frames exported to out/animation-frames/");
    }
}
