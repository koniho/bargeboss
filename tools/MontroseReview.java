package com.bargeboss.game;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** Actual shared renderer, with a review-only full-harbor camera. */
public final class MontroseReview {
    static void capture(Game g,String file)throws Exception{
        BufferedImage image=new BufferedImage((int)g.layout.width,(int)g.layout.height,BufferedImage.TYPE_INT_RGB);
        Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(g,new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File("out/"+file+".png"));
    }
    public static void main(String[] args)throws Exception{
        Game g=new Game();g.challengeId=Challenges.MONTROSE;g.reset();g.configureCanvas(1440,900);
        for(int i=0;i<120;i++)g.step();
        g.layout=MontroseHarbor.overview(1440,900);capture(g,"montrose-overview-v2");
        g.layout=Layout.focused(1333,600,MontroseHarbor.mapX(453),MontroseHarbor.mapY(1330),1.65f,666,320);capture(g,"montrose-inlet-v2");
        g.tug.x=MontroseHarbor.mapX(1003);g.tug.y=MontroseHarbor.mapY(345);g.tug.angle=MontroseHarbor.SLIP_ANGLE;Body b=g.cargo[0];b.x=MontroseHarbor.SLIP_X;b.y=MontroseHarbor.SLIP_Y;b.angle=MontroseHarbor.SLIP_ANGLE;
        g.mission.wasTowed=g.mission.entered=true;g.tow.attach(g,b);g.layout=Layout.focused(1333,600,MontroseHarbor.SLIP_X,MontroseHarbor.SLIP_Y,1.65f,666,320);
        capture(g,"montrose-slip-v2");System.out.println("Montrose actual-renderer overview, inlet and slip captured");
    }
}
