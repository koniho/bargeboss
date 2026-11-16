package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
/** Actual geometry normals: opposite light must move highlights, never enlarge silhouettes. */
public final class NormalBoatReview {
    public static void main(String[] args)throws Exception{
        BufferedImage sheet=ImageIO.read(new File("assets/sprites/montrose-moored-normals-v1.png"));
        BufferedImage base=ImageIO.read(new File("assets/sprites/montrose-moored-boats-v1.png"));
        int checks=0;
        for(int f=0;f<192;f++)for(int y=0;y<128;y++)for(int x=0;x<128;x++){
            int sx=f%16*128+x,sy=f/16*128+y,n=sheet.getRGB(sx,sy),b=base.getRGB(sx,sy);
            if((n>>>24)!=(b>>>24))throw new AssertionError("normal silhouette mismatch "+f);
            if((n>>>24)>250){if((n&255)>205)throw new AssertionError("height outside model");checks++;}
        }
        BufferedImage board=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);Graphics2D g=board.createGraphics();g.setColor(new Color(0x071721));g.fillRect(0,0,960,600);
        for(int kind=0;kind<6;kind++)for(int side=0;side<3;side++){
            int frame=kind*32+4;float x=80+kind*160,y=150+side*180;
            DesktopPainter p=new DesktopPainter(g);p.sprite(SpriteAtlas.MOORED_BOATS,frame,x,y,200,200,0,1);
            float nx=side==0?-24:side==1?24:0,ny=side==2?24:0;
            p.normalSprite(frame,x,y,200,200,nx,ny,13,90,side==2?0xff00ff00:0xffff0000,.7f);
        }
        final int[] submitted={0};Painter nativeProbe=new Painter(){
            public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
            public void spriteBlend(int a,int f,int b,float x,float y,float w,float h,float r,float mix,float alpha){}
            public void rect(float x,float y,float w,float h,int c){}
            public void circle(float x,float y,float r,int c){}
            public void line(float x,float y,float xx,float yy,float w,int c){}
            public void poly(float[] xy,int c){}
            public void normalSprite(int f,float x,float y,float w,float h,float nx,float ny,float nz,float radius,int color,float alpha){submitted[0]++;}
        };
        Game harbor=new Game();harbor.selectChallenge(Challenges.MONTROSE);harbor.reset();MooredBoats.Berth berth=MooredBoats.BERTHS[0];harbor.layout=Layout.focused(960,600,berth.x,berth.y,2,480,350);harbor.tug.x=berth.x+20;harbor.tug.y=berth.y;
        Renderer.draw(harbor,nativeProbe);if(submitted[0]==0)throw new AssertionError("scene wrappers dropped normal-map draws");
        g.dispose();new File("out").mkdirs();ImageIO.write(board,"png",new File("out/moored-directional-light-review.png"));
        MooredBoatLight.Lights sources=new MooredBoatLight.Lights();sources.add(-24,0,13,90,.2f,0xffffd68d);sources.add(24,0,13,90,.2f,0xffffd68d);
        if(sources.count!=2||sources.x[0]*sources.x[1]>=0)throw new AssertionError("opposing sources merged");
        for(int i=0;i<10;i++)sources.add(i,0,13,90,.1f,0xffffffff);if(sources.count!=4)throw new AssertionError("unbounded point lights");
        // +X-facing normal at the cell origin: a lamp behind it must not illuminate it.
        float front=BoatSurfaceLight.response(0xffff8000,.5f,.7f,24,0,13,90);
        float back=BoatSurfaceLight.response(0xffff8000,.5f,.7f,-24,0,13,90);
        if(front<.5f||back!=0)throw new AssertionError("back face lit");
        // A flat deck near a low pedestal receives substantially more than its far end.
        float near=BoatSurfaceLight.response(0xff808000,.5f,.7f,0,0,13,90);
        float far=BoatSurfaceLight.response(0xff808000,.5f+24*RiverCamera.Q/96,.7f-24*RiverCamera.Q*RiverCamera.DEPTH/96,0,0,13,90);
        if(near<far*1.5f)throw new AssertionError("surface distance ignored");
        System.out.println(checks+" packed surface-height checks and 192 exact silhouette registrations passed");
    }
}
