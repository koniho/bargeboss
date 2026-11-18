package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Actual shared renderer review of the overhead chart and command cues. */
public final class OverviewMapReview {
    public static void main(String[] args)throws Exception{
        File dir=new File("out/overview-map");dir.mkdirs();
        for(String scene:new String[]{"shared-slip-wide","montrose-raft","indiana-harbor","barge-push"}){
            Game root=new Game();ProfileScenario.prepare(root,scene,1243,600);Game game=root.activeGame();
            for(int side:new int[]{GameSettings.LEFT,GameSettings.RIGHT}){
                game.settings.setJoystickSide(side);game.visualTicks=30;
                BufferedImage image=new BufferedImage(1243,600,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Renderer.draw(root,new DesktopPainter(g));g.dispose();ImageIO.write(image,"png",new File(dir,scene+"-"+side+".png"));
            }
        }
    }
}
