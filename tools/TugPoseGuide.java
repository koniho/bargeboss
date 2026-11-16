package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;

/** Deterministic 3D blockout used only as image-generation pose guidance. */
public final class TugPoseGuide {
    private static final int CELL=384;
    private static int headingCount=32,poseCount=32,columns=8;
    private static double headingOffset=0;
    private static final double DEPTH=1/StrictMath.sqrt(3),HEIGHT=StrictMath.sqrt(2.0/3);
    private static final class Face {
        double[] x,y,z; int color;
        Face(int n,int rgb) { x=new double[n];y=new double[n];z=new double[n];color=0xff000000|rgb; }
    }
    private final ArrayList<Face> faces=new ArrayList<Face>();
    private final ArrayList<Anchor> anchors=new ArrayList<Anchor>();
    private final int frame;
    private final boolean concept;
    private final double angle,cx,cy,scale;
    private static final class Anchor {
        String id,kind,side,color,role; double x,y,z,r,nx,ny,nz;
        Anchor(String id,String kind,double x,double y,double z,double r,double nx,double ny,double nz,String side,String color,String role) {
            this.id=id;this.kind=kind;this.x=x;this.y=y;this.z=z;this.r=r;
            this.nx=nx;this.ny=ny;this.nz=nz;this.side=side;this.color=color;this.role=role;
        }
    }
    private TugPoseGuide(int frame,boolean concept) {
        this.frame=frame;this.concept=concept;
        angle=(frame+headingOffset)*StrictMath.PI*2/headingCount; cx=(frame%columns+.5)*CELL;
        cy=(frame/columns+.72)*CELL; scale=CELL*.78/100;
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
        if(concept) conceptGeometry(); else originalGeometry();
        // Per-pixel depth prevents a broad deck face incorrectly covering fittings.
        for(Face f:faces) for(int i=1;i<f.x.length-1;i++) triangle(image,depth,f,0,i,i+1);
    }
    private void originalGeometry() {
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
    }
    private void lamp(String id,double x,double y,double z,String role) {
        cylinder(x,y,1.0,z-4,4,0x43433b,0x555449);
        cylinder(x,y,1.3,z,2,0xe7b347,0xffdd7d);
        anchors.add(new Anchor(id,"light",x,y,z+1,1.3,0,0,1,"","#ffca74",role));
    }
    private void tire(int index,double x,int side) {
        double y=side*21.5,z=5,r=4.4,t=1.2;
        for(int i=0;i<16;i++) for(int j=0;j<8;j++) {
            double[][] quad=new double[4][3];int[][] ends={{i,j},{i+1,j},{i+1,j+1},{i,j+1}};
            for(int k=0;k<4;k++) {
                double a=ends[k][0]*Math.PI/8,b=ends[k][1]*Math.PI/4;
                quad[k]=new double[]{x+(r+t*Math.cos(b))*Math.cos(a),y+t*Math.sin(b),z+(r+t*Math.cos(b))*Math.sin(a)};
            }
            face(quad,0x20272c);
        }
        String name=side<0?"port":"starboard";
        anchors.add(new Anchor("tire."+name+"."+index,"tire",x,y,z,r+t,0,side,0,name,"","fender"));
    }
    private void conceptGeometry() {
        // Rounded hull with the concept's layered red/white deckhouse and raised pilothouse.
        double[][] hull=new double[32][2];
        for(int i=0;i<32;i++) {
            double a=i*Math.PI/16;
            hull[i]=new double[]{Math.copySign(29,Math.cos(a))+21*Math.cos(a),21*Math.sin(a)};
        }
        prism(hull,0,9,0x262e33,0xc9662a);
        box(3,0,53,28,9,12,0xa63421,0xc54827);
        box(3,0,53,28,21,9,0xe2d5b6,0xc3c4b4);
        box(3,0,57,32,30,1.5,0x48534d,0x616d61);
        box(14,0,31,26,31.5,14,0xe8ddc2,0xe8ddc2);
        box(14,0,31.5,26.5,36,7,0x243b47,0x243b47);
        box(14,0,35,30,45.5,2,0xbd3c20,0xe66327);
        cylinder(-17,0,4.3,9,42,0x2c3338,0x121c22);
        cylinder(-17,0,4.4,39,3,0xd35b23,0xe2762d);
        anchors.add(new Anchor("stack.main","smokestack",-17,0,51,4.3,0,0,1,"","","engine-exhaust"));
        box(-37,0,10,12,9,5,0xb78b29,0xe3ad36);
        // Short bow pads preserve the rounded bow instead of turning it into a square pontoon.
        box(49,-10,3,7,1,10,0x30383c,0x495054);
        box(49,10,3,7,1,10,0x30383c,0x495054);
        for(int side:new int[]{-1,1}) {
            for(int i=0;i<5;i++) tire(i,-34+i*17,side);
            for(int i=0;i<7;i++) cylinder(-39+i*13,side*19,.45,9,6,0xd9d7c6,0xe2decb);
            box(0,side*19,80,.65,15,.6,0xd9d7c6,0xe2decb);
            for(int i=0;i<5;i++) cylinder(-23+i*12,side*15,.4,31.5,5,0xd9d7c6,0xe2decb);
            box(1,side*15,49,.6,36,.6,0xd9d7c6,0xe2decb);
        }
        cylinder(7,0,.8,47.5,17,0x343b3c,0x444b4c);
        box(7,0,1,12,59,.7,0x343b3c,0x444b4c);
        lamp("light.mast",7,0,64.5,"mast");
        lamp("light.bow.port",39,-14,15,"deck");
        lamp("light.bow.starboard",39,14,15,"deck");
        lamp("light.stern",-41,0,17,"deck");
        lamp("light.roof.work",26,0,47.5,"work");
    }
    private void writeAnchors(StringBuilder csv,BufferedImage image,double[] depth) {
        for(Anchor a:anchors) {
            double x=a.x*Math.cos(angle)-a.y*Math.sin(angle),y=a.x*Math.sin(angle)+a.y*Math.cos(angle);
            double sx=cx+x*scale,sy=cy+(y*DEPTH-a.z*HEIGHT)*scale,z=y*HEIGHT+a.z*DEPTH;
            int px=(int)Math.floor(sx),py=(int)Math.floor(sy);
            boolean inside=px>=0 && py>=0 && px<image.getWidth() && py<image.getHeight();
            double normalY=a.nx*Math.sin(angle)+a.ny*Math.cos(angle);
            boolean visible=inside && depth[py*image.getWidth()+px]<=z+2.0;
            if(a.kind.equals("tire")) visible=normalY>.05;
            csv.append(String.format(Locale.ROOT,"%d,%s,%s,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.7f,%.7f,%.7f,%s,%s,%s,%s%n",
                frame,a.id,a.kind,a.x,a.y,a.z,a.nx,a.ny,a.nz,a.r,
                (sx-frame%columns*CELL)/CELL,(sy-frame/columns*CELL)/CELL,a.r*scale/CELL,
                visible?"visible":"occluded",a.side,a.color,a.role));
        }
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
        headingCount=32;headingOffset=0;poseCount=32;columns=8;
        boolean proof=false;
        ArrayList<String> options=new ArrayList<String>();
        String suffix="";
        for(String arg:args) {
            if(arg.equals("--64")){headingCount=64;suffix="64-";}
            else if(arg.equals("--intermediate")){headingOffset=.5;suffix="intermediate-";}
            else if(arg.equals("--proof"))proof=true;
            else options.add(arg);
        }
        if(headingCount==64 && headingOffset!=0)throw new IllegalArgumentException("Choose --64 or --intermediate");
        poseCount=proof?4:headingCount;columns=proof?4:8;
        if(proof)suffix+="proof-";
        args=options.toArray(new String[options.size()]);
        boolean concept=args.length==1 && args[0].equals("--concept");
        if(args.length>0 && !concept)throw new IllegalArgumentException("Use no args for v4 or --concept for v5");
        BufferedImage guide=new BufferedImage(CELL*columns,CELL*((poseCount+columns-1)/columns),BufferedImage.TYPE_INT_ARGB);
        double[] depth=new double[guide.getWidth()*guide.getHeight()];Arrays.fill(depth,Double.NEGATIVE_INFINITY);
        StringBuilder anchors=new StringBuilder("frame,id,kind,x,y,z,nx,ny,nz,radius,u,v,radius_uv,visibility,side,color,role\n");
        for(int frame=0;frame<poseCount;frame++) {
            TugPoseGuide pose=new TugPoseGuide(frame,concept);pose.draw(guide,depth);
            if(concept)pose.writeAnchors(anchors,guide,depth);
        }
        // Regression: these known unobstructed winch tops must survive the deck.
        if(!concept && !proof && headingCount==32 && headingOffset==0)for(int frame:new int[]{0,4,12,16,20,24,28}) {
            double angle=frame*Math.PI/16,scale=CELL*.78/100;
            int x=(int)Math.floor((frame%columns+.5)*CELL-33*Math.cos(angle)*scale);
            int y=(int)Math.floor((frame/columns+.72)*CELL+(-33*Math.sin(angle)*DEPTH-13*HEIGHT)*scale);
            if((guide.getRGB(x,y)&0xffffff)!=0xe7b93e)
                throw new IllegalStateException("Winch top incorrectly occluded in frame "+frame);
        }
        String version=concept?"v5":"v4";
        File out=new File("docs/art-studies/tug-isometric-"+version+"-"+suffix+"pose-guide.png");
        ImageIO.write(guide,"png",out);
        if(concept)Files.write(new File("docs/art-studies/tug-isometric-v5-"+suffix+"guide-anchors.csv").toPath(),anchors.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println(out+" — "+poseCount+" poses, "+guide.getWidth()+"x"+guide.getHeight()+", fixed 35.264-degree camera, "+(360.0/headingCount)+"-degree steps, offset "+(headingOffset*360/headingCount));
    }
}
