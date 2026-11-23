package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Checks registered canvas masks and exports real shared-renderer day/dusk review views. */
public final class BiminiReview {
    public static void main(String[] args)throws Exception {
        BufferedImage hull=ImageIO.read(new File("assets/sprites/montrose-moored-boats-v1.png"));
        int pixels=0;
        for(int pose=0;pose<192;pose++){
            BufferedImage reference=null;
            for(int color=0;color<BiminiArt.COLORS;color++){
                BufferedImage fabric=ImageIO.read(new File("assets/sprites/"+SpriteAtlas.file(SpriteAtlas.MOORED_BIMINI,BiminiArt.frame(color,pose))));
                if(fabric.getWidth()!=BiminiArt.WIDTH || fabric.getHeight()!=BiminiArt.HEIGHT)throw new AssertionError("Frame-sized canvas required");
                int visible=0;
                for(int y=0;y<BiminiArt.HEIGHT;y++)for(int x=0;x<BiminiArt.WIDTH;x++){
                    int a=fabric.getRGB(x,y)>>>24;
                    if(reference!=null && a!=(reference.getRGB(x,y)>>>24))throw new AssertionError("Palette changes geometry");
                    if(a>0){visible++;int base=hull.getRGB(pose%16*128+x+BiminiArt.LEFT,pose/16*128+y+BiminiArt.TOP)>>>24;
                        if(a>base+2)throw new AssertionError("Canvas outside hull silhouette: "+pose+" "+x+","+y);}
                }
                if(pose<96 && visible<15)throw new AssertionError("Missing motorboat canvas at heading "+pose);
                if(pose>=96 && visible!=0)throw new AssertionError("Sailboats must not have canvas roofs");
                pixels+=visible;reference=fabric;
            }
        }
        new File("out").mkdirs();
        for(int light:new int[]{1,2,3}){
            BufferedImage board=new BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);Graphics2D g=board.createGraphics();
            g.setColor(new Color(0x122e3a));g.fillRect(0,0,1280,720);DesktopPainter p=new DesktopPainter(g);p.lighting(LightingProfile.preset(light));
            for(int kind=0;kind<6;kind++)for(int color=0;color<8;color++){
                float x=80+color*160,y=112+kind*113;int frame=kind*32+6;
                p.sprite(SpriteAtlas.MOORED_BOATS,frame,x,y,180,180,0,1);
                p.sprite(SpriteAtlas.MOORED_BIMINI,BiminiArt.frame(color,frame),x,y,180*BiminiArt.WIDTH/128f,180*BiminiArt.HEIGHT/128f,0,1);
            }
            g.dispose();ImageIO.write(board,"png",new File("out/bimini-palette-"+light+".png"));
            Game game=new Game();game.selectChallenge(Challenges.MONTROSE);game.selectLighting(light);game.reset();game.configureCanvas(1440,900);game.dialogue.close(game);
            game.layout=Layout.focused(1440,900,MontroseHarbor.mapX(1080),MontroseHarbor.mapY(920),2.6f,720,468);
            MontroseReview.capture(game,"bimini-harbor-"+light);
        }
        System.out.println(pixels+" canvas pixels, 1536 frame registrations and 192 heading silhouettes checked");
    }
}
