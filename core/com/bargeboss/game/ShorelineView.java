package com.bargeboss.game;

import java.util.ArrayList;

/** Live shoreline response over immutable, short shore strips. Never part of StaticTerrain. */
public final class ShorelineView {
    static final float MAX_LENGTH=48,FOAM_WIDTH=12,WET_HEIGHT=6;
    private static final class PracticeSegments {static final Segment[] DATA=calumet();}
    private static final class RiverSegments {static final Segment[] DATA=polygon(CalumetRiver.COAST,2);static final HarborPolygonGeometry.Index INDEX=index(DATA);}
    private static final class IndianaSegments {static final Segment[] DATA=polygon(IndianaHarbor.COAST,3);static final HarborPolygonGeometry.Index INDEX=index(DATA);}
    private static final class ChicagoSegments {static final Segment[] DATA=polygon(ChicagoRiver.COAST,4);static final HarborPolygonGeometry.Index INDEX=index(DATA);}
    private static final class MarinaSegments {static final Segment[] DATA=marina();}
    private ShorelineView(){}
    /** Prepare fixed level geometry during reset, outside active drawing. */
    public static void prepare(boolean marina){segments(marina);if(marina)DockTopLight.prepare();}
    public static void prepare(Game g){
        segments(g);if(g.montroseStage())DockTopLight.prepare();
        else if(g.calumetStage())CalumetWaterGeometry.prepare();
        else if(g.indianaStage())IndianaHarbor.geometry();
        else if(g.chicagoStage())ChicagoRiver.geometry();
    }
    static final class Segment {
        final float ax,ay,bx,by,nx,ny,u0,u1,rise,height;
        final boolean wallVisible;
        final float[] foam,foamUV,wallUV,projectedFoam=new float[8],projectedWall=new float[8];
        Segment(float x,float y,float xx,float yy,float normalX,float normalY,float u,float end,float height){
            ax=x;ay=y;bx=xx;by=yy;nx=normalX;ny=normalY;u0=u;u1=end;rise=Math.min(WET_HEIGHT,height);this.height=height;
            wallVisible=-nx+ny>.001f;
            foam=new float[]{ax,ay,bx,by,bx+nx*FOAM_WIDTH,by+ny*FOAM_WIDTH,ax+nx*FOAM_WIDTH,ay+ny*FOAM_WIDTH};
            foamUV=new float[]{u0,0,u1,0,u1,FOAM_WIDTH,u0,FOAM_WIDTH};
            wallUV=new float[]{u0,0,u1,0,u1,rise,u0,rise};
        }
    }
    private static Segment[] calumet(){ArrayList<Segment> out=new ArrayList<Segment>();float[] bank=TrainingChannel.WATER_BOUNDARY;float u=0;
        for(int i=0;i<bank.length;i+=2){int next=(i+2)%bank.length;u=edge(out,bank[i],bank[i+1],bank[next],bank[next+1],u,GroundArt.LAND_RISE,false);}
        return out.toArray(new Segment[out.size()]);
    }
    private static Segment[] marina(){ArrayList<Segment> out=new ArrayList<Segment>();float[] coast=MontroseHarbor.COAST;float u=0;
        for(int i=0;i<coast.length-2;i+=2){float x=(coast[i]+coast[i+2])*.5f,y=(coast[i+1]+coast[i+3])*.5f;
            u=edge(out,coast[i],coast[i+1],coast[i+2],coast[i+3],u,MontroseShoreArt.riseAt(x,y),true);}
        // DOCK_EDGES already excludes internal seams between overlapping deck pieces.
        for(float[] e:MontroseShoreArt.DOCK_EDGES)u=edge(out,e[0],e[1],e[2],e[3],u,MontroseHarbor.DOCK_RISE,true);
        return out.toArray(new Segment[out.size()]);
    }
    private static boolean water(int stage,float x,float y){return stage==1?MontroseHarbor.contains(x,y):stage==2?CalumetRiver.contains(x,y):stage==3?IndianaHarbor.contains(x,y):stage==4?ChicagoRiver.contains(x,y):CalumetMap.contains(x,y);}
    private static Segment[] polygon(float[] bank,int stage){ArrayList<Segment> out=new ArrayList<Segment>();float u=0;
        HarborPolygonGeometry geo=stage==2?CalumetWaterGeometry.geometry():stage==4?ChicagoRiver.geometry():IndianaHarbor.geometry();
        for(int i=0;i<bank.length;i+=2){int j=(i+2)%bank.length;if(geo.shoreEdge(bank[i],bank[i+1],bank[j],bank[j+1]))u=edge(out,bank[i],bank[i+1],bank[j],bank[j+1],u,GroundArt.LAND_RISE,stage);}
        return out.toArray(new Segment[out.size()]);
    }
    private static HarborPolygonGeometry.Index index(Segment[] s){float[][] boxes=new float[s.length][];
        for(int i=0;i<s.length;i++)boxes[i]=new float[]{Math.min(s[i].ax,s[i].bx)-FOAM_WIDTH,Math.min(s[i].ay,s[i].by)-FOAM_WIDTH,Math.max(s[i].ax,s[i].bx)+FOAM_WIDTH,Math.max(s[i].ay,s[i].by)+FOAM_WIDTH};
        return new HarborPolygonGeometry.Index(boxes);
    }
    private static float edge(ArrayList<Segment> out,float ax,float ay,float bx,float by,float u,float rise,boolean marina){return edge(out,ax,ay,bx,by,u,rise,marina?1:0);}
    private static float edge(ArrayList<Segment> out,float ax,float ay,float bx,float by,float u,float rise,int stage){
        float dx=bx-ax,dy=by-ay,length=Physics.length(dx,dy);if(length<.01f)return u;
        float nx=-dy/length,ny=dx/length;
        int count=Math.max(1,(int)StrictMath.ceil(length/MAX_LENGTH));
        for(int i=0;i<count;i++){float a=i/(float)count,b=(i+1)/(float)count;
            float x=ax+dx*a,y=ay+dy*a,xx=ax+dx*b,yy=ay+dy*b,mx=(x+xx)*.5f,my=(y+yy)*.5f;
            float normalX=nx,normalY=ny;
            if(!water(stage,mx+normalX*.5f,my+normalY*.5f)){normalX=-normalX;normalY=-normalY;}
            // A dock root may cover a short coast section; omit that buried receiver.
            if(!water(stage,mx+normalX*.5f,my+normalY*.5f))continue;
            out.add(new Segment(x,y,xx,yy,normalX,normalY,u+length*a,u+length*b,rise));}
        return u+length;
    }
    static Segment[] segments(boolean marina){return marina?MarinaSegments.DATA:PracticeSegments.DATA;}
    static Segment[] segments(Game g){return g.calumetStage()?RiverSegments.DATA:g.indianaStage()?IndianaSegments.DATA:g.chicagoStage()?ChicagoSegments.DATA:segments(g.montroseStage());}
    static Segment[] near(Game g,float l,float t,float r,float b){
        if(!g.calumetStage() && !g.surveyStage())return segments(g);
        Segment[] all=segments(g);HarborPolygonGeometry.Index index=g.calumetStage()?RiverSegments.INDEX:g.chicagoStage()?ChicagoSegments.INDEX:IndianaSegments.INDEX;
        int[] candidates=index.area(l,t,r,b);Segment[] result=new Segment[candidates.length];for(int i=0;i<result.length;i++)result[i]=all[candidates[i]];return result;
    }
    private static Segment[] visible(Game g){RiverCamera c=g.layout.camera;float l=Float.MAX_VALUE,t=l,r=-l,b=-l;
        for(int i=0;i<4;i++){float sx=i%2==0?-32:g.layout.width+32,sy=i<2?-32:g.layout.height+32+GroundArt.LAND_RISE*c.scale;
            float x=c.worldX(sx,sy),y=c.worldY(sx,sy);l=Math.min(l,x);t=Math.min(t,y);r=Math.max(r,x);b=Math.max(b,y);}
        return near(g,l,t,r,b);
    }
    static float phase(Game g){return g.visualTicks*Physics.DT;}
    static float energy(Game g,Segment s){return Physics.clamp(g.water.energy(g,(s.ax+s.bx)*.5f+s.nx*6,(s.ay+s.by)*.5f+s.ny*6,null)/1.8f,0,1);}
    private static boolean project(Game g,Segment s,boolean wall){
        RiverCamera c=g.layout.camera;float[] out=wall?s.projectedWall:s.projectedFoam;
        float l=Float.MAX_VALUE,t=l,r=-l,b=-l;
        for(int i=0;i<8;i+=2){float x,y;
            if(wall){boolean end=i==2 || i==4;x=end?s.bx:s.ax;y=end?s.by:s.ay;}else{x=s.foam[i];y=s.foam[i+1];}
            float sx=c.screenX(x,y),sy=c.screenY(x,y)-(wall && i>=4?s.rise*c.scale:0);
            out[i]=sx;out[i+1]=sy;l=Math.min(l,sx);r=Math.max(r,sx);t=Math.min(t,sy);b=Math.max(b,sy);
        }
        return r>=-2 && l<=g.layout.width+2 && b>=-2 && t<=g.layout.height+2;
    }
    private static float detail(Game g,Segment s){
        return g.montroseStage() && s.height<=MontroseHarbor.DOCK_RISE
                ?SceneDetail.dockWater(g,(s.ax+s.bx)*.5f,(s.ay+s.by)*.5f):1;
    }
    /** Before opaque ground: raised tops and walls cover corner overlaps and landward pixels. */
    public static void water(Game g,Painter p){for(Segment s:visible(g)){
        float detail=detail(g,s);if(detail<.01f || !project(g,s,false))continue;
        p.shoreline(s.projectedFoam,s.foamUV,phase(g),energy(g,s),0,((int)(181*detail)<<24)|0xb9c9b1);
    }}
    /** After opaque walls, before top receivers/props/vessels: never paints foam over a hull. */
    public static void walls(Game g,Painter p){
        p.pushClipOutside(ShoreTopClip.get(g));
        try {for(Segment s:visible(g)){
            float detail=detail(g,s);if(detail<.01f || !s.wallVisible || !project(g,s,true))continue;
            p.shoreline(s.projectedWall,s.wallUV,phase(g),energy(g,s),1,((int)(176*detail)<<24)|0x1d393c);
        }} finally {p.popClip();}
    }
}
