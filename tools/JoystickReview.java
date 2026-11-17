package com.bargeboss.game;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Review the real shared HUD at narrow and wide safe-area sizes. */
public final class JoystickReview {
    private static void capture(Game g,String name)throws Exception {
        BufferedImage image=new BufferedImage((int)g.layout.width,(int)g.layout.height,BufferedImage.TYPE_INT_RGB);
        Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(g,new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File("out/"+name+".png"));
    }
    private static void driveStates()throws Exception {
        BufferedImage image=new BufferedImage(1000,520,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Painter p=new DesktopPainter(graphics);p.rect(0,0,1000,520,0xff101e25);
        for(int col=0;col<4;col++){
            Game g=new Game();g.reset();g.tug.angle=col*(float)Math.PI/2;
            Lettering.center(p,"HEADING "+col*90,col*250+125,30,12,0xffd7dfdc);
            Joystick.draw(g,ScreenView.offset(p,col*250+125-Joystick.centerX(g),145-Joystick.centerY(g)));
        }
        for(int col=0;col<4;col++){
            Game g=new Game();g.reset();g.tug.angle=.65f;g.pointer=true;for(int i=0;i<(col==0?0:col==1?5:35);i++)g.joystick.step(g);
            if(col==3){g.pointer=false;for(int i=0;i<5;i++)g.joystick.step(g);}
            Lettering.center(p,new String[]{"RELEASED","FADING OUT","HELD","FADING IN"}[col],col*250+125,282,12,0xffd7dfdc);
            Joystick.draw(g,ScreenView.offset(p,col*250+125-Joystick.centerX(g),395-Joystick.centerY(g)));
        }
        graphics.dispose();ImageIO.write(image,"png",new File("out/joystick-drive-states.png"));
    }
    private static void throttleStates()throws Exception {
        BufferedImage image=new BufferedImage(1100,480,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Painter p=new DesktopPainter(graphics);p.rect(0,0,1100,480,0xff101e25);
        for(int row=0;row<2;row++)for(int col=0;col<5;col++){
            Game g=new Game();g.reset();g.tug.angle=0;float cx=Joystick.centerX(g),cy=Joystick.centerY(g);
            if(col>0){float distance=Joystick.DEAD_ZONE+(Joystick.TRAVEL-Joystick.DEAD_ZONE)*col/4f;
                float sign=row==0?1:-1,dx=distance*.8660254f*sign,dy=-distance*.5f*sign;
                g.pointerDown(0,cx,cy);g.pointerMove(0,cx+dx,cy+dy);
                for(int i=0;i<40;i++)g.joystick.step(g);
            }
            Joystick.draw(g,ScreenView.offset(p,110+col*220-cx,120+row*240-cy));
        }
        graphics.dispose();ImageIO.write(image,"png",new File("out/joystick-throttle-arcs.png"));
    }
    public static void main(String[] args)throws Exception {
        driveStates();throttleStates();
        for(int width:new int[]{960,1333}) {
            Game g=new Game();g.configureCanvas(width,600);g.reset();
            float cx=Joystick.centerX(g),cy=Joystick.centerY(g);g.pointerDown(0,cx,cy);g.pointerMove(0,cx+25,cy-25);for(int i=0;i<35;i++)g.step();
            capture(g,"joystick-left-"+width);g.cancelInputs();g.settings.setJoystickSide(GameSettings.RIGHT);
            for(int i=0;i<40;i++)g.step();capture(g,"joystick-right-"+width);ReplayInput.tap(g,Layout.SETTINGS);capture(g,"joystick-settings-"+width);
        }
        System.out.println("Joystick and settings frames exported to out/");
    }
}
