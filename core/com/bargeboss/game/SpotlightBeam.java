package com.bargeboss.game;
/** Shared bounded, soft-edged water footprint and descending searchlight volume. */
final class SpotlightBeam {
    private static int color(float range,int edge,boolean airborne,int rgb){
        float across=edge==0 || edge==4?0:edge==2?1:.5f;
        float pool=Physics.clamp((range-.4f)/.6f,0,1);
        float along=airborne?(1-range)*.14f:(float)StrictMath.sin(pool*StrictMath.PI)*.28f;
        return ((int)(255*across*along)<<24)|(rgb&0xffffff);
    }
    /** March each boundary separately so a near bank cannot erase the entire cone. */
    private static float reach(Game g,float originX,float originY,float dx,float dy,float range){
        float last=0;
        for(float at=12;at<=range+12;at+=12){float next=Math.min(range,at);
            if(!g.inWater(originX+dx*next,originY+dy*next)){
                float high=next;for(int i=0;i<5;i++){float mid=(last+high)*.5f;
                    if(g.inWater(originX+dx*mid,originY+dy*mid))last=mid;else high=mid;}return last;
            }last=next;if(next==range)break;
        }return range;
    }
    static void draw(Game g,Painter canvas,float originX,float originY,float angle,float range,float halfAngle,float ox,float oy,int rgb){
        RiverCamera c=g.layout.camera;
        float[] dx=new float[5],dy=new float[5],reach=new float[5];
        for(int i=0;i<5;i++){double a=angle+(i-2)*halfAngle/2;
            dx[i]=(float)StrictMath.cos(a);dy[i]=(float)StrictMath.sin(a);reach[i]=reach(g,originX,originY,dx[i],dy[i],range);}
        // Water footprint followed by a faint volume descending from the registered
        // mast to the surface. Later banks, hulls and UI keep their normal depth order.
        for(int pass=0;pass<2;pass++)for(int edge=0;edge<4;edge++)for(int band=0;band<10;band++){
            float r=band/10f,rr=(band+1)/10f;boolean air=pass==1;
            float da=reach[edge],db=reach[edge+1];
            float x=c.screenX(originX+dx[edge]*r*da,originY+dy[edge]*r*da),y=c.screenY(originX+dx[edge]*r*da,originY+dy[edge]*r*da);
            float xx=c.screenX(originX+dx[edge+1]*r*db,originY+dy[edge+1]*r*db),yy=c.screenY(originX+dx[edge+1]*r*db,originY+dy[edge+1]*r*db);
            float ex=c.screenX(originX+dx[edge]*rr*da,originY+dy[edge]*rr*da),ey=c.screenY(originX+dx[edge]*rr*da,originY+dy[edge]*rr*da);
            float fx=c.screenX(originX+dx[edge+1]*rr*db,originY+dy[edge+1]*rr*db),fy=c.screenY(originX+dx[edge+1]*rr*db,originY+dy[edge+1]*rr*db);
            if(air){x+=ox*(1-r);xx+=ox*(1-r);y+=oy*(1-r);yy+=oy*(1-r);ex+=ox*(1-rr);fx+=ox*(1-rr);ey+=oy*(1-rr);fy+=oy*(1-rr);}
            canvas.shadedTriangle(x,y,color(r,edge,air,rgb),ex,ey,color(rr,edge,air,rgb),fx,fy,color(rr,edge+1,air,rgb));
            canvas.shadedTriangle(x,y,color(r,edge,air,rgb),fx,fy,color(rr,edge+1,air,rgb),xx,yy,color(r,edge+1,air,rgb));
        }
    }
    private SpotlightBeam(){}
}
