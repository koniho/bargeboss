package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual shared collision/rendering paths, captured without synthetic splash injection. */
public final class CollisionSplashReview {
    private static Game fixture(int kind){
        Game g=new Game();g.reset();for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}
        if(kind==1){g.startTutorial();g=g.activeGame();}
        for(int i=0;i<g.cargo.length;i++)g.cargo[i]=null;
        if(kind==0){g.tug.x=270;g.tug.y=500;g.tug.angle=0;g.tug.vx=65;g.addCargo(0,330,500);g.cargo[0].angle=0;}
        else if(kind==1){Buoys.Buoy b=g.buoys.get(g,NavigationLights.count(g)-2);g.tug.x=b.x-38;g.tug.y=b.y;g.tug.angle=0;g.tug.vx=65;}
        else if(kind==2){Buoys.Buoy b=g.buoys.get(g,1);g.tug.x=b.x-18;g.tug.y=b.y-32;g.tug.angle=(float)Math.PI/2;g.tug.vy=65;}
        else {g.tug.x=195;g.tug.y=500;g.tug.angle=(float)Math.PI;g.tug.vx=-65;}
        return g;
    }
    public static void main(String[] args)throws Exception{
        Game[] games={fixture(0),fixture(1),fixture(2),fixture(3)};
        String[] names={"VESSEL / VESSEL","YELLOW PRACTICE BUOY","RED CHANNEL MARKER","QUAY / WATER EDGE"};
        File dir=new File("out/collision-splash-motion");dir.mkdirs();
        BufferedImage board=new BufferedImage(1920,1200,BufferedImage.TYPE_INT_RGB);Graphics2D panels=board.createGraphics();
        for(int tick=0;tick<108;tick++){
            for(Game g:games)g.step();
            if(tick%3!=0)continue;
            BufferedImage frame=new BufferedImage(1280,800,BufferedImage.TYPE_INT_RGB);Graphics2D montage=frame.createGraphics();
            for(int kind=0;kind<games.length;kind++){
                Game g=games[kind];float x=kind==1?Tutorial.DRIVE_X[0]:kind==2?410:kind==3?190:300;
                float y=kind==1?Tutorial.DRIVE_Y[0]:kind==2?770:500;
                g.layout=Layout.focused(960,600,x,y,3.2f,480,350);
                BufferedImage scene=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);Graphics2D canvas=scene.createGraphics();
                canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Renderer.draw(g,new DesktopPainter(canvas));
                canvas.setColor(new Color(0x0a2028));canvas.fillRect(24,540,700,40);
                canvas.setColor(new Color(0xe0f3ed));canvas.drawString(names[kind]+" / SPRAY / IMPACT / SPARKS / tick "+tick,40,565);canvas.dispose();
                montage.drawImage(scene,(kind%2)*640,(kind/2)*400,640,400,null);
                if(tick==12)panels.drawImage(scene,kind%2*960,kind/2*600,null);
            }
            montage.dispose();ImageIO.write(frame,"png",new File(dir,String.format(java.util.Locale.ROOT,"%03d.png",tick/3)));
        }
        panels.dispose();ImageIO.write(board,"png",new File("out/collision-splash-review.png"));
        for(Game g:games)if(g.splashes.emitted==0)throw new AssertionError("Fixture did not generate a real collision splash");
        System.out.println("Directional collision stamp review: four real contact cases and 36 animation frames exported");
    }
}
