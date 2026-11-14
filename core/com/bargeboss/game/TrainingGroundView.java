package com.bargeboss.game;

/** Raised industrial ground and corrugated steel retaining walls, sharing exact water geometry. */
public final class TrainingGroundView {
    private TrainingGroundView() {}
    public static void ground(Game g,Painter canvas) {
        RiverCamera c=g.layout.camera;
        Painter raised=new RiverPainter(GroundArt.raisedCanvas(g,canvas),c);
        float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        for(int corner=0;corner<4;corner++) {
            float sx=corner%2*g.layout.width,sy=corner/2*g.layout.height+GroundArt.LAND_RISE*c.scale;
            float x=c.worldX(sx,sy),y=c.worldY(sx,sy);
            minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
        }
        int firstX=(int)StrictMath.floor(minX/64)-1,lastX=(int)StrictMath.ceil(maxX/64)+1;
        int firstY=(int)StrictMath.floor(minY/64)-1,lastY=(int)StrictMath.ceil(maxY/64)+1;
        for(int row=firstY;row<lastY;row++) for(int col=firstX;col<lastX;col++) {
            if(!tileVisible(g,col,row,true)) continue;
            float x=col*64,y=row*64;
            if(!intersectsWater(x,y,64,64)) {
                raised.rect(x,y,64,64,GroundArt.BASE);
                GroundArt.tile(g,canvas,x+32,y+32);
            } else landPiece(raised,x,y,64,64,0);
        }
    }
    /** The inverse-view rectangle also contains large offscreen corners. Reject them
     * before water tests or painter calls, retaining the complete source-image bounds. */
    static boolean tileVisible(Game g,int col,int row,boolean texture) {
        RiverCamera c=g.layout.camera;
        float sx=c.screenX(col*64+32,row*64+32),sy=c.screenY(col*64+32,row*64+32)-GroundArt.LAND_RISE*c.scale;
        float left,right,top,bottom;
        if(texture) {
            float size=GroundArt.SIZE*c.scale;
            left=sx-size*GroundArt.PIVOT_X;right=sx+size*(1-GroundArt.PIVOT_X);
            top=sy-size*GroundArt.PIVOT_Y;bottom=sy+size*(1-GroundArt.PIVOT_Y);
        } else {
            float rx=64*RiverCamera.Q*c.scale,ry=rx*RiverCamera.DEPTH;
            left=sx-rx;right=sx+rx;top=sy-ry;bottom=sy+ry;
        }
        // Include curb stroke width and raster antialiasing at the viewport edge.
        float margin=2*c.scale+2;
        return right>=-margin && left<=g.layout.width+margin && bottom>=-margin && top<=g.layout.height+margin;
    }
    static boolean intersectsWater(float x,float y,float width,float height) {
        for(float[] r:TrainingChannel.WATER_RECTANGLES)
            if(x<r[2] && x+width>r[0] && y<r[3] && y+height>r[1]) return true;
        return false;
    }
    /** Subtract water exactly. Partial shore cells receive concrete, never a texture spilling into water. */
    private static void landPiece(Painter p,float x,float y,float width,float height,int from) {
        if(width<=0 || height<=0) return;
        for(int i=from;i<TrainingChannel.WATER_RECTANGLES.length;i++) {
            float[] r=TrainingChannel.WATER_RECTANGLES[i];
            float left=Math.max(x,r[0]),right=Math.min(x+width,r[2]);
            float top=Math.max(y,r[1]),bottom=Math.min(y+height,r[3]);
            if(right<=left || bottom<=top) continue;
            landPiece(p,x,y,width,top-y,i+1);
            landPiece(p,x,bottom,width,y+height-bottom,i+1);
            landPiece(p,x,top,left-x,bottom-top,i+1);
            landPiece(p,right,top,x+width-right,bottom-top,i+1);
            return;
        }
        p.rect(x,y,width,height,GroundArt.BASE);
        // A few slab joints keep the untextured shoreline apron readable at game scale.
        if(width>12 && height>12) p.line(x+2,y+height-2,x+width-2,y+height-2,.55f,0x405e6460);
    }
    public static void walls(Game g,Painter canvas) {
        float[] bank=TrainingChannel.WATER_BOUNDARY;
        for(int i=0;i<bank.length;i+=2) {
            int next=(i+2)%bank.length;
            wall(g,canvas,bank[i],bank[i+1],bank[next],bank[next+1],i,false);
        }
    }
    /** Caps are placed after land covers wall backs at concave corners. */
    public static void caps(Game g,Painter canvas) {
        float[] bank=TrainingChannel.WATER_BOUNDARY;
        for(int i=0;i<bank.length;i+=2) {
            int next=(i+2)%bank.length;
            wall(g,canvas,bank[i],bank[i+1],bank[next],bank[next+1],i,true);
        }
    }
    /** Foreground caps may overlap hull artwork; only overlapping painted sections become translucent. */
    public static void foreground(Game g,Painter canvas) {
        float[] bank=TrainingChannel.WATER_BOUNDARY;
        for(int i=0;i<bank.length;i+=2) {
            int next=(i+2)%bank.length;
            float x=bank[i],y=bank[i+1],xx=bank[next],yy=bank[next+1];
            if((xx-x)+(yy-y)<0) wall(g,canvas,x,y,xx,yy,i,true);
        }
    }
    private static void wall(Game g,Painter canvas,float x,float y,float xx,float yy,int seed,boolean capOnly) {
        RiverCamera c=g.layout.camera;
        float dx=xx-x,dy=yy-y,length=Physics.length(dx,dy);if(length<.01f) return;
        dx/=length;dy/=length;float nx=-dy,ny=dx;
        boolean visible=-nx+ny>0;
        if(!capOnly && !visible) return;
        float rise=GroundArt.LAND_RISE*c.scale;
        for(float at=0;at<length;at+=24) {
            float end=Math.min(length,at+24),ax=x+dx*at,ay=y+dy*at,bx=x+dx*end,by=y+dy*end;
            float sx=c.screenX(ax,ay),sy=c.screenY(ax,ay),ex=c.screenX(bx,by),ey=c.screenY(bx,by);
            if(Math.max(sx,ex)<-20 || Math.min(sx,ex)>g.layout.width+20 || Math.max(sy,ey)<-20 || Math.min(sy,ey)-rise>g.layout.height+20) continue;
            Painter p=SceneryOcclusion.obstacles(canvas);
            if(visible && !capOnly) {
                p.poly(new float[]{sx,sy-rise,ex,ey-rise,ex,ey,sx,sy},dx>0?0xff202d38:0xff2c3942);
                // Steel sheets have real folded faces: raised ribs and recessed channels,
                // rather than a repeated flat stroke masquerading as a tall wall.
                for(float d=at;d<end;d+=6) {
                    float a=d,b=Math.min(d+1.5f,end),cc=Math.min(d+4.5f,end),dd=Math.min(d+6,end);
                    sheetFace(g,p,x+dx*a,y+dy*a,x+dx*b+nx*1.3f,y+dy*b+ny*1.3f,0xff43535b);
                    sheetFace(g,p,x+dx*b+nx*1.3f,y+dy*b+ny*1.3f,x+dx*cc+nx*1.3f,y+dy*cc+ny*1.3f,dx>0?0xff293a45:0xff364750);
                    sheetFace(g,p,x+dx*cc+nx*1.3f,y+dy*cc+ny*1.3f,x+dx*dd,y+dy*dd,0xff14232e);
                    int hash=(seed*73+(int)d*19)&255;
                    if(hash<115) {
                        float wx=x+dx*(a+1.8f)+nx*1.3f,wy=y+dy*(a+1.8f)+ny*1.3f;
                        float rx=c.screenX(wx,wy),ry=c.screenY(wx,wy);
                        p.line(rx,ry-rise+2*c.scale,rx,ry-(3+hash%8)*c.scale,.85f*c.scale,0x88634431);
                    }
                }
                p.line(sx,sy-1.5f*c.scale,ex,ey-1.5f*c.scale,3*c.scale,0xff081722);
                p.line(sx,sy-rise*.55f,ex,ey-rise*.55f,1.2f*c.scale,0xff172a35);
            }
            if(capOnly) {
                Painter top=new RiverPainter(GroundArt.raisedCanvas(g,p),c);
                top.poly(new float[]{ax-nx*4,ay-ny*4,bx-nx*4,by-ny*4,bx+nx*.8f,by+ny*.8f,ax+nx*.8f,ay+ny*.8f},0xff43525a);
                top.line(ax-nx*3,ay-ny*3,bx-nx*3,by-ny*3,.85f,0xff738087);
                top.line(ax+nx*.8f,ay+ny*.8f,bx+nx*.8f,by+ny*.8f,1,0xff172731);
                if(((int)at/24)%2==0) {
                    float mx=ax+dx*5-nx*2,my=ay+dy*5-ny*2;
                    top.circle(mx,my,1.2f,0xff1c2c34);
                }
            }
        }
    }
    private static void sheetFace(Game g,Painter canvas,float x,float y,float xx,float yy,int color) {
        RiverCamera c=g.layout.camera;float sx=c.screenX(x,y),sy=c.screenY(x,y),ex=c.screenX(xx,yy),ey=c.screenY(xx,yy),rise=GroundArt.LAND_RISE*c.scale;
        canvas.poly(new float[]{sx,sy-rise,ex,ey-rise,ex,ey,sx,sy},color);
    }
}
