package com.bargeboss.game;

/** Fixed double-line berths. Bobbing is vertical only, so lines cannot release a hull. */
public final class MooredBoats {
    public static final float CELL_WORLD=96;
    public static final int ORIENTATIONS=38,FRAMES=228,COLUMNS=19,ROWS=12;
    public static final class Berth {
        public final float x,y,angle,length,width,ax,ay,bx,by;
        public final int frame,kind;
        final float[] hull;
        Berth(float x,float y,float angle,int orientation,int kind,float ax,float ay,float bx,float by){
            this.x=x;this.y=y;this.angle=angle;this.kind=kind;this.ax=ax;this.ay=ay;this.bx=bx;this.by=by;
            length=length(kind);width=width(kind);frame=kind*ORIENTATIONS+orientation;
            float c=(float)StrictMath.cos(angle),s=(float)StrictMath.sin(angle);hull=new float[8];
            for(int i=0;i<4;i++){float u=(i<2?-1:1)*length*.48f,v=(i==0||i==3?-1:1)*width*.48f;hull[i*2]=x+c*u-s*v;hull[i*2+1]=y+s*u+c*v;}
        }
    }
    public static float length(int kind){return new float[]{39,44,47,40,46,49}[kind];}
    public static float width(int kind){return kind>=3?13:16;}
    public static float angle(int orientation){if(orientation>=30)return (orientation-30+.5f)*(float)Math.PI/4;float[] r=MontroseHarbor.berthRows()[orientation/2];return (float)StrictMath.atan2(r[3]-r[1],r[2]-r[0])+(orientation%2==0?-1:1)*(float)Math.PI/2;}
    public static final Berth[] BERTHS=make();
    private static Berth[] make(){java.util.ArrayList<Berth> out=new java.util.ArrayList<Berth>();float[][] rows=MontroseHarbor.berthRows();
        for(int row=0;row<rows.length;row++){float[] r=rows[row];float dx=r[2]-r[0],dy=r[3]-r[1],len=Physics.length(dx,dy);dx/=len;dy/=len;
            for(float at=45;at+15<len-8;at+=30)for(int side=-1;side<=1;side+=2){
                float px=r[0]+dx*at,py=r[1]+dy*at;
                if(row==1 && side==-1 && py>285)continue; // Mission yacht and its approach remain clear.
                int kind=(row*7+(int)(at/30)*3+(side==1?2:0))%6,orientation=row*2+(side==1?1:0);
                float a=angle(orientation),c=(float)StrictMath.cos(a),s=(float)StrictMath.sin(a),offset=length(kind)*.5f+9;
                float x=MontroseHarbor.mapX(px)+c*offset,y=MontroseHarbor.mapY(py)+s*offset;
                float ax=MontroseHarbor.mapX(px-dx*15)+c*10,ay=MontroseHarbor.mapY(py-dy*15)+s*10;
                float finger=(row<2?42:row<6?28:32)*MontroseHarbor.MAP_SCALE-14;
                float bx=ax+c*finger,by=ay+s*finger;
                Berth b=new Berth(x,y,a,orientation,kind,ax,ay,bx,by);boolean valid=true;
                for(int i=0;i<8;i+=2)if(!MontroseHarbor.contains(b.hull[i],b.hull[i+1]))valid=false;
                if(valid)out.add(b);
            }
        }
        for(float[] star:MontroseHarbor.STAR_CENTERS)for(int arm=0;arm<8;arm++){
            int kind=(arm+(int)star[0])%6;if(kind<0)kind+=6;float a=angle(30+arm),c=(float)StrictMath.cos(a),s=(float)StrictMath.sin(a),near=arm*(float)Math.PI/4,nc=(float)StrictMath.cos(near),ns=(float)StrictMath.sin(near);
            Berth berth=new Berth(star[0]+c*59,star[1]+s*59,a,30+arm,kind,star[0]+nc*31,star[1]+ns*31,star[0]+nc*59,star[1]+ns*59);
            boolean valid=true;for(int i=0;i<8;i+=2)if(!MontroseHarbor.contains(berth.hull[i],berth.hull[i+1]))valid=false;
            if(valid)out.add(berth);
        }
        out.sort((a,b)->Float.compare(a.y-a.x,b.y-b.x));return out.toArray(new Berth[out.size()]);
    }
    public static int count(Game g){return g.montroseStage()?BERTHS.length:0;}
    public static float depth(Game g,int i){Berth b=BERTHS[i];return g.layout.camera.screenY(b.x,b.y);}
    public static float bob(int ticks,int i){return .35f*(float)StrictMath.sin(ticks*.026+i*1.73);}
    public static void draw(Game g,Painter p,int i){Berth b=BERTHS[i];RiverCamera c=g.layout.camera;float x=c.screenX(b.x,b.y),y=c.screenY(b.x,b.y),size=CELL_WORLD*c.scale;
        if(x+size*.5f<0||x-size*.5f>g.layout.width||y+size*.3f<0||y-size*.7f>g.layout.height)return;
        float heave=bob(g.visualTicks,i),co=(float)StrictMath.cos(b.angle),si=(float)StrictMath.sin(b.angle);
        // Two short spans terminate at the near finger; all geometry remains within its berth.
        for(int end=0;end<2;end++){float u=(end==0?-1:1)*b.length*.38f,v=(b.frame%ORIENTATIONS>=30 || b.frame%ORIENTATIONS%2==0?-1:1)*b.width*.38f;
            float hx=b.x+co*u-si*v,hy=b.y+si*u+co*v;
            float tx=end==0?b.ax:b.bx,ty=end==0?b.ay:b.by;
            p.line(c.screenX(tx,ty),c.screenY(tx,ty)-4*c.scale,c.screenX(hx,hy),c.screenY(hx,hy)-(3+heave)*c.scale,.7f*c.scale,0xff867a60);
        }
        p.sprite(SpriteAtlas.MOORED_BOATS,b.frame,x,y-heave*c.scale,size,size,0,1);
    }
    static boolean occupied(float x,float y){for(Berth b:BERTHS)if(Math.abs(x-b.x)<b.length && Math.abs(y-b.y)<b.length && PolygonWater.contains(b.hull,x,y))return true;return false;}
    public static float collide(Body body,CollisionSplashes splashes){float hit=0;
        for(Berth b:BERTHS)if(Math.abs(body.x-b.x)<(body.half+body.radius)+b.length && Math.abs(body.y-b.y)<(body.half+body.radius)+b.length)
            hit=Math.max(hit,PolygonWater.collide(body,b.hull,false,splashes));return hit;
    }
    private MooredBoats(){}
}
