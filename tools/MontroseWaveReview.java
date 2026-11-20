package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual renderer captures across lake, entrance and shelter, with one identical tow. */
public final class MontroseWaveReview {
    public static void main(String[] args)throws Exception{
        File out=new File("out/montrose-waves");out.mkdirs();
        String[] names={"lake-head","lake-beam","entrance","shelter"};
        float[] mx={900,900,350,1000},my={1500,1500,1180,600};
        for(int scene=0;scene<names.length;scene++){
            Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();g.configureCanvas(960,600);
            for(int i=0;i<g.cargo.length;i++)g.cargo[i]=null;
            g.tug.x=MontroseHarbor.mapX(mx[scene]);g.tug.y=MontroseHarbor.mapY(my[scene]);
            g.tug.angle=scene==1?0:-(float)StrictMath.PI/2;
            float c=(float)StrictMath.cos(g.tug.angle),s=(float)StrictMath.sin(g.tug.angle);
            Body tow=new Body(1,0,g.tug.x-c*100,g.tug.y-s*100,g.tug.angle);g.cargo[0]=tow;g.tow.attach(g,tow);
            float x=g.tug.x,y=g.tug.y;
            for(int tick=0;tick<240;tick++){
                g.step();g.layout=Layout.focused(960,600,x,y,2.4f,480,300);
                if(tick%30!=0)continue;
                BufferedImage image=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);
                Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Renderer.draw(g,new DesktopPainter(p));p.dispose();
                ImageIO.write(image,"png",new File(out,names[scene]+"-"+tick+".png"));
            }
        }
        System.out.println("Montrose wave review: "+out);
    }
}
