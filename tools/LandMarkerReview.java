package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Unchanged source pixels, deck registration, independent shadows and stationary beacon review. */
public final class LandMarkerReview {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static void cross(Graphics2D p,float x,float y,Color color){p.setColor(color);p.drawLine((int)x-6,(int)y,(int)x+6,(int)y);p.drawLine((int)x,(int)y-6,(int)x,(int)y+6);}
    private static Game fixture(){Game root=new Game();root.startTutorial();return root.activeGame();}
    private static void stationary(int frame){
        Game g=fixture();float wx=NavigationLights.x(g,frame),wy=NavigationLights.y(g,frame);
        g.layout=Layout.focused(600,500,wx,wy,4,300,430);
        Capture first=null;int firstEffects=0;boolean changed=false;
        for(int tick=0;tick<240;tick+=3){
            g.visualTicks=tick;Capture p=new Capture();LandMarkerArt.draw(g,p,wx,wy,frame,frame==0?NavigationLights.RED:NavigationLights.GREEN,NavigationLights.flash(tick,0));
            check(p.sprites==1 && p.rotation==0,"fixed beacon uses one upright sprite");
            if(first==null){first=p;firstEffects=p.effects;}
            else check(p.x==first.x && p.y==first.y && p.w==first.w && p.h==first.h,"beacon foundation stays fixed as waves/time change");
            changed|=p.effects!=firstEffects;
        }
        check(changed,"land lantern still has a dynamic flash/glow");
    }
    private static void deckClip(){
        Game g=fixture();int litDryPixels=0;
        for(int i=0;i<NavigationLights.entrances(g).length*2;i++){
            float wx=NavigationLights.x(g,i),wy=NavigationLights.y(g,i);
            g.layout=Layout.focused(360,300,wx,wy,4,180,180);RiverCamera c=g.layout.camera;
            BufferedImage im=new BufferedImage(360,300,BufferedImage.TYPE_INT_ARGB);Graphics2D canvas=im.createGraphics();
            LandMarkerArt.ground(g,new DesktopPainter(canvas),wx,wy,i%2==1?0:1,NavigationLights.color(g,i),1);canvas.dispose();
            int leaks=0,painted=0;
            for(int y=0;y<im.getHeight();y++)for(int x=0;x<im.getWidth();x++){
                if((im.getRGB(x,y)>>>24)<4)continue;painted++;
                float px=c.worldX(x+.5f,y+.5f+GroundArt.LAND_RISE*c.scale),py=c.worldY(x+.5f,y+.5f+GroundArt.LAND_RISE*c.scale);
                float margin=1/c.scale;
                for(float[] r:TrainingChannel.WATER_RECTANGLES)if(px>r[0]+margin && px<r[2]-margin && py>r[1]+margin && py<r[3]-margin){leaks++;break;}
            }
            check(leaks==0,"raised-deck shadows/glow do not spill across tutorial water at marker "+i);
            check(painted>50,"deck effects remain visible after water clipping");litDryPixels+=painted;
        }
        System.out.println(litDryPixels+" lit/shadowed deck pixels checked at every tutorial entrance");
    }
    /** Actual production sprite pixels, with the same foreground factory depth rule as gameplay. */
    private static void beaconSightlines(){
        Game g=fixture();int visiblePixels=0;
        for(int marker=0;marker<NavigationLights.entrances(g).length*2;marker++){
            float wx=NavigationLights.x(g,marker),wy=NavigationLights.y(g,marker);
            g.layout=Layout.focused(960,600,wx,wy,2.8f,160,220);RiverCamera c=g.layout.camera;
            BufferedImage beacon=new BufferedImage(320,320,BufferedImage.TYPE_INT_ARGB),foreground=new BufferedImage(320,320,BufferedImage.TYPE_INT_ARGB);
            Graphics2D bp=beacon.createGraphics(),fp=foreground.createGraphics();
            LandMarkerArt.draw(g,new DesktopPainter(bp),wx,wy,marker%2==1?0:1,NavigationLights.color(g,marker),0);bp.dispose();
            DesktopPainter factoryPainter=new DesktopPainter(fp);
            for(int slot=0;slot<TrainingChannelView.FACTORY_PLACEMENTS;slot++){
                float x=TrainingChannelView.factoryX(slot),y=TrainingChannelView.factoryY(slot);
                if(y-x<wy-wx)continue;
                int frame=TrainingChannelView.factoryCell(slot);float size=FactoryArt.size(frame)*c.scale;
                factoryPainter.sprite(SpriteAtlas.FACTORY,frame,c.screenX(x,y),c.screenY(x,y)-GroundArt.LAND_RISE*c.scale,size,size,0,1);
            }
            fp.dispose();int painted=0,hidden=0;
            for(int y=0;y<320;y++)for(int x=0;x<320;x++)if((beacon.getRGB(x,y)>>>24)>128){painted++;if((foreground.getRGB(x,y)>>>24)>128)hidden++;}
            check(painted>1000,"marker review captures the complete opaque beacon silhouette");
            check(hidden==0,"no foreground factory obscures tutorial beacon "+marker+"; hidden pixels="+hidden);visiblePixels+=painted;
        }
        System.out.println(visiblePixels+" opaque beacon pixels remain visible across all four tutorial pairs");
    }
    public static void main(String[] args)throws Exception{
        BufferedImage board=new BufferedImage(1200,1000,BufferedImage.TYPE_INT_RGB);Graphics2D p=board.createGraphics();
        p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);DesktopPainter painter=new DesktopPainter(p);
        for(int frame=0;frame<2;frame++){
            BufferedImage source=ImageIO.read(new File("assets/sprites/"+LandMarkerArt.file(frame)));
            check(source.getWidth()==1254 && source.getHeight()==1254,"source dimensions match registration");
            // Red source has 28 edge samples at alpha 1/255; preserve those negligible original pixels.
            for(int i=0;i<1254;i++)check((source.getRGB(i,0)>>>24)<=1 && (source.getRGB(i,1253)>>>24)<=1 && (source.getRGB(0,i)>>>24)<=1 && (source.getRGB(1253,i)>>>24)<=1,"source gutters have no visible opaque pixels");
            check((source.getRGB(Math.round(LandMarkerArt.LAMP_X*1254),Math.round(LandMarkerArt.LAMP_Y*1254))>>>24)>240,"lamp anchor lands on opaque lens pixels");
            check(SpriteAtlas.pivotX(SpriteAtlas.LAND_MARKER,frame)==LandMarkerArt.PIVOT_X && SpriteAtlas.pivotY(SpriteAtlas.LAND_MARKER,frame)==LandMarkerArt.PIVOT_Y,"full sprite registers its footprint pivot");
            int start=frame*600;p.setColor(new Color(frame==0?0x10262c:0xd4d2c7));p.fillRect(start,0,600,550);
            float x=start+300,y=475,size=490;painter.sprite(SpriteAtlas.LAND_MARKER,frame,x,y,size,size,0,1);
            float lx=x+(LandMarkerArt.LAMP_X-LandMarkerArt.PIVOT_X)*size,ly=y+(LandMarkerArt.LAMP_Y-LandMarkerArt.PIVOT_Y)*size;
            cross(p,x,y,Color.CYAN);cross(p,lx,ly,Color.MAGENTA);
            p.setColor(frame==0?Color.WHITE:Color.BLACK);p.drawString(LandMarkerArt.file(frame)+" / cyan footprint / magenta lens",start+24,530);
            stationary(frame);
        }
        for(int row=0;row<2;row++)for(int col=0;col<4;col++){
            int x=col*300,y=550+row*225,frame=col/2;boolean cast=col%2==1;
            p.setColor(new Color(row==0?0xddd8ca:0x45545b));p.fillRect(x,y,300,225);
            int mask=frame+(cast?LandMarkerArt.COUNT:0);float size=235*SpriteAtlas.widthScale(ShadowArt.LAND_MARKER,mask);
            painter.spriteAffine(ShadowArt.LAND_MARKER,mask,x+150,y+145,size,0,cast?size*-.25f:0,size*(cast?.42f:.13f),.8f);
            cross(p,x+150,y+145,row==0?new Color(0x0d7d93):Color.CYAN);
            p.setColor(row==0?Color.BLACK:Color.WHITE);p.drawString((frame==0?"RED":"GREEN")+" / "+(cast?"CAST":"CONTACT")+" / "+(row==0?"LIGHT":"DARK"),x+18,y+204);
        }
        p.dispose();new File("out").mkdirs();ImageIO.write(board,"png",new File("out/land-marker-review.png"));
        deckClip();beaconSightlines();System.out.println(checks+" land marker source, stationary transform, deck clipping and sightline checks; out/land-marker-review.png");
    }
    private static final class Capture implements Painter{
        int sprites,effects;float x,y,w,h,rotation;
        public void rect(float x,float y,float w,float h,int color){effects=31*effects+color;}
        public void circle(float x,float y,float r,int color){effects=31*effects+color;}
        public void line(float x,float y,float xx,float yy,float w,int color){effects=31*effects+color;}
        public void poly(float[] xy,int color){effects=31*effects+color;}
        public void sprite(int atlas,int frame,float x,float y,float w,float h,float rotation,float alpha){sprites++;this.x=x;this.y=y;this.w=w;this.h=h;this.rotation=rotation;}
        public void spriteBlend(int atlas,int a,int b,float x,float y,float w,float h,float rotation,float mix,float alpha){}
    }
}
