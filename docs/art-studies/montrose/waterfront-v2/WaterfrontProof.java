import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Read-only source composites and independent candidate masks. Does not edit source pixels. */
public final class WaterfrontProof {
  static final String DIR="docs/art-studies/montrose/waterfront-v2/";
  static float[] blur(float[] a,double sigma) {
    int r=(int)Math.ceil(sigma*3);double[] k=new double[r*2+1];double sum=0;
    for(int i=-r;i<=r;i++){k[i+r]=Math.exp(-i*i/(2*sigma*sigma));sum+=k[i+r];}
    for(int i=0;i<k.length;i++)k[i]/=sum;
    float[] tmp=new float[16384],out=new float[16384];
    for(int y=0;y<128;y++)for(int x=0;x<128;x++)for(int i=-r;i<=r;i++)if(x+i>=0&&x+i<128)tmp[y*128+x]+=a[y*128+x+i]*k[i+r];
    for(int y=0;y<128;y++)for(int x=0;x<128;x++)for(int i=-r;i<=r;i++)if(y+i>=0&&y+i<128)out[y*128+x]+=tmp[(y+i)*128+x]*k[i+r];
    return out;
  }
  static void hint(Graphics2D g){g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);}
  static void flat(Graphics2D g,BufferedImage src,int f,double x,double y)throws Exception {
    double top=f==0?329:924,right=f==0?614:1213,left=f==0?38:637;
    AffineTransform source=new AffineTransform(right-top,185,left-top,185,top,149);
    AffineTransform target=new AffineTransform(100,57.735,-100,57.735,x,y);
    target.concatenate(source.createInverse());
    Path2D p=new Path2D.Double();p.moveTo(x,y);p.lineTo(x+100,y+57.735);p.lineTo(x,y+115.47);p.lineTo(x-100,y+57.735);p.closePath();
    Shape old=g.getClip();g.clip(p);g.drawImage(src,target,null);g.setClip(old);
  }
  public static void main(String[] args)throws Exception {
    BufferedImage src=ImageIO.read(new File(DIR+"waterfront-v2-candidate.png"));
    int cell=src.getWidth()/2;BufferedImage graded=new BufferedImage(src.getWidth(),src.getHeight(),2);
    for(int y=0;y<src.getHeight();y++)for(int x=0;x<src.getWidth();x++){
      int p=src.getRGB(x,y),r=p>>>16&255,g=p>>>8&255,b=p&255;
      graded.setRGB(x,y,(p&0xff000000)|(((r*122+127)/255)<<16)|(((g*138+127)/255)<<8)|(b*160+127)/255);
    }
    BufferedImage review=new BufferedImage(1500,1060,2);Graphics2D g=review.createGraphics();hint(g);
    int[] backgrounds={0x132b36,0xe6e3db,0x171b22};String[] names={"Water","Light","Dark"};
    for(int row=0;row<2;row++)for(int col=0;col<3;col++){
      int x=col*500,y=row*530;g.setColor(new Color(backgrounds[col]));g.fillRect(x,y,500,530);
      g.drawImage(row==0?src:graded,x,y+30,500,500,null);
      g.setColor(col==1?Color.DARK_GRAY:Color.WHITE);g.drawString(names[col]+(row==0?" | raw candidate":" | shared cool-industrial grade"),x+14,y+22);
    }
    g.dispose();ImageIO.write(review,"png",new File(DIR+"waterfront-v2-composites.png"));
    BufferedImage masks=new BufferedImage(512,128,2);
    for(int f=0;f<2;f++){
      BufferedImage tiny=new BufferedImage(128,128,2);Graphics2D t=tiny.createGraphics();hint(t);
      t.drawImage(src,16,16,112,112,f*cell,cell,(f+1)*cell,2*cell,null);t.dispose();
      float[] alpha=new float[16384];for(int y=0;y<128;y++)for(int x=0;x<128;x++)alpha[y*128+x]=Math.max(0,((tiny.getRGB(x,y)>>>24)-64)/191f);
      for(int soft=0;soft<2;soft++){
        float[] blurred=blur(alpha,soft==0?1:3.2);int c=f+soft*2;
        for(int y=0;y<128;y++)for(int x=0;x<128;x++){
          int a=Math.round(blurred[y*128+x]*255);
          if((x==0||y==0||x==127||y==127)&&a!=0)throw new AssertionError("Mask gutter");
          masks.setRGB(c*128+x,y,a<<24|0x01060d);
        }
      }
    }
    ImageIO.write(masks,"png",new File(DIR+"waterfront-v2-reference-shadows.png"));
    BufferedImage shadows=new BufferedImage(1152,800,2);g=shadows.createGraphics();hint(g);
    for(int bg=0;bg<3;bg++)for(int soft=0;soft<2;soft++)for(int f=0;f<2;f++){
      int x=bg*384+f*192,y=soft*400;g.setColor(new Color(backgrounds[bg]));g.fillRect(x,y,192,400);
      g.setColor(bg==1?Color.DARK_GRAY:Color.WHITE);g.drawString((f==0?"Wall":"Dock")+(soft==0?" contact":" soft cast"),x+6,y+24);
      g.drawImage(masks,x+8,y+40,x+184,y+216,(f+soft*2)*128,0,(f+soft*2+1)*128,128,null);
      for(int direction=-1;direction<=1;direction++){
        AffineTransform save=g.getTransform();g.translate(x+96,y+260+(direction+1)*48);
        g.transform(new AffineTransform(1.1,0,direction*.7,.22,-64,-32));g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,.55f));
        g.drawImage(masks.getSubimage((f+soft*2)*128,0,128,128),0,0,null);g.setTransform(save);g.setComposite(AlphaComposite.SrcOver);
      }
    }
    g.dispose();ImageIO.write(shadows,"png",new File(DIR+"waterfront-v2-shadow-review.png"));
    BufferedImage joins=new BufferedImage(1200,650,2);g=joins.createGraphics();hint(g);g.setColor(new Color(0x132b36));g.fillRect(0,0,1200,650);
    g.setColor(Color.WHITE);g.drawString("Flat surfaces remapped to exact 30-degree screen axes: straight and corner join proof",24,26);
    for(int f=0;f<2;f++)for(int u=0;u<3;u++)for(int v=0;v<2;v++)if(u<2||v==1)flat(g,src,f,250+f*600+(u-v)*100,80+(u+v)*57.735);
    g.setColor(Color.WHITE);g.drawString("Concrete: visible slab seams; source not certified seamless",24,500);g.drawString("Timber: board rhythm restarts; clip inside edge alpha",624,500);
    g.drawString("Use rigid geometry for curving sea walls, caps, dock rims and pilings. Do not rotate full module billboards.",24,600);
    g.dispose();ImageIO.write(joins,"png",new File(DIR+"waterfront-v2-joins.png"));
    int transparent=0,opaque=0,partial=0;for(int y=0;y<src.getHeight();y++)for(int x=0;x<src.getWidth();x++){int a=src.getRGB(x,y)>>>24;if(a==0)transparent++;else if(a==255)opaque++;else partial++;}
    System.out.println("source="+src.getWidth()+"x"+src.getHeight()+" alpha zero="+transparent+" full="+opaque+" partial="+partial+"; four independent masks have transparent gutters");
  }
}
