package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Read-only magnified source crops for checking vessel light identities and centers. */
public final class VesselLightAnchorReview {
    public static void main(String[] args) throws Exception {
        BufferedImage source=ImageIO.read(new File("assets/sprites/tug-isometric-v5-32.png"));
        File dir=new File("out/vessel-light-review");dir.mkdirs();
        for(int page=0;page<4;page++) {
            BufferedImage out=new BufferedImage(1800,960,BufferedImage.TYPE_INT_RGB);Graphics2D g=out.createGraphics();
            g.setColor(new Color(0x172c36));g.fillRect(0,0,1800,960);
            for(int n=0;n<8;n++) {
                int frame=page*8+n,ox=n%4*450,oy=n/4*480;
                int l=Math.round(source.getWidth()*(frame%8)/8f),r=Math.round(source.getWidth()*(frame%8+1)/8f);
                int t=Math.round(source.getHeight()*(frame/8)/4f),b=Math.round(source.getHeight()*(frame/8+1)/4f);
                g.drawImage(source,ox,oy+24,ox+444,oy+468,l,t,r,b,null);
                g.setFont(new Font("SansSerif",Font.PLAIN,13));g.setColor(Color.WHITE);g.drawString("FRAME "+frame+" / CELL "+(r-l)+"x"+(b-t),ox+8,oy+17);
                for(int v=20;v<220;v+=20){g.setColor(new Color(200,230,255,45));g.drawLine(ox+v*2,oy+24,ox+v*2,oy+468);g.drawLine(ox,oy+24+v*2,ox+444,oy+24+v*2);g.setColor(new Color(0xb9c8ca));g.drawString(""+v,ox+v*2,oy+477);g.drawString(""+v,ox+3,oy+24+v*2);}
                for(int light=0;light<TugLightArt.COUNT;light++)if(TugLightArt.visible(light,frame)) {
                    int x=ox+Math.round(TugLightArt.u(light,frame)*444),y=oy+24+Math.round(TugLightArt.v(light,frame)*444);
                    g.setColor(light==0?Color.CYAN:light==1?Color.GREEN:Color.MAGENTA);g.drawOval(x-8,y-8,16,16);g.drawString(""+light,x+10,y-7);
                }
            }
            g.dispose();ImageIO.write(out,"png",new File(dir,"source-page-"+page+".png"));
        }
    }
}
