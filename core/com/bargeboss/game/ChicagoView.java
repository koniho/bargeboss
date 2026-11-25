package com.bargeboss.game;

/** Chicago River stage composition: dusk city ground, extruded towers, the Navy Pier
 * Centennial Wheel, harbor-lock sector gates and the wheel fire. Shared Java only. */
public final class ChicagoView {
    static final int WHEEL=ChicagoCity.COUNT,LEAVES=WHEEL+1,FIRES=LEAVES+4,COUNT=FIRES+ChicagoFire.PILES;
    private static final int CONCRETE=0xff4b4e53,PARK=0xff3f5a45,ROAD=0xff2c2f34,AVENUE=0xff33363c,PLAZA=0xff5b5a57;
    /** Survey world units per metre; stage art that follows the map is sized in metres. */
    private static final float U=ChicagoSurvey.UNITS_PER_METRE;
    /** Hub height and rim radius in model units (59.7 m wheel). */
    static final float HUB=32*U,RADIUS=27*U,GONDOLAS=42;
    private static final class Models {
        static final ArticulatedModel.Rigid WHEEL=wheel(),SUPPORT=support(),GONDOLA=gondola();
        static final ArticulatedModel.Rigid[] LEAF=leaves();
    }
    /** Concrete city land, triangulated parks (concave, triangulated once) and streets for the
     * shared survey ground cache. Street kinds: 0 avenue, 1-2 street, 3 plaza/pedestrian. */
    static SurveyGround ground(){
        final int LAND=0,GREEN=1,AVE=2,STREET=3,SQUARE=4;
        java.util.ArrayList<float[]> quads=new java.util.ArrayList<float[]>();java.util.ArrayList<Integer> kinds=new java.util.ArrayList<Integer>();
        for(int n=0;n<ChicagoSurvey.ROADS.length;n++){float[] road=ChicagoSurvey.ROADS[n];int kind=ChicagoSurvey.ROAD_KIND[n];
            float width=(kind==0?17:kind==1?12:kind==2?8:4.5f)*U;int style=kind==0?AVE:kind==3?SQUARE:STREET;
            for(int i=2;i<road.length;i+=2){float x=road[i-2],y=road[i-1],xx=road[i],yy=road[i+1];
                if(ChicagoRiver.contains((x+xx)*.5f,(y+yy)*.5f))continue;
                float dx=xx-x,dy=yy-y,len=Physics.length(dx,dy);if(len<.1f)continue;float nx=-dy/len*width*.5f,ny=dx/len*width*.5f;
                quads.add(new float[]{x+nx,y+ny,xx+nx,yy+ny,xx-nx,yy-ny,x-nx,y-ny});kinds.add(style);}}
        int[] roadKinds=new int[kinds.size()];for(int i=0;i<roadKinds.length;i++)roadKinds[i]=kinds.get(i);
        float[][][] parks=new float[ChicagoSurvey.PARKS.length][][];int[] parkKinds=new int[parks.length];
        for(int i=0;i<parks.length;i++){parks[i]=PolygonWater.triangles(ChicagoSurvey.PARKS[i]);parkKinds[i]=GREEN;}
        final int[] colors={CONCRETE,PARK,AVENUE,ROAD,PLAZA};
        return new SurveyGround(ChicagoRiver.geometry(),HarborPolygonGeometry.CELL,new SurveyGround.Style(){
            public int cover(float x,float y){return LAND;}
            public int color(int kind){return colors[kind];}
            public int material(int kind){return kind==GREEN?-1:SurfaceMaterial.CONCRETE;}
            public float wetness(int kind){return .12f;}
        },quads.toArray(new float[quads.size()][]),roadKinds,parks,parkKinds);
    }
    private ChicagoView(){}
    static void prepare(){Models.WHEEL.fragmentCount();Models.LEAF.getClass();ChicagoFire.prepare();}
    private static ArticulatedModel.Builder rigidRoot(){ArticulatedModel.Builder b=new ArticulatedModel.Builder();b.node("root",-1,0,0,0,0,0);return b;}
    private static ArticulatedModel.Rigid wheel(){ArticulatedModel.Builder b=rigidRoot();int steel=0xffd9dde0,led=0xff8fd4ff;
        int sides=36;
        for(int face=-1;face<=1;face+=2){float y=face*2*U;
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
        for(int side=-1;side<=1;side+=2)for(int leg=-1;leg<=1;leg+=2)b.beam(0,leg*15*U,side*6*U,0,0,side*3*U,HUB,3,white,SurfaceMaterial.STEEL);
        b.box(0,0,0,2,35*U,13*U,4,0xff6f7276,SurfaceMaterial.CONCRETE);
        b.box(0,-18*U,0,6,7*U,9*U,12,0xff9fa6ad,SurfaceMaterial.CONCRETE);
        return b.build().rigid();
    }
    private static ArticulatedModel.Rigid gondola(){ArticulatedModel.Builder b=rigidRoot();
        float s=U*.5f; // sized at 2 units per metre
        b.box(0,0,0,-4.5f*s,6*s,5*s,6*s,0xff3e6f9a,SurfaceMaterial.STEEL);
        b.box(0,0,0,-1*s,6.6f*s,5.6f*s,1*s,0xffe6e8ea,SurfaceMaterial.STEEL);
        b.box(0,0,0,-5*s,4.6f*s,5.2f*s,2.2f*s,0xffffd895,SurfaceMaterial.EMISSIVE);
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
    /** Conservative screen rectangles: towers from their extrusion, the wheel from hub and
     * rim, lock leaves from hinge/tip, fires from plume height. */
    static boolean box(Game g,int i,float[] out){RiverCamera c=g.layout.camera;
        if(i<WHEEL){ChicagoCity.screenBox(g,i,out);return true;}
        float x,y,reach,up;
        if(i==WHEEL){x=ChicagoRiver.WHEEL_X;y=ChicagoRiver.WHEEL_Y;reach=(RADIUS+20)*c.scale;up=(GroundArt.LAND_RISE+(HUB+RADIUS+12)*ArticulatedModel.Z_SCALE)*c.scale;}
        else if(i<FIRES){HarborLock lock=g.chicago.lock;int leaf=i-LEAVES;float ax=c.screenX(lock.leafX(leaf),lock.leafY(leaf)),ay=c.screenY(lock.leafX(leaf),lock.leafY(leaf)),bx=c.screenX(lock.tipX(leaf),lock.tipY(leaf)),by=c.screenY(lock.tipX(leaf),lock.tipY(leaf)),pad=24*c.scale;
            out[0]=Math.min(ax,bx)-pad;out[2]=Math.max(ax,bx)+pad;out[1]=Math.min(ay,by)-(GroundArt.LAND_RISE+30)*c.scale;out[3]=Math.max(ay,by)+pad;return true;}
        else{int pile=i-FIRES;x=ChicagoFire.x(pile);y=ChicagoFire.y(pile);reach=60*c.scale;up=(GroundArt.LAND_RISE+260)*c.scale;}
        float sx=c.screenX(x,y),sy=c.screenY(x,y);out[0]=sx-reach;out[2]=sx+reach;out[1]=sy-up;out[3]=sy+reach;return true;
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
        // Lower south rim (toward the pier wall) smokes while the pavilions burn.
        float rx=x-RADIUS*.6f;g.chicago.fire.wheelSmoke(g,p,c.screenX(rx,y),c.screenY(rx,y)-(hub-RADIUS*.8f*ArticulatedModel.Z_SCALE)*c.scale);
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
