package com.bargeboss.game;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.*;import java.nio.file.*;import javax.imageio.ImageIO;
/** Compositing and anchor-estimate overlays. Does not edit production source pixels. */
public final class MontroseAssetReview {
    public static void main(String[] args)throws Exception{
        BufferedImage source=ImageIO.read(new File("assets/sprites/yacht-80ft-v1-32.png"));
        java.util.List<String> anchors=Files.readAllLines(Paths.get("docs/art-studies/montrose/yacht-80ft-v1-guide-anchors.csv"));
        for(int mode=0;mode<3;mode++){
            int n=256;BufferedImage out=new BufferedImage(n*8,n*4,BufferedImage.TYPE_INT_RGB);Graphics2D g=out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            DesktopPainter p=new DesktopPainter(g);
            for(int f=0;f<32;f++){
                int x=f%8*n,y=f/8*n,bg=f%3==0?0xe1e7e4:f%3==1?0x101820:0x17343c;
                g.setColor(new Color(bg));g.fillRect(x,y,n,n);float px=x+n*.5f,py=y+n*.72f;
                if(mode==0){
                    p.sprite(SpriteAtlas.YACHT,f,px,py,n,n,0,1);
                    g.setColor(new Color(0xe97b61));g.drawLine((int)px-5,(int)py,(int)px+5,(int)py);g.drawLine((int)px,(int)py-5,(int)px,(int)py+5);
                    for(int i=1;i<anchors.size();i++){
                        String[] a=anchors.get(i).split(",",-1);if(Integer.parseInt(a[0])!=f)continue;
                        float u=Float.parseFloat(a[10]),v=Float.parseFloat(a[11]);
                        int ax=Math.round(px+(u-.5f)*n),ay=Math.round(py+(v-.72f)*n);
                        g.setColor(a[2].equals("light")?Color.YELLOW:a[2].equals("fender")?Color.CYAN:Color.MAGENTA);
                        g.drawOval(ax-3,ay-3,6,6);
                    }
                }else p.sprite(ShadowArt.YACHT,f+(mode==2?32:0),px,py,n,n,0,1);
                g.setColor(Color.GRAY);g.drawString("POSE "+f+(mode==0?" / ESTIMATED ANCHORS":mode==1?" / CONTACT":" / CAST"),x+8,y+18);
            }
            g.dispose();ImageIO.write(out,"png",new File("out/montrose-yacht-"+(mode==0?"anchors":mode==1?"contact":"cast")+"-v1.png"));
        }
    }
}
