import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.io.*;
import javax.imageio.ImageIO;

/** Read-only artwork composition and deterministic candidate shadow derivation. */
public class ReviewPark {
  static final String DIR="docs/art-studies/montrose/park-v2/";
  static int edge(int n,int count,int size){return Math.round((float)n*size/count);}
  static BufferedImage cell(BufferedImage s,int i){int x=edge(i%4,4,s.getWidth()),y=edge(i/4,2,s.getHeight());return s.getSubimage(x,y,edge(i%4+1,4,s.getWidth())-x,edge(i/4+1,2,s.getHeight())-y);}
  static BufferedImage grade(BufferedImage s){BufferedImage d=new BufferedImage(s.getWidth(),s.getHeight(),2);for(int y=0;y<s.getHeight();y++)for(int x=0;x<s.getWidth();x++){int c=s.getRGB(x,y);d.setRGB(x,y,(c&0xff000000)|(Math.round(((c>>16)&255)*.48f)<<16)|(Math.round(((c>>8)&255)*.54f)<<8)|Math.round((c&255)*.63f));}return d;}
  static float[] blur(float[] s,double sigma){int n=128,r=(int)Math.ceil(3*sigma);double[] k=new double[2*r+1];double sum=0;for(int i=-r;i<=r;i++){k[i+r]=Math.exp(-i*i/(2*sigma*sigma));sum+=k[i+r];}for(int i=0;i<k.length;i++)k[i]/=sum;float[] t=new float[n*n],d=new float[n*n];for(int y=0;y<n;y++)for(int x=0;x<n;x++)for(int i=-r;i<=r;i++)if(x+i>=0&&x+i<n)t[y*n+x]+=s[y*n+x+i]*k[i+r];for(int y=0;y<n;y++)for(int x=0;x<n;x++)for(int i=-r;i<=r;i++)if(y+i>=0&&y+i<n)d[y*n+x]+=t[(y+i)*n+x]*k[i+r];return d;}
  public static void main(String[] args)throws Exception{
    BufferedImage source=ImageIO.read(new File(DIR+"parkland-atlas-v2-alpha.png"));
    BufferedImage review=new BufferedImage(1800,680,1);Graphics2D g=review.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    int[] backgrounds={0x091b25,0xd9dedb,0x111417};String[] labels={"Water","Light","Dark"};
    for(int row=0;row<2;row++)for(int col=0;col<3;col++){g.setColor(new Color(backgrounds[col]));g.fillRect(col*600,row*340,600,340);g.drawImage(row==0?source:grade(source),col*600,row*340+30,600,300,null);g.setColor(row==0&&col==1?Color.BLACK:Color.WHITE);g.drawString(labels[col]+(row==0?" / raw source":" / existing scenery grade"),col*600+12,row*340+20);}
    g.dispose();ImageIO.write(review,"png",new File(DIR+"parkland-composites-v2.png"));
    BufferedImage masks=new BufferedImage(1024,256,2);
    for(int i=2;i<8;i++){
      BufferedImage scaled=new BufferedImage(128,128,2);g=scaled.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(cell(source,i),16,16,96,96,null);g.dispose();float[] m=new float[16384];for(int y=0;y<128;y++)for(int x=0;x<128;x++)m[y*128+x]=Math.max(0,((scaled.getRGB(x,y)>>>24)-64)/191f);
      for(int soft=0;soft<2;soft++){float[] b=blur(m,soft==0?1.0:3.2);for(int y=0;y<128;y++)for(int x=0;x<128;x++){int a=Math.round(b[y*128+x]*255);if((x==0||y==0||x==127||y==127)&&a!=0)throw new AssertionError("mask gutter");masks.setRGB(i*128+x,soft*128+y,(a<<24)|0x01060d);}}
    }
    ImageIO.write(masks,"png",new File(DIR+"parkland-shadow-v2.png"));
    double[][] pivots={{.5,.643},{.5,.643},{.541,.877},{.517,.865},{.484,.702},{.5,.655},{.53,.658},{.49,.66}};
    BufferedImage anchors=new BufferedImage(1774,887,1);g=anchors.createGraphics();g.setColor(new Color(0x142730));g.fillRect(0,0,1774,887);g.drawImage(source,0,0,null);g.setStroke(new BasicStroke(2));
    for(int i=0;i<8;i++){BufferedImage c=cell(source,i);int x=edge(i%4,4,1774)+(int)(pivots[i][0]*c.getWidth()),y=edge(i/4,2,887)+(int)(pivots[i][1]*c.getHeight());g.setColor(Color.CYAN);g.drawOval(x-7,y-7,14,14);g.drawLine(x-12,y,x+12,y);g.drawLine(x,y-12,x,y+12);g.drawString(i+" ground pivot; lights/smoke: none",edge(i%4,4,1774)+15,edge(i/4,2,887)+25);}
    g.dispose();ImageIO.write(anchors,"png",new File(DIR+"parkland-anchors-v2.png"));
    BufferedImage proof=new BufferedImage(1200,600,1);g=proof.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    for(int row=0;row<2;row++)for(int col=0;col<3;col++){int ox=col*400,oy=row*300;g.setColor(new Color(backgrounds[col]));g.fillRect(ox,oy,400,300);int i=row==0?2:6;double px=(16+96*pivots[i][0])/128,py=(16+96*pivots[i][1])/128;for(int soft=1;soft>=0;soft--){double scale=256.0/96,dx=(col-1)*110,dy=-50;AffineTransform tx=new AffineTransform();tx.translate(ox+200,oy+245);tx.concatenate(new AffineTransform(scale,0,soft==0?0:dx/128,soft==0?scale*.10:dy/128,0,0));tx.translate(-128*px,-128*py);g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,soft==0?.7f:.4f));g.drawImage(masks.getSubimage(i*128,soft*128,128,128),tx,null);}g.setComposite(AlphaComposite.SrcOver);BufferedImage c=cell(source,i);g.drawImage(c,ox+200-(int)(256*pivots[i][0]),oy+245-(int)(256*pivots[i][1]),256,256,null);g.setColor(col==1?Color.BLACK:Color.WHITE);g.drawString("Independent contact+cast / direction "+(col-1),ox+10,oy+20);}
    g.dispose();ImageIO.write(proof,"png",new File(DIR+"parkland-shadow-review-v2.png"));
    BufferedImage joins=new BufferedImage(1000,760,1);g=joins.createGraphics();g.setColor(new Color(0x10222b));g.fillRect(0,0,1000,760);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    for(int row=0;row<3;row++)for(int col=0;col<3;col++){BufferedImage c=cell(source,(row+col)%2);int x=260+(col-row)*182,y=30+(col+row)*103;g.drawImage(c,x,y,null);}
    g.setColor(Color.WHITE);g.drawString("Raw lawn adjacency / source pixels unchanged / slight edge seams need runtime land underfill",15,24);
    g.dispose();ImageIO.write(joins,"png",new File(DIR+"parkland-lawn-joins-v2.png"));
    long zero=0,partial=0,opaque=0;for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++){int a=source.getRGB(x,y)>>>24;if(a==0)zero++;else if(a==255)opaque++;else partial++;}System.out.println("alpha0="+zero+" alpha1..254="+partial+" alpha255="+opaque+"; shadow edges zero; lawn masks intentionally empty");
  }
}
