package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Inspection composites only: source PNG pixels remain immutable. */
public final class FactoryArtReview {
    private static final int CELL=600;
    private static void tile(Graphics2D g,int index,int ox,int oy,boolean anchors,Color background) throws Exception {
        BufferedImage source=ImageIO.read(new File("assets/sprites/"+FactoryArt.file(index)));
        int l=source.getWidth(),t=source.getHeight(),r=0,b=0;
        for(int y=0;y<source.getHeight();y++) for(int x=0;x<source.getWidth();x++) if((source.getRGB(x,y)>>>24)>=128) {
            l=Math.min(l,x);t=Math.min(t,y);r=Math.max(r,x);b=Math.max(b,y);
        }
        g.setColor(background);g.fillRect(ox,oy,CELL,CELL);
        g.setColor(background.getRed()>150?new Color(0x27343b):new Color(0xf3dfb9));g.setFont(new Font("SansSerif",Font.BOLD,20));
        g.drawString(index+"  "+FactoryArt.id(index),ox+16,oy+28);
        g.setFont(new Font("SansSerif",Font.PLAIN,14));
        g.drawString((int)(FactoryArt.footprintX(index)/32)+" x "+(int)(FactoryArt.footprintY(index)/32)+" cells | "+FactoryArt.lightCount(index)+" lights | "+FactoryArt.smokeCount(index)+" exhausts",ox+16,oy+49);
        double s=Math.min(540.0/(r-l+1),500.0/(b-t+1));
        double dx=ox+(CELL-(r-l+1)*s)/2-l*s,dy=oy+78-t*s;
        g.drawImage(source,(int)dx,(int)dy,(int)(source.getWidth()*s),(int)(source.getHeight()*s),null);
        if(!anchors) return;
        for(int k=0;k<FactoryArt.lightCount(index);k++) point(g,dx+FactoryArt.lightX(index,k)*source.getWidth()*s,dy+FactoryArt.lightY(index,k)*source.getHeight()*s,"L"+k,Color.YELLOW);
        for(int k=0;k<FactoryArt.smokeCount(index);k++) point(g,dx+FactoryArt.smokeX(index,k)*source.getWidth()*s,dy+FactoryArt.smokeY(index,k)*source.getHeight()*s,"S"+k,Color.CYAN);
        point(g,dx+FactoryArt.pivotX(index)*source.getWidth()*s,dy+FactoryArt.pivotY(index)*source.getHeight()*s,"P",Color.MAGENTA);
    }
    private static void point(Graphics2D g,double x,double y,String label,Color color) {
        g.setColor(Color.BLACK);g.setStroke(new BasicStroke(3));g.drawOval((int)x-5,(int)y-5,10,10);
        g.setColor(color);g.setStroke(new BasicStroke(1));g.drawOval((int)x-5,(int)y-5,10,10);
        g.drawLine((int)x-9,(int)y,(int)x+9,(int)y);g.drawLine((int)x,(int)y-9,(int)x,(int)y+9);
        g.setFont(new Font("SansSerif",Font.BOLD,15));g.setColor(Color.BLACK);g.drawString(label,(int)x+10,(int)y-8);g.setColor(color);g.drawString(label,(int)x+9,(int)y-9);
    }
    public static void main(String[] args) throws Exception {
        File dir=new File("docs/art-studies/factory-v1/reviews");dir.mkdirs();
        for(int page=0;page<4;page++) {
            BufferedImage image=new BufferedImage(CELL*2,CELL*2,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            for(int n=0;n<4;n++) tile(g,page*4+n,n%2*CELL,n/2*CELL,true,new Color(0x183037));
            g.dispose();ImageIO.write(image,"png",new File(dir,"factory-anchors-v1-page-"+(page+1)+".png"));
        }
        for(int variant=0;variant<3;variant++) {
            Color bg=new Color(variant==0?0x183037:variant==1?0xe5dfcc:0x14171c);
            BufferedImage image=new BufferedImage(CELL*4,CELL*4,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
            for(int n=0;n<16;n++) tile(g,n,n%4*CELL,n/4*CELL,false,bg);
            g.dispose();ImageIO.write(image,"png",new File(dir,"factory-contact-v1-"+variant+".png"));
        }
    }
}
