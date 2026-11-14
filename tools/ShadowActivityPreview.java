package com.bargeboss.game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Actual frozen-game ambient animation plus an isolated readable shadow/vehicle study. */
public final class ShadowActivityPreview {
    private static Graphics2D graphics(BufferedImage image){Graphics2D p=image.createGraphics();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);return p;}
    public static void main(String[] args)throws Exception {
        File dir=new File("out/ambient-review");dir.mkdirs();
        Game game=ScenePerformanceBenchmark.fixture(1333,"canal-middle");int start=game.visualTicks;
        BufferedImage frame=new BufferedImage(1333,600,BufferedImage.TYPE_INT_RGB);Graphics2D p=graphics(frame);
        for(int i=0;i<50;i++)Renderer.draw(game,new DesktopPainter(p));
        for(int i=0;i<80;i++) {
            game.visualTicks=start+i*6;Renderer.draw(game,new DesktopPainter(p));
            ImageIO.write(frame,"png",new File(dir,String.format("ambient-%03d.png",i)));
        }
        ImageIO.write(frame,"png",new File(dir,"BargeBoss-active-yard-shadows-v1.png"));p.dispose();
        BufferedImage study=new BufferedImage(1200,600,BufferedImage.TYPE_INT_RGB);p=graphics(study);p.setColor(new Color(0x6a7778));p.fillRect(0,0,1200,600);
        Game g=new Game();g.reset();g.layout=Layout.focused(1200,600,300,500,1,600,300);
        Painter canvas=new DesktopPainter(p);canvas.beginFrame();
        // Same art under three different light vectors: independent masks remain anchored.
        int source=SpriteAtlas.FACTORY,atlas=ShadowArt.forSource(source),f=0;
        for(int i=0;i<3;i++) {
            float x=200+i*400,y=390,size=260,w=size*SpriteAtlas.widthScale(atlas,f),h=size*SpriteAtlas.heightScale(atlas,f);
            canvas.spriteAffine(atlas,f+FactoryArt.COUNT,x,y,w,0,(i-1)*h*.6f,-h*.35f,.6f);
            canvas.sprite(source,f,x,y,size,size,0,1);
        }
        p.setColor(Color.WHITE);p.setFont(new Font(Font.SANS_SERIF,Font.BOLD,19));p.drawString("Same factory art, separate soft shadows with independently controlled direction",80,555);
        p.dispose();ImageIO.write(study,"png",new File(dir,"BargeBoss-independent-shadow-study-v1.png"));
        System.out.println("80 actual ambient frames and independent shadow direction study exported");
    }
}
