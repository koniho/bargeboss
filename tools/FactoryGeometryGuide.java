package com.bargeboss.game;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** Geometry-only construction guide; never modifies generated source pixels. */
public final class FactoryGeometryGuide {
    static final int SIZE=1024;static final float SCALE=2.78f,CX=512,CY=768;
    static int sx(float x,float y) {return Math.round(CX+SCALE*RiverCamera.Q*(x+y));}
    static int sy(float x,float y,float z) {return Math.round(CY+SCALE*RiverCamera.Q*RiverCamera.DEPTH*(y-x)-SCALE*z);}
    static void face(Graphics2D g,float[][] points,int color) {int[] x=new int[points.length],y=new int[points.length];for(int i=0;i<x.length;i++){x[i]=sx(points[i][0],points[i][1]);y[i]=sy(points[i][0],points[i][1],points[i][2]);}g.setColor(new Color(color,true));g.fillPolygon(x,y,x.length);g.setColor(new Color(0xff1c252b,true));g.setStroke(new BasicStroke(3));g.drawPolygon(x,y,x.length);}
    static void box(Graphics2D g,float hx,float hy,float height){
        face(g,new float[][]{{-hx,-hy,0},{-hx,hy,0},{-hx,hy,height},{-hx,-hy,height}},0xff78878c);
        face(g,new float[][]{{-hx,hy,0},{hx,hy,0},{hx,hy,height},{-hx,hy,height}},0xff4d616c);
        face(g,new float[][]{{-hx,-hy,height},{-hx,hy,height},{hx,hy,height},{hx,-hy,height}},0xffa3b1b5);
    }
    static void stack(Graphics2D g,float x,float y,float bottom,float height){
        int a=sx(x,y),b=sy(x,y,bottom),t=sy(x,y,height),r=28;g.setColor(new Color(0xff687c86,true));g.fillRect(a-r,t,r*2,b-t);g.fillOval(a-r,b-15,r*2,30);g.setColor(new Color(0xffb0bec4,true));g.fillOval(a-r,t-15,r*2,30);g.setColor(new Color(0xff24343e,true));g.fillOval(a-r+8,t-12,r*2-16,24);
    }
    public static void main(String[]args)throws Exception{
        File out=new File("docs/art-studies/factory-v1/guides");out.mkdirs();
        for(int i=0;i<FactoryArt.COUNT;i++){
            BufferedImage image=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            float hx=FactoryArt.footprintX(i),hy=FactoryArt.footprintY(i),h=FactoryArt.height(i);
            face(g,new float[][]{{-hx,-hy,0},{-hx,hy,0},{hx,hy,0},{hx,-hy,0}},0xffbdad91);
            float body=Math.min(h,75);if(i==7||i==10||i==13)body=40;
            box(g,hx-5,hy-5,body);
            if(i==0||i==1||i==2||i==3||i==4||i==5||i==6||i==7||i==9||i==12||i==14) stack(g,hx*.5f,-hy*.35f,body,h);
            g.dispose();ImageIO.write(image,"png",new File(out,FactoryArt.id(i)+"-guide.png"));
        }
    }
}
