package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Real shared-renderer views at the finite tutorial edges and the complete overhead chart. */
public final class TutorialCoverageReview {
    private static void save(Game g,String name) throws Exception {
        BufferedImage image=new BufferedImage((int)g.layout.width,(int)g.layout.height,BufferedImage.TYPE_INT_RGB);
        Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.render(new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File("out/tutorial-coverage/"+name+".png"));
        RiverCamera c=g.layout.camera;
        System.out.println(name+" zoom="+c.zoom+" tug="+c.screenX(g.tug.x,g.tug.y)+","+c.screenY(g.tug.x,g.tug.y));
    }
    public static void main(String[] args) throws Exception {
        new File("out/tutorial-coverage").mkdirs();
        for(int width:new int[]{960,1333})for(String scene:new String[]{"tutorial-entrance","canal-middle","barge-bridge"}){
            Game root=new Game();root.configureCanvas(width,600);ProfileScenario.prepare(root,scene,width,600);Game g=root.activeGame();
            g.changeCamera(g.layout);save(g,scene+"-"+width+"-auto");
            g.changeCamera(Layout.focused(width,600,g.tug.x,g.tug.y,.65f,width*.5f,300));
            save(g,scene+"-"+width+"-wide");
        }
        OverviewMap.Chart chart=new OverviewMap.Chart(0);
        BufferedImage image=new BufferedImage(736,736,BufferedImage.TYPE_INT_ARGB);
        Graphics2D p=image.createGraphics();p.scale(4,4);chart.paint(new DesktopPainter(p));p.dispose();
        ImageIO.write(image,"png",new File("out/tutorial-coverage/overhead.png"));
        System.out.println("Finite bounds: "+TrainingScenery.MIN_X+","+TrainingScenery.MIN_Y+" .. "+TrainingScenery.MAX_X+","+TrainingScenery.MAX_Y+"; factories="+TrainingChannelView.FACTORY_PLACEMENTS);
    }
}
