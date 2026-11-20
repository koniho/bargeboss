package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual shared renderer, four entry presets and source/frame/static-cache round trips. */
public final class LightingReview {
    private static BufferedImage render(Game game){
        BufferedImage image=new BufferedImage(960,600,BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics=image.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        DesktopPainter p=new DesktopPainter(graphics);p.prepareScene(game.activeGame().lighting(),LevelAssets.upcoming(game).preparationAtlases());
        for(int i=0;i<6;i++)game.render(p);graphics.dispose();return image;
    }
    public static void main(String[] args)throws Exception{
        File out=new File("out/lighting-review");out.mkdirs();
        String[] scenes={"tutorial-yard","montrose-inlet","calumet-bridge","indiana-harbor"};
        BufferedImage sheet=new BufferedImage(1920,1280,BufferedImage.TYPE_INT_RGB);Graphics2D s=sheet.createGraphics();
        for(int row=0;row<scenes.length;row++){
            Game dawn=new Game();dawn.selectLighting(0);ProfileScenario.prepare(dawn,scenes[row],960,600);BufferedImage first=null;
            for(int id=0;id<4;id++){
                Game game=id==0?dawn:new Game();if(id!=0){game.selectLighting(id);ProfileScenario.prepare(game,scenes[row],960,600);}
                BufferedImage image=render(game);if(id==0)first=image;
                ImageIO.write(image,"png",new File(out,scenes[row]+"-"+id+".png"));
                s.drawImage(image,id*480,row*320+20,480,300,null);s.setColor(Color.WHITE);s.drawString(scenes[row]+" / "+LightingProfile.preset(id).name,id*480+8,row*320+15);
            }
            BufferedImage again=render(dawn);int differences=0;
            for(int y=0;y<600;y++)for(int x=0;x<960;x++)if(first.getRGB(x,y)!=again.getRGB(x,y))differences++;
            if(differences!=0)throw new AssertionError(scenes[row]+" cache round trip changed "+differences+" pixels");
            System.out.println(scenes[row]+": four presets rendered; 576000 cache round-trip pixels matched");
        }
        s.dispose();ImageIO.write(sheet,"png",new File(out,"profiles.png"));
    }
}
