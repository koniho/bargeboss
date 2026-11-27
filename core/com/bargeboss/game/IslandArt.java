package com.bargeboss.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/** Retained granite shelves, moss, white-pine groves and summer cottages. World geometry is fixed;
 * only projection and shared lighting run while drawing. No raster preparation or pose tables. */
final class IslandArt {
    static final float RIM=4,TOP=9;
    private static final int GRANITE=0xff8d9590,BANK=0xff586753;
    private static final Island[] ISLANDS=new Island[ArenaCourse.Islands.ISLES.length];
    private static final Prop[] PROPS;
    private static final class Prop {
        final float x,y,size;final int frame;final ArticulatedModel cottage;
        Prop(float x,float y,float size,int frame,ArticulatedModel cottage){this.x=x;this.y=y;this.size=size;this.frame=frame;this.cottage=cottage;}
    }
    private static final class Island {
        final float x,y,r;final int frame;
        Island(int id,float[] at,ArrayList<Prop> props){
            x=at[0];y=at[1];r=at[2];frame=id%4;
            boolean house=id%3==1;float hx=x-r*.2f,hy=y+r*.15f;
            if(house)props.add(new Prop(hx,hy,85,-1,IslandCottage.build(id,hx,hy)));
            int trees=6+(int)(r/22);
            for(int k=0;k<trees;k++){
                double a=rand(id,k+200)*6.283185;float d=r*.68f*(float)StrictMath.sqrt(rand(id,k+300));
                float tx=x+(float)StrictMath.cos(a)*d,ty=y+(float)StrictMath.sin(a)*d;
                // Keep the cottage's porch and front approach readable, including foliage footprints.
                if(house && Math.abs(tx-hx)<49 && ty>hy-24 && ty<hy+68)continue;
                int frame=k%6;props.add(new Prop(tx,ty,frame==4?45:64+rand(id,k+400)*34,frame,null));
            }
        }
    }
    static {
        ArrayList<Prop> props=new ArrayList<Prop>();
        for(int i=0;i<ISLANDS.length;i++)ISLANDS[i]=new Island(i,ArenaCourse.Islands.ISLES[i],props);
        float e=ArenaCourse.Islands.SHORE,w=ArenaCourse.Islands.W,h=ArenaCourse.Islands.H;
        float[][] edges={{e,e,w-e,e,0,-1},{e,h-e,w-e,h-e,0,1},{e,e,e,h-e,-1,0},{w-e,e,w-e,h-e,1,0}};
        for(int j=0;j<edges.length;j++){float[] ed=edges[j];float len=Physics.length(ed[2]-ed[0],ed[3]-ed[1]);
            for(int k=0;k<len/48;k++)for(int row=0;row<2;row++){
                float t=24+k*48+row*23,off=20+row*42+rand(j,k)*14;
                float x=ed[0]+(ed[2]-ed[0])*t/len+ed[4]*off,y=ed[1]+(ed[3]-ed[1])*t/len+ed[5]*off;
                props.add(new Prop(x,y,64+rand(k,j+40)*30,(k+j+row)%6,null));
            }
        }
        Collections.sort(props,new Comparator<Prop>(){public int compare(Prop a,Prop b){return Float.compare(a.y-a.x,b.y-b.x);}});
        PROPS=props.toArray(new Prop[props.size()]);
    }
    private IslandArt(){}
    static void prepare(){}
    private static float rand(int i,int k){int h=(i*7919+k*104729)*0x45d9f3b;h^=h>>>16;h*=0x45d9f3b;h^=h>>>16;return (h&65535)/65535f;}
    static void draw(Game g,Painter p,RiverCamera c,ArenaCourse.Islands course,float[] view){
        shores(p,c,course,view);
        for(Island s:ISLANDS){if(s.x+s.r<view[0]-60 || s.x-s.r>view[2]+60 || s.y+s.r<view[1]-60 || s.y-s.r>view[3]+60)continue;float size=s.r*2*384/360;p.sprite(SpriteAtlas.ISLAND_GROUND,s.frame,c.screenX(s.x,s.y),c.screenY(s.x,s.y)-5*c.scale,size*c.scale,size*2/3*c.scale,0,1);}
    }
    /** Merge upright scenery with vessels by ground contact, not the top of a tall tree. */
    static void props(Game g,Painter p,float from,float to){
        RiverCamera c=g.layout.camera;
        for(Prop a:PROPS){float depth=c.screenY(a.x,a.y);if(depth<=from)continue;if(depth>to)break;
            float x=c.screenX(a.x,a.y),y=depth-TOP*c.scale,pad=a.size*c.scale;
            if(x+pad<0 || x-pad>g.layout.width || y<0 || y-pad>g.layout.height)continue;
            if(a.cottage!=null)a.cottage.draw(g,p,a.cottage.pose(0));
            else if(a.frame==6)p.sprite(SpriteAtlas.MARINA,6,x,y,a.size*c.scale,a.size*c.scale,0,1);
            else IslandWoodland.draw(p,c,a.frame,a.x,a.y,TOP,a.size);
        }
    }
    /** Same registered opaque contours as the painted plates. Foreground land cuts hulls/ropes;
     * a conservative one-pixel inset avoids clipping the water at antialiased rocky edges. */
    static float[] shore(RiverCamera c,float left,float top,float right,float bottom,float depth){
        ArrayList<float[]> pieces=new ArrayList<float[]>();int count=0;
        for(Island a:ISLANDS){
            if(a.x+a.r*1.1f<left || a.x-a.r*1.1f>right || a.y+a.r*1.1f<top || a.y-a.r*1.1f>bottom)continue;
            float[] q=IslandGroundContours.POINTS[a.frame];float size=a.r*2*384/360*c.scale,cx=c.screenX(a.x,a.y),cy=c.screenY(a.x,a.y)-5*c.scale;
            for(int k=0;k<q.length;k+=2){int next=(k+2)%q.length;
                float[] tri={cx,cy,cx+(q[k]-.5f)*size,cy+(q[k+1]-.5f)*size*2/3,cx+(q[next]-.5f)*size,cy+(q[next+1]-.5f)*size*2/3};
                tri=ShoreOcclusion.below(tri,depth-TOP*c.scale);if(tri.length<6)continue;pieces.add(tri);count+=(tri.length/2-2)*6;
            }
        }
        float e=ArenaCourse.Islands.SHORE,w=ArenaCourse.Islands.W,h=ArenaCourse.Islands.H;
        float[][] banks={{left,top,right,Math.min(bottom,e)},{left,Math.max(top,h-e),right,bottom},{left,Math.max(top,e),Math.min(right,e),Math.min(bottom,h-e)},{Math.max(left,w-e),Math.max(top,e),right,Math.min(bottom,h-e)}};
        for(float[] r:banks){if(r[0]>=r[2] || r[1]>=r[3])continue;
            float rise=TOP*c.scale;float[] q={c.screenX(r[0],r[1]),c.screenY(r[0],r[1])-rise,c.screenX(r[2],r[1]),c.screenY(r[2],r[1])-rise,c.screenX(r[2],r[3]),c.screenY(r[2],r[3])-rise,c.screenX(r[0],r[3]),c.screenY(r[0],r[3])-rise};
            q=ShoreOcclusion.below(q,depth-rise);if(q.length<6)continue;pieces.add(q);count+=(q.length/2-2)*6;
        }
        float[] result=new float[count];int at=0;
        for(float[] q:pieces)for(int k=2;k<q.length-2;k+=2){result[at++]=q[0];result[at++]=q[1];result[at++]=q[k];result[at++]=q[k+1];result[at++]=q[k+2];result[at++]=q[k+3];}
        return result;
    }
    private static void shores(Painter p,RiverCamera c,ArenaCourse.Islands course,float[] view){
        float w=course.w,h=course.h,e=ArenaCourse.Islands.SHORE,far=3000;
        float[][] banks={{-far,-far,w+far,e},{-far,h-e,w+far,h+far},{-far,e,e,h-e},{w-e,e,w+far,h-e}};
        for(float[] r:banks){
            if(r[2]<view[0]-60 || r[0]>view[2]+60 || r[3]<view[1]-60 || r[1]>view[3]+60)continue;
            float l=Math.max(r[0],view[0]-100),t=Math.max(r[1],view[1]-100),rr=Math.min(r[2],view[2]+100),b=Math.min(r[3],view[3]+100);
            p.convexQuad(c.screenX(l,t),c.screenY(l,t)-TOP*c.scale,c.screenX(rr,t),c.screenY(rr,t)-TOP*c.scale,c.screenX(rr,b),c.screenY(rr,b)-TOP*c.scale,c.screenX(l,b),c.screenY(l,b)-TOP*c.scale,p.lighting().scenery(BANK));
        }
        float[][] edges={{e,e,w-e,e},{e,h-e,w-e,h-e},{e,e,e,h-e},{w-e,e,w-e,h-e}};
        for(float[] ed:edges)p.line(c.screenX(ed[0],ed[1]),c.screenY(ed[0],ed[1])-RIM*c.scale,c.screenX(ed[2],ed[3]),c.screenY(ed[2],ed[3])-RIM*c.scale,4*c.scale,p.lighting().scenery(GRANITE));
    }
}
