package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual renderer: quiet idle, cruising, loaded slow push and astern travel. */
public final class WaterlineReview {
    public static void main(String[] args)throws Exception {
        File dir=new File("out/waterline");dir.mkdirs();Game[] games=new Game[8];Body[] boats=new Body[8];
        for(int i=0;i<8;i++) {
            int kind=i/4,view=i%4;Game g=new Game();g.mode=Game.TITLE;games[i]=g;
            float angle=view*(float)Math.PI/2;
            Body b=new Body(kind==0?0:1,kind==0?-1:0,300,400,angle);boats[i]=b;
            float speed=view==0?0:view==2?7:view==3?-40:45;
            b.vx=(float)StrictMath.cos(angle)*speed;b.vy=(float)StrictMath.sin(angle)*speed;
            b.engineLoad=kind==0?(view==0?0:view==2?1:.7f):0;b.floatingInitialized=true;
            if(kind==0){g.tug=b;g.reversing=view==3;}
        }
        for(int tick=0;tick<168;tick++) {
            for(int i=0;i<8;i++) {
                Game g=games[i];Body b=boats[i];g.visualTicks=tick;
                b.x+=b.vx/60;b.y+=b.vy/60;b.heave=(float)StrictMath.sin(tick*.05)*1.1f;
                g.water.foam.step();g.water.foam.emit(g,b);
            }
            if(tick<60 || tick%3!=0)continue;
            BufferedImage image=new BufferedImage(1000,560,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();
            graphics.setColor(new Color(0x0b2732));graphics.fillRect(0,0,1000,560);
            for(int i=0;i<8;i++) {
                int kind=i/4,view=i%4;Game g=games[i];Body b=boats[i];
                Graphics2D tile=(Graphics2D)graphics.create(view*250,kind*280,250,280);
                tile.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                RiverCamera base=new RiverCamera(0,0,1.8f);
                RiverCamera camera=new RiverCamera(125-base.screenX(b.x,b.y),185-base.screenY(b.x,b.y),1.8f);
                g.layout=Layout.renderTile(250,280,camera);
                Painter world=new RiverPainter(new DesktopPainter(tile),camera);
                g.water.foam.draw(g,world);VesselView.draw(g,world,b,kind==0,false);
                tile.setColor(new Color(0xa8c9c8));tile.drawString((kind==0?"TUG":"BARGE")+new String[]{" / IDLE"," / CRUISING"," / SLOW PUSH"," / ASTERN"}[view],45,250);tile.dispose();
            }
            graphics.dispose();ImageIO.write(image,"png",new File(dir,String.format("%03d.png",(tick-60)/3)));
        }
        System.out.println("Sprite foam animation exported to out/waterline");
    }
}
