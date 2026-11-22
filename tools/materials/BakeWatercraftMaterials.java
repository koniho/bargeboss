package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Reproducible unlit color / octahedral normal-height / emission-roughness export.
 * Rigid geometry and material coordinates rotate together; no painted pixels infer normals. */
public final class BakeWatercraftMaterials {
    static final int CELL=256,AA=2,SIZE=CELL*AA,HEADINGS=32;
    static final String[] FAMILIES={"tug","barge","yacht","motorboat-0","motorboat-1","motorboat-2","sailboat-3","sailboat-4","sailboat-5"};
    static double span(String name){return name.equals("barge")?77.5/.78:name.equals("tug")||name.equals("yacht")?100/.78:96;}
    static final double D=1/StrictMath.sqrt(3),H=StrictMath.sqrt(2.0/3),Q=StrictMath.sqrt(.5);
    static final double PIVOT_X=.5,PIVOT_Y=.72;
    static final double MIN_HEIGHT=-8,HEIGHT_RANGE=96;
    static final class Frame {
        final int[][] pixels={new int[SIZE*SIZE],new int[SIZE*SIZE],new int[SIZE*SIZE]};
        final double[] depth=new double[SIZE*SIZE];
        Frame(){Arrays.fill(depth,Double.NEGATIVE_INFINITY);}
    }
    static int clamp(double v){return Math.max(0,Math.min(255,(int)StrictMath.round(v)));}
    static double noise(double x,double y,double z){return StrictMath.sin(x*12.9898+y*78.233+z*37.719)*43758.5453%1;}
    /** Authored palette, not light baked onto opposing faces. */
    static int material(int rgb){
        int r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
        if(r>180 && g>140 && b<150)return 0xdca83c;
        if(r>105 && r>g*1.45 && g>b*1.05)return 0xad4227;
        if(r>170 && g>170 && b>150)return 0xccc9b6;
        if(b>r*1.1 && r<90)return 0x203641;
        if(r<85 && g<100 && b<105)return 0x292f32;
        return rgb;
    }
    static void triangle(Frame f,double[][] v,int color,double heading,double span){
        double angle=heading*StrictMath.PI/16,ca=StrictMath.cos(angle),sa=StrictMath.sin(angle),scale=SIZE/span;
        double[] sx=new double[3],sy=new double[3],depth=new double[3];
        for(int i=0;i<3;i++){
            double rx=v[i][0]*ca-v[i][1]*sa,ry=v[i][0]*sa+v[i][1]*ca;
            sx[i]=SIZE*PIVOT_X+rx*scale;sy[i]=SIZE*PIVOT_Y+(ry*D-v[i][2]*H)*scale;depth[i]=ry*H+v[i][2]*D;
        }
        double ux=v[1][0]-v[0][0],uy=v[1][1]-v[0][1],uz=v[1][2]-v[0][2];
        double vx=v[2][0]-v[0][0],vy=v[2][1]-v[0][1],vz=v[2][2]-v[0][2];
        double nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx,n=StrictMath.sqrt(nx*nx+ny*ny+nz*nz);
        if(n<1e-8)return;nx/=n;ny/=n;nz/=n;
        double rx=nx*ca-ny*sa,ry=nx*sa+ny*ca;
        if(ry*H+nz*D<0){rx=-rx;ry=-ry;nz=-nz;}
        // Authored sprite heading is world heading -45 degrees. Export world normals.
        nx=(rx-ry)*Q;ny=(rx+ry)*Q;
        double l=StrictMath.abs(nx)+StrictMath.abs(ny)+StrictMath.abs(nz),ox=nx/l,oy=ny/l;
        if(nz<0){double old=ox;ox=(1-StrictMath.abs(oy))*StrictMath.copySign(1,old);oy=(1-StrictMath.abs(old))*StrictMath.copySign(1,oy);}
        int normal=0xff000000|(clamp((ox+1)*127.5)<<16)|(clamp((oy+1)*127.5)<<8);
        int minX=Math.max(0,(int)StrictMath.floor(Math.min(sx[0],Math.min(sx[1],sx[2])))),maxX=Math.min(SIZE-1,(int)StrictMath.ceil(Math.max(sx[0],Math.max(sx[1],sx[2]))));
        int minY=Math.max(0,(int)StrictMath.floor(Math.min(sy[0],Math.min(sy[1],sy[2])))),maxY=Math.min(SIZE-1,(int)StrictMath.ceil(Math.max(sy[0],Math.max(sy[1],sy[2]))));
        double den=(sy[1]-sy[2])*(sx[0]-sx[2])+(sx[2]-sx[1])*(sy[0]-sy[2]);if(StrictMath.abs(den)<1e-9)return;
        boolean emission=color==0xffdd7d;int albedo=material(color);
        double roughness=emission?.25:albedo==0x203641?.22:albedo==0x292f32?.85:.67;
        for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++){
            double a=((sy[1]-sy[2])*(x+.5-sx[2])+(sx[2]-sx[1])*(y+.5-sy[2]))/den;
            double b=((sy[2]-sy[0])*(x+.5-sx[2])+(sx[0]-sx[2])*(y+.5-sy[2]))/den,c=1-a-b;if(a<0||b<0||c<0)continue;
            int at=y*SIZE+x;double z=a*depth[0]+b*depth[1]+c*depth[2];if(z<=f.depth[at])continue;f.depth[at]=z;
            double px=a*v[0][0]+b*v[1][0]+c*v[2][0],py=a*v[0][1]+b*v[1][1]+c*v[2][1],pz=a*v[0][2]+b*v[1][2]+c*v[2][2];
            // Local-space paint wear stays attached through every heading. No directional lighting.
            double grain=noise(StrictMath.floor(px*3),StrictMath.floor(py*3),StrictMath.floor(pz*3));
            double coarse=noise(StrictMath.floor(px*.65),StrictMath.floor(py*.65),StrictMath.floor(pz*.65));
            double variation=emission?1:.84+.13*grain+.10*coarse;
            int r=clamp(((albedo>>16)&255)*variation),g=clamp(((albedo>>8)&255)*variation),bb=clamp((albedo&255)*variation);
            f.pixels[0][at]=0xff000000|(r<<16)|(g<<8)|bb;
            f.pixels[1][at]=normal|clamp((pz-MIN_HEIGHT)*255/HEIGHT_RANGE);
            f.pixels[2][at]=0xff000000|(clamp(roughness*255)<<16)|(emission?0xff00:0);
        }
    }
    static BufferedImage[] bake(WatercraftMesh mesh,double heading,double span){
        Frame f=new Frame();for(WatercraftMesh.Face face:mesh.faces)for(int i=1;i<face.xyz.length-1;i++)triangle(f,new double[][]{face.xyz[0],face.xyz[i],face.xyz[i+1]},face.color,heading,span);
        BufferedImage[] out={new BufferedImage(CELL,CELL,BufferedImage.TYPE_INT_ARGB),new BufferedImage(CELL,CELL,BufferedImage.TYPE_INT_ARGB),new BufferedImage(CELL,CELL,BufferedImage.TYPE_INT_ARGB)};
        for(int y=0;y<CELL;y++)for(int x=0;x<CELL;x++)for(int map=0;map<3;map++){
            int count=0,r=0,g=0,b=0;for(int j=0;j<AA;j++)for(int i=0;i<AA;i++){int p=f.pixels[map][(y*AA+j)*SIZE+x*AA+i];if((p>>>24)==0)continue;count++;r+=(p>>16)&255;g+=(p>>8)&255;b+=p&255;}
            if(count>0)out[map].setRGB(x,y,(clamp(count*255.0/(AA*AA))<<24)|((r/count)<<16)|((g/count)<<8)|(b/count));
        }return out;
    }
    public static void main(String[] args)throws Exception{
        File dir=new File(args.length==0?"build/materials/export":args[0]);if(!dir.isDirectory()&&!dir.mkdirs())throw new java.io.IOException("Cannot create "+dir);
        for(String name:FAMILIES){
            WatercraftMesh mesh=new WatercraftMesh();if(name.equals("tug"))mesh=WatercraftModels.tug();else if(name.equals("barge"))BargePoseGuide.materialModel(mesh::face);else if(name.equals("yacht"))YachtPoseGuide.materialModel(mesh::face);else BakeMooredBoats.materialModel(name.charAt(name.length()-1)-'0',mesh::face);
            double span=span(name);
            BufferedImage[] sheets=new BufferedImage[3];for(int i=0;i<3;i++)sheets[i]=new BufferedImage(CELL*8,CELL*4,BufferedImage.TYPE_INT_ARGB);
            File frames=new File(dir,name+"-frames");if(!frames.isDirectory()&&!frames.mkdirs())throw new java.io.IOException("Cannot create "+frames);
            for(int frame=0;frame<HEADINGS;frame++){
                BufferedImage[] cell=bake(mesh,frame,span);
                BufferedImage packed=new BufferedImage(CELL,CELL*3,BufferedImage.TYPE_INT_ARGB);
                for(int i=0;i<3;i++){
                    sheets[i].getRaster().setRect(frame%8*CELL,frame/8*CELL,cell[i].getRaster());
                    packed.getRaster().setRect(0,i*CELL,cell[i].getRaster());
                }
                ImageIO.write(packed,"png",new File(frames,String.format(java.util.Locale.ROOT,"%02d.png",frame)));
            }
            java.util.Properties metadata=new java.util.Properties();
            metadata.setProperty("status","prototype-not-production");
            metadata.setProperty("format","watercraft-material-v1");
            metadata.setProperty("headings","32");metadata.setProperty("cell","256");
            metadata.setProperty("pivot","0.5,0.72");metadata.setProperty("worldSpan",Double.toString(span));
            metadata.setProperty("heightMin",Double.toString(MIN_HEIGHT));metadata.setProperty("heightRange",Double.toString(HEIGHT_RANGE));
            metadata.setProperty("bands","srgb-albedo,linear-oct-rg-height-b,linear-roughness-r-emission-g-reserved-b");
            // Stable serialization (Properties.store adds a wall-clock timestamp).
            try(java.io.PrintWriter writer=new java.io.PrintWriter(new File(dir,name+".properties"),"UTF-8")){
                for(String key:new java.util.TreeSet<String>(metadata.stringPropertyNames()))writer.println(key+"="+metadata.getProperty(key));
            }
            String[] maps={"base","normal-height","material"};for(int i=0;i<3;i++)ImageIO.write(sheets[i],"png",new File(dir,name+"-"+maps[i]+".png"));
            System.out.println(name+": "+mesh.faces.size()+" rigid faces, 32 registered material headings, cell span "+span);
        }
    }
}
