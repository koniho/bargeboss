package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
public final class SpeakerPortraitReview {
    public static void main(String[] args)throws Exception{
        File dir=new File("out/instructor-portrait-v1");dir.mkdirs();
        for(int width:new int[]{960,1333}){
            Game root=new Game();ProfileScenario.prepare(root,"instructor-teach",width,600);
            Game g=root.activeGame();g.radio.transmit(RadioAudio.TEACHER);
            for(int i=0;i<16;i++)root.step();
            BufferedImage im=new BufferedImage(width,600,BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics=im.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Renderer.draw(root,new DesktopPainter(graphics));graphics.dispose();
            ImageIO.write(im,"png",new File(dir,"instructor-"+width+".png"));
            if(!SpeakerPortrait.visible(g,"INSTRUCTOR / CHANNEL 08"))throw new AssertionError("Teacher portrait missing");
            g.dialogue.show(g,"INSTRUCTOR","READY?","TAKE YOUR TIME.","YES");
            for(int i=0;i<120;i++)g.radio.step();
            if(!SpeakerPortrait.visible(g,"INSTRUCTOR"))throw new AssertionError("Portrait lost between transmissions");
            g.radio.transmit(RadioAudio.REPLY);
            if(!SpeakerPortrait.visible(g,"INSTRUCTOR"))throw new AssertionError("Portrait lost during reply");
            g.dialogue.showFrom(g,RadioAudio.BRIDGE,"BRIDGE","READY?","WAIT.","YES");
            if(SpeakerPortrait.visible(g,"INSTRUCTOR / CHANNEL 08"))throw new AssertionError("Bridge uses teacher face");
        }
        if(SpeakerPortrait.facing(700,960)!=-1 || SpeakerPortrait.facing(200,960)!=1)throw new AssertionError("Must face center");
        int[] ticks={0,14,21,30,38,49};
        for(int i=0;i<6;i++)if(SpeakerPortrait.frame(ticks[i])!=i)throw new AssertionError("Authored frame timing");
        if(SpeakerPortrait.frame(61)!=0)throw new AssertionError("Loop");
        System.out.println("Speaker identity, inward facing and authored timing passed.");
    }
}
