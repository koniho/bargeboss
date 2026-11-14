package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Deterministic 3D blockout used only as image-generation pose guidance. */
public final class TugPoseGuide {
    private static final int CELL=384;
    private static final double DEPTH=1/StrictMath.sqrt(3),HEIGHT=StrictMath.sqrt(2.0/3);
    private static final class Face {
        double[] x,y,z; int color;
        Face(int n,int rgb) { x=new double[n];y=new double[n];z=new double[n];color=0xff000000|rgb; }
    }
    private final ArrayList<Face> faces=new ArrayList<Face>();
    private final double angle,cx,cy,scale;
    private TugPoseGuide(int frame) {
        angle=frame*StrictMath.PI/16; cx=(frame%8+.5)*CELL;
        cy=(frame/8+.72)*CELL; scale=CELL*.78/100;
    }
    private void face(double[][] vertices,int rgb) {
        Face face=new Face(vertices.length,rgb);
        for(int i=0;i<vertices.length;i++) {
            double[] v=vertices[i];
            double x=v[0]*StrictMath.cos(angle)-v[1]*StrictMath.sin(angle);
            double y=v[0]*StrictMath.sin(angle)+v[1]*StrictMath.cos(angle);
            face.x[i]=cx+x*scale;face.y[i]=cy+(y*DEPTH-v[2]*HEIGHT)*scale;
            face.z[i]=y*HEIGHT+v[2]*DEPTH;
        }
        faces.add(face);
    }
    private void prism(double[][] outline,double low,double high,int side,int top) {
        double[][] roof=new double[outline.length][3];
        for(int i=0;i<outline.length;i++) {
            int j=(i+1)%outline.length;
            double[] a=outline[i],b=outline[j]; roof[i]=new double[]{a[0],a[1],high};
            face(new double[][]{{a[0],a[1],low},{b[0],b[1],low},{b[0],b[1],high},{a[0],a[1],high}},side);
        }
        face(roof,top);
    }
    private void box(double x,double y,double w,double h,double z,double height,int side,int top) {
        prism(new double[][]{{x-w/2,y-h/2},{x+w/2,y-h/2},{x+w/2,y+h/2},{x-w/2,y+h/2}},z,z+height,side,top);
    }
    private void cylinder(double x,double y,double radius,double z,double height,int side,int top) {
        double[][] polygon=new double[12][2];
        for(int i=0;i<12;i++) { double a=i*Math.PI/6;polygon[i][0]=x+radius*StrictMath.cos(a);polygon[i][1]=y+radius*StrictMath.sin(a); }
        prism(polygon,z,z+height,side,top);
    }
    private void draw(BufferedImage image,double[] depth) {
        double[][] hull={{-50,-11},{-45,-20},{38,-20},{50,-14},{50,14},{38,20},{-45,20},{-50,11}};
        prism(hull,0,7,0x252d32,0xb85d27);
        box(12,0,38,27,7,12,0xd6cdb2,0xe5d7b5);
        box(14,0,32,25,19,9,0xded8bf,0xe3d6b5);
        // Dark window band and roof are fixed geometry, never swapped between ends.
        box(14,0,32.5,25.5,21,5,0x25414d,0x304c56);
        box(14,0,36,29,28,2,0xaa4d21,0xe18335);
        cylinder(-12,0,4.5,7,25,0x34363b,0x151e25);
        cylinder(-12,0,4.6,25,3,0xcd7429,0xd88230);
        box(-33,0,12,14,7,6,0xb38725,0xe7b93e);
        // Two unmistakable push knees ONLY on the bow (+X).
        box(49,-12,5,5,0,17,0x3d454a,0x647074);
        box(49,12,5,5,0,17,0x3d454a,0x647074);
        for(int i=0;i<5;i++) {
            box(-36+i*17,-20.5,9,3,1,6,0x161d24,0x344048);
            box(-36+i*17,20.5,9,3,1,6,0x161d24,0x344048);
        }
        // Per-pixel depth is essential: sorting whole faces can wrongly cover
        // a distant winch with the broad deck face even when it is above deck.
        for(Face f:faces) for(int i=1;i<f.x.length-1;i++) triangle(image,depth,f,0,i,i+1);
    }
    private static void triangle(BufferedImage image,double[] depth,Face f,int a,int b,int c) {
        int left=Math.max(0,(int)Math.floor(Math.min(f.x[a],Math.min(f.x[b],f.x[c]))));
        int right=Math.min(image.getWidth()-1,(int)Math.ceil(Math.max(f.x[a],Math.max(f.x[b],f.x[c]))));
        int top=Math.max(0,(int)Math.floor(Math.min(f.y[a],Math.min(f.y[b],f.y[c]))));
        int bottom=Math.min(image.getHeight()-1,(int)Math.ceil(Math.max(f.y[a],Math.max(f.y[b],f.y[c]))));
        double determinant=(f.y[b]-f.y[c])*(f.x[a]-f.x[c])+(f.x[c]-f.x[b])*(f.y[a]-f.y[c]);
        if(Math.abs(determinant)<1e-10)return;
        for(int y=top;y<=bottom;y++) for(int x=left;x<=right;x++) {
            double wa=((f.y[b]-f.y[c])*(x+.5-f.x[c])+(f.x[c]-f.x[b])*(y+.5-f.y[c]))/determinant;
            double wb=((f.y[c]-f.y[a])*(x+.5-f.x[c])+(f.x[a]-f.x[c])*(y+.5-f.y[c]))/determinant;
            double wc=1-wa-wb;
            if(wa<0 || wb<0 || wc<0)continue;
            double z=wa*f.z[a]+wb*f.z[b]+wc*f.z[c];int at=y*image.getWidth()+x;
            if(z>depth[at]) { depth[at]=z;image.setRGB(x,y,f.color); }
        }
    }
    public static void main(String[] args) throws Exception {
        BufferedImage guide=new BufferedImage(CELL*8,CELL*4,BufferedImage.TYPE_INT_ARGB);
        double[] depth=new double[guide.getWidth()*guide.getHeight()];Arrays.fill(depth,Double.NEGATIVE_INFINITY);
        for(int frame=0;frame<32;frame++) new TugPoseGuide(frame).draw(guide,depth);
        // Regression: these known unobstructed winch tops must survive the deck.
        for(int frame:new int[]{0,4,12,16,20,24,28}) {
            double angle=frame*Math.PI/16,scale=CELL*.78/100;
            int x=(int)Math.floor((frame%8+.5)*CELL-33*Math.cos(angle)*scale);
            int y=(int)Math.floor((frame/8+.72)*CELL+(-33*Math.sin(angle)*DEPTH-13*HEIGHT)*scale);
            if((guide.getRGB(x,y)&0xffffff)!=0xe7b93e)
                throw new IllegalStateException("Winch top incorrectly occluded in frame "+frame);
        }
        File out=new File("docs/art-studies/tug-isometric-v4-pose-guide.png");
        ImageIO.write(guide,"png",out);
        System.out.println(out+" — 32 poses, 3072x1536, fixed 35.264-degree camera, 11.25-degree steps");
    }
}
