package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual shared vessel renderer, all32 headings and newly authored fixtures. */
public final class VesselLightingPreview {
    public static void main(String[] args)throws Exception {
        File dir=new File("out/vessel-light-review");dir.mkdirs();
        for(int page=0;page<4;page++) {
            BufferedImage image=new BufferedImage(1600,1160,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
            g.setColor(new Color(0x091b24));g.fillRect(0,0,1600,1160);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            for(int n=0;n<8;n++) {
                int f=page*8+n,ox=n%2*800,oy=n/2*290;
                Game game=new Game();game.visualTicks=180;
                Body tug=game.tug;tug.x=300;tug.y=500;tug.angle=(f-RiverCamera.DIRECTION_OFFSET)*(float)(Math.PI*2/32);tug.engineLoad=.18f;
                Body barge=new Body(1,args.length==0?0:Integer.parseInt(args[0]),300,500,tug.angle);barge.unloadingTicks=args.length==0?180:0;
                for(int k=0;k<2;k++) {
                    Body body=k==0?tug:barge;float scale=3.7f;
                    RiverCamera base=new RiverCamera(0,0,scale);
                    RiverCamera camera=new RiverCamera(ox+200+k*400-base.screenX(body.x,body.y),oy+193-base.screenY(body.x,body.y),scale);
                    VesselView.draw(game,new RiverPainter(new DesktopPainter(g),camera),body,k==0,false);
                }
                g.setFont(new Font("SansSerif",Font.PLAIN,16));g.setColor(new Color(0xd3dedf));g.drawString("SOURCE HEADING "+f+" | TUG PAINTED LIGHTS + NEW BARGE DECK FITTINGS",ox+18,oy+276);
            }
            g.dispose();ImageIO.write(image,"png",new File(dir,(args.length==0?"runtime":"loaded-"+args[0])+"-headings-"+page+".png"));
        }
    }
}
