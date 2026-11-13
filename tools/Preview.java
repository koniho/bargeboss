package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Captures the actual shared renderer, not a mockup. */
public final class Preview {
    public static void main(String[] args) throws Exception {
        File directory=new File("out"); directory.mkdirs();
        Game game=new Game(); capture(game,"title"); game.reset(); capture(game,"stage");
        game.down(game.cargo[0].x,game.cargo[0].y);
        for(int i=0;i<180;i++) game.step(); capture(game,"push");
        game.background(); capture(game,"pause");
        game.mode=Game.FINISH; game.score=4120; game.delivered=6; capture(game,"finish");
        System.out.println("Shared renderer: out/{title,stage,push,pause,finish}.png");
    }
    private static void capture(Game game,String name) throws Exception {
        BufferedImage image=new BufferedImage(600,960,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(game,new DesktopPainter(g)); g.dispose(); ImageIO.write(image,"png",new File("out/"+name+".png"));
    }
}
