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
    /** Prepare complete part-local geometry once. Parts must remain separated along
     * the authored axis in root coordinates throughout their travel. The returned
     * motion owns reusable render buffers; keep it on one rendering thread. */
    public Motion motion(int root,float ax,float ay,float az) {return new Motion(root,ax,ay,az);}
    private static final class Part {
        final int node;final Branch tree;final Poly[] fragments;
        Part(int node,Branch tree){this.node=node;this.tree=tree;ArrayList<Poly> all=new ArrayList<Poly>();tree.flatten(all);fragments=all.toArray(new Poly[all.size()]);}
    }
    /** Live rigid animation, not a table of sampled poses. No BSP build in pose(). */
    public final class Motion {
        private final Part[] parts;
        private final int root,count;
        private final float ax,ay,az;
        private final Pose live;
        private float amount=Float.NaN;
        private Motion(int root,float ax,float ay,float az){
            float length=(float)StrictMath.sqrt(ax*ax+ay*ay+az*az);
            if(root<0 || root>=nodes.length || !Float.isFinite(length) || length<.001f)throw new IllegalArgumentException("Motion needs a root and separating axis");
            this.root=root;this.ax=ax/length;this.ay=ay/length;this.az=az/length;
            ArrayList<Part> list=new ArrayList<Part>();int total=0;
            for(int node=0;node<nodes.length;node++){
                Branch tree=null;Stats build=new Stats();
                // Both front and back faces are retained. Their visibility changes continuously.
                for(Face f:faces)if(f.node==node){
                    int ancestor=node;while(ancestor>=0 && ancestor!=root)ancestor=nodes[ancestor].parent;
                    if(ancestor<0)throw new IllegalArgumentException("Motion root must contain every geometry node");
                    Poly poly=new Poly(f.xyz,f.uv,node,f.color,f.material);
                    if(tree==null)tree=new Branch(poly);else tree.insert(poly,build);
                }
                if(tree!=null){Part part=new Part(node,tree);for(Poly f:part.fragments)f.motionIndex=total++;list.add(part);}
            }
            parts=list.toArray(new Part[list.size()]);count=total;live=new Pose(this);
        }
        public int fragmentCount(){return count;}
        public Pose pose(float value){
            if(!Float.isFinite(value))throw new IllegalArgumentException("Non-finite motion");
            if(Float.floatToIntBits(amount)!=Float.floatToIntBits(value)){amount=Float.NaN;live.update(this,value);amount=value;}
            return live;
        }
    }
    /** Prepared single-part geometry for freely moving vessels. No pose/BSP rebuilds. */
    public Rigid rigid(){return new Rigid();}
    public final class Rigid {
        private final Branch tree;
        private final int fragments,maxVertices;
        private final ThreadLocal<RigidDraw> drawing=new ThreadLocal<RigidDraw>(){
            protected RigidDraw initialValue(){return new RigidDraw(maxVertices,faces.length);}
        };
        private Rigid(){
            if(nodes.length!=1 || nodes[0].x!=0 || nodes[0].y!=0 || nodes[0].z!=0 || nodes[0].yaw!=0)
                throw new IllegalArgumentException("Rigid fleet model requires one identity root");
            Branch built=null;Stats stats=new Stats();
            int source=0;for(Face f:faces){Poly p=new Poly(f.xyz,f.uv,0,f.color,f.material);p.sourceIndex=source++;if(built==null)built=new Branch(p);else built.insert(p,stats);}
            tree=built;ArrayList<Poly> all=new ArrayList<Poly>();tree.flatten(all);fragments=all.size();
            int max=0;for(Poly p:all)max=Math.max(max,p.xyz.length/3);maxVertices=max;
        }
        public int fragmentCount(){return fragments;}
        public void draw(Game g,Painter p,float x,float y,float heave,float yaw,float pitch,float roll,FaceLighting light){
            RigidDraw d=drawing.get();d.p=p;d.c=g.layout.camera;d.light=light;d.wx=x;d.wy=y;d.heave=heave;java.util.Arrays.fill(d.shadeReady,false);
            float ca=(float)StrictMath.cos(yaw),sa=(float)StrictMath.sin(yaw),cp=(float)StrictMath.cos(pitch),sp=(float)StrictMath.sin(pitch),cr=(float)StrictMath.cos(roll),sr=(float)StrictMath.sin(roll);
            float[] m=d.m;
            m[0]=ca*cp;m[1]=ca*sp*sr-sa*cr;m[2]=ca*sp*cr+sa*sr;
            m[3]=sa*cp;m[4]=sa*sp*sr+ca*cr;m[5]=sa*sp*cr-ca*sr;
            m[6]=-sp;m[7]=cp*sr;m[8]=cp*cr;
            d.vx=-m[0]+m[3]+m[6];d.vy=-m[1]+m[4]+m[7];d.vz=-m[2]+m[5]+m[8];
            d.visit(tree);
        }
    }
    private static final class RigidDraw {
        final float[] m=new float[9],vertices,clipped;final float[][] screens;final boolean[] shadeReady;final int[] shades;
        Painter p;RiverCamera c;FaceLighting light;float wx,wy,heave,vx,vy,vz;
        RigidDraw(int max,int sources){shadeReady=new boolean[sources];shades=new int[sources];vertices=new float[max*3];clipped=new float[(max+2)*3];screens=new float[max+3][];for(int i=3;i<screens.length;i++)screens[i]=new float[i*2];}
        void visit(Branch node){
            boolean front=node.plane.nx*vx+node.plane.ny*vy+node.plane.nz*vz>0;
            Branch far=front?node.back:node.front,near=front?node.front:node.back;
            if(far!=null)visit(far);
            for(Poly f:node.coplanar)if(f.nx*vx+f.ny*vy+f.nz*vz>EPS)paint(f);
            if(near!=null)visit(near);
        }
        void paint(Poly f){
            int n=f.xyz.length/3;
            for(int i=0;i<n;i++){int j=i*3;float x=f.xyz[j],y=f.xyz[j+1],z=f.xyz[j+2];
                vertices[j]=m[0]*x+m[1]*y+m[2]*z;vertices[j+1]=m[3]*x+m[4]*y+m[5]*z;vertices[j+2]=m[6]*x+m[7]*y+m[8]*z;}
            // Continuous rocking intersects the water plane instead of exposing the underside.
            int count=0;
            for(int i=0;i<n;i++){int a=i*3,b=((i+1)%n)*3;float az=vertices[a+2],bz=vertices[b+2];
                if(az>=0){for(int k=0;k<3;k++)clipped[count*3+k]=vertices[a+k];count++;}
                if((az<0)!=(bz<0)){float t=az/(az-bz);for(int k=0;k<3;k++)clipped[count*3+k]=vertices[a+k]+t*(vertices[b+k]-vertices[a+k]);count++;}}
            if(count<3)return;
            int source=f.sourceIndex;
            if(!shadeReady[source]){
                float nx=m[0]*f.nx+m[1]*f.ny+m[2]*f.nz,ny=m[3]*f.nx+m[4]*f.ny+m[5]*f.nz,nz=m[6]*f.nx+m[7]*f.ny+m[8]*f.nz;
                float x=m[0]*f.sourceX+m[1]*f.sourceY+m[2]*f.sourceZ,y=m[3]*f.sourceX+m[4]*f.sourceY+m[5]*f.sourceZ,z=m[6]*f.sourceX+m[7]*f.sourceY+m[8]*f.sourceZ;
                shades[source]=light.shade(f.color,f.color,x,y,z,nx,ny,nz);shadeReady[source]=true;
            }
            int color=shades[source];
            for(int i=0;i<count;i++){int j=i*3;float xx=clipped[j],yy=clipped[j+1],zz=clipped[j+2];clipped[j]=c.screenX(wx+xx,wy+yy);clipped[j+1]=c.screenY(wx+xx,wy+yy)-(zz*Z_SCALE+heave)*c.scale;}
            if(count==4)p.convexQuad(clipped[0],clipped[1],clipped[3],clipped[4],clipped[6],clipped[7],clipped[9],clipped[10],color);
            else {float[] screen=screens[count];for(int i=0;i<count;i++){screen[i*2]=clipped[i*3];screen[i*2+1]=clipped[i*3+1];}p.poly(screen,color);}
        }
    }
    private static final class Node {
        final String id;final int parent;final float x,y,z,yaw,pitchRate,moveX,moveY,moveZ;
        Node(String id,int p,float x,float y,float z,float yaw,float rate,float mx,float my,float mz) {this.id=id;parent=p;this.x=x;this.y=y;this.z=z;this.yaw=yaw;pitchRate=rate;moveX=mx;moveY=my;moveZ=mz;}
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
            return movingNode(id,parent,x,y,z,yaw,pitchRate,0,0,0);
        }
        /** Rigid translation in parent coordinates, for lift spans and counterweights. */
        public int movingNode(String id,int parent,float x,float y,float z,float yaw,float pitchRate,float mx,float my,float mz) {
            if(parent>=nodes.size() || parent < -1) throw new IllegalArgumentException("Parent must precede child");
            for(Node n:nodes)if(n.id.equals(id))throw new IllegalArgumentException("Duplicate rigid part ID");
            nodes.add(new Node(id,parent,x,y,z,yaw,pitchRate,mx,my,mz));return nodes.size()-1;
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
            beam(node,ax,ay,az,bx,by,bz,width,width,color,material);
        }
        /** Rectangular section: webbing's broad face and thin edge share one rigid frame. */
        public void beam(int node,float ax,float ay,float az,float bx,float by,float bz,float width,float thickness,int color,int material) {
            float dx=bx-ax,dy=by-ay,dz=bz-az,len=(float)StrictMath.sqrt(dx*dx+dy*dy+dz*dz);
            if(len<EPS)throw new IllegalArgumentException("Rigid beam needs length");
            dx/=len;dy/=len;dz/=len;
            float ux=-dy,uy=dx,uz=0,ul=(float)StrictMath.sqrt(ux*ux+uy*uy);
            if(ul<EPS){ux=1;uy=0;ul=1;}
            ux/=ul;uy/=ul;float vx=dy*uz-dz*uy,vy=dz*ux-dx*uz,vz=dx*uy-dy*ux,h=width*.5f,t=thickness*.5f;
            float[][] p=new float[8][3];
            for(int end=0;end<2;end++)for(int i=0;i<4;i++) {
                float su=(i==0 || i==3)?-h:h,sv=i<2?-t:t;
                p[end*4+i]=new float[]{(end==0?ax:bx)+su*ux+sv*vx,(end==0?ay:by)+su*uy+sv*vy,(end==0?az:bz)+su*uz+sv*vz};
            }
            prism(node,color,material,p);
        }
        /** Fixed upright tapered round section; topology is authored once, never during draw. */
        public void cylinder(int node,float z,float height,float radius,float topRadius,int sides,int color) {
            if(sides<3 || sides>16 || height<=0 || radius<=0 || topRadius<=0)throw new IllegalArgumentException("Invalid round section");
            float[] top=new float[sides*3],bottom=new float[sides*3];
            for(int i=0;i<sides;i++){
                double a=2*StrictMath.PI*i/sides,aa=2*StrictMath.PI*(i+1)/sides;
                float x=(float)StrictMath.cos(a),y=(float)StrictMath.sin(a),xx=(float)StrictMath.cos(aa),yy=(float)StrictMath.sin(aa);
                faces.add(new Face(node,color,-1,new float[]{x*radius,y*radius,z,xx*radius,yy*radius,z,xx*topRadius,yy*topRadius,z+height,x*topRadius,y*topRadius,z+height}));
                top[i*3]=x*topRadius;top[i*3+1]=y*topRadius;top[i*3+2]=z+height;
                int j=sides-1-i;bottom[j*3]=x*radius;bottom[j*3+1]=y*radius;bottom[j*3+2]=z;
            }
            faces.add(new Face(node,color,-1,top));faces.add(new Face(node,color,-1,bottom));
        }
        private void prism(int node,int color,int material,float[][] p) {
            // Outward winding for both boxes and beam frames.
            int[][] indices={{3,2,1,0},{4,5,6,7},{0,1,5,4},{1,2,6,5},{2,3,7,6},{3,0,4,7}};
            for(int[] f:indices) {float[] v=new float[12];for(int j=0;j<4;j++)for(int k=0;k<3;k++)v[j*3+k]=p[f[j]][k];faces.add(new Face(node,color,material,v));}
        }
        public void polygon(int node,int color,float... xyz){faces.add(new Face(node,color,-1,xyz.clone()));}
        public ArticulatedModel build() {return new ArticulatedModel(nodes.toArray(new Node[nodes.size()]),faces.toArray(new Face[faces.size()]));}
    }
    public final class Pose {
        private final float[][] matrix=new float[nodes.length][12];
        private Poly[] polygons=new Poly[faces.length];
        private final float[][] projectedBounds=new float[nodes.length][];
        private final float[][] worldBounds=new float[nodes.length][];
        private boolean prepared;
        private int polygonCount;
        private float depthMin=Float.POSITIVE_INFINITY,depthMax=Float.NEGATIVE_INFINITY;
        private Poly[] transformed;
        private float[][] partRanges;
        private int[] partOrder;
        private int fragments;
        private Pose(Motion motion){
            polygons=new Poly[motion.count];transformed=new Poly[motion.count];partOrder=new int[motion.parts.length];partRanges=new float[motion.parts.length][2];
            for(Part part:motion.parts)for(Poly source:part.fragments)transformed[source.motionIndex]=new Poly(new float[source.xyz.length],source.uv,source);
            prepared=true;fragments=motion.count;
        }
        private void update(Motion motion,float amount){
            transform(amount);polygonCount=0;depthMin=Float.POSITIVE_INFINITY;depthMax=Float.NEGATIVE_INFINITY;
            for(int n=0;n<nodes.length;n++){
                if(worldBounds[n]!=null){for(int k=0;k<3;k++){worldBounds[n][k]=Float.POSITIVE_INFINITY;worldBounds[n][k+3]=Float.NEGATIVE_INFINITY;}}
                if(projectedBounds[n]!=null){projectedBounds[n][0]=projectedBounds[n][1]=Float.POSITIVE_INFINITY;projectedBounds[n][2]=projectedBounds[n][3]=Float.NEGATIVE_INFINITY;}
            }
            float[] root=matrix[motion.root];
            float axisX=root[0]*motion.ax+root[1]*motion.ay+root[2]*motion.az;
            float axisY=root[4]*motion.ax+root[5]*motion.ay+root[6]*motion.az;
            float axisZ=root[8]*motion.ax+root[9]*motion.ay+root[10]*motion.az;
            for(int i=0;i<motion.parts.length;i++){
                Part part=motion.parts[i];float[] m=matrix[part.node];float lo=Float.POSITIVE_INFINITY,hi=Float.NEGATIVE_INFINITY;
                for(Poly source:part.fragments){Poly target=transformed[source.motionIndex];
                    target.nx=m[0]*source.nx+m[1]*source.ny+m[2]*source.nz;target.ny=m[4]*source.nx+m[5]*source.ny+m[6]*source.nz;target.nz=m[8]*source.nx+m[9]*source.ny+m[10]*source.nz;
                    for(int j=0;j<source.xyz.length;j+=3){float x=source.xyz[j],y=source.xyz[j+1],z=source.xyz[j+2];
                        float xx=m[0]*x+m[1]*y+m[2]*z+m[3],yy=m[4]*x+m[5]*y+m[6]*z+m[7],zz=m[8]*x+m[9]*y+m[10]*z+m[11];
                        target.xyz[j]=xx;target.xyz[j+1]=yy;target.xyz[j+2]=zz;bounds(part.node,xx,yy,zz);
                        float along=(xx-root[3])*axisX+(yy-root[7])*axisY+(zz-root[11])*axisZ;lo=Math.min(lo,along);hi=Math.max(hi,along);
                    }
                    target.depthBounds();depthMin=Math.min(depthMin,target.depthMin);depthMax=Math.max(depthMax,target.depthMax);
                    target.d=target.nx*target.xyz[0]+target.ny*target.xyz[1]+target.nz*target.xyz[2];
                }
                partRanges[i][0]=lo;partRanges[i][1]=hi;partOrder[i]=i;
            }
            for(int i=1;i<partOrder.length;i++){int id=partOrder[i],j=i;while(j>0 && partRanges[partOrder[j-1]][0]>partRanges[id][0]){partOrder[j]=partOrder[j-1];j--;}partOrder[j]=id;}
            for(int i=1;i<partOrder.length;i++)if(partRanges[partOrder[i-1]][1]>partRanges[partOrder[i]][0]+.01f)
                throw new IllegalStateException("Rigid motion parts overlap their separating axis: "+nodes[motion.parts[partOrder[i-1]].node].id+" / "+nodes[motion.parts[partOrder[i]].node].id+" at "+amount);
            boolean forward=-axisX+axisY+axisZ>=0;
            for(int i=0;i<partOrder.length;i++){
                Part part=motion.parts[partOrder[forward?i:partOrder.length-1-i]];float[] m=matrix[part.node];
                part.tree.order(this,-m[0]+m[4]+m[8],-m[1]+m[5]+m[9],-m[2]+m[6]+m[10]);
            }

        }
        private void bounds(int node,float x,float y,float z){
            if(worldBounds[node]==null)worldBounds[node]=new float[]{x,y,z,x,y,z};else{
                float[] b=worldBounds[node];b[0]=Math.min(b[0],x);b[1]=Math.min(b[1],y);b[2]=Math.min(b[2],z);b[3]=Math.max(b[3],x);b[4]=Math.max(b[4],y);b[5]=Math.max(b[5],z);
            }
            float px=RiverCamera.Q*(x+y),py=RiverCamera.Q*RiverCamera.DEPTH*(y-x)-z*Z_SCALE;
            if(projectedBounds[node]==null)projectedBounds[node]=new float[]{px,py,px,py};else{
                float[] b=projectedBounds[node];b[0]=Math.min(b[0],px);b[1]=Math.min(b[1],py);b[2]=Math.max(b[2],px);b[3]=Math.max(b[3],py);
            }
        }
        private Pose(float amount) {
            transform(amount);
            polygonCount=faces.length;
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
                depthMin=Math.min(depthMin,polygons[i].depthMin);depthMax=Math.max(depthMax,polygons[i].depthMax);
            }
        }
        private void transform(float amount) {
            for(int i=0;i<nodes.length;i++) {
                Node n=nodes[i];float a=n.pitchRate*amount,cy=(float)StrictMath.cos(n.yaw),sy=(float)StrictMath.sin(n.yaw),cp=(float)StrictMath.cos(a),sp=(float)StrictMath.sin(a);
                float[] m={cy*cp,-sy,-cy*sp,n.x+n.moveX*amount,sy*cp,cy,-sy*sp,n.y+n.moveY*amount,sp,0,cp,n.z+n.moveZ*amount};
                if(n.parent<0)matrix[i]=m;else {
                    float[] p=matrix[n.parent],o=matrix[i];
                    for(int r=0;r<3;r++){for(int c=0;c<3;c++)o[r*4+c]=p[r*4]*m[c]+p[r*4+1]*m[4+c]+p[r*4+2]*m[8+c];o[r*4+3]=p[r*4]*m[3]+p[r*4+1]*m[7]+p[r*4+2]*m[11]+p[r*4+3];}
                }
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
            for(int index=0;index<polygonCount;index++) {Poly f=polygons[index];
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
    /** Build immutable face ordering during level preparation, before active draws. */
    public void prepare(Pose pose) {
        if(pose.prepared)return;
        Stats build=new Stats();Branch tree=null;
        for(Poly f:pose.polygons)if(-f.nx+f.ny+f.nz>EPS) {
            if(tree==null)tree=new Branch(f);else tree.insert(f,build);
        }
        ArrayList<Poly> ordered=new ArrayList<Poly>();if(tree!=null)tree.flatten(ordered);
        // Retain final visible fragments only: discard construction tree, split intermediates,
        // back faces and original polygons replaced by fragments. Same painter order/UVs.
        pose.polygons=ordered.toArray(new Poly[ordered.size()]);pose.polygonCount=pose.polygons.length;pose.fragments=build.fragments;pose.prepared=true;
    }
    /** Opt-in material response for rigid scenery, evaluated on the prepared face geometry. */
    public interface FaceLighting {int shade(int ambient,int albedo,float x,float y,float z,float nx,float ny,float nz);}
    public Stats drawLit(Game g,Painter p,Pose pose,FaceLighting lighting){return drawRange(g,p,pose,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY,lighting);}
    public Stats draw(Game g,Painter p,Pose pose) {return drawRange(g,p,pose,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY);}
    /** Clip rigid faces to a ground-depth interval before merging billboard craft/ropes. */
    public Stats drawRange(Game g,Painter p,Pose pose,float min,float max) {return drawRange(g,p,pose,min,max,null);}
    private Stats drawRange(Game g,Painter p,Pose pose,float min,float max,FaceLighting lighting) {
        Stats stats=new Stats();stats.sourceFaces=faces.length;
        // Particle depth cursors visit many empty intervals: geometry cannot contribute here.
        if(pose.depthMax<=min || pose.depthMin>max)return stats;
        float[] alpha=new float[nodes.length];boolean[] visible=new boolean[nodes.length];
        for(int i=0;i<nodes.length;i++) {float[] b=pose.nodeBounds(g.layout.camera,i);visible[i]=b!=null && b[2]>=0 && b[0]<=g.layout.width && b[3]>=0 && b[1]<=g.layout.height;
            if(visible[i])alpha[i]=pose.opacity(g,p,i);else stats.culledNodes++;}
        for(int i=0;i<pose.polygonCount;i++){Poly f=pose.polygons[i];if(visible[f.node] && -f.nx+f.ny+f.nz>EPS)stats.visibleFaces++;}
        if(stats.visibleFaces==0)return stats;
        prepare(pose);
        stats.fragments=pose.fragments;
        FaceLighting scene=SceneryLighting.get(g);
        p.beginSurfaceBatch();
        try {
            for(int i=0;i<pose.polygonCount;i++){Poly f=pose.polygons[i];if(alpha[f.node]>0){Poly clipped=Branch.slice(f,min,max);if(clipped!=null){clipped.paint(p,g.layout.camera,alpha[f.node],scene,lighting,f.cx,f.cy,f.cz);stats.drawnFaces++;}}}
        } finally {p.endSurfaceBatch();}
        return stats;
    }
    private static final class Poly {
        final float[] xyz,uv;final int node,color,material;float nx,ny,nz,d,depthMin,depthMax,cx,cy,cz,sourceX,sourceY,sourceZ;int motionIndex,sourceIndex;
        Poly(float[] p,float[] texture,int n,int c,int m) {
            xyz=p;uv=texture;node=n;color=c;material=m;float ax=p[3]-p[0],ay=p[4]-p[1],az=p[5]-p[2],bx=p[6]-p[0],by=p[7]-p[1],bz=p[8]-p[2];
            float x=ay*bz-az*by,y=az*bx-ax*bz,z=ax*by-ay*bx,len=(float)StrictMath.sqrt(x*x+y*y+z*z);
            nx=x/len;ny=y/len;nz=z/len;d=nx*p[0]+ny*p[1]+nz*p[2];depthBounds();sourceX=cx;sourceY=cy;sourceZ=cz;
        }
        Poly(float[] p,float[] texture,Poly original) {xyz=p;uv=texture;node=original.node;color=original.color;material=original.material;sourceIndex=original.sourceIndex;sourceX=original.sourceX;sourceY=original.sourceY;sourceZ=original.sourceZ;nx=original.nx;ny=original.ny;nz=original.nz;d=original.d;depthBounds();}
        /** Geometry-derived bounds refresh only when a rigid pose changes, never per slice. */
        void depthBounds(){
            depthMin=Float.MAX_VALUE;depthMax=-Float.MAX_VALUE;cx=cy=cz=0;
            for(int i=0;i<xyz.length;i+=3){cx+=xyz[i];cy+=xyz[i+1];cz+=xyz[i+2];float depth=xyz[i+1]-xyz[i];depthMin=Math.min(depthMin,depth);depthMax=Math.max(depthMax,depth);}
            int count=xyz.length/3;cx/=count;cy/=count;cz/=count;
        }
        void paint(Painter p,RiverCamera c,float alpha,FaceLighting scene,FaceLighting lighting,float x,float y,float z) {
            float[] xy=new float[xyz.length/3*2];for(int i=0,j=0;i<xyz.length;i+=3,j+=2){xy[j]=c.screenX(xyz[i],xyz[i+1]);xy[j+1]=c.screenY(xyz[i],xyz[i+1])-xyz[i+2]*Z_SCALE*c.scale;}
            float light=p.lighting().face(nx,ny,nz);
            int r=(int)((color>>16&255)*light),g=(int)((color>>8&255)*light),b=(int)((color&255)*light);
            int shaded=((int)(255*alpha)<<24)|(r<<16)|(g<<8)|b;
            if(material!=SurfaceMaterial.EMISSIVE)shaded=SceneLighting.scale(shaded,p.lighting().sunColor>>16&255,p.lighting().sunColor>>8&255,p.lighting().sunColor&255);
            int ambient=material==SurfaceMaterial.EMISSIVE?shaded:material<0?p.lighting().scenery(shaded):p.lighting().surface(shaded,material);
            if(material!=SurfaceMaterial.EMISSIVE){
                ambient=scene.shade(ambient,color,x,y,z,nx,ny,nz);
                if(lighting!=null)ambient=lighting.shade(ambient,color,x,y,z,nx,ny,nz);
            }
            if(material<0)p.poly(xy,ambient);
            else p.surfacePolygon(xy,uv,material|SurfaceMaterial.LIT,ambient,material==SurfaceMaterial.CONCRETE?.12f:.04f);
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
        private static Poly slice(Poly f,float min,float max){
            float lo=f.depthMin,hi=f.depthMax;
            if(hi<=min || lo>max)return null;
            if(lo>=min && hi<=max)return f;
            Poly q=f;
            if(lo<min){float[] ds=new float[q.xyz.length/3];for(int i=0;i<ds.length;i++)ds[i]=q.xyz[i*3+1]-q.xyz[i*3]-min;q=clip(q,ds,true);}
            if(hi>max){float[] ds=new float[q.xyz.length/3];for(int i=0;i<ds.length;i++)ds[i]=q.xyz[i*3+1]-q.xyz[i*3]-max;q=clip(q,ds,false);}
            return q.xyz.length>=9?q:null;
        }
        void flatten(ArrayList<Poly> out) {
            boolean positive=-plane.nx+plane.ny+plane.nz>0;Branch far=positive?back:front,near=positive?front:back;
            if(far!=null)far.flatten(out);out.addAll(coplanar);if(near!=null)near.flatten(out);
        }
        void order(Pose pose,float vx,float vy,float vz){
            boolean positive=plane.nx*vx+plane.ny*vy+plane.nz*vz>0;Branch far=positive?back:front,near=positive?front:back;
            if(far!=null)far.order(pose,vx,vy,vz);
            for(Poly f:coplanar)if(f.nx*vx+f.ny*vy+f.nz*vz>EPS)pose.polygons[pose.polygonCount++]=pose.transformed[f.motionIndex];
            if(near!=null)near.order(pose,vx,vy,vz);
        }
    }
}
