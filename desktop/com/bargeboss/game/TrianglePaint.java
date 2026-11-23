package com.bargeboss.game;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
/** Java2D reference for the GPU's linear premultiplied vertex-color interpolation. */
final class TrianglePaint implements Paint {
    private final float[] xy;private final int[] colors;
    TrianglePaint(float ax,float ay,int ac,float bx,float by,int bc,float cx,float cy,int cc){xy=new float[]{ax,ay,bx,by,cx,cy};colors=new int[]{ac,bc,cc};}
    public int getTransparency(){return Transparency.TRANSLUCENT;}
    public PaintContext createContext(ColorModel cm,Rectangle r,Rectangle2D user,AffineTransform tx,RenderingHints hints){
        float[] p=xy.clone();tx.transform(p,0,p,0,3);
        final float den=(p[3]-p[5])*(p[0]-p[4])+(p[4]-p[2])*(p[1]-p[5]);
        final float[][] rgba=new float[3][4];for(int i=0;i<3;i++){float a=(colors[i]>>>24)/255f;rgba[i][3]=a;for(int k=0;k<3;k++)rgba[i][k]=(colors[i]>>(16-k*8)&255)*a;}
        return new PaintContext(){public void dispose(){}public ColorModel getColorModel(){return ColorModel.getRGBdefault();}
            public Raster getRaster(int x,int y,int w,int h){WritableRaster raster=getColorModel().createCompatibleWritableRaster(w,h);int[] pixels=new int[w*h];
                for(int j=0;j<h;j++)for(int i=0;i<w;i++){float xx=x+i+.5f,yy=y+j+.5f;
                    float a=((p[3]-p[5])*(xx-p[4])+(p[4]-p[2])*(yy-p[5]))/den,b=((p[5]-p[1])*(xx-p[4])+(p[0]-p[4])*(yy-p[5]))/den,c=1-a-b;
                    float alpha=Math.max(0,Math.min(1,a*rgba[0][3]+b*rgba[1][3]+c*rgba[2][3]));int color=Math.round(alpha*255)<<24;
                    if(alpha>0)for(int k=0;k<3;k++)color|=Math.max(0,Math.min(255,Math.round((a*rgba[0][k]+b*rgba[1][k]+c*rgba[2][k])/alpha)))<<(16-k*8);pixels[j*w+i]=color;
                }raster.setDataElements(0,0,w,h,pixels);return raster;}
        };
    }
}
