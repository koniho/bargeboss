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
        if(direction!=-1){
            float[] light=new float[4];if(direction>=0)light[direction]=1;
            for(int d=0;d<4;d++)p.sprite(SurfaceLightArt.forSource(source),SurfaceLightArt.frame(source,d,frame),100,165,170,170,0,SurfaceLighting.shade(light,d));
        }
        g.dispose();return image;
    }
    private static BufferedImage transition(int source,int frame,boolean light){
        BufferedImage image=new BufferedImage(200,220,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();Painter p=new DesktopPainter(g);
        int next=(frame+1)%32;p.spriteBlend(source,frame,next,100,165,170,170,0,.5f,1);
        if(light){float[] energy={1,0,0,0};for(int d=0;d<4;d++)p.spriteBlend(SurfaceLightArt.forSource(source),SurfaceLightArt.frame(source,d,frame),SurfaceLightArt.frame(source,d,next),100,165,170,170,0,.5f,SurfaceLighting.shade(energy,d));}
        g.dispose();return image;
    }
    private static void protectedCore(BufferedImage image,int source,int frame,float u,float v){
        for(int d=0;d<4;d++){
            int cell=SurfaceLightArt.frame(source,d,frame),n=SurfaceLightArt.CELL;
            check((image.getRGB(cell%8*n+(int)(u*n),cell/8*n+(int)(v*n))>>>24)==0,"painted lamp core remains unshaded at every heading");
        }
    }
    private static double energy(BufferedImage image){
        double sum=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
            int p=image.getRGB(x,y);sum+=(p>>>24)/255.0*(.2126*((p>>>16)&255)+.7152*((p>>>8)&255)+.0722*(p&255));
        }return sum;
    }
    public static void main(String[] args)throws Exception{
        BufferedImage sheet=new BufferedImage(1200,1250,BufferedImage.TYPE_INT_RGB);Graphics2D canvas=sheet.createGraphics();
        canvas.setColor(new Color(0x0a1825));canvas.fillRect(0,0,1200,1250);canvas.setColor(Color.WHITE);canvas.setFont(new Font("SansSerif",Font.BOLD,16));
        String[] labels={"PAINTED SOURCE","AWAY FROM LIGHT","LAMP ON LEFT","LAMP ON RIGHT"};
        for(int i=0;i<4;i++)canvas.drawString(labels[i],i*300+50,28);
        int row=0;
        for(int source:SurfaceLightArt.sources()){
            int sample=source==SpriteAtlas.BUOY?2:10;
            for(int col=0;col<4;col++)canvas.drawImage(render(source,sample,col==0?-1:col==1?-2:col==2?2:0),col*300+50,row*200+10,null);
            double original=energy(render(source,sample,-1)),dark=energy(render(source,sample,-2)),near=energy(render(source,sample,0));
            check(dark<original*.84,"unlit shading must be visibly darker for source "+source+": "+dark/original);
            check(near>dark*1.08,"a nearby lamp must visibly reveal the original material for source "+source);
            BufferedImage response=ImageIO.read(new File("assets/sprites/"+SurfaceLightArt.file(SurfaceLightArt.forSource(source))));
            for(int f=0;f<SpriteAtlas.count(source);f++){
                if(source==SpriteAtlas.TUG)for(int k=0;k<TugLightArt.COUNT;k++)if(TugLightArt.visible(k,f))
                    protectedCore(response,source,f,TugLightArt.u(k,f),TugLightArt.v(k,f));
                if(source==SpriteAtlas.BUOY)protectedCore(response,source,f,BuoyArt.lampX(f),BuoyArt.lampY(f));
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
        canvas.dispose();ImageIO.write(sheet,"png",new File("out/surface-shading-materials-v2.png"));
        Game g=new Game();g.reset();g.configureCanvas(1333,600);g.tug.x=232;g.tug.y=450;g.tug.angle=-(float)Math.PI/2;
        for(int i=0;i<g.cargo.length;i++)g.cargo[i]=null;
        g.cargo[0]=new Body(10,1,240,550,-(float)Math.PI/2);g.cargo[1]=new Body(11,0,355,505,0);
        g.layout=Layout.focused(1333,600,295,480,2,690,330);g.visualTicks=48;
        BufferedImage scene=new BufferedImage(1333,600,BufferedImage.TYPE_INT_RGB);Graphics2D p=scene.createGraphics();Renderer.draw(g,new DesktopPainter(p));p.dispose();
        ImageIO.write(scene,"png",new File("out/surface-shading-harbor-v2.png"));
        System.out.println(checks+" native surface-light pixels stay inside source silhouettes; material and harbor reviews exported");
    }
}
