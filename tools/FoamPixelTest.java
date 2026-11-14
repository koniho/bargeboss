package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Native raster check: transparent texture cells and detached foam on both banks. */
public final class FoamPixelTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        BufferedImage atlas=ImageIO.read(new File("assets/sprites/vessel-foam-v1.png"));
        check(atlas.getColorModel().hasAlpha() && atlas.getWidth()%2==0 && atlas.getHeight()%2==0,"RGBA 2x2 foam atlas");
        int cw=atlas.getWidth()/2,ch=atlas.getHeight()/2;
        for(int f=0;f<4;f++) {
            int transparent=0,painted=0;
            for(int y=0;y<ch;y+=2)for(int x=0;x<cw;x+=2) {
                int a=atlas.getRGB(f%2*cw+x,f/2*ch+y)>>>24;
                if(a==0)transparent++;if(a>40)painted++;
                if(x<3 || y<3 || x>=cw-3 || y>=ch-3)check(a<=1,"foam cell padding has no visible alpha (at most 1/255 quantization residue)");
            }
            check(transparent>cw*ch/16 && painted>100,"foam contains both open water gaps and visible filaments");
        }
        int wet=0;
        for(int bank=0;bank<4;bank++)for(int heading=0;heading<8;heading++) {
            Game root=new Game();root.startTutorial();Game g=root.activeGame();Body b=g.tug;
            b.x=bank==0?-1610:bank==1?TrainingChannel.BASIN_LEFT+16:bank==2?TrainingChannel.MIN-16:-1610;
            b.y=bank==0?TrainingChannel.BASIN_BOTTOM-16:bank==3?TrainingChannel.BASIN_TOP+16:600;
            b.angle=heading*(float)StrictMath.PI/4;b.engineLoad=1;
            b.vx=(float)StrictMath.cos(b.angle)*40;b.vy=(float)StrictMath.sin(b.angle)*40;
            for(int tick=0;tick<100;tick++){g.visualTicks=tick;g.water.foam.step();g.water.foam.emit(g,b);}
            g.layout=Layout.focused(384,384,b.x,b.y,1.8f,192,230);RiverCamera c=g.layout.camera;
            BufferedImage image=new BufferedImage(384,384,BufferedImage.TYPE_INT_ARGB_PRE);Graphics2D graphics=image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.water.foam.draw(g,new RiverPainter(new DesktopPainter(graphics),c));graphics.dispose();
            for(int y=2;y<382;y+=3)for(int x=2;x<382;x+=3) {
                float wx=c.worldX(x+.5f,y+.5f+GroundArt.LAND_RISE*c.scale),wy=c.worldY(x+.5f,y+.5f+GroundArt.LAND_RISE*c.scale);
                boolean dry=!TrainingChannel.contains(wx,wy) && !TrainingChannel.contains(wx-2,wy) && !TrainingChannel.contains(wx+2,wy)
                    && !TrainingChannel.contains(wx,wy-2) && !TrainingChannel.contains(wx,wy+2);
                if(dry)check((image.getRGB(x,y)>>>24)==0,"detached foam cannot paint any raised land, near or far bank");
                else if((image.getRGB(x,y)>>>24)>5)wet++;
            }
        }
        check(wet>100,"foam remains visible on navigable water");
        System.out.println(checks+" foam atlas transparency and detached shoreline pixel assertions passed");
    }
}
