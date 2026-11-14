package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Controlled old/new glow raster comparison against the820ea8c shell algorithm. */
public final class GlowKernelReview {
    private static final int BG=0x091b24;
    private static long energy(BufferedImage image) {
        long sum=0;
        for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++) {
            int color=image.getRGB(x,y);
            for(int shift=0;shift<=16;shift+=8)sum+=Math.max(0,(color>>shift&255)-(BG>>shift&255));
        }
        return sum;
    }
    private static int tint(int rgb,float alpha){return ((int)Physics.clamp(alpha,0,255)<<24)|(rgb&0xffffff);}
    /** Frozen rendering reference only; no game state or alternate gameplay implementation. */
    private static void original(Painter p,boolean compact,float opacity,int tick) {
        float x=200,y=70,radius=10,phase=.8f;int color=0xffffb34f;
        float power=2.4f*SceneryEffects.intensity(tick,phase),breath=1+.05f*(float)StrictMath.sin(tick*.016+phase*1.13);
        float dx=radius*.16f*(float)StrictMath.sin(tick*.009+phase),dy=radius*.12f*(float)StrictMath.sin(tick*.013+phase*1.7);
        if(compact) {
            p.circle(x+dx*.9f,y+dy*.9f,radius*4.58f*breath,tint(color,opacity*power*5.7f));
            p.circle(x+dx*.6f,y+dy*.6f,radius*3.32f*breath,tint(color,opacity*power*8.6f));
            p.circle(x+dx*.25f,y+dy*.25f,radius*1.85f*breath,tint(color,opacity*power*26));
        } else {
            for(int i=10;i>=1;i--){float t=i/10f;p.circle(x+dx*t,y+dy*t,radius*(.8f+4.2f*t)*breath,tint(color,opacity*power*(1.4f+3.2f*(1-t))));}
            for(int i=0;i<3;i++) {
                double angle=phase+i*2.1+tick*.003;
                p.circle(x+(float)StrictMath.cos(angle)*radius*.65f,y+(float)StrictMath.sin(angle)*radius*.4f,radius*1.65f,tint(color,opacity*power*5));
            }
        }
        p.circle(x,y,radius*.65f,tint(0xffe6ab,opacity*power*19));
    }
    public static void main(String[] args)throws Exception {
        File directory=new File("out/glow-kernel-review");directory.mkdirs();
        BufferedImage sheet=new BufferedImage(800,16*140,BufferedImage.TYPE_INT_RGB);Graphics2D contact=sheet.createGraphics();
        int row=0;double minimum=Double.MAX_VALUE,maximum=0;
        System.out.println("compact,opacity,tick,oldEnergy,newEnergy,ratio");
        for(boolean compact:new boolean[]{false,true})for(float opacity:new float[]{1,.28f})for(int tick:new int[]{0,90,180,360}) {
            long[] energies=new long[2];
            for(int version=0;version<2;version++) {
                BufferedImage image=new BufferedImage(400,140,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
                g.setColor(new Color(BG));g.fillRect(0,0,400,140);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                DesktopPainter painter=new DesktopPainter(g);painter.beginFrame();
                if(version==0) {
                    original(painter,compact,opacity,tick);
                } else {
                    if(compact)SceneryEffects.compactGlow(painter,200,70,10,0xffffb34f,opacity,tick,.8f);
                    else SceneryEffects.glow(painter,200,70,10,0xffffb34f,opacity,tick,.8f);
                }
                energies[version]=energy(image);
                g.setColor(new Color(0xc4d2d8));g.setFont(new Font("SansSerif",Font.PLAIN,13));
                g.drawString((version==0?"ORIGINAL":"CACHED")+" / "+(compact?"DISTANT":"CLOSE")+" / OPACITY "+opacity+" / TICK "+tick,8,19);
                g.dispose();contact.drawImage(image,version*400,row*140,null);
            }
            double ratio=energies[1]/(double)energies[0];minimum=Math.min(minimum,ratio);maximum=Math.max(maximum,ratio);
            System.out.printf(java.util.Locale.ROOT,"%s,%.2f,%d,%d,%d,%.4f%n",compact,opacity,tick,energies[0],energies[1],ratio);
            if(ratio<.95 || ratio>1.30)throw new AssertionError("Glow energy changed excessively: "+ratio);
            row++;
        }
        contact.dispose();ImageIO.write(sheet,"png",new File(directory,"BargeBoss-cached-glow-review-v1.png"));
        System.out.printf(java.util.Locale.ROOT,"Brightness ratio range %.4f..%.4f; full and faded warm light preserved.%n",minimum,maximum);
    }
}
