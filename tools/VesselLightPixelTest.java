package com.bargeboss.game;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Read-only production PNG validation; semantic identity is separately reviewed. */
public final class VesselLightPixelTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        BufferedImage tug=ImageIO.read(new File("assets/sprites/"+SpriteAtlas.file(SpriteAtlas.TUG,0)));
        int enabled=0;
        for(int f=0;f<32;f++)for(int k=0;k<TugLightArt.COUNT;k++)if(TugLightArt.visible(k,f)) {
            enabled++;int l=Math.round(tug.getWidth()*SpriteAtlas.sourceLeft(0,f)),t=Math.round(tug.getHeight()*SpriteAtlas.sourceTop(0,f));
            int r=Math.round(tug.getWidth()*SpriteAtlas.sourceRight(0,f)),b=Math.round(tug.getHeight()*SpriteAtlas.sourceBottom(0,f));
            int x=l+Math.round(TugLightArt.u(k,f)*(r-l)),y=t+Math.round(TugLightArt.v(k,f)*(b-t));
            check((tug.getRGB(x,y)>>>24)>128,"light sits on opaque painted fixture: "+f+"/"+k);
            boolean warm=false;for(int yy=y-3;yy<=y+3;yy++)for(int xx=x-3;xx<=x+3;xx++) {
                int color=tug.getRGB(xx,yy),red=color>>16&255,green=color>>8&255,blue=color&255;
                warm|=(color>>>24)>128 && red>170 && green>105 && green>red*.62f && blue<red*.5f;
            }
            check(warm,"reviewed source contains warm fixture pixels: "+f+"/"+k);
        }
        check(enabled==79,"79 reviewed visible points, unresolved17 disabled");
        BufferedImage barge=ImageIO.read(new File("assets/sprites/"+SpriteAtlas.file(SpriteAtlas.BARGE,0)));
        for(int f=0;f<32;f++) {
            Body body=new Body(1,0,300,500,(f-RiverCamera.DIRECTION_OFFSET)*(float)(Math.PI/16));
            float size=2*(body.half+body.radius)/BargeArt.HULL_FRACTION;
            for(int k=0;k<2;k++) {
                float local=VesselLighting.bargeLocalX(body,k),dx=local*(float)Math.cos(body.angle),dy=local*(float)Math.sin(body.angle);
                float u=BargeArt.pivotX(false,f)+RiverCamera.Q*(dx+dy)/(size*BargeArt.HULL_SCALE);
                float v=BargeArt.pivotY(false,f)+(RiverCamera.Q*RiverCamera.DEPTH*(dy-dx)-VesselLighting.BARGE_LAMP_Z*.81649658f)/(size*BargeArt.HULL_SCALE);
                int l=Math.round(barge.getWidth()*SpriteAtlas.sourceLeft(1,f)),t=Math.round(barge.getHeight()*SpriteAtlas.sourceTop(1,f));
                int r=Math.round(barge.getWidth()*SpriteAtlas.sourceRight(1,f)),b=Math.round(barge.getHeight()*SpriteAtlas.sourceBottom(1,f));
                int x=l+Math.round(u*(r-l)),y=t+Math.round(v*(b-t));
                check(x>=l && x<r && y>=t && y<b,"authored fitting lies inside source hull cell");
                check((barge.getRGB(x,y)>>>24)>200,"authored fitting attaches to opaque hull rather than floating: "+f+"/"+k);
            }
        }
        System.out.println(checks+" production vessel light pixel assertions passed");
    }
}
