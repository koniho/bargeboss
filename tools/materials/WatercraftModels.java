package com.bargeboss.game;

/** Authored material meshes. Dimensions use the same 100-unit tug datum as the pose guide. */
public final class WatercraftModels {
    private final WatercraftMesh mesh=new WatercraftMesh();
    private void face(int c,double[]... p){mesh.face(p,c);}
    private void prism(double[][] outline,double z,double top,int c){
        double[][] roof=new double[outline.length][3];
        for(int i=0;i<outline.length;i++){int j=(i+1)%outline.length;double[] a=outline[i],b=outline[j];roof[i]=new double[]{a[0],a[1],top};face(c,new double[]{a[0],a[1],z},new double[]{b[0],b[1],z},new double[]{b[0],b[1],top},new double[]{a[0],a[1],top});}face(c,roof);
    }
    private double[][] rectangle(double x,double y,double l,double w,double bevel){double[][] p={{-l/2+bevel,-w/2},{l/2-bevel,-w/2},{l/2,-w/2+bevel},{l/2,w/2-bevel},{l/2-bevel,w/2},{-l/2+bevel,w/2},{-l/2,w/2-bevel},{-l/2,-w/2+bevel}};for(double[] v:p){v[0]+=x;v[1]+=y;}return p;}
    private void box(double x,double y,double z,double l,double w,double h,double bevel,int color){prism(rectangle(x,y,l,w,bevel),z,z+h,color);}
    private void tube(double[] a,double[] b,double r,int color,int sides){
        double dx=b[0]-a[0],dy=b[1]-a[1],dz=b[2]-a[2],len=Math.sqrt(dx*dx+dy*dy+dz*dz);dx/=len;dy/=len;dz/=len;
        double ux=0,uy=dz,uz=-dy;if(Math.abs(dx)>.9){ux=-dz;uy=0;uz=dx;}double ulen=Math.sqrt(ux*ux+uy*uy+uz*uz);ux/=ulen;uy/=ulen;uz/=ulen;
        double vx=dy*uz-dz*uy,vy=dz*ux-dx*uz,vz=dx*uy-dy*ux;double[][] first=new double[sides][3],last=new double[sides][3];
        for(int i=0;i<sides;i++){double angle=i*2*Math.PI/sides,c=Math.cos(angle)*r,s=Math.sin(angle)*r;first[i]=new double[]{a[0]+ux*c+vx*s,a[1]+uy*c+vy*s,a[2]+uz*c+vz*s};last[i]=new double[]{b[0]+ux*c+vx*s,b[1]+uy*c+vy*s,b[2]+uz*c+vz*s};}
        for(int i=0;i<sides;i++)face(color,first[i],first[(i+1)%sides],last[(i+1)%sides],last[i]);face(color,last);
    }
    private void vertical(double x,double y,double z,double height,double radius,int color){tube(new double[]{x,y,z},new double[]{x,y,z+height},radius,color,20);}
    private void tire(double x,double y,double z){
        for(int i=0;i<24;i++)for(int j=0;j<12;j++){
            double[][] p=new double[4][];int[][] corners={{i,j},{i+1,j},{i+1,j+1},{i,j+1}};
            for(int k=0;k<4;k++){double a=corners[k][0]*Math.PI/12,b=corners[k][1]*Math.PI/6;p[k]=new double[]{x+(3.2+1.25*Math.cos(b))*Math.cos(a),y+1.25*Math.sin(b),z+(3.2+1.25*Math.cos(b))*Math.sin(a)};}face(0x24292b,p);
        }
        tube(new double[]{x,y,9.7},new double[]{x,y,z+2},.22,0x69594b,6);
    }
    private void light(double x,double y,double z){vertical(x,y,z-2,2,.6,0x565b53);vertical(x,y,z,1.7,1,0xffdd7d);vertical(x,y,z+1.7,.4,1.25,0x9d7339);}
    public static WatercraftMesh tug(){WatercraftModels m=new WatercraftModels();m.tugGeometry();return m.mesh;}
    private void tugGeometry(){
        double[][] hull=new double[64][2];for(int i=0;i<64;i++){double a=i*Math.PI/32;hull[i]=new double[]{Math.copySign(29,Math.cos(a))+21*Math.cos(a),20.5*Math.sin(a)};}
        prism(hull,-2,1,0x693723);prism(hull,1,8.5,0x343b3e);prism(hull,8.5,9.3,0xc27c36);
        double[][] deck=new double[64][2];for(int i=0;i<64;i++)deck[i]=new double[]{hull[i][0]*.975,hull[i][1]*.975};prism(deck,9.3,9.7,0x745040);
        for(int i=0;i<64;i++){int j=(i+1)%64;tube(new double[]{hull[i][0],hull[i][1],8.7},new double[]{hull[j][0],hull[j][1],8.7},.48,0xd6a24c,6);}
        box(0,0,9.7,53,28,13,2,0x97402d);box(0,0,22.7,53,28,7.8,2,0xc6c6b4);
        box(0,0,30.5,57,32,1.5,2.2,0x9b9d89);
        // Raised wheelhouse, with individually framed windows rather than a dark box band.
        box(12,0,32,32,25,13,3,0xcac9ba);
        for(int side:new int[]{-1,1}){
            for(int i=0;i<4;i++)box(1+i*7.2,side*12.55,36,5.6,.55,7.1,.2,0x203641);
            for(int i=0;i<3;i++)box(side<0?28.05:-4.05,-7+i*7,36,.5,5.2,7.1,.2,0x203641);
            box(-17,side*14.12,12.2,6.1,.4,10.3,.4,0x79392e);
            box(-17,side*14.4,18.6,3.8,.25,3,.4,0x263d43);
            vertical(-17,side*14.7,15.8,.65,.3,0xd1b06a);
            for(int i=0;i<3;i++){
                double x=-4+i*10;tube(new double[]{x,side*14.1,17},new double[]{x,side*14.45,17},1.9,0xbda785,20);
                tube(new double[]{x,side*14.46,17},new double[]{x,side*14.6,17},1.35,0x253a41,20);
            }
            for(int i=0;i<5;i++)tire(-34+i*17,side*21.2,4.7);
            for(int i=0;i<7;i++)vertical(-39+i*13,side*19.2,9.7,6,.4,0xb4b8aa);
            tube(new double[]{-39,side*19.2,15.7},new double[]{39,side*19.2,15.7},.4,0xc4c6b7,8);
            tube(new double[]{-39,side*19.2,12.9},new double[]{39,side*19.2,12.9},.25,0xc4c6b7,6);
            for(int i=0;i<5;i++)vertical(-23+i*12,side*15.6,32,4.7,.35,0xd1d0bd);
            tube(new double[]{-23,side*15.6,36.7},new double[]{25,side*15.6,36.7},.35,0xd1d0bd,8);
        }
        box(12,0,45,36,29,1.5,3.1,0x9b3725);box(12,0,46.5,34,27,.5,2.6,0xb94929);
        vertical(-18,0,9.7,41.5,4.3,0x303638);vertical(-18,0,40,3.2,4.34,0xbe6c2d);vertical(-18,0,50.4,1,4.65,0x343939);
        vertical(-18,0,51.4,.25,3.7,0x10191b);
        // Stern capstan, spool cheeks and drum.
        box(-37,0,9.7,11,13,2.5,1,0x9d7434);tube(new double[]{-37,-5,15},new double[]{-37,5,15},2.3,0x675749,20);
        for(int side:new int[]{-1,1})tube(new double[]{-37,side*5,15},new double[]{-37,side*5.6,15},3.2,0xc59235,20);
        vertical(7,0,47,17,.65,0x626450);tube(new double[]{7,-6,59},new double[]{7,6,59},.38,0x626450,8);
        vertical(17,5,47,13,.2,0x747a72);vertical(21,-5,47,9,.2,0x747a72);
        light(7,0,64);light(38,-14,15);light(38,14,15);light(-42,0,16);light(25,0,47.2);
        for(int side:new int[]{-1,1}){box(48,side*10,1.8,3.8,5.8,8.5,.6,0x2b3234);vertical(39,side*7,9.7,2,1.1,0x50574f);tube(new double[]{37,side*7,11.7},new double[]{41,side*7,11.7},.6,0x50574f,8);}
    }
}
