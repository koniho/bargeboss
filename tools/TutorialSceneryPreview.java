package com.bargeboss.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Capture the complete real tutorial and reject accidental legacy scenery draws. */
public final class TutorialSceneryPreview {
    private static String directory="out/tutorial-scenery";
    private static int shownVariants,maxFactoryDraws,maxVisibleFactories;
    private static BufferedImage capture(Game root,String name) throws Exception {
        BufferedImage image=new BufferedImage(Math.round(root.activeGame().layout.width),Math.round(root.activeGame().layout.height),BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        ReviewedPainter reviewed=new ReviewedPainter(new DesktopPainter(graphics),image.getWidth(),image.getHeight());
        root.render(reviewed);shownVariants|=reviewed.variants;
        maxFactoryDraws=Math.max(maxFactoryDraws,reviewed.factoryDraws);maxVisibleFactories=Math.max(maxVisibleFactories,reviewed.visibleFactories);
        if(reviewed.factoryDraws>128)throw new AssertionError("Unbounded factory drawing in "+name+": "+reviewed.factoryDraws);
        System.out.println(name+": "+reviewed.factoryDraws+" factory draws / "+reviewed.visibleFactories+" substantially visible");
        graphics.dispose();ImageIO.write(image,"png",new File(directory+"/"+name+".png"));return image;
    }
    public static void main(String[] args) throws Exception {
        int width=args.length>0?Integer.parseInt(args[0]):960,height=args.length>1?Integer.parseInt(args[1]):600;
        if(width!=960 || height!=600) directory="out/tutorial-scenery-"+width+"x"+height;
        if(args.length>2)directory=args[2];
        new File(directory).mkdirs();
        BufferedImage[] lessons=new BufferedImage[11];String[] titles=new String[11];
        Game root=new Game();root.configureCanvas(width,height);root.startTutorial();Game g=root.activeGame();
        int previous=-1,channelTicks=0,ticks=0,harborTicks=0,turnTicks=0;boolean basinExit=false;
        while(ticks++<16000) {
            int lesson=g.tutorial.lesson;
            if(lesson!=previous) {
                lessons[lesson]=capture(root,"lesson-"+lesson);titles[lesson]=g.tutorial.title();previous=lesson;
            }
            if(lesson==Tutorial.DONE) break;
            if(lesson==Tutorial.MOVE && g.tutorial.driveMarker==1 && ++turnTicks==60) capture(root,"open-water-turn");
            if(lesson==Tutorial.CHANNEL) {
                channelTicks++;
                if(!basinExit && g.tug.x>TrainingChannel.MIN+15) {capture(root,"basin-exit");basinExit=true;}
                if(g.tutorial.clearedGate==1 && g.tutorial.gateClearTicks==78) capture(root,"gate-cleared");
                if(channelTicks==700) capture(root,"canal-middle");
                if(channelTicks==1700) capture(root,"canal-upstream");
            }
            if(lesson>=Tutorial.HOLD && harborTicks<121) {
                if(harborTicks%30==0) capture(root,"harbor-entry-"+(harborTicks/30));
                harborTicks++;
            }
            if(lesson==Tutorial.TIE && g.cargo[0].unloadingTicks==20) capture(root,"unloading-hook");
            ReplayInput.tutorial(root);root.step();
        }
        if(g.tutorial.lesson!=Tutorial.DONE) throw new AssertionError("Tutorial stopped at lesson "+g.tutorial.lesson+" / gate "+g.tutorial.channelGate);
        for(int i=0;i<lessons.length;i++) if(lessons[i]==null) throw new AssertionError("Missing lesson "+i);
        BufferedImage sheet=new BufferedImage(width*2,(height+20)*6,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=sheet.createGraphics();
        graphics.setColor(new Color(17,30,40));graphics.fillRect(0,0,width*2,(height+20)*6);graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));
        for(int i=0;i<lessons.length;i++) {
            int x=i%2*width,y=i/2*(height+20);graphics.setColor(Color.WHITE);graphics.drawString((i<10?"LESSON "+(i+1):"COMPLETE")+" — "+titles[i],x+12,y+16);
            graphics.drawImage(lessons[i],x,y+20,null);
        }
        graphics.dispose();ImageIO.write(sheet,"png",new File(directory+"/BargeBoss-continuous-tutorial-v1.png"));
        int samples=0;
        for(int slot=0;slot<TrainingChannelView.FACTORY_PLACEMENTS;slot++) {
            int cell=TrainingChannelView.factoryCell(slot);
            float x=TrainingChannelView.factoryX(slot),y=TrainingChannelView.factoryY(slot);
            float hx=FactoryArt.footprintX(cell),hy=FactoryArt.footprintY(cell);
            for(float dx=-hx;dx<=hx;dx+=4) for(float dy=-hy;dy<=hy;dy+=4) {
                if(TrainingChannel.contains(x+dx,y+dy)) throw new AssertionError("Factory enters water at "+x+","+y);
                samples++;
            }
        }
        System.out.println("All10 lessons completed in "+ticks+" ticks; no legacy scenery draws; "+samples+" factory-footprint samples clear of water.");
        System.out.println("Factory capture audit: "+Integer.bitCount(shownVariants)+" / "+FactoryArt.COUNT+" variants; mask="+Integer.toHexString(shownVariants)
                +"; max draws="+maxFactoryDraws+", max substantially visible="+maxVisibleFactories+", total placed="+TrainingChannelView.FACTORY_PLACEMENTS);
    }
    private static final class ReviewedPainter implements Painter {
        final Painter delegate;final float width,height;int factoryDraws,visibleFactories,variants;
        ReviewedPainter(Painter p,float w,float h) {delegate=p;width=w;height=h;}
        private void check(int atlas) {
            if(atlas==SpriteAtlas.SCENERY || atlas==SpriteAtlas.STATIC_HARBOR || atlas==SpriteAtlas.TUTORIAL_SCENERY || atlas==SpriteAtlas.CHIMNEYS || atlas==SpriteAtlas.BRIDGE)
                throw new AssertionError("Legacy scenery in tutorial: atlas "+atlas);
        }
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha) {
            check(a);
            if(a==SpriteAtlas.FACTORY) {
                factoryDraws++;
                float[] bounds=FactoryArtData.BOUNDS[f];
                float left=x+(bounds[0]-FactoryArt.pivotX(f))*w,top=y+(bounds[1]-FactoryArt.pivotY(f))*h;
                float right=x+(bounds[2]-FactoryArt.pivotX(f))*w,bottom=y+(bounds[3]-FactoryArt.pivotY(f))*h;
                float area=(right-left)*(bottom-top);
                float overlap=Math.max(0,Math.min(right,width)-Math.max(left,0))*Math.max(0,Math.min(bottom,height)-Math.max(top,0));
                if(alpha>=.25f && overlap>area*.15f) {visibleFactories++;variants|=1<<f;}
            }
            delegate.sprite(a,f,x,y,w,h,r,alpha);
        }
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float b,float alpha) {check(a);delegate.spriteBlend(a,f,ff,x,y,w,h,r,b,alpha);}
        public void rect(float x,float y,float w,float h,int c) {delegate.rect(x,y,w,h,c);}
        public void circle(float x,float y,float r,int c) {delegate.circle(x,y,r,c);}
        public void poly(float[] points,int c) {delegate.poly(points,c);}
        public void line(float x,float y,float xx,float yy,float w,int c) {delegate.line(x,y,xx,yy,w,c);}
    }
}
