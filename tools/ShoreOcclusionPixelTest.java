package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Actual hull pixels, all hull headings plus 64-pose equipment, both lower banks and both upper banks. */
public final class ShoreOcclusionPixelTest {
    private static int checks;
    // This probe projects the full land rise; floating docks have their own lower clip.
    private static boolean land(Game g,float x,float y){return g.montroseStage()?!MontroseHarbor.openWater(x,y):!g.inWater(x,y);}
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args) {
        cachedWalls();
        if(args.length>0 && args[0].equals("terrain"))return;
        wallCorners();
        Game root=new Game();root.startTutorial();Game g=root.activeGame();if(args.length>0){g=new Game();g.challengeId=args[0].startsWith("montrose")?Challenges.MONTROSE:args[0].equals("calumet")?0:Challenges.INDIANA;g.reset();}int wetPixels=0,landProbes=0;
        for(int bank=0;bank<4;bank++)for(int kind=0;kind<6;kind++)for(int heading=0;heading<(kind>=4?64:32);heading++) {
            float x=bank==0?-1610:bank==1?TrainingChannel.BASIN_LEFT+16:bank==2?TrainingChannel.MIN-16:-1610;
            float y=bank==0?TrainingChannel.BASIN_BOTTOM-16:bank==3?TrainingChannel.BASIN_TOP+16:600;
            if(g.calumetStage() || g.indianaStage()){
                float[] coast=g.calumetStage()?CalumetRiver.COAST:IndianaHarbor.COAST;
                int edge=(coast.length/8*bank)/2*2,next=(edge+2)%coast.length;
                float dx=coast[next]-coast[edge],dy=coast[next+1]-coast[edge+1],len=Physics.length(dx,dy),sign=PolygonWater.area(coast)>0?1:-1;
                x=(coast[edge]+coast[next])*.5f-dy/len*sign*16;y=(coast[edge+1]+coast[next+1])*.5f+dx/len*sign*16;
            }
            if(g.montroseStage()){
                float[][] endpoints={{1520,-660,1320,-750},{2240,-1150,2290,-1370},{2125,-1470,2095,-1410},{1990,-552,1870,-560}};
                if(args.length>0 && args[0].equals("montrose-haulout"))endpoints=new float[][]{{1040,275,1040,205},{1080,205,1080,275},{1040,205,1080,205},{1080,275,1105,275}};
                float[] a=endpoints[bank];float ax=MontroseHarbor.mapX(a[0]),ay=MontroseHarbor.mapY(a[1]),bx=MontroseHarbor.mapX(a[2]),by=MontroseHarbor.mapY(a[3]);
                float dx=bx-ax,dy=by-ay,len=Physics.length(dx,dy),nx=-dy/len,ny=dx/len;
                x=(ax+bx)*.5f;y=(ay+by)*.5f;if(!MontroseHarbor.openWater(x+nx*8,y+ny*8)){nx=-nx;ny=-ny;}x+=nx*16;y+=ny*16;
            }
            Body b=new Body(kind==0?0:1,kind>=4?2:kind-1,x,y,heading*(float)Math.PI/(kind>=4?32:16));b.ability=kind==4?BargeAbilities.FIREWORKS:kind==5?BargeAbilities.FIRE:BargeAbilities.NONE;b.floatingInitialized=true;b.heave=(heading%3-1)*1.5f;
            b.vx=45*(float)StrictMath.cos(b.angle);b.vy=45*(float)StrictMath.sin(b.angle);
            float scale=heading%2==0?1.8f:2.7f;RiverCamera base=new RiverCamera(0,0,scale);
            RiverCamera c=new RiverCamera(192-base.screenX(x,y),240-base.screenY(x,y),scale);
            g.layout=Layout.renderTile(384,384,c);
            BufferedImage image=new BufferedImage(384,384,BufferedImage.TYPE_INT_ARGB_PRE);Graphics2D graphics=image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Painter p=new DesktopPainter(graphics);p.beginFrame();VesselView.draw(g,new RiverPainter(p,c),b,kind==0,false);
            // Native clip must be fully restored before subsequent scenery/UI drawing.
            p.rect(2,2,4,4,0xffff00ff);graphics.dispose();check(image.getRGB(3,3)==0xffff00ff,"clip stack restores after a vessel");
            float rise=(g.montroseStage()?MontroseHarbor.LAND_RISE:GroundArt.LAND_RISE)*scale,depth=c.screenY(x,y);
            for(int py=20;py<380;py+=3)for(int px=8;px<376;px+=3) {
                float wx=c.worldX(px+.5f,py+.5f+rise),wy=c.worldY(px+.5f,py+.5f+rise);
                boolean land=land(g,wx,wy);
                // Ignore one-pixel antialias boundary and the sprite's ground-depth plane.
                boolean dryInterior=land && land(g,wx-2,wy) && land(g,wx+2,wy)
                    && land(g,wx,wy-2) && land(g,wx,wy+2);
                if(dryInterior && py+rise>depth+2 && Math.abs(wx-x)<150 && Math.abs(wy-y)<150) {
                    landProbes++;check((image.getRGB(px,py)>>>24)==0,"painted hull/cargo/light pixels must not show through solid foreground land: bank="+bank+" kind="+kind+" heading="+heading+" pixel="+px+","+py);
                } else if(!land && (image.getRGB(px,py)>>>24)>40)wetPixels++;
            }
        }
        check(landProbes>10000 && wetPixels>10000,"regression covers both hidden and visible hull pixels");
        System.out.println(checks+" production hull/cargo shoreline pixel assertions passed across all legacy hull and 64 equipment headings");
    }
    /** Cached terrain must retain the same visible wall faces as the live geometry. */
    private static void cachedWalls(){
        Game root=new Game();root.startTutorial();Game g=root.activeGame();
        int probes=0;float[] bank=TrainingChannel.WATER_BOUNDARY;
        for(int edge=0;edge<bank.length;edge+=2){
            int next=(edge+2)%bank.length;
            float dx=bank[next]-bank[edge],dy=bank[next+1]-bank[edge+1];
            if(dx+dy<=0)continue; // Only water-facing walls visible from this camera.
            float x=(bank[edge]+bank[next])*.5f,y=(bank[edge+1]+bank[next+1])*.5f;
            for(float scale:new float[]{1,2}){
                RiverCamera base=new RiverCamera(0,0,scale);
                g.layout=Layout.renderTile(256,256,new RiverCamera(128-base.screenX(x,y),150-base.screenY(x,y),scale));
                BufferedImage direct=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB_PRE);
                BufferedImage cached=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D a=direct.createGraphics(),b=cached.createGraphics();
                try{
                    TrainingGroundView.walls(g,new DesktopPainter(a));
                    DesktopPainter p=new DesktopPainter(b);p.beginFrame();StaticTerrain.draw(g,p,false);
                }finally{a.dispose();b.dispose();}
                for(int py=8;py<248;py+=3)for(int px=8;px<248;px+=3){
                    // Require a solid interior neighborhood, excluding raster/clip edges.
                    boolean solid=true;
                    for(int ox=-2;ox<=2;ox++)for(int oy=-2;oy<=2;oy++)
                        if((direct.getRGB(px+ox,py+oy)>>>24)<250)solid=false;
                    if(!solid)continue;
                    probes++;
                    check((cached.getRGB(px,py)>>>24)>=250,
                            "cached training wall missing: edge="+edge+" scale="+scale+" pixel="+px+","+py);
                }
            }
        }
        check(probes>1000,"cached wall regression covers solid faces across training banks and zooms");
        System.out.println(probes+" cached training wall interior pixels retained without lighting");
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
