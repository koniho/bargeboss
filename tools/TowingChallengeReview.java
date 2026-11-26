package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Shared-renderer captures of the towing-challenge basin: overview, start, mid-run and result. */
public final class TowingChallengeReview {
    private static File out;
    private static void capture(Game root,String name)throws Exception{
        Game g=root.activeGame();
        BufferedImage image=new BufferedImage((int)g.layout.width,(int)g.layout.height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        root.render(new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File(out,name+".png"));
        System.out.println(name+": phase="+g.tutorial.challenge.phase+" tied="+g.tutorial.challenge.tied+" clock="+g.tutorial.challenge.clockLabel()+" tug="+(int)g.tug.x+","+(int)g.tug.y);
    }
    /** World-space site plan of the training water with challenge barges and berths. */
    private static void sitePlan(Game g)throws Exception{
        float scale=.22f;int w=(int)((TrainingScenery.MAX_X-TrainingScenery.MIN_X)*scale),h=(int)((TrainingScenery.MAX_Y-TrainingScenery.MIN_Y)*scale);
        BufferedImage map=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D m=map.createGraphics();
        m.setColor(new Color(0x293139));m.fillRect(0,0,w,h);m.scale(scale,scale);m.translate(-TrainingScenery.MIN_X,-TrainingScenery.MIN_Y);
        m.setColor(new Color(0x777865));
        for(int row=IndustrialRoadView.MIN_ROW;row<IndustrialRoadView.MAX_ROW;row++)for(int col=IndustrialRoadView.MIN_COL;col<IndustrialRoadView.MAX_COL;col++)if(IndustrialRoadView.roadClear(col,row))m.fillRect(col*64,row*64,64,64);
        m.setColor(new Color(0x90b1bd));for(int i=0;i<TrainingChannelView.FACTORY_PLACEMENTS;i++)m.fillRect((int)TrainingChannelView.factoryX(i)-29,(int)TrainingChannelView.factoryY(i)-29,58,58);
        m.setColor(new Color(0x1d5866));for(float[] r:TrainingChannel.WATER_RECTANGLES)m.fillRect((int)r[0],(int)r[1],(int)(r[2]-r[0]),(int)(r[3]-r[1]));
        float[] b=TrainingChannel.WATER_BOUNDARY;m.setColor(new Color(0xe9e2c8));m.setStroke(new BasicStroke(6));
        for(int i=0;i<b.length;i+=2){int j=(i+2)%b.length;m.drawLine((int)b[i],(int)b[i+1],(int)b[j],(int)b[j+1]);}
        m.setColor(new Color(0xffc52f));for(Body hull:g.tutorial.challenge.barges)if(hull!=null)m.fillRect((int)(hull.x-18),(int)(hull.y-38),36,76);
        m.setColor(new Color(0x28e7ff));for(int row=0;row<TowingChallenge.BARGES;row++)m.fillOval((int)TrainingChannel.CHALLENGE_RIGHT+30,(int)TowingChallenge.rowY(row)-12,24,24);
        m.setColor(Color.RED);m.fillOval((int)g.tug.x-20,(int)g.tug.y-20,40,40);
        m.dispose();ImageIO.write(map,"png",new File(out,"site-plan.png"));
    }
    public static void main(String[] args)throws Exception{
        out=new File(args.length>0?args[0]:"out/towing-challenge");out.mkdirs();
        int width=args.length>1?Integer.parseInt(args[1]):1333;
        Game root=new Game();root.configureCanvas(width,600);ReplayInput.startPractice(root);Game g=root.activeGame();
        g.tutorial.lesson=Tutorial.CHALLENGE;g.tutorial.setup(g);
        sitePlan(g);
        // Basin overview through the production renderer at the widest practice zoom.
        Layout saved=g.layout;
        g.dialogue.close(g);
        g.layout=Layout.focused(g.layout.width,g.layout.height,250,-2280,.5f,g.layout.width*.5f,g.layout.height*.5f);capture(root,"basin-overview");
        g.layout=Layout.focused(g.layout.width,g.layout.height,250,-2280,PinchZoom.MIN,g.layout.width*.5f,g.layout.height*.5f);capture(root,"basin-overview-pinch-min");
        if(args.length>2)return;
        g.layout=saved;g.tutorial.setup(g);
        for(int i=0;i<40;i++)root.step();
        capture(root,"intro-radio");
        boolean start=false,mid=false,tie=false,late=false,result=false;
        for(int tick=0;tick<6500 && g.tutorial.lesson==Tutorial.CHALLENGE;tick++){
            TowingChallenge c=g.tutorial.challenge;
            if(!start && c.phase==TowingChallenge.RUN && c.remaining<=TowingChallenge.TICKS-20){capture(root,"challenge-start");start=true;}
            if(!tie && c.phase==TowingChallenge.RUN && g.ropePhase==3){capture(root,"challenge-tie");tie=true;}
            if(!mid && c.phase==TowingChallenge.RUN && c.remaining<=30*60){capture(root,"challenge-mid");mid=true;}
            if(!late && c.phase==TowingChallenge.RUN && c.remaining<=8*60){capture(root,"challenge-final-seconds");late=true;}
            if(!result && c.phase==TowingChallenge.RESULT && g.dialogue.ready()){capture(root,"challenge-result");result=true;}
            ReplayInput.tutorial(root);root.step();
        }
        System.out.println("lesson="+g.tutorial.lesson+" best="+g.settings.towingChallengeBest()+" save="+root.saveProgress().replace('\n','|'));
        if(!start || !mid || !result || g.tutorial.lesson!=Tutorial.DONE)throw new AssertionError("towing challenge review did not finish");
    }
}
