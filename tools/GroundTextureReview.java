package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.util.Locale;
import javax.imageio.ImageIO;

/** Read-only alpha registration and tile-join review; source pixels stay unchanged. */
public final class GroundTextureReview {
    public static void main(String[] args) throws Exception {
        BufferedImage im=ImageIO.read(new File(args[0]));int w=im.getWidth(),h=im.getHeight();
        int left=w,right=-1,top=h,bottom=-1,lY=0,rY=0,tX=0,bX=0,edge=0,opaque=0;
        for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
            int alpha=im.getRGB(x,y)>>>24;
            if(alpha>=128) {
                if(x<left){left=x;lY=y;}if(x>right){right=x;rY=y;}
                if(y<top){top=y;tX=x;}if(y>bottom){bottom=y;bX=x;}
                if(alpha==255)opaque++;
            }
            if(alpha>0&&(x==0||y==0||x==w-1||y==h-1))edge++;
        }
        float cx=(left+right)*.5f,cy=(lY+rY)*.5f;
        float[] x={left,tX,right,bX},y={lY,top,rY,bottom};
        try(PrintWriter out=new PrintWriter("docs/art-studies/ground-v1/registration.txt")) {
            out.printf(Locale.ROOT,"Dimensions %dx%d; alpha>=128 bounds [%d,%d,%d,%d]; edge pixels %d; fully opaque pixels %d%n",w,h,left,top,right,bottom,edge,opaque);
            for(int i=0;i<4;i++){int j=(i+1)%4;out.printf(Locale.ROOT,"Corner %d=(%.0f,%.0f), edge angle %.3f degrees%n",i,x[i],y[i],StrictMath.atan2(y[j]-y[i],x[j]-x[i])*180/Math.PI);}
            out.printf(Locale.ROOT,"Pivot %.9f %.9f; uniform runtime size %.9f for64-unit footprint%n",cx/w,cy/h,64*2*RiverCamera.Q*w/(right-left));
        }
        BufferedImage review=new BufferedImage(1500,650,BufferedImage.TYPE_INT_RGB);Graphics2D g=review.createGraphics();
        int[] backgrounds={0xff173c45,0xff353d43,0xffe0d9cc};
        for(int panel=0;panel<3;panel++) {
            g.setColor(new Color(backgrounds[panel],true));g.fillRect(panel*500,0,500,650);
            float scale=160f/(right-left),dx=80,dy=(bottom-top)*scale*.5f;
            for(int row=0;row<3;row++)for(int col=0;col<3;col++) {
                float px=panel*500+250+(col-row)*dx,py=180+(row+col)*dy;
                g.drawImage(im,Math.round(px-cx*scale),Math.round(py-cy*scale),Math.round(w*scale),Math.round(h*scale),null);
            }
            g.setColor(panel==2?Color.BLACK:Color.WHITE);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));
            g.drawString("3 x 3 registered joins / source unchanged",panel*500+55,575);
        }
        g.dispose();ImageIO.write(review,"png",new File("docs/art-studies/ground-v1/ground-join-review.png"));
    }
}
