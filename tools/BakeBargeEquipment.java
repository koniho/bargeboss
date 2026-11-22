package com.bargeboss.game;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Offline rigid deck equipment. All 64 cells are independent 5.625 degree poses. */
public final class BakeBargeEquipment {
    private static WatercraftMesh mesh;
    private static void box(double x,double y,double z,double w,double d,double h,int color){
        double[][] p={{x-w/2,y-d/2,z},{x+w/2,y-d/2,z},{x+w/2,y+d/2,z},{x-w/2,y+d/2,z},{x-w/2,y-d/2,z+h},{x+w/2,y-d/2,z+h},{x+w/2,y+d/2,z+h},{x-w/2,y+d/2,z+h}};
        for(int[] f:new int[][]{{0,1,5,4},{1,2,6,5},{2,3,7,6},{3,0,4,7},{4,5,6,7}})mesh.face(new double[][]{p[f[0]],p[f[1]],p[f[2]],p[f[3]]},color);
    }
    private static void cylinder(double x,double y,double z,double r,double h,int color){
        double[][] top=new double[12][3];for(int i=0;i<12;i++){
            double a=i*Math.PI/6,b=(i+1)*Math.PI/6;double xx=x+r*Math.cos(a),yy=y+r*Math.sin(a),nx=x+r*Math.cos(b),ny=y+r*Math.sin(b);
            mesh.face(new double[][]{{xx,yy,z},{nx,ny,z},{nx,ny,z+h},{xx,yy,z+h}},color);top[i]=new double[]{xx,yy,z+h};
        }mesh.face(top,color);
    }
    private static WatercraftMesh equipment(boolean fire){
        mesh=new WatercraftMesh();box(0,0,10.7,60,23,.8,0x5b5c55);
        if(fire){
            // Red pump rooms, pale roofs, exposed plumbing, yellow generator: Memphis reference.
            box(-15,0,11.5,25,18,13,0xac3029);box(-15,0,24.5,26,19,1,0xb8b7a5);
            for(int side:new int[]{-1,1}){box(-15,side*9.1,14,13,.4,8,0x7c2626);box(-15,side*9.4,20,9,.5,3,0x313e43);}
            box(-29,0,11.5,5,14,7,0xdab342);
            for(int side:new int[]{-1,1}){
                box(5,side*5,12,24,1.8,1.8,0xb74330);
                cylinder(18,side*7,12,2.6,7,0x9e3227);cylinder(18,side*7,19,3.2,2,0xd05e3b);
                // Moving monitor barrels are separate live geometry.
                for(int x=-28;x<=28;x+=14)box(x,side*11,12,.7,.7,7,0x9a9f97);
                box(0,side*11,19,58,.7,.7,0xc3c3ae);
            }
            box(-24,6,25.5,1.5,1.5,5,0x323d42);
        }else{
            for(int x=-20;x<=20;x+=20)for(int y=-6;y<=6;y+=12){
                box(x,y,11.5,14,9,2,0x8d4b32);
                for(int k=-1;k<=1;k++){cylinder(x+k*4,y,13.5,1.6,8,0x333c46);cylinder(x+k*4,y,21,1.7,.7,0xbba371);cylinder(x+k*4,y,21.7,1.1,.2,0x121e2b);}
            }
            box(-29,0,11.5,4,12,5,0x36575e);
        }
        return mesh;
    }
    public static void main(String[] args)throws Exception{
        new File("out/equipment").mkdirs();
        for(int kind=0;kind<2;kind++){
            String name=kind==0?"fireworks":"fire";WatercraftMesh model=equipment(kind==1);
            BufferedImage sheet=new BufferedImage(2048,2048,BufferedImage.TYPE_INT_ARGB);
            BufferedImage normal=new BufferedImage(2048,2048,BufferedImage.TYPE_INT_ARGB),material=new BufferedImage(2048,2048,BufferedImage.TYPE_INT_ARGB);
            for(int frame=0;frame<64;frame++){
                BufferedImage[] maps=BakeWatercraftMaterials.bake(model,frame*.5,77.5/.78);
                // Reference path uses the same neutral key on geometry, not a view-dependent paint warp.
                for(int y=0;y<256;y++)for(int x=0;x<256;x++){
                    int p=maps[0].getRGB(x,y),n=maps[1].getRGB(x,y);if((p>>>24)==0)continue;
                    double nx=((n>>16)&255)/127.5-1,ny=((n>>8)&255)/127.5-1,nz=1-Math.abs(nx)-Math.abs(ny);
                    double light=.72+.28*Math.max(0,nz)+.15*Math.max(0,-nx-ny);
                    int r=Math.min(255,(int)(((p>>16)&255)*light)),g=Math.min(255,(int)(((p>>8)&255)*light)),b=Math.min(255,(int)((p&255)*light));
                    maps[0].setRGB(x,y,(p&0xff000000)|(r<<16)|(g<<8)|b);
                }
                sheet.getRaster().setRect(frame%8*256,frame/8*256,maps[0].getRaster());normal.getRaster().setRect(frame%8*256,frame/8*256,maps[1].getRaster());material.getRaster().setRect(frame%8*256,frame/8*256,maps[2].getRaster());
            }
            ImageIO.write(sheet,"png",new File("assets/sprites/"+name+"-deck-v1-64.png"));
            ImageIO.write(normal,"png",new File("out/equipment/"+name+"-normal-height.png"));ImageIO.write(material,"png",new File("out/equipment/"+name+"-material.png"));
            System.out.println(name+": 64 rigid poses; 256 cell; pivot .5,.72; span 99.358974");
        }
    }
}
