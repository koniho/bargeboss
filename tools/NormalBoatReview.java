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
            if((n>>>24)>250 && x>0 && x<127 && y>0 && y<127 && n==sheet.getRGB(sx-1,sy) && n==sheet.getRGB(sx+1,sy) && n==sheet.getRGB(sx,sy-1) && n==sheet.getRGB(sx,sy+1)){float u=((n>>16)&255)/127.5f-1,v=((n>>8)&255)/127.5f-1,z=(n&255)/127.5f-1;
                float length=u*u+v*v+z*z;if(length<.9 || length>1.1)throw new AssertionError("non-unit face normal");checks++;}
        }
        BufferedImage board=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);Graphics2D g=board.createGraphics();g.setColor(new Color(0x071721));g.fillRect(0,0,960,600);
        for(int kind=0;kind<6;kind++)for(int side=0;side<3;side++){
            int frame=kind*32+4;float x=80+kind*160,y=150+side*180;
            DesktopPainter p=new DesktopPainter(g);p.sprite(SpriteAtlas.MOORED_BOATS,frame,x,y,200,200,0,1);
            float nx=side==0?-.94f:side==1?.94f:0,ny=side==2?.94f:0;
            p.normalSprite(frame,x,y,200,200,nx,ny,.341174f,side==2?0xff00ff00:0xffff0000,.7f);
        }
        final int[] submitted={0};Painter nativeProbe=new Painter(){
            public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
            public void spriteBlend(int a,int f,int b,float x,float y,float w,float h,float r,float mix,float alpha){}
            public void rect(float x,float y,float w,float h,int c){}
            public void circle(float x,float y,float r,int c){}
            public void line(float x,float y,float xx,float yy,float w,int c){}
            public void poly(float[] xy,int c){}
            public void normalSprite(int f,float x,float y,float w,float h,float nx,float ny,float nz,int color,float alpha){submitted[0]++;}
        };
        Game harbor=new Game();harbor.selectChallenge(Challenges.MONTROSE);harbor.reset();MooredBoats.Berth berth=MooredBoats.BERTHS[0];harbor.layout=Layout.focused(960,600,berth.x,berth.y,2,480,350);harbor.tug.x=berth.x+20;harbor.tug.y=berth.y;
        Renderer.draw(harbor,nativeProbe);if(submitted[0]==0)throw new AssertionError("scene wrappers dropped normal-map draws");
        g.dispose();new File("out").mkdirs();ImageIO.write(board,"png",new File("out/moored-directional-light-review.png"));
        float[] left=new float[12],right=new float[12];MooredBoatLight.add(left,0,0,-30,0,60,1,0xffff0000,0,-1);MooredBoatLight.add(right,0,0,30,0,60,1,0xffff0000,0,-1);
        if(left[3]>=0||right[3]<=0||left[0]!=right[0]||left[1]!=0)throw new AssertionError("colored light direction field");
        System.out.println(checks+" geometric unit-normal checks and 192 exact silhouette registrations passed");
    }
}
