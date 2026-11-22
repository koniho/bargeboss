package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Dark UI contact sheet; real shared panel exports for both bridge operators. */
public final class NpcPortraitReview {
    static final String[] IDS={"bridge-operator-steel","bridge-operator-night","harbor-dispatcher","lock-keeper","dock-foreman","fire-rescue-captain"};
    static final String[] LABELS={"STEELWORKS BRIDGE","NIGHT BRIDGE","HARBOR DISPATCH","LOCK KEEPER","DOCK FOREMAN","FIRE RESCUE"};
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        File dir=new File("docs/art-studies/npc-portraits-v1");dir.mkdirs();
        BufferedImage contact=new BufferedImage(960,640,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=contact.createGraphics();graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Painter p=new DesktopPainter(graphics);p.rect(0,0,960,640,0xff091923);
        Lettering.heading(p,"HARBOR RADIO CREW",26,20,27,PilotGlass.TEXT);
        for(int i=0;i<6;i++){
            BufferedImage sheet=ImageIO.read(new File("assets/sprites/portrait-"+IDS[i]+"-v1.png"));
            check(sheet.getWidth()==576 && sheet.getHeight()==384,"Sheet grid "+IDS[i]);
            check(sheet.getColorModel().hasAlpha(),"Alpha channel "+IDS[i]);
            for(int frame=0;frame<6;frame++){
                int transparent=0,solid=0;
                for(int y=0;y<192;y++)for(int x=0;x<192;x++){
                    int a=sheet.getRGB(frame%3*192+x,frame/3*192+y)>>>24;
                    if(a==0)transparent++;if(a>200)solid++;
                }
                check(transparent>3000 && solid>5000,"Nonempty cutout "+IDS[i]+"/"+frame);
            }
            int x=18+i%3*314,y=72+i/3*280;
            PilotGlass.panel(p,x,y,296,264,PilotGlass.FILL);
            graphics.drawImage(sheet,x+52,y+7,x+244,y+199,0,0,192,192,null);
            Lettering.heading(p,LABELS[i],x+16,y+216,16,PilotGlass.AMBER);
            Lettering.draw(p,"6 FRAMES / TRANSPARENT",x+16,y+242,10,PilotGlass.MUTED);
        }
        graphics.dispose();ImageIO.write(contact,"png",new File(dir,"crew-review.png"));
        for(int gate:new int[]{0,1}){
            Game root=new Game();ProfileScenario.prepare(root,"instructor-teach",1333,600);Game g=root.activeGame();
            g.tutorial.towBridges.calling=gate;
            g.dialogue.showFrom(g,RadioAudio.BRIDGE,"BRIDGE CONTROL / CHANNEL 08","BRIDGE CONTROL HERE, CAPTAIN.","READY TO ENTER THE NEXT BASIN?","YES. OPEN THE BRIDGE.","STAND BY.");
            for(int i=0;i<20;i++)root.step();
            check(SpeakerPortrait.atlas(g,"BRIDGE")== (gate==1?SpriteAtlas.BRIDGE_OPERATOR_NIGHT:SpriteAtlas.BRIDGE_OPERATOR_STEEL),"Assigned operator");
            BufferedImage im=new BufferedImage(1333,600,BufferedImage.TYPE_INT_RGB);Graphics2D canvas=im.createGraphics();canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Renderer.draw(root,new DesktopPainter(canvas));canvas.dispose();ImageIO.write(im,"png",new File(dir,"bridge-"+gate+".png"));
            for(int i=0;i<120;i++)g.radio.step();
            check(SpeakerPortrait.visible(g,"BRIDGE"),"Operator remains while awaiting reply");
            g.radio.transmit(RadioAudio.REPLY);
            check(SpeakerPortrait.atlas(g,"BRIDGE")!=(SpriteAtlas.INSTRUCTOR_PORTRAIT),"Reply retains bridge identity");
        }
        System.out.println("36 cutout frames and bridge conversation identity/persistence passed.");
    }
}
