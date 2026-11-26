package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
/** Reports painted base anchors, rather than assuming equal cells imply registration. */
public final class PortraitRegistrationReview {
    static final String[] FILES={"instructor-portrait-v2","portrait-bridge-operator-steel-v1","portrait-bridge-operator-night-v1","portrait-harbor-dispatcher-v1","portrait-lock-keeper-v1","portrait-dock-foreman-v1","portrait-fire-rescue-captain-v1","portrait-gavin-dockmaster-v1","portrait-chas-v1","portrait-alex-v1","portrait-captain-whitney-v1","portrait-captain-wesley-v1","portrait-captain-dot-v2"};
    public static void main(String[] args)throws Exception{
        BufferedImage out=new BufferedImage(960,FILES.length*180,BufferedImage.TYPE_INT_RGB);Graphics2D p=out.createGraphics();p.setColor(new Color(9,25,35));p.fillRect(0,0,960,FILES.length*180);
        boolean corrected=args.length>0;
        for(int row=0;row<FILES.length;row++){
            BufferedImage sheet=ImageIO.read(new File("assets/sprites/"+FILES[row]+".png"));
            System.out.print(FILES[row]+": ");float firstX=0,firstY=0;
            for(int frame=0;frame<6;frame++){
                int left=192,right=0,top=192,bottom=0;
                BufferedImage cell=sheet.getSubimage(frame%3*192,frame/3*192,192,192);
                for(int y=0;y<192;y++)for(int x=0;x<192;x++)if((cell.getRGB(x,y)>>>24)>128){left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
                int baseLeft=192,baseRight=0;for(int y=bottom-8;y<=bottom;y++)for(int x=0;x<192;x++)if((cell.getRGB(x,y)>>>24)>128){baseLeft=Math.min(baseLeft,x);baseRight=Math.max(baseRight,x);}
                System.out.print(frame+"="+(baseLeft+baseRight)*.5+","+bottom+" bbox="+left+","+top+","+right+"; ");
                int atlas=row>=10?SpriteAtlas.WHITNEY_PORTRAIT+row-10:row==9?SpriteAtlas.ALEX_PORTRAIT:row==8?SpriteAtlas.CHAS_PORTRAIT:row==7?SpriteAtlas.GAVIN_PORTRAIT:SpriteAtlas.INSTRUCTOR_PORTRAIT+row;
                float anchorX=(baseLeft+baseRight)*.5f-192*SpriteAtlas.pivotX(atlas,frame),anchorY=bottom-192*SpriteAtlas.pivotY(atlas,frame);
                if(frame==0){firstX=anchorX;firstY=anchorY;}
                if(Math.abs(anchorX-firstX)>.001f || Math.abs(anchorY-firstY)>.001f)throw new AssertionError("Unregistered base "+FILES[row]+"/"+frame);
                if(corrected)new DesktopPainter(p).spriteAffine(atlas,frame,frame*160+80,row*180+100,-160,0,0,160,1);
                else p.drawImage(cell,frame*160,row*180+20,160,160,null);p.setColor(Color.CYAN);p.drawLine(frame*160,row*180+164,frame*160+159,row*180+164);
            }
            System.out.println();p.setColor(Color.WHITE);p.drawString(FILES[row],5,row*180+14);
        }
        p.dispose();ImageIO.write(out,"png",new File(corrected?"out/portrait-registration-after.png":"out/portrait-registration-before.png"));
    }
}
