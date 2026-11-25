package com.bargeboss.game;

/** Chicago River stage composition: dusk city ground, extruded towers, the Navy Pier
 * Centennial Wheel, harbor-lock sector gates and the wheel fire. Shared Java only. */
public final class ChicagoView {
    static final int WHEEL=ChicagoCity.COUNT,LEAVES=WHEEL+1,FIRES=LEAVES+4,COUNT=FIRES+ChicagoFire.PILES;
    private static final int CONCRETE=0xff4b4e53,PARK=0xff3f5a45,ROAD=0xff2c2f34,AVENUE=0xff33363c,PLAZA=0xff5b5a57;
    /** Hub height and rim radius in model units (2 world units per metre, 59.7 m wheel). */
    static final float HUB=64,RADIUS=54,GONDOLAS=42;
    private static final class Models {
        static final ArticulatedModel.Rigid WHEEL=wheel(),SUPPORT=support(),GONDOLA=gondola();
        static final ArticulatedModel.Rigid[] LEAF=leaves();
        /** Park polygons are concave; triangulate once, never during drawing. */
        static final float[][][] PARKS=parks();
        static final float[][] PARK_BOUNDS=parkBounds();
    }
    /** Dry land clipped once per survey cell and street quads filtered against water once;
     * frames only cull and project. Bounded by the fixed play box (about 380 cells). */
    private static final class Ground {
        static final float CELL=HarborPolygonGeometry.CELL;
        static final int COL0=(int)StrictMath.floor(ChicagoRiver.MIN_X/CELL),ROW0=(int)StrictMath.floor(ChicagoRiver.MIN_Y/CELL);
        static final int COLS=(int)StrictMath.floor(ChicagoRiver.MAX_X/CELL)-COL0+1,ROWS=(int)StrictMath.floor(ChicagoRiver.MAX_Y/CELL)-ROW0+1;
        static final float[][][] DRY=dry();
        static final float[][] ROADS;static final float[][] ROAD_BOUNDS;static final int[] ROAD_COLOR;
        static {java.util.ArrayList<float[]> q=new java.util.ArrayList<float[]>();java.util.ArrayList<int[]> meta=new java.util.ArrayList<int[]>();
            for(int n=0;n<ChicagoSurvey.ROADS.length;n++){float[] road=ChicagoSurvey.ROADS[n];int kind=ChicagoSurvey.ROAD_KIND[n];
                float width=kind==0?34:kind==1?24:kind==2?16:9;int color=kind==0?AVENUE:kind==3?PLAZA:ROAD;
                for(int i=2;i<road.length;i+=2){float x=road[i-2],y=road[i-1],xx=road[i],yy=road[i+1];
                    if(ChicagoRiver.contains((x+xx)*.5f,(y+yy)*.5f))continue;
                    float dx=xx-x,dy=yy-y,len=Physics.length(dx,dy);if(len<.1f)continue;float nx=-dy/len*width*.5f,ny=dx/len*width*.5f;
                    q.add(new float[]{x+nx,y+ny,xx+nx,yy+ny,xx-nx,yy-ny,x-nx,y-ny});meta.add(new int[]{color});}}
            ROADS=q.toArray(new float[q.size()][]);ROAD_BOUNDS=new float[ROADS.length][];ROAD_COLOR=new int[ROADS.length];
            for(int i=0;i<ROADS.length;i++){ROAD_BOUNDS[i]=PolygonWater.bounds(ROADS[i]);ROAD_COLOR[i]=meta.get(i)[0];}
        }
        private static float[][][] dry(){float[][][] out=new float[COLS*ROWS][][];HarborPolygonGeometry geo=ChicagoRiver.geometry();
            for(int col=0;col<COLS;col++)for(int row=0;row<ROWS;row++){float l=(COL0+col)*CELL,t=(ROW0+row)*CELL;out[col*ROWS+row]=geo.dry(l,t,l+CELL,t+CELL);}
            return out;}
    }
    private static float[][][] parks(){float[][][] out=new float[ChicagoSurvey.PARKS.length][][];for(int i=0;i<out.length;i++)out[i]=PolygonWater.triangles(ChicagoSurvey.PARKS[i]);return out;}
    private static float[][] parkBounds(){float[][] out=new float[ChicagoSurvey.PARKS.length][];for(int i=0;i<out.length;i++)out[i]=PolygonWater.bounds(ChicagoSurvey.PARKS[i]);return out;}
    private ChicagoView(){}
    static void prepare(){ChicagoRiver.geometry();Models.WHEEL.fragmentCount();Models.PARKS.getClass();Ground.DRY.getClass();LocationBridges.prepare(LocationBridges.chicago());}
    private static ArticulatedModel.Builder rigidRoot(){ArticulatedModel.Builder b=new ArticulatedModel.Builder();b.node("root",-1,0,0,0,0,0);return b;}
    private static ArticulatedModel.Rigid wheel(){ArticulatedModel.Builder b=rigidRoot();int steel=0xffd9dde0,led=0xff8fd4ff;
        int sides=36;
        for(int face=-1;face<=1;face+=2){float y=face*4;
            for(int i=0;i<sides;i++){double a=2*Math.PI*i/sides,aa=2*Math.PI*(i+1)/sides;
                float x0=(float)StrictMath.cos(a)*RADIUS,z0=(float)StrictMath.sin(a)*RADIUS,x1=(float)StrictMath.cos(aa)*RADIUS,z1=(float)StrictMath.sin(aa)*RADIUS;
                b.beam(0,x0,y,z0,x1,y,z1,2.2f,steel,SurfaceMaterial.STEEL);
                if(i%2==0)b.beam(0,0,y*.6f,0,x0,y,z0,.9f,steel,SurfaceMaterial.STEEL);
            }
            // Night LED ring just inside the rim.
            for(int i=0;i<sides;i+=2){double a=2*Math.PI*i/sides,aa=2*Math.PI*(i+2)/sides;float r=RADIUS-4;
                b.beam(0,(float)StrictMath.cos(a)*r,y,(float)StrictMath.sin(a)*r,(float)StrictMath.cos(aa)*r,y,(float)StrictMath.sin(aa)*r,1.1f,led,SurfaceMaterial.EMISSIVE);}
        }
        for(int i=0;i<sides;i+=3){double a=2*Math.PI*i/sides;b.beam(0,(float)StrictMath.cos(a)*RADIUS,-4,(float)StrictMath.sin(a)*RADIUS,(float)StrictMath.cos(a)*RADIUS,4,(float)StrictMath.sin(a)*RADIUS,1.2f,steel,SurfaceMaterial.STEEL);}
        b.box(0,0,0,0,9,12,9,0xff8f969c,SurfaceMaterial.STEEL);
        return b.build().rigid();
    }
    private static ArticulatedModel.Rigid support(){ArticulatedModel.Builder b=rigidRoot();int white=0xffcfd3d6;
        for(int side=-1;side<=1;side+=2)for(int leg=-1;leg<=1;leg+=2)b.beam(0,leg*30,side*12,0,0,side*6,HUB,3,white,SurfaceMaterial.STEEL);
        b.box(0,0,0,2,70,26,4,0xff6f7276,SurfaceMaterial.CONCRETE);
        b.box(0,-36,0,6,14,18,12,0xff9fa6ad,SurfaceMaterial.CONCRETE);
        return b.build().rigid();
    }
    private static ArticulatedModel.Rigid gondola(){ArticulatedModel.Builder b=rigidRoot();
        b.box(0,0,0,-4.5f,6,5,6,0xff3e6f9a,SurfaceMaterial.STEEL);
        b.box(0,0,0,-1,6.6f,5.6f,1,0xffe6e8ea,SurfaceMaterial.STEEL);
        b.box(0,0,0,-5,4.6f,5.2f,2.2f,0xffffd895,SurfaceMaterial.EMISSIVE);
        return b.build().rigid();
    }
    private static ArticulatedModel.Rigid[] leaves(){HarborLock lock=new HarborLock();ArticulatedModel.Rigid[] out=new ArticulatedModel.Rigid[4];
        float top=GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE+2;
        for(int leaf=0;leaf<4;leaf++){ArticulatedModel.Builder b=rigidRoot();float l=lock.leafLength[leaf];
            b.box(0,l*.5f,0,top*.5f,l,3.2f,top,0xff6e6158,SurfaceMaterial.RUST);
            for(int i=0;i<=4;i++)b.box(0,l*i/4f,0,top*.5f,1.6f,3.8f,top+1,0xff4f555a,SurfaceMaterial.STEEL);
            b.beam(0,0,-1.8f,top+3,l,-1.8f,top+3,.6f,0xffd8c27a,SurfaceMaterial.SAFETY);
            out[leaf]=b.build().rigid();}
        return out;
    }
    public static void draw(Game g,Painter p){
        p.rect(0,0,g.layout.width,g.layout.height,g.lighting()==LightingProfile.DUSK?0xff121820:g.lighting().water);
        WaveView.surface(g,p);ArcadeView.water(g,new RiverPainter(p,g.layout.camera));
        ShorelineView.water(g,p);LocationBridges.shadows(g,p);
        IndianaShoreTiles.walls(g,p,ChicagoRiver.COAST,ChicagoRiver.geometry());ground(g,p);
        ShorelineView.walls(g,p);
        VesselShoreLight.draw(g,p);
        Renderer.river(g,new RiverPainter(p,g.layout.camera));
    }
    private static void ground(Game g,Painter p){float[] b=IndianaShoreTiles.visible(g);RiverCamera c=g.layout.camera;
        int c0=Math.max(0,(int)StrictMath.floor(b[0]/Ground.CELL)-Ground.COL0),c1=Math.min(Ground.COLS-1,(int)StrictMath.floor(b[2]/Ground.CELL)-Ground.COL0);
        int r0=Math.max(0,(int)StrictMath.floor(b[1]/Ground.CELL)-Ground.ROW0),r1=Math.min(Ground.ROWS-1,(int)StrictMath.floor(b[3]/Ground.CELL)-Ground.ROW0);
        for(int col=c0;col<=c1;col++)for(int row=r0;row<=r1;row++)for(float[] dry:Ground.DRY[col*Ground.ROWS+row])surface(g,p,dry,CONCRETE,SurfaceMaterial.CONCRETE);
        for(int i=0;i<Models.PARKS.length;i++){float[] box=Models.PARK_BOUNDS[i];if(box[2]<b[0] || box[0]>b[2] || box[3]<b[1] || box[1]>b[3])continue;
            for(float[] tri:Models.PARKS[i])surface(g,p,tri,PARK,-1);}
        if(c.scale<.35f)return;
        for(int i=0;i<Ground.ROADS.length;i++){float[] box=Ground.ROAD_BOUNDS[i];if(box[2]<b[0] || box[0]>b[2] || box[3]<b[1] || box[1]>b[3])continue;
            surface(g,p,Ground.ROADS[i],Ground.ROAD_COLOR[i],SurfaceMaterial.CONCRETE);}
    }
    private static void surface(Game g,Painter p,float[] world,int color,int material){float[] q=new float[world.length];RiverCamera c=g.layout.camera;
        for(int i=0;i<world.length;i+=2){q[i]=c.screenX(world[i],world[i+1]);q[i+1]=c.screenY(world[i],world[i+1])-GroundArt.LAND_RISE*c.scale;}
        if(material<0)p.poly(q,p.lighting().scenery(color));else p.surfacePolygon(q,world,material,color,.12f);
    }
    /** Visible landmark ids in ground-depth order; static towers are presorted offline. */
    static int[] ordered(Game g){int[] ids=new int[COUNT];int n=0;
        for(int i=0;i<ChicagoCity.COUNT;i++)if(ChicagoCity.visible(g,i))ids[n++]=i;
        for(int i=WHEEL;i<COUNT;i++)ids[n++]=i;
        for(int i=1;i<n;i++){int id=ids[i],j=i;float d=depth(g,id);while(j>0 && depth(g,ids[j-1])>d){ids[j]=ids[j-1];j--;}ids[j]=id;}
        return java.util.Arrays.copyOf(ids,n);
    }
    static float depth(Game g,int i){RiverCamera c=g.layout.camera;
        if(i<WHEEL)return ChicagoCity.depth(g,i);
        if(i==WHEEL)return c.screenY(ChicagoRiver.WHEEL_X,ChicagoRiver.WHEEL_Y);
        if(i<FIRES){HarborLock lock=g.chicago.lock;int leaf=i-LEAVES;return c.screenY((lock.leafX(leaf)+lock.tipX(leaf))*.5f,(lock.leafY(leaf)+lock.tipY(leaf))*.5f);}
        int pile=i-FIRES;return g.chicago.fire.depth(g,pile)+60*c.scale;
    }
    static void landmark(Game g,Painter p,int i){
        if(i<WHEEL){ChicagoCity.draw(g,p,i,g.chicago.fades.alpha(i));return;}
        if(i==WHEEL){wheel(g,p);return;}
        if(i<FIRES){leaf(g,p,i-LEAVES);return;}
        g.chicago.fire.draw(g,p,i-FIRES);
    }
    /** One revolution per 90 s of shared animation time; gondolas stay level. */
    static float spin(Game g){return g.visualTicks*(float)(2*Math.PI/(90*60));}
    private static void wheel(Game g,Painter p){RiverCamera c=g.layout.camera;float x=ChicagoRiver.WHEEL_X,y=ChicagoRiver.WHEEL_Y;
        float sx=c.screenX(x,y),sy=c.screenY(x,y),pad=(RADIUS*2+40)*c.scale;
        if(sx<-pad || sx>g.layout.width+pad || sy<-pad || sy-(HUB+RADIUS)*c.scale>g.layout.height+pad)return;
        float deck=GroundArt.LAND_RISE,hub=deck+HUB*ArticulatedModel.Z_SCALE,spin=spin(g);
        Models.SUPPORT.drawSolid(g,p,x,y,deck,0,0,0);
        Models.WHEEL.drawSolid(g,p,x,y,hub,0,spin,0);
        // Same rigid rotation as the rim: a model point at angle t moves to t-spin.
        for(int i=0;i<GONDOLAS;i++){double a=2*Math.PI*i/GONDOLAS-spin;
            float gx=(float)StrictMath.cos(a)*(RADIUS+1),gz=(float)StrictMath.sin(a)*(RADIUS+1);
            Models.GONDOLA.drawSolid(g,p,x+gx,y,hub+gz*ArticulatedModel.Z_SCALE,0,0,0);}
    }
    private static void leaf(Game g,Painter p,int leaf){HarborLock lock=g.chicago.lock;
        Models.LEAF[leaf].drawSolid(g,p,lock.leafX(leaf),lock.leafY(leaf),0,lock.leafAngle(leaf),0,0);
        if(leaf%2==0){RiverCamera c=g.layout.camera;int gate=leaf/2;float hx=lock.leafX(leaf),hy=lock.leafY(leaf);
            float sx=c.screenX(hx,hy),sy=c.screenY(hx,hy)-(GroundArt.LAND_RISE+14)*c.scale;
            int color=lock.green(gate)?0xff59f08a:0xffff3b2e;
            p.material(sx,sy,14*c.scale,0,0,14*c.scale,EffectMaterial.GLOW,0,1,color);
            p.material(sx,sy,4*c.scale,0,0,4*c.scale,EffectMaterial.DOT,0,1,0xffffffff);}
    }
    static void lights(Game g,SceneryLighting out){
        HarborLock lock=g.chicago.lock;
        for(int k=0;k<2;k++)out.add(lock.leafX(k*2),lock.leafY(k*2),GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE+18,120,.6f,lock.green(k)?0xff59f08a:0xffff3b2e);
        out.add(ChicagoRiver.WHEEL_X,ChicagoRiver.WHEEL_Y,GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE+HUB,200,.5f,0xff8fd4ff);
        g.chicago.fire.lights(g,out);
    }
    /** Building fades for towers that cover the tug or its tow; advanced at 60 Hz. */
    static final class Fades {
        private static final float OUT=.72f/18,IN=.72f/27,MIN=.2f;
        private final float[] alpha=new float[ChicagoCity.COUNT];
        private final float[] box=new float[4];
        Fades(){java.util.Arrays.fill(alpha,1);}
        float alpha(int i){return alpha[i];}
        void step(Game g){RiverCamera c=g.layout.camera;float w=g.layout.width,h=g.layout.height;
            for(int i=0;i<alpha.length;i++){float target=1;ChicagoCity.screenBox(g,i,box);
                if(box[2]>=-8 && box[0]<=w+8 && box[3]>=-8 && box[1]<=h+8){float depth=ChicagoCity.depth(g,i);
                    if(covers(g,i,g.tug,depth))target=MIN;
                    else for(Body b:g.cargo)if(b!=null && b.active && covers(g,i,b,depth)){target=MIN;break;}}
                if(target!=alpha[i])alpha[i]+=Physics.clamp(target-alpha[i],-OUT,IN);
            }
        }
        /** A tower fades only when it stands in front of a hull and its projected solid covers
         * the hull or a narrow margin around it. Nearby towers that do not hide it stay solid. */
        private boolean covers(Game g,int i,Body b,float depth){RiverCamera c=g.layout.camera;float x=c.screenX(b.x,b.y),y=c.screenY(b.x,b.y);
            if(y>=depth)return false;float r=(b.radius+8)*c.scale,up=(b.radius+20)*c.scale;
            if(x+r<box[0] || x-r>box[2] || y+r*.5f<box[1] || y-up>box[3])return false;
            return ChicagoCity.covers(g,i,x,y) || ChicagoCity.covers(g,i,x-r,y) || ChicagoCity.covers(g,i,x+r,y)
                    || ChicagoCity.covers(g,i,x,y-up) || ChicagoCity.covers(g,i,x,y+r*.5f);}
    }
}
