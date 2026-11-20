package com.bargeboss.game;

/** Survey land-use tiles. Only visible cells draw; wide views aggregate cells before clipping. */
final class IndianaShoreTiles {
    private static final int[] COLOR={0xff50604c,0xff928d7d,0xff88796a,0xffa29272,0xff625d53,0xff596574,0xff444849};
    private IndianaShoreTiles(){}
    static int kind(float x,float y){int col=(int)StrictMath.floor((x-IndianaSurvey.MIN_X)/IndianaSurvey.CELL),row=(int)StrictMath.floor((y-IndianaSurvey.MIN_Y)/IndianaSurvey.CELL);
        return row<0||col<0||row>=IndianaSurvey.ROWS||col>=IndianaSurvey.COLS?0:IndianaSurvey.COVER[row].charAt(col)-'0';}
    static float[] visible(Game g){RiverCamera c=g.layout.camera;float[] b={Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
        for(int i=0;i<4;i++){float x=c.worldX((i&1)==0?-32:g.layout.width+32,(i&2)==0?-32:g.layout.height+32),y=c.worldY((i&1)==0?-32:g.layout.width+32,(i&2)==0?-32:g.layout.height+32);b[0]=Math.min(b[0],x);b[1]=Math.min(b[1],y);b[2]=Math.max(b[2],x);b[3]=Math.max(b[3],y);}return b;}
    static void ground(Game g,Painter p){float[] b=visible(g);float step=IndianaSurvey.CELL;
        while((b[2]-b[0])*(b[3]-b[1])/(step*step)>650)step*=2;
        float left=(float)StrictMath.floor(b[0]/step)*step,top=(float)StrictMath.floor(b[1]/step)*step;
        for(float x=left;x<b[2];x+=step)for(float y=top;y<b[3];y+=step){int kind=kind(x+step*.5f,y+step*.5f);
            for(float[] dry:IndianaHarbor.geometry().dry(x,y,x+step,y+step))surface(g,p,dry,kind);
        }
        if(step<=256)roads(g,p,b);
    }
    private static void surface(Game g,Painter p,float[] world,int kind){float[] projected=new float[world.length];RiverCamera c=g.layout.camera;
        for(int i=0;i<world.length;i+=2){projected[i]=c.screenX(world[i],world[i+1]);projected[i+1]=c.screenY(world[i],world[i+1])-GroundArt.LAND_RISE*c.scale;}
        if(kind==0)p.poly(projected,p.lighting().scenery(COLOR[0]));
        else p.surfacePolygon(projected,world,kind==4?SurfaceMaterial.RUST:SurfaceMaterial.CONCRETE,COLOR[kind],.14f);
    }
    private static void roads(Game g,Painter p,float[] bounds){for(int n=0;n<IndianaSurvey.ROADS.length;n++){
        float[] road=IndianaSurvey.ROADS[n];int kind=IndianaSurvey.ROADS_KIND[n];float width=kind==4?7:15;
        for(int i=2;i<road.length;i+=2){float x=road[i-2],y=road[i-1],xx=road[i],yy=road[i+1];
            if(Math.max(x,xx)<bounds[0] || Math.min(x,xx)>bounds[2] || Math.max(y,yy)<bounds[1] || Math.min(y,yy)>bounds[3])continue;
            if(IndianaHarbor.contains((x+xx)*.5f,(y+yy)*.5f))continue;
            float dx=xx-x,dy=yy-y,len=Physics.length(dx,dy);if(len<.1f)continue;float nx=-dy/len*width*.5f,ny=dx/len*width*.5f;
            float[] q={x+nx,y+ny,xx+nx,yy+ny,xx-nx,yy-ny,x-nx,y-ny};float[] box=PolygonWater.bounds(q);
            for(float[] dry:IndianaHarbor.geometry().dry(box[0],box[1],box[2],box[3])){float[] piece=PolygonWater.clip(q,dry);if(piece.length>=6)surface(g,p,piece,kind);}
        }
    }}
    static void walls(Game g,Painter p){float[] box=visible(g),coast=IndianaHarbor.COAST;RiverCamera c=g.layout.camera;float sign=PolygonWater.area(coast)>0?1:-1;
        for(int i=0;i<coast.length;i+=2){int next=(i+2)%coast.length;float x=coast[i],y=coast[i+1],xx=coast[next],yy=coast[next+1];
            if(Math.max(x,xx)<box[0] || Math.min(x,xx)>box[2] || Math.max(y,yy)<box[1] || Math.min(y,yy)>box[3] || !IndianaHarbor.geometry().shoreEdge(x,y,xx,yy))continue;
            float dx=xx-x,dy=yy-y,len=Physics.length(dx,dy),nx=-dy/len*sign,ny=dx/len*sign;if(-nx+ny<=0)continue;
            float ax=c.screenX(x,y),ay=c.screenY(x,y),bx=c.screenX(xx,yy),by=c.screenY(xx,yy),h=GroundArt.LAND_RISE*c.scale;
            p.surfacePolygon(new float[]{ax,ay,bx,by,bx,by-h,ax,ay-h},new float[]{0,0,len,0,len,GroundArt.LAND_RISE,0,GroundArt.LAND_RISE},SurfaceMaterial.CORRUGATED,0xff73685e,.65f);
        }
    }
}
