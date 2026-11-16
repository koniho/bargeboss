package com.bargeboss.game;

import java.util.ArrayList;

/** Simple coastline polygons and convex dock footprints, shared across hosts. */
final class PolygonWater {
    private PolygonWater(){}
    static float cross(float ax,float ay,float bx,float by,float x,float y){return (bx-ax)*(y-ay)-(by-ay)*(x-ax);}
    static float area(float[] p){if(p.length<6)return 0;double a=0;
        for(int i=2;i<p.length-2;i+=2)a+=(double)(p[i]-p[0])*(p[i+3]-p[1])-(double)(p[i+2]-p[0])*(p[i+1]-p[1]);
        return (float)(a*.5);
    }
    static boolean contains(float[] p,float x,float y){boolean in=false;for(int i=0,j=p.length-2;i<p.length;j=i,i+=2){float ax=p[i],ay=p[i+1],bx=p[j],by=p[j+1];if((ay>y)!=(by>y) && x<(bx-ax)*(y-ay)/(by-ay)+ax)in=!in;}return in;}
    static float[] bounds(float[] p){float[] b={Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};for(int i=0;i<p.length;i+=2){b[0]=Math.min(b[0],p[i]);b[1]=Math.min(b[1],p[i+1]);b[2]=Math.max(b[2],p[i]);b[3]=Math.max(b[3],p[i+1]);}return b;}
    static boolean overlaps(float[] a,float[] b){return a[0]<b[2] && a[2]>b[0] && a[1]<b[3] && a[3]>b[1];}
    static float[] edge(float[] p,float ax,float ay,float bx,float by,boolean positive){
        if(p.length<6)return new float[0];float[] out=new float[p.length*2+4];int n=0;
        float x=p[p.length-2],y=p[p.length-1],d=cross(ax,ay,bx,by,x,y)*(positive?1:-1);boolean inside=d>=0;
        for(int i=0;i<p.length;i+=2){float xx=p[i],yy=p[i+1],dd=cross(ax,ay,bx,by,xx,yy)*(positive?1:-1);boolean next=dd>=0;
            if(inside!=next){float t=d/(d-dd);out[n++]=x+(xx-x)*t;out[n++]=y+(yy-y)*t;}
            if(next){out[n++]=xx;out[n++]=yy;}x=xx;y=yy;d=dd;inside=next;
        }
        if(n<6)return new float[0];float[] r=new float[n];System.arraycopy(out,0,r,0,n);return r;
    }
    static float[] clip(float[] p,float[] convex){boolean pos=area(convex)>0;for(int i=0;i<convex.length && p.length>=6;i+=2){int j=(i+2)%convex.length;p=edge(p,convex[i],convex[i+1],convex[j],convex[j+1],pos);}return p;}
    static float[] rectangle(float[] p,float l,float t,float r,float b){return clip(p,new float[]{l,t,r,t,r,b,l,b});}
    static void subtract(float[] p,float[] hole,ArrayList<float[]> out){boolean pos=area(hole)>0;for(int i=0;i<hole.length && p.length>=6;i+=2){int j=(i+2)%hole.length;float[] outside=edge(p,hole[i],hole[i+1],hole[j],hole[j+1],!pos);if(outside.length>=6)out.add(outside);p=edge(p,hole[i],hole[i+1],hole[j],hole[j+1],pos);}}
    static float[][] triangles(float[] p){
        int count=p.length/2;int[] index=new int[count];for(int i=0;i<count;i++)index[i]=area(p)>0?i:count-1-i;
        ArrayList<float[]> out=new ArrayList<float[]>();int guard=count*count;
        while(count>3 && guard-->0){boolean cut=false;
            for(int k=0;k<count;k++){int a=index[(k+count-1)%count]*2,b=index[k]*2,c=index[(k+1)%count]*2;
                if(cross(p[a],p[a+1],p[b],p[b+1],p[c],p[c+1])<=.001f)continue;
                boolean occupied=false;
                for(int n=0;n<count;n++){int v=index[n]*2;if(v==a || v==b || v==c)continue;
                    if(cross(p[a],p[a+1],p[b],p[b+1],p[v],p[v+1])>=0 && cross(p[b],p[b+1],p[c],p[c+1],p[v],p[v+1])>=0 && cross(p[c],p[c+1],p[a],p[a+1],p[v],p[v+1])>=0){occupied=true;break;}}
                if(occupied)continue;out.add(new float[]{p[a],p[a+1],p[b],p[b+1],p[c],p[c+1]});System.arraycopy(index,k+1,index,k,count-k-1);count--;cut=true;break;
            }
            if(!cut)throw new IllegalArgumentException("Coastline must be a simple polygon");
        }
        out.add(new float[]{p[index[0]*2],p[index[0]*2+1],p[index[1]*2],p[index[1]*2+1],p[index[2]*2],p[index[2]*2+1]});return out.toArray(new float[out.size()][]);
    }
    static float collide(Body b,float[] p,boolean keepInside,CollisionSplashes splashes){float impact=0;
        for(int disc=0;disc<3;disc++){
            float x=b.discX(disc),y=b.discY(disc),best=Float.MAX_VALUE,qx=0,qy=0,edgeDX=1,edgeDY=0;
            for(int i=0;i<p.length;i+=2){int j=(i+2)%p.length;float dx=p[j]-p[i],dy=p[j+1]-p[i+1],len=dx*dx+dy*dy;if(len==0)continue;
                float t=Physics.clamp(((x-p[i])*dx+(y-p[i+1])*dy)/len,0,1),xx=p[i]+t*dx,yy=p[i+1]+t*dy,d=(x-xx)*(x-xx)+(y-yy)*(y-yy);
                if(d<best){best=d;qx=xx;qy=yy;edgeDX=dx;edgeDY=dy;}}
            boolean valid=contains(p,x,y)==keepInside;float dist=(float)StrictMath.sqrt(best),penetration=valid?b.radius-dist:b.radius+dist;
            if(penetration<=0)continue;
            float nx=(valid?1:-1)*(x-qx)/Math.max(.001f,dist),ny=(valid?1:-1)*(y-qy)/Math.max(.001f,dist);
            if(dist<.001f){float sign=(area(p)>0?1:-1)*(keepInside?1:-1),length=Physics.length(edgeDX,edgeDY);nx=-edgeDY/length*sign;ny=edgeDX/length*sign;}Physics.wallSplash(splashes,b,qx+nx*.75f,qy+ny*.75f,nx,ny,x,y);
            b.x+=nx*penetration;b.y+=ny*penetration;float inward=b.vx*nx+b.vy*ny;
            if(inward<0){impact=Math.max(impact,-inward);b.vx-=inward*nx*1.08f;b.vy-=inward*ny*1.08f;}b.spin*=.94f;
        }return impact;
    }
}
