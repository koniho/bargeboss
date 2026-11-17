package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Actual shared renderer; fixed states isolate mission artwork, flights and target registration. */
public final class MontroseRescuePreview {
    private static void capture(Game g,String name)throws Exception{BufferedImage image=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Renderer.draw(g,new DesktopPainter(p));p.dispose();new File("out").mkdirs();ImageIO.write(image,"png",new File("out/"+name+".png"));}
    private static Game game(int id){Game g=new Game();g.selectChallenge(id);g.reset();return g;}
    private static void focus(Game g,float x,float y){g.layout=Layout.focused(960,600,x,y,3.6f,480,312);}
    private static Game swimmers(){Game g=game(Challenges.SWIMMER_RESCUE);g.tug.x=660;g.tug.y=640;focus(g,660,590);return g;}
    private static void tapWorld(Game g,float x,float y){ReplayInput.down(g,x,y);ReplayInput.up(g,x,y);}
    private static void steps(Game g,int n){for(int i=0;i<n;i++)g.step();}
    public static void main(String[] args)throws Exception{
        Game g=game(Challenges.RAFT_RESCUE);capture(g,"rescue-rafted");
        tapWorld(g,(g.cargo[0].x+g.cargo[1].x)*.5f,g.cargo[0].y);capture(g,"rescue-separated");
        focus(g,830,-272);capture(g,"rescue-dock-c");
        g=game(Challenges.SWIMMER_RESCUE);capture(g,"rescue-approach");
        g=swimmers();capture(g,"rescue-swimmers");
        tapWorld(g,g.rescue.x[0],g.rescue.y[0]);steps(g,18);capture(g,"rescue-buoy-flight");
        steps(g,30);capture(g,"rescue-buoy-secured");
    }
}
