package com.bargeboss.game;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
/** Full shared renderer at each enabled berth, plus every Montrose variant and lock. */
public final class AllCranesReview {
    public static void main(String[] args)throws Exception{
        new File("out/cranes").mkdirs();int count=0;
        BufferedImage sheet=new BufferedImage(1440,((Challenges.COUNT+8)/3)*300,BufferedImage.TYPE_INT_RGB);
        // Individual full-size files are authoritative; sheet is a navigation index.
        java.util.ArrayList<BufferedImage> frames=new java.util.ArrayList<BufferedImage>();
        for(int level=-1;level<Challenges.COUNT;level++){
            Game root=new Game(),g=root;if(level<0){root.startTutorial();g=root.activeGame();}else{g.challengeId=level;g.reset();}
            for(int berth=0;berth<3;berth++){
                boolean single=g.montroseStage() || g.lockStage();if(single && berth>0)continue;
                if(!single && !g.berthDisplayEnabled(berth))continue;
                float x,y;if(g.montroseStage()){x=MontroseBoatHoist.X;y=MontroseBoatHoist.Y;}else if(g.lockStage()){x=300;y=50;}else{x=g.dockCranes[berth].x;y=g.dockCranes[berth].y;}
                g.layout=Layout.focused(960,600,x+20,y,2,480,380);
                BufferedImage image=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Renderer.draw(g,new DesktopPainter(p));
                p.setColor(Color.WHITE);p.setFont(new Font("SansSerif",Font.BOLD,18));p.drawString((level<0?"PRACTICE":Challenges.title(level))+" / BERTH "+berth,20,580);p.dispose();
                ImageIO.write(image,"png",new File("out/cranes/level-"+level+"-berth-"+berth+".png"));frames.add(image);count++;
            }
        }
        sheet=new BufferedImage(1440,((count+2)/3)*300,BufferedImage.TYPE_INT_RGB);Graphics2D p=sheet.createGraphics();for(int i=0;i<count;i++)p.drawImage(frames.get(i),(i%3)*480,(i/3)*300,480,300,null);p.dispose();ImageIO.write(sheet,"png",new File("out/cranes/all-levels.png"));System.out.println("Rendered "+count+" full-scene crane/lock views across practice and every level");
    }
}
