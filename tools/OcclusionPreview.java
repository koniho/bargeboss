package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Same production sprites and mask query: clear, padded corner, blocked and behind. */
public final class OcclusionPreview {
    public static void main(String[] args)throws Exception {
        BufferedImage image=new BufferedImage(1200,800,BufferedImage.TYPE_INT_RGB);
        Graphics2D all=image.createGraphics();
        for(int panel=0;panel<4;panel++) {
            Graphics2D g=(Graphics2D)all.create(panel%2*600,panel/2*400,600,400);
            g.setColor(new Color(0x081c27));g.fillRect(0,0,600,400);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            SceneryOcclusion p=new SceneryOcclusion(new DesktopPainter(g));
            float factoryX=300,factoryY=350,size=360;
            float boatX=panel==0?480:panel==1?180:300,boatY=panel==1?130:320;
            if(panel!=3)p.sprite(SpriteAtlas.TUG,4,boatX,boatY,110,110,0,1);
            float alpha=SceneryOcclusion.spriteOpacity(p,SpriteAtlas.FACTORY,3,factoryX,factoryY,size,size);
            p.sprite(SpriteAtlas.FACTORY,3,factoryX,factoryY,size,size,0,alpha);
            if(panel==3)p.sprite(SpriteAtlas.TUG,4,boatX,boatY,110,110,0,1);
            g.setColor(new Color(0xffdf99));g.setFont(new Font("SansSerif",Font.BOLD,17));
            String[] labels={"NEARBY — stays opaque","TRANSPARENT CORNER — stays opaque","OBSCURING BOAT — fades","BEHIND BOAT — stays opaque"};
            g.drawString(labels[panel],20,28);g.setFont(new Font("SansSerif",Font.PLAIN,14));g.drawString("Factory opacity: "+Math.round(alpha*100)+"%",20,50);
            g.dispose();
        }
        all.dispose();ImageIO.write(image,"png",new File("out/BargeBoss-scenery-occlusion-v1.png"));
    }
}
