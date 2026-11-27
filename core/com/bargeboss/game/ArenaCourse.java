package com.bargeboss.game;

/** Geometry for a tag/chase arena: water test, walls, gates (bridges and lock gates as segments that
 * block while shut) and a centreline route graph for NPCs. TagArena runs the same games on any course. */
abstract class ArenaCourse {
    float w,h;
    /** Gate k spans the channel from (ax,ay) to (bx,by); bridge[k] says how it is drawn. */
    float[] ax,ay,bx,by;boolean[] bridge;
    float[] nodeX,nodeY;int[][] links;
    abstract boolean contains(float x,float y);
    /** Fixed land: push one hull disc out of shores and islands; returns its inward impact speed. */
    abstract float walls(Body b,CollisionSplashes splashes,float x,float y,float r);
    int gates(){return ax.length;}
    float gateX(int k){return (ax[k]+bx[k])*.5f;}
    float gateY(int k){return (ay[k]+by[k])*.5f;}

    /** Push a hull disc at (x,y) out along (nx,ny) by depth; reflect inward velocity; splash. */
    static float push(Body b,CollisionSplashes splashes,float x,float y,float depth,float nx,float ny){
        if(depth<=0)return 0;
        b.x+=nx*depth;b.y+=ny*depth;
        float inward=b.vx*nx+b.vy*ny,impact=0;
        if(inward<0){impact=-inward;b.vx-=inward*nx*1.08f;b.vy-=inward*ny*1.08f;}
        b.spin*=.94f;
        if(splashes!=null)Physics.wallSplash(splashes,b,x-nx*b.radius,y-ny*b.radius,nx,ny,x,y);
        return impact;
    }
    /** A shut gate is a thin wall along its segment. */
    float gateWall(Body b,CollisionSplashes splashes,int k,float x,float y,float r){
        float dx=bx[k]-ax[k],dy=by[k]-ay[k],len2=dx*dx+dy*dy,t=Physics.clamp(((x-ax[k])*dx+(y-ay[k])*dy)/len2,0,1);
        float qx=ax[k]+dx*t,qy=ay[k]+dy*t,ox=x-qx,oy=y-qy,d=Physics.length(ox,oy);
        if(d>=r+4)return 0;
        float len=(float)StrictMath.sqrt(len2),nx=-dy/len,ny=dx/len;if(ox*nx+oy*ny<0){nx=-nx;ny=-ny;}
        return push(b,splashes,x,y,r+4-d,nx,ny);
    }
    /** Whether the straight line a-b crosses gate k's segment. */
    boolean crosses(int k,float x0,float y0,float x1,float y1){
        float d1x=x1-x0,d1y=y1-y0,d2x=bx[k]-ax[k],d2y=by[k]-ay[k],den=d1x*d2y-d1y*d2x;
        if(Math.abs(den)<1e-4f)return false;
        float t=((ax[k]-x0)*d2y-(ay[k]-y0)*d2x)/den,u=((ax[k]-x0)*d1y-(ay[k]-y0)*d1x)/den;
        return t>=0 && t<=1 && u>=0 && u<=1;
    }
    /** Straight water between two points (gates ignored). */
    boolean water(float x0,float y0,float x1,float y1,float step){
        float len=Physics.length(x1-x0,y1-y0);
        for(float t=0;t<=len;t+=step)if(!contains(x0+(x1-x0)*t/len,y0+(y1-y0)*t/len))return false;
        return true;
    }

    /** The tag circuit: TagArena's loop canal, islands, two lift bridges and the lock. */
    static final class Loop extends ArenaCourse {
        Loop(){
            w=TagArena.W;h=TagArena.H;int n=TagArena.GATES;ax=new float[n];ay=new float[n];bx=new float[n];by=new float[n];bridge=new boolean[n];
            for(int k=0;k<n;k++){ax[k]=bx[k]=TagArena.GATE_X[k];ay[k]=TagArena.GATE_Y0[k];by[k]=TagArena.GATE_Y1[k];bridge[k]=k<=TagArena.BRIDGE_S;}
            nodeX=TagArena.NODE_X;nodeY=TagArena.NODE_Y;links=TagArena.LINKS;
        }
        boolean contains(float x,float y){return TagArena.contains(x,y);}
        float walls(Body b,CollisionSplashes splashes,float x,float y,float r){
            float impact=0;
            impact=Math.max(impact,push(b,splashes,x,y,r-x,1,0));
            impact=Math.max(impact,push(b,splashes,x,y,x+r-w,-1,0));
            impact=Math.max(impact,push(b,splashes,x,y,r-y,0,1));
            impact=Math.max(impact,push(b,splashes,x,y,y+r-h,0,-1));
            for(float[] s:TagArena.ISLANDS){
                float qx=Physics.clamp(x,s[0],s[2]),qy=Physics.clamp(y,s[1],s[3]),dx=x-qx,dy=y-qy,d=Physics.length(dx,dy);
                boolean inside=x>s[0] && x<s[2] && y>s[1] && y<s[3];
                if(!inside && d>=r)continue;
                float nx,ny,depth;
                if(inside){float l=x-s[0],rr=s[2]-x,t=y-s[1],bb=s[3]-y,m=Math.min(Math.min(l,rr),Math.min(t,bb));
                    nx=m==l?-1:m==rr?1:0;ny=m==t?-1:m==bb?1:0;if(nx!=0)ny=0;depth=m+r;}
                else{nx=dx/Math.max(.001f,d);ny=dy/Math.max(.001f,d);depth=r-d;}
                impact=Math.max(impact,push(b,splashes,x,y,depth,nx,ny));
            }
            return impact;
        }
    }

    /** Thousand Islands: open river water between wooded granite islands, with footbridges linking a
     * few neighbours. Islands are circles for collision (drawn as rough rock outlines); the route graph
     * is a grid of clear water points linked wherever the straight line stays on the water. */
    static final class Islands extends ArenaCourse {
        static final float W=2600,H=1800,SHORE=60;
        /** Island centres and radii (granite rim included). */
        static final float[][] ISLES={{330,330,105},{760,240,75},{1180,400,135},{1680,260,90},{2200,330,120},
            {470,820,95},{930,860,70},{1400,830,150},{1900,790,85},{2330,930,105},
            {360,1360,125},{820,1370,85},{1240,1300,75},{1720,1380,135},{2180,1470,90}};
        /** Footbridges: pairs of islands joined across a narrow channel. */
        static final int[][] FOOTBRIDGES={{1,2},{6,7},{11,12}};
        Islands(){
            w=W;h=H;int n=FOOTBRIDGES.length;ax=new float[n];ay=new float[n];bx=new float[n];by=new float[n];bridge=new boolean[n];
            for(int k=0;k<n;k++){
                float[] a=ISLES[FOOTBRIDGES[k][0]],b=ISLES[FOOTBRIDGES[k][1]];float dx=b[0]-a[0],dy=b[1]-a[1],d=Physics.length(dx,dy);dx/=d;dy/=d;
                ax[k]=a[0]+dx*(a[2]-6);ay[k]=a[1]+dy*(a[2]-6);bx[k]=b[0]-dx*(b[2]-6);by[k]=b[1]-dy*(b[2]-6);bridge[k]=true;
            }
            buildRoutes();
        }
        boolean contains(float x,float y){
            if(x<SHORE || x>w-SHORE || y<SHORE || y>h-SHORE)return false;
            for(float[] s:ISLES)if(Physics.length(x-s[0],y-s[1])<s[2])return false;
            return true;
        }
        float walls(Body b,CollisionSplashes splashes,float x,float y,float r){
            float impact=0;
            impact=Math.max(impact,push(b,splashes,x,y,SHORE+r-x,1,0));
            impact=Math.max(impact,push(b,splashes,x,y,x+r-(w-SHORE),-1,0));
            impact=Math.max(impact,push(b,splashes,x,y,SHORE+r-y,0,1));
            impact=Math.max(impact,push(b,splashes,x,y,y+r-(h-SHORE),0,-1));
            for(float[] s:ISLES){
                float dx=x-s[0],dy=y-s[1],d=Physics.length(dx,dy);
                if(d>=s[2]+r)continue;
                float nx=d<.001f?1:dx/d,ny=d<.001f?0:dy/d;
                impact=Math.max(impact,push(b,splashes,x,y,s[2]+r-d,nx,ny));
            }
            return impact;
        }
        /** Clear distance from a point to the nearest island rim or shore. */
        float clearance(float x,float y){
            float c=Math.min(Math.min(x-SHORE,w-SHORE-x),Math.min(y-SHORE,h-SHORE-y));
            for(float[] s:ISLES)c=Math.min(c,Physics.length(x-s[0],y-s[1])-s[2]);
            return c;
        }
        private void buildRoutes(){
            java.util.ArrayList<float[]> pts=new java.util.ArrayList<float[]>();
            for(float y=SHORE+90;y<h-SHORE-60;y+=140)for(float x=SHORE+90;x<w-SHORE-60;x+=140)if(clearance(x,y)>=55)pts.add(new float[]{x,y});
            int n=pts.size();nodeX=new float[n];nodeY=new float[n];for(int i=0;i<n;i++){nodeX[i]=pts.get(i)[0];nodeY[i]=pts.get(i)[1];}
            links=new int[n][];
            for(int i=0;i<n;i++){
                java.util.ArrayList<Integer> near=new java.util.ArrayList<Integer>();
                for(int j=0;j<n;j++){if(i==j)continue;float d=Physics.length(nodeX[i]-nodeX[j],nodeY[i]-nodeY[j]);
                    if(d<=210 && clearPath(nodeX[i],nodeY[i],nodeX[j],nodeY[j]))near.add(j);}
                links[i]=new int[near.size()];for(int k=0;k<links[i].length;k++)links[i][k]=near.get(k);
            }
        }
        /** Water all the way with room for a hull (gates ignored). */
        boolean clearPath(float x0,float y0,float x1,float y1){
            float len=Physics.length(x1-x0,y1-y0);
            for(float t=0;t<=len;t+=12)if(clearance(x0+(x1-x0)*t/len,y0+(y1-y0)*t/len)<26)return false;
            return true;
        }
    }
}
