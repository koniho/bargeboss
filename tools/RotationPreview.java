package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Real atlas rendering: one full turn around stationary, always-visible crosshairs. */
public final class RotationPreview {
    private static final int FRAMES=240;
    private static final String[] NAMES={"TUG","EMPTY BARGE","STEEL BARGE","FREIGHT BARGE","BULK BARGE","BULK PILE"};
    private RotationPreview() {}
    public static void main(String[] args) throws Exception {
        File directory=new File("out/animation-frames/rotation"); directory.mkdirs();
        Game g=new Game(); g.reset(); g.visualTicks=0;
        for(int i=0;i<FRAMES;i++) {
            float angle=(float)(i*Math.PI*2/FRAMES);
            BufferedImage image=render(g,angle);
            ImageIO.write(image,"png",new File(directory,String.format("%04d.png",i)));
            if(i==0) ImageIO.write(image,"png",new File("out/rotation-centers.png"));
        }
        // Quarter-size snapshots make all thirty-two exact source headings reviewable.
        BufferedImage sheet=new BufferedImage(1920,600,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=sheet.createGraphics();
        for(int i=0;i<32;i++) {
            BufferedImage image=render(g,(float)(i*Math.PI*2/32));
            graphics.drawImage(image,(i%8)*240,(i/8)*150,240,150,null);
        }
        graphics.dispose(); ImageIO.write(sheet,"png",new File("out/rotation-headings.png"));
        System.out.println("Stationary 360-degree sprite preview: "+FRAMES+" frames at 30fps in out/animation-frames/rotation/");
    }
    private static BufferedImage render(Game g,float angle) {
        BufferedImage image=new BufferedImage(Layout.WIDTH,Layout.HEIGHT,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        DesktopPainter painter=new DesktopPainter(graphics);
        painter.rect(0,0,Layout.WIDTH,Layout.HEIGHT,VisualStyle.NAVY);
        Lettering.draw(painter,"FIXED PIVOT / 360-DEGREE TURN / 32 HEADINGS",22,14,15,VisualStyle.CREAM);
        Lettering.draw(painter,String.format("%03d DEG",Math.round(angle*180/Math.PI)),814,15,13,VisualStyle.GOLD);
        for(int panel=0;panel<6;panel++) {
            float x=16+(panel%3)*312,y=48+(panel/3)*259,cx=x+148,cy=y+153;
            VisualStyle.panel(painter,x,y,296,246,0xff163b4c);
            Lettering.center(painter,NAMES[panel],cx,y+15,13,VisualStyle.CREAM);
            for(int i=0;i<11;i++) {
                painter.line(cx-110+i*22,cy-85,cx-110+i*22,cy+70,1,0xff234b59);
                painter.line(cx-110,cy-80+i*15,cx+110,cy-80+i*15,1,0xff234b59);
            }
            painter.line(cx-112,cy,cx+112,cy,1.4f,0xffba649d);
            painter.line(cx,cy-88,cx,cy+70,1.4f,0xffba649d);
            if(panel==0) {
                Body tug=new Body(0,-1,cx,cy,angle); tug.lineAttached=true;
                VesselView.draw(g,new SpritesOnly(painter,1.9f,cx,cy),tug,true);
            } else if(panel>=1 && panel<=4) {
                Body barge=new Body(1,panel==2?0:panel==4?2:1,cx,cy,angle); barge.lineAttached=true;
                if(panel==1) barge.unloadingTicks=Game.UNLOAD_TICKS;
                VesselView.draw(g,new SpritesOnly(painter,1.55f,cx,cy),barge,false);
            } else {
                int atlas=panel==2?SpriteAtlas.STEEL:panel==3?SpriteAtlas.CONTAINER:SpriteAtlas.BULK;
                SpriteAtlas.draw(painter,atlas,angle,cx,cy,145,145,1);
            }
            // The bright marker is drawn last: it cannot be carried around by an atlas frame.
            painter.line(cx-6,cy,cx+6,cy,2,0xffff81b9);
            painter.line(cx,cy-6,cx,cy+6,2,0xffff81b9);
            painter.rect(cx-1,cy-1,2,2,VisualStyle.CREAM);
            Lettering.center(painter,"FOOTPRINT CENTER",cx,y+226,9,0xffeab1d1);
        }
        Lettering.center(painter,"CROSSHAIRS STAY FIXED. NO BOBBING, MOVEMENT OR RESIDUAL IMAGE ROTATION.",480,583,10,VisualStyle.MUTED);
        graphics.dispose(); return image;
    }
    /** Show only live vessel sprite layers; scale around the unchanged physics anchor. */
    private static final class SpritesOnly implements Painter {
        private final Painter painter; private final float scale,cx,cy;
        SpritesOnly(Painter painter,float scale,float cx,float cy) { this.painter=painter;this.scale=scale;this.cx=cx;this.cy=cy; }
        public void sprite(int atlas,int frame,float x,float y,float w,float h,float rotation,float alpha) {
            painter.sprite(atlas,frame,cx+(x-cx)*scale,cy+(y-cy)*scale,w*scale,h*scale,rotation,alpha);
        }
        public void spriteBlend(int atlas,int frameA,int frameB,float x,float y,float w,float h,float rotation,float blend,float alpha) {
            painter.spriteBlend(atlas,frameA,frameB,cx+(x-cx)*scale,cy+(y-cy)*scale,w*scale,h*scale,rotation,blend,alpha);
        }
        public void rect(float x,float y,float w,float h,int c) {}
        public void circle(float x,float y,float r,int c) {}
        public void poly(float[] xy,int c) {}
        public void line(float x,float y,float xx,float yy,float w,int c) {}
    }
}
