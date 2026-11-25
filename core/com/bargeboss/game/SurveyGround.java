package com.bargeboss.game;

import java.util.HashMap;

/** Shared raised land surface for surveyed levels. Land-cover cells are clipped to dry land
 * once and kept; wide views use doubled cell tiers (as many as 650 cells per view) with their own
 * cache; area fills are triangulated by the level; street quads are clipped to land the first
 * time they are visible. Frames only cull and project. Cache size is bounded by the area the
 * camera can reach, never by time. */
final class SurveyGround {
    /** Per-level land palette. A negative material draws a flat scenery-lit polygon. */
    interface Style {
        int cover(float x,float y);
        int color(int kind);
        int material(int kind);
        float wetness(int kind);
    }
    /** Maximum cells per view before stepping to the next tier. */
    static final float MAX_VIEW_CELLS=650;
    /** Streets draw while the stepped cell is at most this size (wide views omit them). */
    static final float ROAD_MAX_STEP=256;
    private static final int TIERS=8;
    private static final float[][] NONE=new float[0][];
    /** Base land-cover cell: the survey's cover raster size, doubled per tier. */
    final float cell;
    private final HarborPolygonGeometry geometry;private final Style style;
    private final float[][] roads,roadBounds;private final int[] roadKinds;private final float[][][] roadPieces;
    private final float[][][] areas;private final float[][] areaBounds;private final int[] areaKinds;
    @SuppressWarnings("unchecked")
    private final HashMap<Long,Cell>[] tiers=new HashMap[TIERS];
    private static final class Cell {final float[][] pieces;final int kind;Cell(float[][] pieces,int kind){this.pieces=pieces;this.kind=kind;}}

    /** Roads are world quads already filtered against water; areas are triangle lists. */
    SurveyGround(HarborPolygonGeometry geometry,float cell,Style style,float[][] roads,int[] roadKinds,float[][][] areas,int[] areaKinds){
        this.geometry=geometry;this.cell=cell;this.style=style;this.roads=roads;this.roadKinds=roadKinds;this.areas=areas;this.areaKinds=areaKinds;
        roadBounds=new float[roads.length][];for(int i=0;i<roads.length;i++)roadBounds[i]=PolygonWater.bounds(roads[i]);
        roadPieces=new float[roads.length][][];
        areaBounds=new float[areas.length][];
        for(int i=0;i<areas.length;i++){float[] b={Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
            for(float[] t:areas[i])for(int k=0;k<t.length;k+=2){b[0]=Math.min(b[0],t[k]);b[1]=Math.min(b[1],t[k+1]);b[2]=Math.max(b[2],t[k]);b[3]=Math.max(b[3],t[k+1]);}
            areaBounds[i]=b;}
        for(int i=0;i<TIERS;i++)tiers[i]=new HashMap<Long,Cell>();
    }
    /** Visible world rectangle with a 32 px screen margin. */
    static float[] visible(Game g){RiverCamera c=g.layout.camera;float[] b={Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
        for(int i=0;i<4;i++){float sx=(i&1)==0?-32:g.layout.width+32,sy=(i&2)==0?-32:g.layout.height+32,x=c.worldX(sx,sy),y=c.worldY(sx,sy);
            b[0]=Math.min(b[0],x);b[1]=Math.min(b[1],y);b[2]=Math.max(b[2],x);b[3]=Math.max(b[3],y);}return b;}
    /** Smallest tier whose cells keep a view at or below MAX_VIEW_CELLS. */
    int tier(float[] b){int tier=0;float step=cell;while(tier<TIERS-1 && (b[2]-b[0])*(b[3]-b[1])/(step*step)>MAX_VIEW_CELLS){step*=2;tier++;}return tier;}
    private Cell cell(int tier,int col,int row){Long key=HarborPolygonGeometry.key(col,row);Cell cell=tiers[tier].get(key);
        if(cell==null){float step=this.cell*(1<<tier),x=col*step,y=row*step;float[][] dry=geometry.dry(x,y,x+step,y+step);
            cell=new Cell(dry.length==0?NONE:dry,style.cover(x+step*.5f,y+step*.5f));tiers[tier].put(key,cell);}
        return cell;}
    void draw(Game g,Painter p){float[] b=visible(g);int tier=tier(b);float step=cell*(1<<tier);
        int c0=(int)StrictMath.floor(b[0]/step),c1=(int)StrictMath.floor(b[2]/step),r0=(int)StrictMath.floor(b[1]/step),r1=(int)StrictMath.floor(b[3]/step);
        for(int col=c0;col<=c1;col++)for(int row=r0;row<=r1;row++){Cell cell=cell(tier,col,row);for(float[] dry:cell.pieces)surface(g,p,dry,cell.kind);}
        for(int i=0;i<areas.length;i++){float[] box=areaBounds[i];if(box[2]<b[0] || box[0]>b[2] || box[3]<b[1] || box[1]>b[3])continue;
            for(float[] t:areas[i])surface(g,p,t,areaKinds[i]);}
        if(step>ROAD_MAX_STEP)return;
        for(int i=0;i<roads.length;i++){float[] box=roadBounds[i];if(box[2]<b[0] || box[0]>b[2] || box[3]<b[1] || box[1]>b[3])continue;
            for(float[] piece:roadPieces(i))surface(g,p,piece,roadKinds[i]);}
    }
    private float[][] roadPieces(int i){float[][] pieces=roadPieces[i];if(pieces!=null)return pieces;
        float[] q=roads[i],box=roadBounds[i];java.util.ArrayList<float[]> out=new java.util.ArrayList<float[]>();
        for(float[] dry:geometry.dry(box[0],box[1],box[2],box[3])){float[] piece=PolygonWater.clip(q,dry);if(piece.length>=6)out.add(piece);}
        return roadPieces[i]=out.isEmpty()?NONE:out.toArray(new float[out.size()][]);}
    private void surface(Game g,Painter p,float[] world,int kind){float[] q=new float[world.length];RiverCamera c=g.layout.camera;float rise=GroundArt.LAND_RISE*c.scale;
        for(int i=0;i<world.length;i+=2){q[i]=c.screenX(world[i],world[i+1]);q[i+1]=c.screenY(world[i],world[i+1])-rise;}
        int material=style.material(kind),color=style.color(kind);
        if(material<0)p.poly(q,p.lighting().scenery(color));else p.surfacePolygon(q,world,material,color,style.wetness(kind));
    }
    int cachedCells(){int n=0;for(HashMap<Long,Cell> t:tiers)n+=t.size();return n;}
    /** Stage-entry warm-up: a small play box is clipped completely (cells and streets);
     * larger ones warm the base tier around the start so the opening frames never clip. */
    static final int FULL_WARM_CELLS=1200;
    void prepare(float minX,float minY,float maxX,float maxY,float startX,float startY){
        float cols=(float)StrictMath.ceil((maxX-minX)/cell)+1,rows=(float)StrictMath.ceil((maxY-minY)/cell)+1;
        if(cols*rows<=FULL_WARM_CELLS){
            for(int col=(int)StrictMath.floor(minX/cell);col<=(int)StrictMath.floor(maxX/cell);col++)
                for(int row=(int)StrictMath.floor(minY/cell);row<=(int)StrictMath.floor(maxY/cell);row++)cell(0,col,row);
            for(int i=0;i<roads.length;i++)roadPieces(i);
        }else warm(startX,startY,900);
    }
    /** Warm the cells around a point so the first frame does not clip them. */
    void warm(float x,float y,float radius){float[] b={x-radius,y-radius,x+radius,y+radius};
        for(int col=(int)StrictMath.floor(b[0]/cell);col<=(int)StrictMath.floor(b[2]/cell);col++)
            for(int row=(int)StrictMath.floor(b[1]/cell);row<=(int)StrictMath.floor(b[3]/cell);row++)cell(0,col,row);}

    /** Visible seawall faces of a survey coast ring; open play bounds are skipped. */
    static void walls(Game g,Painter p,SurveyLevel level){float[] box=visible(g),coast=level.coast();HarborPolygonGeometry geometry=level.geometry();
        RiverCamera c=g.layout.camera;float sign=PolygonWater.area(coast)>0?1:-1;
        for(int i=0;i<coast.length;i+=2){int next=(i+2)%coast.length;float x=coast[i],y=coast[i+1],xx=coast[next],yy=coast[next+1];
            if(Math.max(x,xx)<box[0] || Math.min(x,xx)>box[2] || Math.max(y,yy)<box[1] || Math.min(y,yy)>box[3] || !geometry.shoreEdge(x,y,xx,yy))continue;
            float dx=xx-x,dy=yy-y,len=Physics.length(dx,dy),nx=-dy/len*sign,ny=dx/len*sign;if(-nx+ny<=0)continue;
            float ax=c.screenX(x,y),ay=c.screenY(x,y),bx=c.screenX(xx,yy),by=c.screenY(xx,yy),h=GroundArt.LAND_RISE*c.scale;
            p.surfacePolygon(new float[]{ax,ay,bx,by,bx,by-h,ax,ay-h},new float[]{0,0,len,0,len,GroundArt.LAND_RISE,0,GroundArt.LAND_RISE},SurfaceMaterial.CORRUGATED,0xff73685e,.65f);
        }
    }
}
