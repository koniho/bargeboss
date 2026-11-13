package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Every capture is a real shared-renderer state driven through the controls. */
public final class Preview {
    private static void tap(Game g,float x,float y) { g.pointerDown(0,x,y); g.pointerUp(0,x,y); }
    private static void steps(Game g,int n) { for(int i=0;i<n;i++) g.step(); }
    public static void main(String[] args) throws Exception {
        new File("out").mkdirs();
        Game g=new Game(); capture(g,"title"); g.reset(); capture(g,"stage");
        tap(g,110,831); steps(g,40); capture(g,"forward");
        g.pointerDown(1,510,865); steps(g,45); capture(g,"turning"); g.pointerUp(1,510,865);
        tap(g,110,864); steps(g,6); capture(g,"coasting");
        tap(g,290,908); steps(g,4); capture(g,"braking");
        g.reset(); tap(g,110,900); steps(g,55); capture(g,"reverse");
        g.reset(); tap(g,110,831);
        for(int i=0;i<1200;i++) { g.step(); if(g.control==Game.PUSHING) { capture(g,"push"); break; } }
        for(int i=0;i<1200;i++) { g.step(); if(g.cargo[0].dockTime>.3f) { capture(g,"docking"); break; } }
        g.background(); capture(g,"pause");
        g.reset(); tap(g,290,790); g.down(g.cargo[0].x,g.cargo[0].y); steps(g,6); capture(g,"holding");
        for(int i=0;i<1200;i++) { g.step(); if(g.control==Game.ALIGNING) { capture(g,"aligning"); break; } }
        g.move(g.downX-65,g.downY-65); steps(g,25); capture(g,"steering");
        g=new Game(); tap(g,300,608); capture(g,"tutorial-0");
        int previous=0;
        for(int i=0;i<2400;i++) {
            Game a=g.activeGame();
            switch(a.tutorial.lesson) {
                case Tutorial.THROTTLE: if(a.helm.throttle==0) tap(g,110,831); break;
                case Tutorial.WHEEL: if(a.helm.rudder==0) g.pointerDown(1,510,865); break;
                case Tutorial.STOP: g.pointerUp(1,510,865); if(!a.braking) tap(g,290,908); break;
                case Tutorial.REVERSE:
                    if(!a.tutorial.reversed && a.helm.throttle==0) tap(g,110,900);
                    else if(a.tutorial.reversed && !a.braking) tap(g,290,908); break;
                case Tutorial.DOCK: if(a.helm.throttle==0) tap(g,110,831); break;
                default: break;
            }
            g.step();
            if(a.tutorial.lesson!=previous) { previous=a.tutorial.lesson; capture(g,"tutorial-"+previous); }
            if(previous==Tutorial.DONE) break;
        }
        if(previous!=Tutorial.DONE) throw new IllegalStateException("Preview tutorial failed");
        sheet("control-states",new String[]{"forward","turning","reverse","coasting","push","docking"});
        sheet("tutorial",new String[]{"tutorial-0","tutorial-1","tutorial-2","tutorial-3","tutorial-4","tutorial-5"});
        sheet("assist-states",new String[]{"holding","aligning","steering"});
        System.out.println("Shared renderer: control-states.png, tutorial.png and assist-states.png in out/");
    }
    private static void capture(Game game,String name) throws Exception {
        BufferedImage image=new BufferedImage(600,960,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(game,new DesktopPainter(g)); g.dispose(); ImageIO.write(image,"png",new File("out/"+name+".png"));
    }
    private static void sheet(String name,String[] scenes) throws Exception {
        BufferedImage sheet=new BufferedImage(900,510*((scenes.length+2)/3),BufferedImage.TYPE_INT_RGB);
        Graphics2D p=sheet.createGraphics(); p.setColor(new Color(0x101e25)); p.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
        p.setFont(new Font("SansSerif",Font.BOLD,14));
        for(int i=0;i<scenes.length;i++) {
            int x=i%3*300,y=i/3*510; p.setColor(new Color(0xeee7d4)); p.drawString(scenes[i].toUpperCase(),x+12,y+20);
            p.drawImage(ImageIO.read(new File("out/"+scenes[i]+".png")),x,y+30,300,480,null);
        }
        p.dispose(); ImageIO.write(sheet,"png",new File("out/"+name+".png"));
    }
}
