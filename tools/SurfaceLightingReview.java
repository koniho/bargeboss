package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual native-renderer material/face comparison and alpha-support regression. */
public final class SurfaceLightingReview {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static BufferedImage render(int source,int frame,int direction){
        BufferedImage image=new BufferedImage(200,220,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();Painter p=new DesktopPainter(g);
        p.sprite(source,frame,100,165,170,170,0,1);
        if(direction>=0)p.sprite(SurfaceLightArt.forSource(source),SurfaceLightArt.frame(source,direction,frame),100,165,170,170,0,.62f);
        g.dispose();return image;
    }
    private static BufferedImage transition(int source,int frame,boolean light){
        BufferedImage image=new BufferedImage(200,220,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();Painter p=new DesktopPainter(g);
        int next=(frame+1)%32;p.spriteBlend(source,frame,next,100,165,170,170,0,.5f,1);
        if(light)p.spriteBlend(SurfaceLightArt.forSource(source),SurfaceLightArt.frame(source,0,frame),SurfaceLightArt.frame(source,0,next),100,165,170,170,0,.5f,.62f);
        g.dispose();return image;
    }
    public static void main(String[] args)throws Exception{
        BufferedImage sheet=new BufferedImage(900,1250,BufferedImage.TYPE_INT_RGB);Graphics2D canvas=sheet.createGraphics();
        canvas.setColor(new Color(0x0a1825));canvas.fillRect(0,0,900,1250);canvas.setColor(Color.WHITE);canvas.setFont(new Font("SansSerif",Font.BOLD,16));
        String[] labels={"PAINTED SOURCE","LAMP ON LEFT","LAMP ON RIGHT"};
        for(int i=0;i<3;i++)canvas.drawString(labels[i],i*300+50,28);
        int row=0;
        for(int source:SurfaceLightArt.sources()){
            int sample=source==SpriteAtlas.BUOY?2:10;
            for(int col=0;col<3;col++)canvas.drawImage(render(source,sample,col==0?-1:col==1?2:0),col*300+50,row*200+10,null);
            for(int f=0;f<SpriteAtlas.count(source);f++){
                BufferedImage base=render(source,f,-1);int changed=0;
                for(int direction=0;direction<4;direction++){
                    BufferedImage lit=render(source,f,direction);
                    for(int y=0;y<220;y++)for(int x=0;x<200;x++){
                        int a=base.getRGB(x,y),b=lit.getRGB(x,y);
                        check((a>>>24)==0?(b>>>24)==0:(b>>>24)-(a>>>24)<=10,"lighting stays inside conservative opaque source support: "+source+"/"+f);
                        if(a!=b)changed++;
                    }
                }
                check(changed>20,"all turn frames and receiver types visibly receive light: "+source+"/"+f+" pixels "+changed);
            }
            if(source!=SpriteAtlas.BUOY)for(int f=0;f<32;f++){
                BufferedImage base=transition(source,f,false),lit=transition(source,f,true);
                for(int y=0;y<220;y++)for(int x=0;x<200;x++)if((base.getRGB(x,y)>>>24)==0)
                    check((lit.getRGB(x,y)>>>24)==0,"turning lighting stays inside the blended silhouette");
            }
            row++;
        }
        canvas.dispose();ImageIO.write(sheet,"png",new File("out/surface-lighting-materials.png"));
        Game g=new Game();g.reset();g.configureCanvas(1333,600);g.tug.x=232;g.tug.y=450;g.tug.angle=-(float)Math.PI/2;
        for(int i=0;i<g.cargo.length;i++)g.cargo[i]=null;
        g.cargo[0]=new Body(10,1,240,550,-(float)Math.PI/2);g.cargo[1]=new Body(11,0,355,505,0);
        g.layout=Layout.focused(1333,600,295,480,2,690,330);g.visualTicks=48;
        BufferedImage scene=new BufferedImage(1333,600,BufferedImage.TYPE_INT_RGB);Graphics2D p=scene.createGraphics();Renderer.draw(g,new DesktopPainter(p));p.dispose();
        ImageIO.write(scene,"png",new File("out/surface-lighting-harbor.png"));
        System.out.println(checks+" native surface-light pixels stay inside source silhouettes; material and harbor reviews exported");
    }
}
