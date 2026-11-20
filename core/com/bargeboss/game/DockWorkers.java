package com.bargeboss.game;

import java.util.ArrayList;
import java.util.Arrays;

/** Small world-space quay crew. Feet remain on verified dry, raised ground in every pose. */
public final class DockWorkers {
    private static final Face[] FACES=mesh();
    private DockWorkers() {}
    static boolean dry(float x,float y,float radius) {
        for(float[] r:CalumetMap.water())if(x+radius>=r[0] && x-radius<=r[2] && y+radius>=r[1] && y-radius<=r[3])return false;
        return true;
    }
    /** Search real terrain, not the legacy rectangular river or a screen-space offset. */
    static void shoreAnchor(Game g,int berth,float offset,float radius,float[] out) {
        if(g.calumetStage() || g.indianaStage()){
            int d=g.calumetStage()?g.voyage.destination(berth):0;
            float nx=g.calumetStage()?g.voyage.normalX(d):IndianaHarbor.DOCK_NX,ny=g.calumetStage()?g.voyage.normalY(d):IndianaHarbor.DOCK_NY;
            float x=g.dockX(berth)-ny*offset,y=g.dockY(berth)+nx*offset;
            for(int i=64;i<400;i+=8){float xx=x+nx*i,yy=y+ny*i;
                if(!g.inWater(xx,yy) && !g.inWater(xx-radius,yy) && !g.inWater(xx+radius,yy) && !g.inWater(xx,yy-radius) && !g.inWater(xx,yy+radius)){out[0]=xx;out[1]=yy;return;}}
            out[0]=x+nx*140;out[1]=y+ny*140;return;
        }
        float x=g.orderStage()?PhysicalBerths.LEFT-25:g.berthRight(berth)?480:120;
        float y=g.dockY(berth)+offset,sign=g.berthRight(berth)?1:-1;
        for(int ring=0;ring<=32;ring++)for(int dy=-ring;dy<=ring;dy++) {
            float xx=x+sign*(ring-Math.abs(dy))*8,yy=y+dy*8;
            if(dry(xx,yy,radius)){out[0]=xx;out[1]=yy;return;}
        }
        throw new IllegalStateException("No dry dock apron");
    }
    static void position(Game g,int id,float[] out){if(id>=6)TrainingYardCrew.position(id-6,out);else shoreAnchor(g,id/2,id%2==0?-60:60,7,out);}
    static float depth(Game g,int id){float[] at=new float[2];position(g,id,at);return g.layout.camera.screenY(at[0],at[1]);}
    static float footY(Game g,int id){return depth(g,id)-GroundArt.LAND_RISE*g.layout.camera.scale;}
    static int[] ordered(Game g) {
        int total=6+(g.tutorial!=null?TrainingYardCrew.count():0);int[] ids=new int[total];int count=0;if(g.montroseStage())return new int[0];
        float[] at=new float[2];RiverCamera c=g.layout.camera;
        for(int id=0;id<total;id++)if(id>=6 || g.berthDisplayEnabled(id/2)) {
            position(g,id,at);float x=c.screenX(at[0],at[1]),y=footY(g,id);
            if(x< -20*c.scale || x>g.layout.width+20*c.scale || y< -8*c.scale || y>g.layout.height+35*c.scale)continue;
            int j=count;while(j>0 && depth(g,ids[j-1])>depth(g,id)){ids[j]=ids[j-1];j--;}
            ids[j]=id;count++;
        }
        return Arrays.copyOf(ids,count);
    }
    private static float gesture(Game g,int berth,int id) {
        if(g.celebrationTicks>0 && Math.abs(g.celebrationY-g.dockY(berth))<54)return .9f+.12f*(float)StrictMath.sin(g.visualTicks*.12+id);
        for(Body b:g.cargo)if(b!=null && b.active && !b.deliveryRecorded && (g.sharedBerthStage() || b.berth==berth))
            if(b.unloadingTicks>0 || b.lineAttached)return .35f+.2f*(float)StrictMath.sin(g.visualTicks*.08+id);
        return .04f*(float)StrictMath.sin(g.visualTicks*.025+id);
    }
    static void draw(Game g,Painter p,int id) {
        if(id>=6){TrainingYardCrew.draw(g,p,id-6);return;}
        float[] at=new float[2];position(g,id,at);drawAt(g,p,at[0],at[1],gesture(g,id/2,id));
    }
    static void drawAt(Game g,Painter p,float wx,float wy,float motion){
        RiverCamera c=g.layout.camera;
        float cosine=(float)StrictMath.cos(motion),sine=(float)StrictMath.sin(motion);
        // Continuous shoulder rotation; boot vertices never depend on animation time.
        for(Face face:FACES) {
            float[] xy=face.projected;
            for(int i=0,j=0;i<face.xyz.length;i+=3,j+=2) {
                float x=face.xyz[i],y=face.xyz[i+1],z=face.xyz[i+2];
                if(face.arm!=0){float dx=x-face.arm*3.1f,dz=z-15;
                    x=face.arm*3.1f+dx*cosine-dz*sine*face.arm;
                    z=15+dx*sine*face.arm+dz*cosine;}
                xy[j]=c.screenX(wx+x,wy+y);xy[j+1]=c.screenY(wx+x,wy+y)-(GroundArt.LAND_RISE+z)*c.scale;
            }
            if(face.material<0)p.poly(xy,SceneLighting.sceneryColor(face.color));else p.surfacePolygon(xy,face.uv,face.material,face.color,0);
        }
    }
    private static final class Face {
        final float[] xyz,uv,projected;final int color,material,arm;
        Face(float[] v,int c,int m,int a){xyz=v;color=c;material=m;arm=a;uv=new float[v.length/3*2];projected=new float[uv.length];for(int i=0,j=0;i<v.length;i+=3,j+=2){uv[j]=v[i]+v[i+1];uv[j+1]=v[i+2];}}
    }
    private static Face[] mesh() {
        ArrayList<Face> f=new ArrayList<Face>();
        for(int side=-1;side<=1;side+=2) {
            prism(f,side*1.7f,.5f,0,1.35f,2.3f,2,1,0xff282b2b,-1,0);
            prism(f,side*1.65f,0,2,1.2f,1.5f,7.5f,.85f,0xff3f4747,-1,0);
        }
        prism(f,0,0,9,3.1f,2,7.2f,.82f,0xffa7643c,-1,0);
        prism(f,0,0,11.3f,3.12f,2.03f,.65f,1,0xffb7af8b,-1,0);
        for(int side=-1;side<=1;side+=2) {
            prism(f,side*3.5f,0,9.8f,1.05f,1.2f,5.5f,1,0xff925e3e,-1,side);
            prism(f,side*3.5f,0,8.7f,.9f,1,1.5f,1,0xff967960,-1,side);
        }
        prism(f,0,0,16.2f,1.8f,1.65f,3.4f,.9f,0xffb39170,-1,0);
        prism(f,0,0,19,2.7f,2.4f,.55f,1,0xffad914f,SurfaceMaterial.SAFETY,0);
        prism(f,0,0,19.55f,2.1f,1.9f,2,.65f,0xffb89c59,SurfaceMaterial.SAFETY,0);
        return f.toArray(new Face[f.size()]);
    }
    /** Octagonal tapered solids, with shared fixed lighting and no screen-aligned pixel blocks. */
    private static void prism(ArrayList<Face> f,float x,float y,float z,float rx,float ry,float h,float taper,int color,int material,int arm) {
        float[] top=new float[24];
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4,b=(i+1)*Math.PI/4;
            float ax=(float)StrictMath.cos(a)*rx,ay=(float)StrictMath.sin(a)*ry,bx=(float)StrictMath.cos(b)*rx,by=(float)StrictMath.sin(b)*ry;
            top[i*3]=x+ax*taper;top[i*3+1]=y+ay*taper;top[i*3+2]=z+h;
            if(-(ax+bx)+(ay+by)>0)f.add(new Face(new float[]{x+ax,y+ay,z,x+bx,y+by,z,x+bx*taper,y+by*taper,z+h,x+ax*taper,y+ay*taper,z+h},shade(color,i<4?.8f:.62f),material,arm));
        }
        f.add(new Face(top,color,material,arm));
    }
    private static int shade(int c,float s){return 0xff000000|((int)(((c>>16)&255)*s)<<16)|((int)(((c>>8)&255)*s)<<8)|(int)((c&255)*s);}
}
