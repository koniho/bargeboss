package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Production shared-renderer captures of mouth radio, opening and instructor handoff. */
public final class TutorialRadioReview {
    public static void main(String[] args) throws Exception {
        File out=new File("out/tutorial-radio");out.mkdirs();
        for(int width:new int[]{960,1333})for(String scene:new String[]{"canal-radio","canal-operator","canal-opening","canal-follow","canal-milestone"}){
            Game root=new Game();root.configureCanvas(width,600);ProfileScenario.prepare(root,scene,width,600);Game g=root.activeGame();
            BufferedImage image=new BufferedImage(width,600,BufferedImage.TYPE_INT_RGB);
            Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            root.render(new DesktopPainter(p));p.dispose();ImageIO.write(image,"png",new File(out,scene+"-"+width+".png"));
            System.out.println(scene+" "+width+" phase="+g.tutorial.canal.phase+" lift="+g.passages.amount(0)+" house="+g.tutorial.canal.houseX(g)+","+g.tutorial.canal.houseY(g));
        }
    }
}
