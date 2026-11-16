package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;

/** Fixed-datum empty cargo barge blockout; generates guides and metadata only. */
public final class BargePoseGuide {
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
    private final boolean bulk;
    private int cargoKind=2;
    private final double angle,cx,cy,scale;
    private static final class Anchor {
        String id,kind,side,color,role; double x,y,z,r,nx,ny,nz;
        Anchor(String id,String kind,double x,double y,double z,double r,double nx,double ny,double nz,String side,String color,String role) {
            this.id=id;this.kind=kind;this.x=x;this.y=y;this.z=z;this.r=r;
            this.nx=nx;this.ny=ny;this.nz=nz;this.side=side;this.color=color;this.role=role;
        }
    }
    private BargePoseGuide(int frame,boolean bulk) {
        this.frame=frame;this.bulk=bulk;
        angle=(frame+headingOffset)*StrictMath.PI*2/headingCount; cx=(frame%columns+.5)*CELL;
        cy=(frame/columns+.72)*CELL; scale=CELL*.78/77.5;
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
        if(bulk) {
            // Depth-only hull pass: cargo cannot paint through the near coaming.
            // Clear its color attachment while preserving the rendered hull depth.
            BargePoseGuide hull=new BargePoseGuide(frame,false);hull.geometry();
            for(Face f:hull.faces) for(int i=1;i<f.x.length-1;i++) triangle(image,depth,f,0,i,i+1);
            for(int y=frame/columns*CELL;y<(frame/columns+1)*CELL;y++)
                for(int x=frame%columns*CELL;x<(frame%columns+1)*CELL;x++) image.setRGB(x,y,0);
            if(cargoKind==0)coilGeometry();else if(cargoKind==1)containerGeometry();else bulkGeometry();
        } else geometry();
        for(Face f:faces) for(int i=1;i<f.x.length-1;i++) triangle(image,depth,f,0,i,i+1);
    }
    private void ring(double outerX,double outerY,double innerX,double innerY,double z,double high,int side,int top) {
        box(0,-(outerY+innerY)/4,outerX,(outerY-innerY)/2,z,high-z,side,top);
        box(0,(outerY+innerY)/4,outerX,(outerY-innerY)/2,z,high-z,side,top);
        box(-(outerX+innerX)/4,0,(outerX-innerX)/2,innerY,z,high-z,side,top);
        box((outerX+innerX)/4,0,(outerX-innerX)/2,innerY,z,high-z,side,top);
    }
    private void cleat(String id,double x,double y) {
        cylinder(x,y,1.1,5.8,1.7,0x313941,0x555b5b);
        box(x,y,4.1,1.3,7.5,.8,0x343a3d,0x737774);
        anchors.add(new Anchor(id,"mooring",x,y,7.5,1.7,0,0,1,y<0?"port":"starboard","","mooring-line"));
    }
    private void tire(String id,double x,double y,double nx,double ny,String side) {
        double z=3.7,r=2.6,t=.7;
        for(int i=0;i<16;i++) for(int j=0;j<8;j++) {
            double[][] quad=new double[4][3];int[][] ends={{i,j},{i+1,j},{i+1,j+1},{i,j+1}};
            for(int k=0;k<4;k++) {
                double a=ends[k][0]*Math.PI/8,b=ends[k][1]*Math.PI/4;
                double horizontal=(r+t*Math.cos(b))*Math.cos(a),normal=t*Math.sin(b);
                quad[k]=new double[]{x+horizontal*ny+normal*nx,y-horizontal*nx+normal*ny,z+(r+t*Math.cos(b))*Math.sin(a)};
            }
            face(quad,0x171f25);
        }
        anchors.add(new Anchor(id,"tire",x,y,z,r+t,nx,ny,0,side,"","fender"));
        anchors.add(new Anchor("contact."+id,"contact",x+nx*t,y+ny*t,z,t,nx,ny,0,side,"","fender-contact"));
    }
    private void geometry() {
        double[][] hull={{-38.75,-13.5},{-34.75,-17.5},{34.75,-17.5},{38.75,-13.5},
                {38.75,13.5},{34.75,17.5},{-34.75,17.5},{-38.75,13.5}};
        // The top of this low hull is the hold floor: no hidden solid deck over the well.
        prism(hull,0,3,0x242d34,0x4b4740);
        ring(69.5,35,62,25,3,5.8,0x5e3930,0x896048);
        box(-36.1,0,5.3,27,3,2.8,0x5e3930,0x896048);
        box(36.1,0,5.3,27,3,2.8,0x5e3930,0x896048);
        ring(66,28,62,24,5.8,10.5,0x8a412d,0xba6a3e);
        ring(66.6,28.6,61.4,23.4,10.5,11,0xc88a37,0xd7a452);
        // Sparse plate seams in the recessed empty floor remain attached through the turn.
        for(int i=-2;i<=2;i++) box(i*10,0,.28,23.5,3.01,.04,0x302f2c,0x302f2c);
        box(0,0,61.5,.25,3.01,.04,0x302f2c,0x302f2c);
        // Bow-only yellow chevron; +X is unambiguous in all four cardinal views.
        face(new double[][]{{34,-4,5.9},{37,0,5.9},{34,4,5.9},{34.7,0,5.9}},0xefb844);
        for(int side:new int[]{-1,1}) {
            cleat("mooring."+(side<0?"port":"starboard")+".stern",-34,side*11.7);
            cleat("mooring."+(side<0?"port":"starboard")+".bow",34,side*11.7);
            for(int i=0;i<4;i++) tire("tire."+(side<0?"port":"starboard")+"."+i,-27+i*18,side*18,0,side,side<0?"port":"starboard");
            tire("tire.stern."+(side<0?"port":"starboard"),-39,side*9,-1,0,"stern");
            box(38.6,side*8,1.0,4,1.0,4.5,0x1e272d,0x46535b);
            anchors.add(new Anchor("contact.bow."+(side<0?"port":"starboard"),"contact",39.1,side*8,3.25,2,1,0,0,"bow","","push-pad"));
        }
        cylinder(-35.5,0,1.5,5.8,1.5,0x363b3e,0x797973);
        box(-35.5,0,1.6,5.0,7.3,.7,0x343a3d,0x737774);
        anchors.add(new Anchor("tow.stern","tow",-35.5,0,7.3,1.5,0,0,1,"stern","","tow-line"));
        anchors.add(new Anchor("cargo.origin","cargo-origin",0,0,3.05,1,0,0,1,"","","empty-hold-floor"));
    }
    private void containerGeometry(){
        for(int bay=-1;bay<=1;bay++){
            double x=bay*20;
            box(x,0,18.5,21,3.1,13,0x874326,0xc18b4b);
            for(int side:new int[]{-1,1})for(int i=-3;i<=3;i++)box(x+i*2.4,side*10.55,.35,.3,3.4,12.3,0x6c3825,0xb17c3f);
        }
        anchors.add(new Anchor("cargo.origin","cargo-origin",0,0,3.05,1,0,0,1,"","","hold-floor-registration"));
    }
    private void coilGeometry(){
        for(int bay=-1;bay<=1;bay++)for(int row:new int[]{-1,1}){
            double x=bay*20,y=row*5.2,z=11,outer=7.5,inner=2.7,half=4.5;
            box(x,y,16,9.7,3.05,.7,0x584231,0x79614a);
            for(int i=0;i<32;i++){
                double a=i*Math.PI/16,b=(i+1)*Math.PI/16;
                double[][] out={{x+outer*Math.cos(a),y-half,z+outer*Math.sin(a)},{x+outer*Math.cos(b),y-half,z+outer*Math.sin(b)},{x+outer*Math.cos(b),y+half,z+outer*Math.sin(b)},{x+outer*Math.cos(a),y+half,z+outer*Math.sin(a)}};
                face(out,0x899392);
                for(int side:new int[]{-1,1})face(new double[][]{{x+outer*Math.cos(a),y+side*half,z+outer*Math.sin(a)},{x+outer*Math.cos(b),y+side*half,z+outer*Math.sin(b)},{x+inner*Math.cos(b),y+side*half,z+inner*Math.sin(b)},{x+inner*Math.cos(a),y+side*half,z+inner*Math.sin(a)}},side<0?0xb3bab5:0x727e80);
                face(new double[][]{{x+inner*Math.cos(a),y-half,z+inner*Math.sin(a)},{x+inner*Math.cos(b),y-half,z+inner*Math.sin(b)},{x+inner*Math.cos(b),y+half,z+inner*Math.sin(b)},{x+inner*Math.cos(a),y+half,z+inner*Math.sin(a)}},0x343e42);
            }
        }
        anchors.add(new Anchor("cargo.origin","cargo-origin",0,0,3.05,1,0,0,1,"","","hold-floor-registration"));
    }
    private void bulkGeometry() {
        // One fixed asymmetric coal mesh, in the barge hold's local coordinates.
        int count=24;double[][][] rings=new double[3][count][3];
        for(int ring=0;ring<3;ring++) for(int i=0;i<count;i++) {
            double a=i*Math.PI*2/count,rough=1+.055*Math.sin(i*2.7)+.035*Math.cos(i*4.3);
            double radius=ring==0?1:ring==1?.77:.39;
            double z=ring==0?3:ring==1?(cargoKind==3?12:7.5)+1.1*Math.sin(i*1.9):(cargoKind==3?17:11.1)+.6*Math.cos(i*2.3);
            rings[ring][i]=new double[]{(cargoKind==3?29:24)*Math.cos(a)*radius*rough-ring*1.15,
                    11*Math.sin(a)*radius*rough+ring*.5,z};
        }
        int[] shades={0x252b30,0x31383e,0x3d454a,0x232a2e,0x485056,0x30373d};
        for(int ring=0;ring<2;ring++) for(int i=0;i<count;i++) {
            int j=(i+1)%count;
            face(new double[][]{rings[ring][i],rings[ring][j],rings[ring+1][i]},shades[(i+ring*2)%shades.length]);
            face(new double[][]{rings[ring][j],rings[ring+1][j],rings[ring+1][i]},shades[(i*3+ring+1)%shades.length]);
        }
        double[] peak={-4,1.5,cargoKind==3?20:13};
        for(int i=0;i<count;i++) face(new double[][]{rings[2][i],rings[2][(i+1)%count],peak},shades[(i+2)%shades.length]);
        anchors.add(new Anchor("cargo.origin","cargo-origin",0,0,3.05,1,0,0,1,"","","hold-floor-registration"));
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
            if(a.kind.equals("contact")) visible=normalY>.05 && visible;
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
        int kind=args.length>0 && args[0].equals("--steel")?0:args.length>0 && args[0].equals("--containers")?1:args.length>0 && args[0].equals("--coal-v2")?3:2;
        boolean bulk=kind!=2 || args.length==1 && args[0].equals("--bulk");
        if(args.length>0 && !bulk) throw new IllegalArgumentException("No args for empty barge; --bulk for separate coal pile");
        BufferedImage guide=new BufferedImage(CELL*columns,CELL*((poseCount+columns-1)/columns),BufferedImage.TYPE_INT_ARGB);
        double[] depth=new double[guide.getWidth()*guide.getHeight()];Arrays.fill(depth,Double.NEGATIVE_INFINITY);
        StringBuilder anchors=new StringBuilder("frame,id,kind,x,y,z,nx,ny,nz,radius,u,v,radius_uv,visibility,side,color,role\n");
        StringBuilder registration=new StringBuilder("frame,pivot_x,pivot_y,uniform_scale,raw_iou,registered_iou,shift_x,shift_y\n");
        for(int frame=0;frame<poseCount;frame++) {
            BargePoseGuide pose=new BargePoseGuide(frame,bulk);pose.cargoKind=kind;pose.draw(guide,depth);pose.writeAnchors(anchors,guide,depth);
            registration.append(frame).append(",0.5,0.72,1,1,1,0,0\n");
        }
        // The empty well must remain visible; a solid deck must not cover its origin.
        for(int frame=0;frame<poseCount;frame++) {
            int px=(int)((frame%columns+.5)*CELL),py=(int)((frame/columns+.72)*CELL-3.05*HEIGHT*CELL*.78/77.5);
            int rgb=guide.getRGB(px,py)&0xffffff;
            if(!bulk && rgb!=0x302f2c && rgb!=0x4b4740) throw new IllegalStateException("Hold floor occluded at frame "+frame);
            int left=frame%columns*CELL,top=frame/columns*CELL;
            for(int i=0;i<CELL;i++) if((guide.getRGB(left+i,top)>>>24)!=0 || (guide.getRGB(left+i,top+CELL-1)>>>24)!=0
                    || (guide.getRGB(left,top+i)>>>24)!=0 || (guide.getRGB(left+CELL-1,top+i)>>>24)!=0)
                throw new IllegalStateException("Guide crosses source-cell gutter: "+frame);
        }
        String prefix="docs/art-studies/"+(kind==0?"cargo-steel":kind==1?"cargo-container":kind==3?"cargo-coal":bulk?"bulk":"barge")+(kind==3?"-isometric-v2-":"-isometric-v1-");
        prefix+=suffix;
        ImageIO.write(guide,"png",new File(prefix+"pose-guide.png"));
        Files.write(new File(prefix+"guide-anchors.csv").toPath(),anchors.toString().getBytes(StandardCharsets.UTF_8));
        Files.write(new File(prefix+"guide-registration.csv").toPath(),registration.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println(prefix+"pose-guide.png — "+poseCount+" poses, "+guide.getWidth()+"x"+guide.getHeight()+", fixed 35.264-degree camera, "+(360.0/headingCount)+"-degree steps, offset "+(headingOffset*360/headingCount));
        System.out.println(poseCount+" source-gutter checks passed; "+(bulk?"1 cargo origin per pose":"open holds checked; 28 named anchors per pose")+"; no lights or smoke outlets.");
    }
}
