package com.bargeboss.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Read-only review of the production atlas and runtime stack attachment. */
public final class TugRuntimePreview {
    public static void main(String[] args) throws Exception {
        BufferedImage out=new BufferedImage(2048,1024,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=out.createGraphics();g.setColor(new Color(0x173d49));g.fillRect(0,0,2048,1024);
        DesktopPainter p=new DesktopPainter(g);
        for(int frame=0;frame<32;frame++) {
            float x=(frame%8)*256+128,y=(frame/8)*256+191,size=220;
            p.sprite(SpriteAtlas.TUG,frame,x,y,size,size,0,1);
            float sx=x+(TugArt.stackX(frame)-TugArt.pivotX(frame))*size*TugArt.SCALE;
            float sy=y+(TugArt.stackY(frame)-TugArt.pivotY(frame))*size*TugArt.SCALE;
            int c=TugArt.stackReviewed(frame)?0xff77ffcc:0xffff6688;
            p.line(sx-7,sy,sx+7,sy,1,c);p.line(sx,sy-7,sx,sy+7,1,c);
            p.line(x-5,y,x+5,y,1,0xffe9c987);p.line(x,y-5,x,y+5,1,0xffe9c987);
            g.setColor(Color.WHITE);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,13));
            g.drawString("FRAME "+frame+(frame==10?" / OUTLET OCCLUDED":""),(frame%8)*256+12,(frame/8)*256+22);
        }
        g.dispose();File file=new File("out/isometric-tug/runtime-stack-anchors.png");file.getParentFile().mkdirs();
        ImageIO.write(out,"png",file);System.out.println(file);
    }
}
