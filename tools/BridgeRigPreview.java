package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** The production shared bridge view, captured across its articulation cycle. */
public final class BridgeRigPreview {
    private static Game fixture() {
        Game g=new Game();g.challengeId=1;g.reset();g.ticks=500;
        g.layout=Layout.focused(960,600,300,Bridge.Y,1.65f,480,405);
        g.tug.x=300;g.tug.y=Bridge.Y+500;g.trafficBoat.x=8000;g.trafficBoat.y=8000;
        for(Body b:g.cargo)if(b!=null)b.active=false;
        for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}
        return g;
    }
    private static BufferedImage frame(Game g,String label,boolean showBoat) {
        BufferedImage image=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Painter p=new DesktopPainter(graphics),world=new RiverPainter(p,g.layout.camera);
        p.rect(0,0,960,600,HarborAtmosphere.WATER_BASE);WaveView.surface(g,p);
        world.rect(Physics.LEFT-500,Bridge.Y-700,500,1400,0xff383d3d);
        world.rect(Physics.RIGHT,Bridge.Y-700,500,1400,0xff383d3d);
        world.line(Physics.LEFT,Bridge.Y-700,Physics.LEFT,Bridge.Y+700,5,0xff7d7970);
        world.line(Physics.RIGHT,Bridge.Y-700,Physics.RIGHT,Bridge.Y+700,5,0xff7d7970);
        if(showBoat)VesselView.draw(g,world,g.tug,true);
        BridgeView.draw(g,p);
        Lettering.draw(p,"ARTICULATED BRIDGE / "+label,20,20,14,VisualStyle.CREAM);
        Lettering.draw(p,"SHARED JAVA MESH / RIGID LEAVES / ATTACHED LIGHTS",20,47,9,VisualStyle.GOLD);
        graphics.dispose();return image;
    }
    public static void main(String[] args) throws Exception {
        File study=new File("docs/art-studies/bridge-rig-v1");study.mkdirs();
        File frames=new File("out/bridge-rig-frames");frames.mkdirs();
        int[] phases={Bridge.CLOSED,Bridge.WAITING,Bridge.OPENING,Bridge.OPENING,Bridge.OPENING,Bridge.OPEN,Bridge.CLOSING,Bridge.CLOSING,Bridge.OPENING};
        int[] ticks={0,180,30,60,90,120,30,60,60};
        String[] labels={"CLOSED","WAITING","OPENING 25%","OPENING 50%","OPENING 75%","OPEN","CLOSING 75%","CLOSING 50%","VESSEL OCCLUSION"};
        BufferedImage contact=new BufferedImage(2880,1800,BufferedImage.TYPE_INT_RGB);Graphics2D cg=contact.createGraphics();
        for(int i=0;i<labels.length;i++) {
            Game g=fixture();g.bridge.phase=phases[i];g.bridge.ticks=ticks[i];g.visualTicks=120+i*30;
            if(i==8){g.tug.x=300;g.tug.y=Bridge.Y+25;g.tug.angle=(float)Math.PI/2;}
            BufferedImage capture=frame(g,labels[i],i==8);cg.drawImage(capture,i%3*960,i/3*600,null);
            ImageIO.write(capture,"png",new File(frames,"phase-"+i+".png"));
        }
        cg.dispose();ImageIO.write(contact,"png",new File(study,"BargeBoss-articulated-bridge-v1-contact.png"));
        for(int i=0;i<48;i++) {
            Game g=fixture();g.bridge.phase=i<24?Bridge.OPENING:Bridge.CLOSING;g.bridge.ticks=(i%24)*5;g.visualTicks=i*5;
            ImageIO.write(frame(g,i<24?"OPENING":"CLOSING",false),"png",new File(frames,String.format("cycle-%03d.png",i)));
        }
        Game stage=fixture();stage.bridge.phase=Bridge.OPENING;stage.bridge.ticks=60;stage.visualTicks=120;
        stage.tug.x=300;stage.tug.y=Bridge.Y+150;
        BufferedImage actual=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);Graphics2D ag=actual.createGraphics();
        ag.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(stage,new DesktopPainter(ag));ag.dispose();ImageIO.write(actual,"png",new File(study,"BargeBoss-articulated-bridge-v1-stage.png"));
        System.out.println("Bridge phases, occlusion and 48 shared-renderer cycle frames exported.");
    }
}
