package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/** Desktop Java2D draw cost and per-tick allocation of the towing-challenge basin against existing
 * training fixtures. JVM timings are relative evidence only; native frame pacing needs device runs. */
public final class TowingChallengeProfile {
    public static void main(String[] args){
        String[] scenes=args.length>0?args:new String[]{"towing-challenge","towing-challenge-wide","barge-bridge","harbor-close","tutorial-yard","canal-middle"};
        com.sun.management.ThreadMXBean threads=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
        long thread=Thread.currentThread().getId();
        System.out.println("scene,drawMedianMs,drawP95Ms,calls,sprites,polys,lines,layerCalls,stepAllocBytes,drawAllocBytes,barges,idleStepAllocBytes");
        for(String scene:scenes){
            Game root=new Game();ProfileScenario.prepare(root,scene,1320,600);Game g=root.activeGame();
            BufferedImage image=new BufferedImage(1320,600,BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics=image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            DesktopPainter painter=new DesktopPainter(graphics);
            boolean wide=scene.endsWith("-wide");Layout held=g.layout;
            for(int i=0;i<40;i++){if(!wide)ProfileScenario.replayTick(root);else g.layout=held;root.render(painter);}
            int frames=150;long[] times=new long[frames];long stepAlloc=0,drawAlloc=0;
            for(int i=0;i<frames;i++){
                long a=threads.getThreadAllocatedBytes(thread);
                if(wide){g.layout=held;root.step();g.layout=held;}else ProfileScenario.replayTick(root);
                long b=threads.getThreadAllocatedBytes(thread);
                long start=System.nanoTime();root.render(painter);times[i]=System.nanoTime()-start;
                long c=threads.getThreadAllocatedBytes(thread);
                stepAlloc+=b-a;drawAlloc+=c-b;
            }
            Arrays.sort(times);
            ScenePerformanceBenchmark.Counter counter=new ScenePerformanceBenchmark.Counter(1320,600,true);
            counter.beginFrame();Renderer.draw(root,counter);counter.clear();counter.beginFrame();Renderer.draw(root,counter);
            int barges=0;RiverCamera cam=g.layout.camera;
            for(Body b:g.cargo)if(b!=null && b.active){float x=cam.screenX(b.x,b.y),y=cam.screenY(b.x,b.y);if(x>0 && x<1320 && y>0 && y<600)barges++;}
            // Input-free ticks isolate simulation allocation from the replay player's gestures.
            if(g.pointer)g.cancelInputs();long idle=threads.getThreadAllocatedBytes(thread);for(int i=0;i<60;i++)root.step();idle=(threads.getThreadAllocatedBytes(thread)-idle)/60;
            System.out.printf("%s,%.2f,%.2f,%d,%d,%d,%d,%d,%d,%d,%d,%d%n",scene,times[frames/2]/1e6,times[frames*95/100]/1e6,counter.calls(),counter.sprites,counter.polys,counter.lines,counter.layerCalls,stepAlloc/frames,drawAlloc/frames,barges,idle);
            graphics.dispose();
        }
    }
}
