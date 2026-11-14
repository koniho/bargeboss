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
        Game g=new Game(); capture(g,"title");
        g.mode=Game.CHALLENGES; capture(g,"challenges-locked"); g.mode=Game.TITLE;
        for(int i=0;i<90;i++) g.advance(1.0/60); capture(g,"title-animated");
        g.reset(); capture(g,"stage");
        steps(g,120); capture(g,"feeder-inbound");
        steps(g,45); capture(g,"feeder-dropoff");
        steps(g,65); capture(g,"feeder-return");
        g.reset(); g.tug.x=260; g.tug.y=760; steps(g,180); capture(g,"feeder-blocked");
        sheet("feeder-phases",new String[]{"feeder-inbound","feeder-dropoff","feeder-return","feeder-blocked"});
        g.reset();
        ReplayInput.down(g,g.tug.x,g.tug.y); ReplayInput.move(g,g.downX,g.downY-100);
        steps(g,45); capture(g,"driving"); ReplayInput.up(g,g.pointerX,g.pointerY);
        steps(g,4); capture(g,"coasting"); ReplayInput.tap(g,Layout.BRAKE); steps(g,4); capture(g,"braking");
        ReplayInput.controlFixture(g); ReplayInput.down(g,g.cargo[0].x,g.cargo[0].y); steps(g,6); capture(g,"holding");
        for(int i=0;i<1200;i++) { g.step(); if(g.control==Game.ALIGNING) { capture(g,"aligning"); break; } }
        for(int i=0;i<1200;i++) { g.step(); if(g.control==Game.PUSHING) { capture(g,"push"); break; } }
        ReplayInput.move(g,g.downX-65,g.downY-65); steps(g,20); capture(g,"steering");
        ReplayInput.controlFixture(g); Body b=g.cargo[0]; ReplayInput.down(g,b.x,b.y);
        for(int i=0;i<1200 && !g.canTie(b);i++) g.step();
        if(!g.canTie(b)) throw new IllegalStateException("Preview cargo never reached berth");
        ReplayInput.up(g,g.pointerX,g.pointerY); capture(g,"ready-to-tie");
        ReplayInput.down(g,b.x,b.y); ReplayInput.move(g,b.x+65,b.y-60); capture(g,"aim-miss");
        ReplayInput.up(g,b.x+65,b.y-60); steps(g,Game.ROPE_FLIGHT_TICKS+8); capture(g,"retracting");
        steps(g,Game.ROPE_RETURN_TICKS-8);
        ReplayInput.down(g,b.x,b.y); ReplayInput.move(g,Game.TIE_X[2]+8,Game.DOCK_Y[2]-8); capture(g,"aiming");
        ReplayInput.up(g,Game.TIE_X[2]+8,Game.DOCK_Y[2]-8); steps(g,9); capture(g,"lasso");
        steps(g,9); capture(g,"bollard-hit"); steps(g,4);capture(g,"line-snap");
        steps(g,8);capture(g,"tying-up");
        steps(g,24);capture(g,"drawing-alongside");
        waitUnload(g,b,1);capture(g,"parallel-to-shore");
        waitUnload(g,b,85); capture(g,"unloading");
        waitUnload(g,b,Game.UNLOAD_TICKS); steps(g,12); capture(g,"delivery");
        steps(g,90); capture(g,"crew-idle");
        g.background(); capture(g,"pause"); g.pause();
        ReplayInput.tap(g,Layout.HELP); capture(g,"help"); ReplayInput.tap(g,Layout.START);
        g.tug.x=Physics.RIGHT-15; g.tug.vx=100; g.step(); capture(g,"impact");
        g.reset(); g.ticks=1000; capture(g,"traffic-warning");
        g.ticks=1200; capture(g,"traffic");
        g.reset();
        for(int i=0;i<Game.SHIFT_TICKS;i++) { ReplayInput.work(g); g.step(); }
        capture(g,"shift-complete");
        for(int i=0;i<60;i++) g.advance(1.0/60); capture(g,"results-animated");
        g.mode=Game.CHALLENGES; capture(g,"challenges-unlocked");
        g.reset(); steps(g,Game.SHIFT_TICKS); capture(g,"shift-ended");
        g=new Game(); ReplayInput.tap(g,Layout.LEARN); capture(g,"tutorial-0");
        int previous=0;
        for(int i=0;i<3000;i++) {
            Game a=g.activeGame(); ReplayInput.tutorial(g); g.step();
            if(a.tutorial.lesson!=previous) { previous=a.tutorial.lesson; capture(g,"tutorial-"+previous); }
            if(previous==Tutorial.DONE) break;
        }
        if(previous!=Tutorial.DONE) throw new IllegalStateException("Preview tutorial failed");
        g=new Game();g.startTutorial();Game tow=g.activeGame();
        tow.tutorial.lesson=Tutorial.CONNECT;tow.tutorial.setup(tow);b=tow.cargo[0];
        ReplayInput.down(g,b.x,b.y);ReplayInput.move(g,tow.tug.x,tow.tug.y);capture(g,"tow-aim");
        ReplayInput.up(g,tow.tug.x,tow.tug.y);steps(g,9);capture(g,"tow-flight");
        steps(g,9);capture(g,"tow-catch");
        ReplayInput.down(g,tow.tug.x,tow.tug.y);ReplayInput.move(g,tow.downX,tow.downY-100);
        steps(g,60);capture(g,"tow-pulling");
        ReplayInput.up(g,tow.pointerX,tow.pointerY);g=tow;tow.tutorial=null;ReplayInput.tap(g,Layout.RELEASE);steps(g,5);capture(g,"tow-release");
        sheet("tow-phases",new String[]{"tow-aim","tow-flight","tow-catch","tow-pulling","tow-release","tutorial-8"});
        g=new Game();g.reset();steps(g,180);capture(g,"first-watch-overview");
        ReplayInput.down(g,260,730);for(int i=0;i<1800 && !g.canTie(g.cargo[0]);i++) g.step();steps(g,180);capture(g,"docking-zoom");
        g=new Game();g.challengeId=1;g.reset();capture(g,"bridge-start");
        boolean wait=false,opening=false,open=false,dock=false;
        for(int i=0;i<9000 && !dock;i++) {
            ReplayInput.work(g);g.step();
            if(!wait && g.bridge.phase==Bridge.WAITING && g.bridge.ticks>240) { capture(g,"bridge-wait");wait=true; }
            if(!opening && g.bridge.phase==Bridge.OPENING && g.bridge.ticks>60) { capture(g,"bridge-opening");opening=true; }
            if(!open && g.bridge.phase==Bridge.OPEN && Math.abs(g.tug.y-Bridge.Y)<100) { capture(g,"bridge-open");open=true; }
            for(Body load:g.cargo) if(load!=null && load.active && load.unloadingTicks>30) { capture(g,"bridge-destination");dock=true;break; }
        }
        if(!wait || !opening || !open || !dock) throw new IllegalStateException("Bridge preview did not complete passage");
        g=new Game();g.reset();for(Feeder feeder:g.feeders){feeder.phase=Feeder.IDLE;feeder.load=null;}
        b=new Body(1,2,Physics.LEFT+17.5f,g.dockY(2),(float)Math.PI/2);g.cargo[0]=b;
        g.tug.x=300;g.tug.y=b.y+120;g.layout=new Layout(960,600,148,380,1.25f);
        capture(g,"shore-ready-to-tie");ReplayInput.lasso(g,b);steps(g,Game.ROPE_FLIGHT_TICKS+5);
        capture(g,"shore-lasso-catch");
        sheet("bridge-progression",new String[]{"first-watch-overview","docking-zoom","bridge-wait","bridge-opening","bridge-open","bridge-destination"});
        sheet("lasso-phases",new String[]{"aim-miss","retracting","aiming","lasso","bollard-hit","line-snap"});
        sheet("screens",new String[]{"title","title-animated","help","pause","shift-complete","shift-ended"});
        sheet("all-phases",new String[]{"stage","driving","coasting","braking","holding","aligning","push","steering","ready-to-tie","lasso","tying-up","unloading","delivery","crew-idle","impact","traffic-warning","traffic","shift-complete"});
        sheet("control-states",new String[]{"driving","holding","steering","lasso","unloading","delivery"});
        sheet("tutorial",new String[]{"tutorial-0","tutorial-1","tutorial-2","tutorial-3","tutorial-4","tutorial-5","tutorial-6","tutorial-7","tutorial-8","tutorial-9"});
        sheet("assist-states",new String[]{"holding","aligning","steering"});
        sheet("mooring",new String[]{"ready-to-tie","lasso","tying-up","unloading","delivery","stage"});
        System.out.println("Shared renderer: control-states.png, tutorial.png and mooring.png in out/");
    }
    private static void waitUnload(Game g,Body b,int target) {
        for(int i=0;i<1200 && b.unloadingTicks<target && g.mode==Game.PLAY;i++) g.step();
        if(b.unloadingTicks<target) throw new IllegalStateException("Preview unloading stalled at "+b.unloadingTicks+" of "+target);
    }
    private static void capture(Game game,String name) throws Exception {
        BufferedImage image=new BufferedImage(Layout.WIDTH,Layout.HEIGHT,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(game,new DesktopPainter(g)); g.dispose(); ImageIO.write(image,"png",new File("out/"+name+".png"));
    }
    private static void sheet(String name,String[] scenes) throws Exception {
        BufferedImage sheet=new BufferedImage(960,330*((scenes.length+1)/2),BufferedImage.TYPE_INT_RGB);
        Graphics2D p=sheet.createGraphics(); p.setColor(new Color(0x101e25)); p.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
        p.setFont(new Font("SansSerif",Font.BOLD,14));
        for(int i=0;i<scenes.length;i++) {
            int x=i%2*480,y=i/2*330; p.setColor(new Color(0xeee7d4)); p.drawString(scenes[i].toUpperCase(),x+12,y+20);
            p.drawImage(ImageIO.read(new File("out/"+scenes[i]+".png")),x,y+30,480,300,null);
        }
        p.dispose(); ImageIO.write(sheet,"png",new File("out/"+name+".png"));
    }
}
