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
public final class YachtPoseGuide {
    private static final int CELL=384;
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
    private YachtPoseGuide(int frame,boolean concept) {
        this.frame=frame;this.concept=concept;
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
        yachtGeometry();
        // Per-pixel depth prevents a broad deck face incorrectly covering fittings.
        for(Face f:faces) for(int i=1;i<f.x.length-1;i++) triangle(image,depth,f,0,i,i+1);
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
    private void yachtGeometry() {
        // 80 ft flybridge motor yacht. +X is the pointed bow; broad transom is aft.
        double[][] hull={{-50,-10},{-47,-14},{-24,-14},{8,-13},{31,-9},{45,-4},{50,0},{45,4},{31,9},{8,13},{-24,14},{-47,14},{-50,10}};
        prism(hull,0,9,0xdddccf,0xf1eee2);
        double[][] stripe={{-49,-10},{-46,-14.15},{-24,-14.15},{8,-13.15},{31,-9.15},{45,-4.15},{49.5,0},{45,4.15},{31,9.15},{8,13.15},{-24,14.15},{-46,14.15},{-49,10}};
        prism(stripe,1,2.2,0x254954,0x254954);
        box(-42,0,14,20,9,1,0x9c7147,0xc69c67);
        box(-6,0,51,21,9,12,0xe8e4d9,0xf9f2dc);
        box(-4,0,47,21.5,14,5,0x203c4b,0x314958);
        box(-6,0,54,23,21,2,0xe1ddd2,0xfff5df);
        box(-5,0,29,14,23,6,0xdcd9cd,0xf6ecdb);
        box(6,0,8,14.5,25,3,0x264653,0x254454);
        box(-6,0,34,17,29,1.8,0xdddccf,0xfaf4e6);
        box(29,0,16,11,9,2,0xbca98d,0xe3cfac);
        box(-47,0,9,18,6,1,0xa17a50,0xc9a16d);
        cylinder(-12,0,.6,31,10,0xe4e2db,0xffffff);
        box(-12,0,1,12,38,.8,0xd7d9d8,0xf9f9ef);
        lamp("light.mast",-12,0,41,"mast");
        for(int side:new int[]{-1,1}) {
            for(int i=0;i<5;i++) {
                double x=-40+i*17,y=side*(x>25?8.5:13);
                cylinder(x,y,.35,9,4,0xd2d9d7,0xf4f3e8);
            }
            box(-8,side*13,65,.45,13,.45,0xc8d4d1,0xf3f2e8);
            for(int i=0;i<3;i++) {
                double x=-30+i*18,y=side*14.5;
                cylinder(x,y,1.7,2,6,0xd5dbdf,0xf7f7eb);
                anchors.add(new Anchor("fender."+(side<0?"port":"starboard")+"."+i,"fender",x,y,5,1.7,0,side,0,side<0?"port":"starboard","","contact"));
            }
        }
        lamp("light.nav.port",24,-9,13,"navigation-port");
        lamp("light.nav.starboard",24,9,13,"navigation-starboard");
        lamp("light.stern",-48,0,10,"stern");
        anchors.add(new Anchor("tow.bow","tow",46,0,8,1,1,0,0,"","","bow-cleat"));
        anchors.add(new Anchor("mooring.stern","mooring",-45,0,9,1,-1,0,0,"","","stern-cleat"));
        // Diesels are stopped during this mission: no stack and no tire fenders.
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
                (sx-frame%8*CELL)/CELL,(sy-frame/8*CELL)/CELL,a.r*scale/CELL,
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
    public static void main(String[] args)throws Exception {
        BufferedImage guide=new BufferedImage(CELL*8,CELL*4,BufferedImage.TYPE_INT_ARGB);
        double[] depth=new double[guide.getWidth()*guide.getHeight()];Arrays.fill(depth,Double.NEGATIVE_INFINITY);
        StringBuilder anchors=new StringBuilder("frame,id,kind,x,y,z,nx,ny,nz,radius,u,v,radius_uv,visibility,side,color,role\n");
        for(int frame=0;frame<32;frame++) {
            YachtPoseGuide pose=new YachtPoseGuide(frame,false);pose.draw(guide,depth);pose.writeAnchors(anchors,guide,depth);
        }
        File dir=new File("docs/art-studies/montrose");dir.mkdirs();
        ImageIO.write(guide,"png",new File(dir,"yacht-80ft-v1-guide.png"));
        Files.write(new File(dir,"yacht-80ft-v1-guide-anchors.csv").toPath(),anchors.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("Yacht guide: 32 unique model headings; waterline pivot .5,.72; length 100 / beam 28 model units");
    }
}
