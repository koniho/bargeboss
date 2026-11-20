package com.bargeboss.game;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;
/** Offline rigid-model sprite bake. Geometry rotates before the shared isometric projection. */
public final class BakeMooredBoats {
    static final int CELL=128;
    static class Face {float[] xyz;int color;float depth;Face(float[] p,int c){xyz=p;color=c;}}
    static ArrayList<Face> faces;static float angle;
    private static boolean materialExport;
    /** Export rigid geometry; flatten the legacy top/side palette into unlit materials. */
    public static void materialModel(int kind,java.util.function.BiConsumer<double[][],Integer> sink){
        ArrayList<Face> oldFaces=faces;float oldAngle=angle;boolean oldExport=materialExport;
        try{
            angle=0;faces=new ArrayList<Face>();materialExport=true;model(kind);
            for(Face face:faces){double[][] vertices=new double[face.xyz.length/3][3];
                for(int i=0;i<vertices.length;i++)for(int j=0;j<3;j++)vertices[i][j]=face.xyz[i*3+j];
                sink.accept(vertices,face.color&0xffffff);
            }
        }finally{faces=oldFaces;angle=oldAngle;materialExport=oldExport;}
    }
    static void face(int color,float... p){Face f=new Face(p,color);for(int i=0;i<p.length;i+=3){float x=p[i],y=p[i+1];p[i]=x*(float)Math.cos(angle)-y*(float)Math.sin(angle);p[i+1]=x*(float)Math.sin(angle)+y*(float)Math.cos(angle);f.depth+=p[i+1]-p[i]+p[i+2]*.9f;}f.depth/=p.length/3;faces.add(f);}
    static void box(float x,float y,float z,float l,float w,float h,int top,int side){
        if(materialExport)side=top;
        face(side,x,y,z,x+l,y,z,x+l,y,z+h,x,y,z+h);face(side,x+l,y,z,x+l,y+w,z,x+l,y+w,z+h,x+l,y,z+h);
        face(side,x+l,y+w,z,x,y+w,z,x,y+w,z+h,x+l,y+w,z+h);face(side,x,y+w,z,x,y,z,x,y,z+h,x,y+w,z+h);
        face(top,x,y,z+h,x+l,y,z+h,x+l,y+w,z+h,x,y+w,z+h);
    }
    static void model(int kind){float l=MooredBoats.length(kind),w=MooredBoats.width(kind);int[] hulls={0xff424d53,0xff333e49,0xff4b4f4c,0xff384750,0xff4a4c49,0xff3c404e};int base=hulls[kind];
        float[] xs={-.5f,-.38f,.21f,.43f,.52f,.43f,.21f,-.38f},ys={-.30f,-.48f,-.47f,-.25f,0,.25f,.47f,.48f};
        float[] deck=new float[24];for(int i=0;i<8;i++){deck[i*3]=xs[i]*l;deck[i*3+1]=ys[i]*w;deck[i*3+2]=3.5f;
            int j=(i+1)%8;face(base,xs[i]*l,ys[i]*w,3.5f,xs[j]*l,ys[j]*w,3.5f,xs[j]*l*.94f,ys[j]*w*.83f,0,xs[i]*l*.94f,ys[i]*w*.83f,0);}
        face(0xff58605e,deck);
        box(-l*.34f,-w*.31f,3.5f,l*.35f,w*.62f,.7f,0xff252f34,0xff222c32);
        if(kind<3){box(-l*.06f,-w*.33f,3.5f,l*(kind==2?.38f:.27f),w*.66f,5.5f,0xff626764,0xff34434c);
            box(-l*.03f,-w*.34f,6,l*.22f,w*.68f,1.6f,0xff293b43,0xff1b303b);
            box(-l*.08f,-w*.37f,9,l*.33f,w*.74f,.7f,0xff5a6263,0xff3a454b);
            if(kind==1)box(-l*.29f,-w*.35f,5,l*.22f,w*.7f,3,0xff283d46,0xff24343c);
            if(kind==2)box(l*.03f,-w*.22f,10,l*.19f,w*.44f,2,0xff535c5b,0xff303f48);
        }else{box(-l*.12f,-w*.31f,3.5f,l*.35f,w*.62f,3.2f,0xff5a625f,0xff34414a);
            box(l*.08f,-.4f,5,.7f,.8f,kind==5?45:39,0xff596065,0xff48545d);
            box(-l*.28f,-.9f,10,l*.39f,1.8f,1.7f,0xff485461,0xff303d49);
            // Furled sails and fine standing rigging, not bright raised sails.
            face(0xff46515b,l*.09f,0,43,-l*.43f,0,4,-l*.43f+.45f,0,4,l*.09f+.35f,0,43);
            face(0xff46515b,l*.09f,0,43,l*.48f,0,4,l*.48f+.45f,0,4,l*.09f+.35f,0,43);
        }
        for(int side=-1;side<=1;side+=2)for(int i=0;i<3;i++)box(-l*.26f+i*l*.23f,side*w*.45f-.65f,1.5f,1.8f,1.3f,2.8f,0xff4b555a,0xff2b373e);
        for(int end=-1;end<=1;end+=2)box(end*l*.38f-.9f,-.6f,3.7f,1.8f,1.2f,.5f,0xff68706e,0xff39464c);
    }
    /** Affine height on each rigid face, packed beside an octahedral world normal. */
    static final class SurfacePaint implements Paint {
        final float nx,ny,nz,d;final int rg;
        SurfacePaint(float x,float y,float z,float px,float py,float pz){nx=x;ny=y;nz=z;d=x*px+y*py+z*pz;float l=Math.abs(x)+Math.abs(y)+Math.abs(z);x/=l;y/=l;
            if(z<0){float old=x;x=(1-Math.abs(y))*(x>=0?1:-1);y=(1-Math.abs(old))*(y>=0?1:-1);}rg=0xff000000|(Math.round((x+1)*127.5f)<<16)|(Math.round((y+1)*127.5f)<<8);}
        public int getTransparency(){return Transparency.OPAQUE;}
        public PaintContext createContext(java.awt.image.ColorModel cm,Rectangle bounds,Rectangle2D ub,AffineTransform tx,RenderingHints hints){
            final AffineTransform inverse;try{inverse=tx.createInverse();}catch(Exception e){throw new IllegalArgumentException(e);}
            return new PaintContext(){public void dispose(){}public java.awt.image.ColorModel getColorModel(){return java.awt.image.ColorModel.getRGBdefault();}
                public java.awt.image.Raster getRaster(int x,int y,int w,int h){java.awt.image.WritableRaster raster=getColorModel().createCompatibleWritableRaster(w,h);int[] pixels=new int[w*h];double[] xy=new double[2];
                    for(int j=0;j<h;j++)for(int i=0;i<w;i++){xy[0]=x+i+.5;xy[1]=y+j+.5;inverse.transform(xy,0,xy,0,1);
                        double sx=(xy[0]/CELL-.5)*96,sy=(xy[1]/CELL-.7)*96;
                        double a=(nx+ny)*sx/(2*RiverCamera.Q)+(ny-nx)*sy/(2*RiverCamera.Q*RiverCamera.DEPTH);
                        double den=nz+(ny-nx)*ArticulatedModel.Z_SCALE/(2*RiverCamera.Q*RiverCamera.DEPTH);
                        double z=Math.abs(den)<.00001?0:(d-a)/den;pixels[j*w+i]=rg|Math.max(0,Math.min(255,(int)Math.round(z*255/64)));}
                    raster.setDataElements(0,0,w,h,pixels);return raster;}};
        }
    }
    public static void main(String[] args)throws Exception{BufferedImage sheet=new BufferedImage(CELL*MooredBoats.COLUMNS,CELL*60,BufferedImage.TYPE_INT_ARGB);Graphics2D g=sheet.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        for(int layer=0;layer<5;layer++)for(int kind=0;kind<6;kind++)for(int view=0;view<MooredBoats.ORIENTATIONS;view++){angle=view*(float)(Math.PI*2/MooredBoats.ORIENTATIONS);faces=new ArrayList<Face>();model(kind);faces.sort((a,b)->Float.compare(a.depth,b.depth));int frame=layer*MooredBoats.BASE_FRAMES+kind*MooredBoats.ORIENTATIONS+view,ox=frame%MooredBoats.COLUMNS*CELL,oy=frame/MooredBoats.COLUMNS*CELL;g.translate(ox,oy);
            for(Face f:faces){Path2D path=new Path2D.Float();for(int i=0;i<f.xyz.length;i+=3){float x=f.xyz[i],y=f.xyz[i+1],z=f.xyz[i+2];double sx=CELL*.5+RiverCamera.Q*(x+y)*CELL/MooredBoats.CELL_WORLD,sy=CELL*.7+(RiverCamera.Q*RiverCamera.DEPTH*(y-x)-.81649658*z)*CELL/MooredBoats.CELL_WORLD;if(i==0)path.moveTo(sx,sy);else path.lineTo(sx,sy);}path.closePath();int rgb=f.color;int value=Math.min(255,(((rgb>>16)&255)+((rgb>>8)&255)+(rgb&255)));if(layer==4){float[] q=f.xyz;float ux=q[3]-q[0],uy=q[4]-q[1],uz=q[5]-q[2],vx=q[6]-q[0],vy=q[7]-q[1],vz=q[8]-q[2];float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,n=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);g.setPaint(new SurfacePaint(nx/n,ny/n,nz/n,q[0],q[1],q[2]));}else g.setColor(layer==0?new Color(((rgb>>16)&255)/2,((rgb>>8)&255)/2,(rgb&255)/2,255):new Color(layer==1?value:0,layer==2?value:0,layer==3?value:0,255));g.fill(path);}g.translate(-ox,-oy);
        }g.dispose();ImageIO.write(sheet.getSubimage(0,0,CELL*16,CELL*48),"png",new File("assets/sprites/montrose-moored-boats-v1.png"));ImageIO.write(sheet.getSubimage(0,CELL*48,CELL*16,CELL*12),"png",new File("assets/sprites/montrose-moored-normals-v1.png"));System.out.println("Baked 192 model headings with dark base and RGB receiving layers");}
}
