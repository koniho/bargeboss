package com.bargeboss.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Capture the complete real tutorial and reject accidental legacy scenery draws. */
public final class TutorialSceneryPreview {
    private static BufferedImage capture(Game root,String name) throws Exception {
        BufferedImage image=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics();root.render(new ReviewedPainter(new DesktopPainter(graphics)));
        graphics.dispose();ImageIO.write(image,"png",new File("out/tutorial-scenery/"+name+".png"));return image;
    }
    public static void main(String[] args) throws Exception {
        new File("out/tutorial-scenery").mkdirs();
        BufferedImage[] lessons=new BufferedImage[11];String[] titles=new String[11];
        Game root=new Game();root.startTutorial();Game g=root.activeGame();
        int previous=-1,channelTicks=0,ticks=0;
        while(ticks++<16000) {
            int lesson=g.tutorial.lesson;
            if(lesson!=previous) {
                lessons[lesson]=capture(root,"lesson-"+lesson);titles[lesson]=g.tutorial.title();previous=lesson;
            }
            if(lesson==Tutorial.DONE) break;
            if(lesson==Tutorial.CHANNEL) {
                channelTicks++;
                if(g.tutorial.clearedGate==1 && g.tutorial.gateClearTicks==78) capture(root,"gate-cleared");
                if(channelTicks==700) capture(root,"canal-middle");
                if(channelTicks==1700) capture(root,"canal-upstream");
            }
            ReplayInput.tutorial(root);root.step();
        }
        if(g.tutorial.lesson!=Tutorial.DONE) throw new AssertionError("Tutorial stopped at lesson "+g.tutorial.lesson+" / gate "+g.tutorial.channelGate);
        for(int i=0;i<lessons.length;i++) if(lessons[i]==null) throw new AssertionError("Missing lesson "+i);
        BufferedImage sheet=new BufferedImage(1920,3720,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=sheet.createGraphics();
        graphics.setColor(new Color(17,30,40));graphics.fillRect(0,0,1920,3720);graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));
        for(int i=0;i<lessons.length;i++) {
            int x=i%2*960,y=i/2*620;graphics.setColor(Color.WHITE);graphics.drawString((i<10?"LESSON "+(i+1):"COMPLETE")+" — "+titles[i],x+12,y+16);
            graphics.drawImage(lessons[i],x,y+20,null);
        }
        graphics.dispose();ImageIO.write(sheet,"png",new File("out/tutorial-scenery/BargeBoss-isometric-tutorial-v3.png"));
        int samples=0;
        for(int x=120;x<=1540;x+=5) for(int side=-1;side<=1;side+=2) {
            float y=TrainingChannelView.propY(x,side,44);
            for(int dx=-44;dx<=44;dx+=4) for(int dy=-44;dy<=44;dy+=4) {
                if(TrainingChannel.contains(x+dx,y+dy)) throw new AssertionError("Prop enters canal at "+x+","+y);
                samples++;
            }
        }
        System.out.println("All10 lessons completed in "+ticks+" ticks; no legacy SCENERY draws; "+samples+" prop-footprint samples clear of water.");
    }
    private static final class ReviewedPainter implements Painter {
        final Painter delegate;ReviewedPainter(Painter p) { delegate=p; }
        private void check(int atlas) { if(atlas==SpriteAtlas.SCENERY) throw new AssertionError("Legacy scenery in tutorial"); }
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha) {check(a);delegate.sprite(a,f,x,y,w,h,r,alpha);}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float b,float alpha) {check(a);delegate.spriteBlend(a,f,ff,x,y,w,h,r,b,alpha);}
        public void rect(float x,float y,float w,float h,int c) {delegate.rect(x,y,w,h,c);}
        public void circle(float x,float y,float r,int c) {delegate.circle(x,y,r,c);}
        public void poly(float[] points,int c) {delegate.poly(points,c);}
        public void line(float x,float y,float xx,float yy,float w,int c) {delegate.line(x,y,xx,yy,w,c);}
    }
}
