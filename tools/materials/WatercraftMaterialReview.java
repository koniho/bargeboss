package com.bargeboss.game;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Offline material shader reference and all-heading visual proof, never runtime image processing. */
public final class WatercraftMaterialReview {
    static float channel(int c,int shift){return ((c>>shift)&255)/255f;}
    static BufferedImage light(BufferedImage base,BufferedImage normal,BufferedImage material,int heading,float lx,float ly,float lz,float span){
        int cell=base.getWidth()/8;BufferedImage out=new BufferedImage(cell,cell,BufferedImage.TYPE_INT_ARGB);
        int ox=heading%8*cell,oy=heading/8*cell;
        for(int y=0;y<cell;y++)for(int x=0;x<cell;x++){
            int albedo=base.getRGB(ox+x,oy+y);float a=(albedo>>>24)/255f;if(a==0)continue;
            int n=normal.getRGB(ox+x,oy+y),m=material.getRGB(ox+x,oy+y);
            float nx=channel(n,16)*2-1,ny=channel(n,8)*2-1,nz=1-Math.abs(nx)-Math.abs(ny);
            if(nz<0){float old=nx;nx=(1-Math.abs(ny))*Math.signum(old);ny=(1-Math.abs(old))*Math.signum(ny);}
            float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);nx/=len;ny/=len;nz/=len;
            float z=channel(n,0)*96-8,u=((x+.5f)/cell-.5f)*span,v=(((y+.5f)/cell-.72f)*span+.81649658f*z)/.57735026f;
            float px=(u-v)*.70710678f,py=(u+v)*.70710678f;
            float dx=lx-px,dy=ly-py,dz=lz-z,d=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);dx/=d;dy/=d;dz/=d;
            float diffuse=Math.max(0,nx*dx+ny*dy+nz*dz)*1.2f;
            float hx=dx-.57735026f,hy=dy+.57735026f,hz=dz+.57735026f,hl=(float)Math.sqrt(hx*hx+hy*hy+hz*hz);
            float rough=channel(m,16),spec=.06f*(float)Math.pow(Math.max(0,(nx*hx+ny*hy+nz*hz)/hl),4+60*(1-rough));
            float emission=channel(m,8),ambient=.045f+.06f*Math.max(0,nz);
            int color=0;float[] tint={1,.65f,.33f},sky={.63f,.8f,1};
            for(int ch=0;ch<3;ch++){int shift=16-ch*8;float b=channel(albedo,shift);float value=(float)Math.pow(b,2.2)*(ambient*sky[ch]+diffuse*tint[ch]+emission*1.8f)+spec*tint[ch];color|=Math.min(255,Math.round(255*(float)Math.pow(Math.max(0,value),1/2.2)))<<shift;}
            out.setRGB(x,y,((int)(a*255)<<24)|color);
        }return out;
    }
    public static void main(String[] args)throws Exception{
        File dir=new File(args.length==0?"build/materials/export":args[0]);
        BufferedImage proof=new BufferedImage(1024,3*300,BufferedImage.TYPE_INT_RGB);Graphics2D p=proof.createGraphics();p.setColor(new Color(0x071e29));p.fillRect(0,0,proof.getWidth(),proof.getHeight());
        int row=0;
        for(String name:new String[]{"tug","barge","yacht"}){
            BufferedImage base=ImageIO.read(new File(dir,name+"-base.png")),normal=ImageIO.read(new File(dir,name+"-normal-height.png")),material=ImageIO.read(new File(dir,name+"-material.png"));
            for(int col=0;col<4;col++)p.drawImage(light(base,normal,material,col*8+4,col%2==0?-90:90,-55,70,name.equals("barge")?77.5f/.78f:100/.78f),col*256,row*300+24,null);
            p.setColor(new Color(0x9bb7c3));p.setFont(new Font("SansSerif",Font.PLAIN,15));p.drawString(name.toUpperCase()+" / GEOMETRY MATERIAL PROTOTYPE",14,row*300+20);row++;
        }p.dispose();ImageIO.write(proof,"png",new File(dir,"material-light-proof.png"));
    }
}
