package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Actual hull pixels, all 32 headings, both lower banks and both upper banks. */
public final class ShoreOcclusionPixelTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args) {
        wallCorners();
        Game root=new Game();root.startTutorial();Game g=root.activeGame();if(args.length>0){g=new Game();g.challengeId=Challenges.INDIANA;g.reset();}int wetPixels=0,landProbes=0;
        for(int bank=0;bank<4;bank++)for(int kind=0;kind<4;kind++)for(int heading=0;heading<32;heading++) {
            float x=bank==0?-1610:bank==1?TrainingChannel.BASIN_LEFT+16:bank==2?TrainingChannel.MIN-16:-1610;
            float y=bank==0?TrainingChannel.BASIN_BOTTOM-16:bank==3?TrainingChannel.BASIN_TOP+16:600;
            if(g.indianaStage()){x=bank==0?266:bank==1?484:bank==2?116:1134;y=bank<2?700:-300;}
            Body b=new Body(kind==0?0:1,kind-1,x,y,heading*(float)Math.PI/16);b.floatingInitialized=true;b.heave=(heading%3-1)*1.5f;
            b.vx=45*(float)StrictMath.cos(b.angle);b.vy=45*(float)StrictMath.sin(b.angle);
            float scale=heading%2==0?1.8f:2.7f;RiverCamera base=new RiverCamera(0,0,scale);
            RiverCamera c=new RiverCamera(192-base.screenX(x,y),240-base.screenY(x,y),scale);
            g.layout=Layout.renderTile(384,384,c);
            BufferedImage image=new BufferedImage(384,384,BufferedImage.TYPE_INT_ARGB_PRE);Graphics2D graphics=image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Painter p=new DesktopPainter(graphics);p.beginFrame();VesselView.draw(g,new RiverPainter(p,c),b,kind==0,false);
            // Native clip must be fully restored before subsequent scenery/UI drawing.
            p.rect(2,2,4,4,0xffff00ff);graphics.dispose();check(image.getRGB(3,3)==0xffff00ff,"clip stack restores after a vessel");
            float rise=GroundArt.LAND_RISE*scale,depth=c.screenY(x,y);
            for(int py=20;py<380;py+=3)for(int px=8;px<376;px+=3) {
                float wx=c.worldX(px+.5f,py+.5f+rise),wy=c.worldY(px+.5f,py+.5f+rise);
                boolean land=!g.inWater(wx,wy);
                // Ignore one-pixel antialias boundary and the sprite's ground-depth plane.
                boolean dryInterior=land && !g.inWater(wx-2,wy) && !g.inWater(wx+2,wy)
                    && !g.inWater(wx,wy-2) && !g.inWater(wx,wy+2);
                if(dryInterior && py+rise>depth+2 && Math.abs(wx-x)<150 && Math.abs(wy-y)<150) {
                    landProbes++;check((image.getRGB(px,py)>>>24)==0,"painted hull/cargo/light pixels must not show through solid foreground land: bank="+bank+" kind="+kind+" heading="+heading+" pixel="+px+","+py);
                } else if(!land && (image.getRGB(px,py)>>>24)>40)wetPixels++;
            }
        }
        check(landProbes>10000 && wetPixels>10000,"regression covers both hidden and visible hull pixels");
        System.out.println(checks+" production hull/cargo shoreline pixel assertions passed across all 32 headings");
    }
    private static void wallCorners(){
        Game root=new Game();root.startTutorial();Game g=root.activeGame();
        for(int i=0;i<g.cargo.length;i++)g.cargo[i]=null;
        int visible=0,hidden=0;float[] bank=TrainingChannel.WATER_BOUNDARY;
        for(int corner=0;corner<bank.length;corner+=2){
            float x=bank[corner],y=bank[corner+1];
            // Put a work light on the navigable side of each corner.
            for(int dx=-1;dx<=1;dx+=2)for(int dy=-1;dy<=1;dy+=2){
                if(!g.inWater(x+dx*20,y+dy*20))continue;
                g.tug.x=x+dx*20;g.tug.y=y+dy*20;
                RiverCamera base=new RiverCamera(0,0,2);
                RiverCamera c=new RiverCamera(128-base.screenX(x,y),150-base.screenY(x,y),2);
                g.layout=Layout.renderTile(256,256,c);
                BufferedImage image=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D graphics=image.createGraphics();Painter p=new DesktopPainter(graphics);
                TrainingGroundView.walls(g,p);ShorelineView.walls(g,p);VesselShoreLight.walls(g,p);
                p.rect(1,1,2,2,0xffff00ff);graphics.dispose();
                check(image.getRGB(1,1)==0xffff00ff,"wall receiver clips restore");
                for(int py=8;py<248;py+=2)for(int px=8;px<248;px+=2){
                    float wx=c.worldX(px+.5f,py+.5f+GroundArt.LAND_RISE*c.scale);
                    float wy=c.worldY(px+.5f,py+.5f+GroundArt.LAND_RISE*c.scale);
                    boolean dry=!g.inWater(wx,wy)&&!g.inWater(wx-2,wy)&&!g.inWater(wx+2,wy)
                        &&!g.inWater(wx,wy-2)&&!g.inWater(wx,wy+2);
                    int alpha=image.getRGB(px,py)>>>24;
                    if(dry){hidden++;check(alpha==0,"wall wetness/light cannot leak through raised corner: "+corner+" at "+px+","+py);}
                    else if(alpha>4)visible++;
                }
            }
        }
        check(hidden>10000 && visible>100,"corner checks retain visible walls and exclude foreground land");
    }

}
