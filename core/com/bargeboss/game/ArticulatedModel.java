package com.bargeboss.game;

import java.util.ArrayList;

/** Small rigid XYZ scene graph. Hosts receive only projected Painter primitives. */
public final class ArticulatedModel {
    public static final float Z_SCALE=.81649658f;
    private static final float EPS=.002f;
    private final Node[] nodes;
    private final Face[] faces;
    // One derived pose only: no gameplay, camera, opacity or clock state enters this cache.
    private Pose cachedPose;
    private float cachedAmount=Float.NaN;
    private ArticulatedModel(Node[] n,Face[] f) {nodes=n;faces=f;}
    public int nodeCount() {return nodes.length;}
    public int sourceFaceCount() {return faces.length;}
    public String nodeName(int node) {return nodes[node].id;}
    public int parent(int node) {return nodes[node].parent;}
    public int nodeId(String id) {for(int i=0;i<nodes.length;i++)if(nodes[i].id.equals(id))return i;return -1;}
    public Pose pose(float amount) {
        if(cachedPose==null || Float.floatToIntBits(amount)!=Float.floatToIntBits(cachedAmount)) {
            cachedPose=new Pose(amount);cachedAmount=amount;
        }
        return cachedPose;
    }
    private static final class Node {
        final String id;final int parent;final float x,y,z,yaw,pitchRate;
        Node(String id,int p,float x,float y,float z,float yaw,float rate) {this.id=id;parent=p;this.x=x;this.y=y;this.z=z;this.yaw=yaw;pitchRate=rate;}
    }
    private static final class Face {
        final int node,color,material;final float[] xyz,uv;
        Face(int n,int c,int m,float[] p) {
            node=n;color=c;material=m;xyz=p;uv=new float[p.length/3*2];
            // Basis is established once in part-local space and survives every pose/split.
            float ux=p[3]-p[0],uy=p[4]-p[1],uz=p[5]-p[2];
            float vx=p[p.length-3]-p[0],vy=p[p.length-2]-p[1],vz=p[p.length-1]-p[2];
            float ul=(float)StrictMath.sqrt(ux*ux+uy*uy+uz*uz),vl=(float)StrictMath.sqrt(vx*vx+vy*vy+vz*vz);
            ux/=ul;uy/=ul;uz/=ul;vx/=vl;vy/=vl;vz/=vl;
            for(int i=0,j=0;i<p.length;i+=3,j+=2){uv[j]=p[i]*ux+p[i+1]*uy+p[i+2]*uz;uv[j+1]=p[i]*vx+p[i+1]*vy+p[i+2]*vz;}
        }
    }
    /** A builder owns all geometry; completed models contain no gameplay state. */
    public static final class Builder {
        private final ArrayList<Node> nodes=new ArrayList<Node>();
        private final ArrayList<Face> faces=new ArrayList<Face>();
        /** Local upward pitch is applied before yaw; children inherit the complete parent transform. */
        public int node(String id,int parent,float x,float y,float z,float yaw,float pitchRate) {
            if(parent>=nodes.size() || parent < -1) throw new IllegalArgumentException("Parent must precede child");
            for(Node n:nodes)if(n.id.equals(id))throw new IllegalArgumentException("Duplicate rigid part ID");
            nodes.add(new Node(id,parent,x,y,z,yaw,pitchRate));return nodes.size()-1;
        }
        public void box(int node,float x,float y,float z,float width,float depth,float height,int color) {
            box(node,x,y,z,width,depth,height,color,-1);
        }
        public void box(int node,float x,float y,float z,float width,float depth,float height,int color,int material) {
            float a=width*.5f,b=depth*.5f,c=height*.5f;
            prism(node,color,material,new float[][]{{x-a,y-b,z-c},{x+a,y-b,z-c},{x+a,y+b,z-c},{x-a,y+b,z-c},
                    {x-a,y-b,z+c},{x+a,y-b,z+c},{x+a,y+b,z+c},{x-a,y+b,z+c}});
        }
        public void beam(int node,float ax,float ay,float az,float bx,float by,float bz,float width,int color) {
            beam(node,ax,ay,az,bx,by,bz,width,color,-1);
        }
        public void beam(int node,float ax,float ay,float az,float bx,float by,float bz,float width,int color,int material) {
            float dx=bx-ax,dy=by-ay,dz=bz-az,len=(float)StrictMath.sqrt(dx*dx+dy*dy+dz*dz);
            if(len<EPS)throw new IllegalArgumentException("Rigid beam needs length");
            dx/=len;dy/=len;dz/=len;
            float ux=-dy,uy=dx,uz=0,ul=(float)StrictMath.sqrt(ux*ux+uy*uy);
            if(ul<EPS){ux=1;uy=0;ul=1;}
            ux/=ul;uy/=ul;float vx=dy*uz-dz*uy,vy=dz*ux-dx*uz,vz=dx*uy-dy*ux,h=width*.5f;
            float[][] p=new float[8][3];
            for(int end=0;end<2;end++)for(int i=0;i<4;i++) {
                float su=(i==0 || i==3)?-h:h,sv=i<2?-h:h;
                p[end*4+i]=new float[]{(end==0?ax:bx)+su*ux+sv*vx,(end==0?ay:by)+su*uy+sv*vy,(end==0?az:bz)+su*uz+sv*vz};
            }
            prism(node,color,material,p);
        }
        private void prism(int node,int color,int material,float[][] p) {
            // Outward winding for both boxes and beam frames.
            int[][] indices={{3,2,1,0},{4,5,6,7},{0,1,5,4},{1,2,6,5},{2,3,7,6},{3,0,4,7}};
            for(int[] f:indices) {float[] v=new float[12];for(int j=0;j<4;j++)for(int k=0;k<3;k++)v[j*3+k]=p[f[j]][k];faces.add(new Face(node,color,material,v));}
        }
        public ArticulatedModel build() {return new ArticulatedModel(nodes.toArray(new Node[nodes.size()]),faces.toArray(new Face[faces.size()]));}
    }
    public final class Pose {
        private final float[][] matrix=new float[nodes.length][12];
        private final Poly[] polygons=new Poly[faces.length];
        private final float[][] projectedBounds=new float[nodes.length][];
        private final float[][] worldBounds=new float[nodes.length][];
        private Branch tree;
        private int fragments;
        private Pose(float amount) {
            for(int i=0;i<nodes.length;i++) {
                Node n=nodes[i];float a=n.pitchRate*amount,cy=(float)StrictMath.cos(n.yaw),sy=(float)StrictMath.sin(n.yaw),cp=(float)StrictMath.cos(a),sp=(float)StrictMath.sin(a);
                float[] m={cy*cp,-sy,-cy*sp,n.x,sy*cp,cy,-sy*sp,n.y,sp,0,cp,n.z};
                if(n.parent<0)matrix[i]=m;else {
                    float[] p=matrix[n.parent],o=matrix[i];
                    for(int r=0;r<3;r++){for(int c=0;c<3;c++)o[r*4+c]=p[r*4]*m[c]+p[r*4+1]*m[4+c]+p[r*4+2]*m[8+c];o[r*4+3]=p[r*4]*m[3]+p[r*4+1]*m[7]+p[r*4+2]*m[11]+p[r*4+3];}
                }
            }
            for(int i=0;i<faces.length;i++) {
                Face f=faces[i];float[] xyz=new float[f.xyz.length];
                for(int j=0;j<xyz.length;j+=3){float[] p=point(f.node,f.xyz[j],f.xyz[j+1],f.xyz[j+2]);xyz[j]=p[0];xyz[j+1]=p[1];xyz[j+2]=p[2];
                    if(worldBounds[f.node]==null)worldBounds[f.node]=new float[]{p[0],p[1],p[2],p[0],p[1],p[2]};else {
                        float[] wb=worldBounds[f.node];for(int k=0;k<3;k++){wb[k]=Math.min(wb[k],p[k]);wb[k+3]=Math.max(wb[k+3],p[k]);}
                    }
                    float x=RiverCamera.Q*(p[0]+p[1]),y=RiverCamera.Q*RiverCamera.DEPTH*(p[1]-p[0])-p[2]*Z_SCALE;
                    if(projectedBounds[f.node]==null)projectedBounds[f.node]=new float[]{x,y,x,y};else {
                        float[] b=projectedBounds[f.node];b[0]=Math.min(b[0],x);b[1]=Math.min(b[1],y);b[2]=Math.max(b[2],x);b[3]=Math.max(b[3],y);
                    }
                }
                polygons[i]=new Poly(xyz,f.uv,f.node,f.color,f.material);
            }
        }
        public float[] point(int node,float x,float y,float z) {float[] p=normal(node,x,y,z),m=matrix[node];p[0]+=m[3];p[1]+=m[7];p[2]+=m[11];return p;}
        public float[] normal(int node,float x,float y,float z) {float[] m=matrix[node];return new float[]{m[0]*x+m[1]*y+m[2]*z,m[4]*x+m[5]*y+m[6]*z,m[8]*x+m[9]*y+m[10]*z};}
        public float[] nodeWorldBounds(int node) {return worldBounds[node]==null?null:worldBounds[node].clone();}
        public float[] nodeBounds(RiverCamera c,int node) {
            float[] b=projectedBounds[node];if(b==null)return null;
            float x=c.screenX(0,0),y=c.screenY(0,0);
            return new float[]{x+b[0]*c.scale,y+b[1]*c.scale,x+b[2]*c.scale,y+b[3]*c.scale};
        }
        /** Moving shore machinery remains solid; only buildings may fade. */
        public float opacity(Game g,Painter painter,int node) {return 1;}
        /** Exact convex-face ray test; halos do not shine through opaque bridge parts. */
        public boolean occluded(float[] origin) {
            for(Poly f:polygons) {
                float den=-f.nx+f.ny+f.nz;if(Math.abs(den)<EPS)continue;
                float t=(f.d-f.nx*origin[0]-f.ny*origin[1]-f.nz*origin[2])/den;if(t<=.08f)continue;
                float x=origin[0]-t,y=origin[1]+t,z=origin[2]+t;boolean inside=true;
                for(int i=0;i<f.xyz.length;i+=3){int j=(i+3)%f.xyz.length;float ax=f.xyz[j]-f.xyz[i],ay=f.xyz[j+1]-f.xyz[i+1],az=f.xyz[j+2]-f.xyz[i+2],bx=x-f.xyz[i],by=y-f.xyz[i+1],bz=z-f.xyz[i+2];
                    if((ay*bz-az*by)*f.nx+(az*bx-ax*bz)*f.ny+(ax*by-ay*bx)*f.nz < -.05f){inside=false;break;}}
                if(inside)return true;
            }
            return false;
        }
    }
    public static final class Stats {public int sourceFaces,visibleFaces,fragments,drawnFaces,culledNodes;}
    public Stats draw(Game g,Painter p,Pose pose) {
        Stats stats=new Stats();stats.sourceFaces=faces.length;float[] alpha=new float[nodes.length];boolean[] visible=new boolean[nodes.length];
        for(int i=0;i<nodes.length;i++) {float[] b=pose.nodeBounds(g.layout.camera,i);visible[i]=b!=null && b[2]>=0 && b[0]<=g.layout.width && b[3]>=0 && b[1]<=g.layout.height;
            if(visible[i])alpha[i]=pose.opacity(g,p,i);else stats.culledNodes++;}
        for(Poly f:pose.polygons)if(visible[f.node] && -f.nx+f.ny+f.nz>EPS)stats.visibleFaces++;
        if(stats.visibleFaces==0)return stats;
        if(pose.tree==null) {
            Stats build=new Stats();
            for(Poly f:pose.polygons)if(-f.nx+f.ny+f.nz>EPS) {
                if(pose.tree==null)pose.tree=new Branch(f);else pose.tree.insert(f,build);
            }
            pose.fragments=build.fragments;
        }
        stats.fragments=pose.fragments;
        if(pose.tree!=null)pose.tree.paint(p,g.layout.camera,alpha,stats);
        return stats;
    }
    private static final class Poly {
        final float[] xyz,uv;final int node,color,material;final float nx,ny,nz,d;
        Poly(float[] p,float[] texture,int n,int c,int m) {
            xyz=p;uv=texture;node=n;color=c;material=m;float ax=p[3]-p[0],ay=p[4]-p[1],az=p[5]-p[2],bx=p[6]-p[0],by=p[7]-p[1],bz=p[8]-p[2];
            float x=ay*bz-az*by,y=az*bx-ax*bz,z=ax*by-ay*bx,len=(float)StrictMath.sqrt(x*x+y*y+z*z);
            nx=x/len;ny=y/len;nz=z/len;d=nx*p[0]+ny*p[1]+nz*p[2];
        }
        Poly(float[] p,float[] texture,Poly original) {xyz=p;uv=texture;node=original.node;color=original.color;material=original.material;nx=original.nx;ny=original.ny;nz=original.nz;d=original.d;}
        void paint(Painter p,RiverCamera c,float alpha) {
            float[] xy=new float[xyz.length/3*2];for(int i=0,j=0;i<xyz.length;i+=3,j+=2){xy[j]=c.screenX(xyz[i],xyz[i+1]);xy[j+1]=c.screenY(xyz[i],xyz[i+1])-xyz[i+2]*Z_SCALE*c.scale;}
            float light=.62f+.38f*Math.max(0,-nx*.35f-ny*.25f+nz*.9f);
            int r=(int)((color>>16&255)*light),g=(int)((color>>8&255)*light),b=(int)((color&255)*light);
            int shaded=((int)(255*alpha)<<24)|(r<<16)|(g<<8)|b;
            if(material<0)p.poly(xy,material==SurfaceMaterial.EMISSIVE?shaded:SceneLighting.sceneryColor(shaded));
            else p.surfacePolygon(xy,uv,material,shaded,material==SurfaceMaterial.CONCRETE?.12f:.04f);
        }
    }
    /** Plane splits resolve crossing truss/deck faces, including the lifted poses. */
    private static final class Branch {
        final Poly plane;final ArrayList<Poly> coplanar=new ArrayList<Poly>();Branch front,back;
        Branch(Poly p){plane=p;coplanar.add(p);}
        void insert(Poly p,Stats stats) {
            boolean pos=false,neg=false;float[] distances=new float[p.xyz.length/3];
            for(int i=0;i<distances.length;i++){int k=i*3;float d=plane.nx*p.xyz[k]+plane.ny*p.xyz[k+1]+plane.nz*p.xyz[k+2]-plane.d;distances[i]=d;if(d>EPS)pos=true;if(d < -EPS)neg=true;}
            if(!pos&&!neg){coplanar.add(p);return;}
            if(!neg){if(front==null)front=new Branch(p);else front.insert(p,stats);return;}
            if(!pos){if(back==null)back=new Branch(p);else back.insert(p,stats);return;}
            Poly a=clip(p,distances,true),b=clip(p,distances,false);stats.fragments++;
            if(front==null)front=new Branch(a);else front.insert(a,stats);
            if(back==null)back=new Branch(b);else back.insert(b,stats);
        }
        private static Poly clip(Poly p,float[] ds,boolean positive) {
            float[] result=new float[p.xyz.length+6],texture=new float[(p.xyz.length/3+2)*2];int size=0,uvSize=0;
            for(int i=0;i<ds.length;i++) {int j=(i+1)%ds.length;float a=ds[i],b=ds[j];boolean in=positive?a>=-EPS:a<=EPS;
                if(in){for(int k=0;k<3;k++)result[size++]=p.xyz[i*3+k];for(int k=0;k<2;k++)texture[uvSize++]=p.uv[i*2+k];}
                if((a>EPS&&b < -EPS)||(a < -EPS&&b>EPS)){float t=a/(a-b);for(int k=0;k<3;k++)result[size++]=p.xyz[i*3+k]+t*(p.xyz[j*3+k]-p.xyz[i*3+k]);for(int k=0;k<2;k++)texture[uvSize++]=p.uv[i*2+k]+t*(p.uv[j*2+k]-p.uv[i*2+k]);}
            }
            float[] exact=new float[size];System.arraycopy(result,0,exact,0,size);float[] exactUV=new float[uvSize];System.arraycopy(texture,0,exactUV,0,uvSize);return new Poly(exact,exactUV,p);
        }
        void paint(Painter p,RiverCamera c,float[] alpha,Stats stats) {
            boolean positive=-plane.nx+plane.ny+plane.nz>0;Branch far=positive?back:front,near=positive?front:back;
            if(far!=null)far.paint(p,c,alpha,stats);for(Poly f:coplanar)if(alpha[f.node]>0){f.paint(p,c,alpha[f.node]);stats.drawnFaces++;}if(near!=null)near.paint(p,c,alpha,stats);
        }
    }
}
