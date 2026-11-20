package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual shared composition for rain/panel and peak-lightning visual review. */
public final class WeatherReview {
    private static void capture(Game g,String name)throws Exception{
        BufferedImage image=new BufferedImage(1244,600,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Renderer.draw(g,new DesktopPainter(graphics));graphics.dispose();
        ImageIO.write(image,"png",new File("out/weather-"+name+".png"));
    }
    public static void main(String[] args)throws Exception{
        Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();g.configureCanvas(1244,600);
        for(int i=0;i<180;i++)g.step();
        g.settings.developerWaves=true;g.waveControls.down(g,93,30,90);g.waveControls.up(g,93,30,90);
        g.settings.adjustRain(3);g.settings.adjustLightning(1);capture(g,"rain");
        g.settings.weatherFlashTick=g.visualTicks-4;capture(g,"lightning");
        System.out.println("Actual shared weather/panel and peak-lightning frames exported");
    }
}
