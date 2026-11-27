package com.bargeboss.game;

/** Thousand Islands land: wooded riverbanks around the arena and granite islets with pines and the odd
 * summer cottage. Rough outlines are deterministic per island; colours are muted to sit with the water. */
final class IslandArt {
    private IslandArt(){}
    static final int GRANITE=0xff8b7c72,GRANITE_DARK=0xff5f544e,MOSS=0xff3d5634,MOSS_LIGHT=0xff4b6a3f,PINE=0xff24392a,PINE_LIGHT=0xff2f4a35,TRUNK=0xff3b2a1e,BANK=0xff3a5131;
    static final float RIM=4,TOP=9;
    private static float rand(int i,int k){int h=(i*7919+k*104729)*0x45d9f3b;h^=h>>>16;h*=0x45d9f3b;h^=h>>>16;return (h&65535)/65535f;}
    static void draw(Game g,Painter p,RiverCamera c,ArenaCourse.Islands course,float[] view){
        shores(p,c,course,view);
        for(int i=0;i<ArenaCourse.Islands.ISLES.length;i++){
            float[] s=ArenaCourse.Islands.ISLES[i];
            if(s[0]+s[2]<view[0]-60 || s[0]-s[2]>view[2]+60 || s[1]+s[2]<view[1]-60 || s[1]-s[2]>view[3]+60)continue;
            isle(g,p,c,i,s[0],s[1],s[2]);
        }
    }
    private static float sx(RiverCamera c,float x,float y){return c.screenX(x,y);}
    private static float sy(RiverCamera c,float x,float y,float z){return c.screenY(x,y)-z*c.scale;}
    private static float[] outline(RiverCamera c,int i,float cx,float cy,float r,float z,int salt){
        int n=16;float[] xy=new float[n*2];
        for(int k=0;k<n;k++){double a=k*Math.PI*2/n;float rr=r*(.9f+.16f*rand(i,k+salt));float x=cx+(float)Math.cos(a)*rr,y=cy+(float)Math.sin(a)*rr;xy[k*2]=sx(c,x,y);xy[k*2+1]=sy(c,x,y,z);}
        return xy;
    }
    private static void isle(Game g,Painter p,RiverCamera c,int i,float cx,float cy,float r){
        // Granite skirt at the waterline, then the mossy top.
        p.poly(outline(c,i,cx,cy,r+2,0,0),GRANITE_DARK);
        p.poly(outline(c,i,cx,cy,r,RIM,0),GRANITE);
        p.poly(outline(c,i,cx,cy,r*.8f,TOP,40),MOSS);
        p.poly(outline(c,i,cx+r*.15f,cy-r*.1f,r*.45f,TOP+.5f,80),MOSS_LIGHT);
        // A summer cottage on every third island.
        if(i%3==1)cottage(p,c,cx-r*.2f,cy+r*.15f,i);
        // Pines: tall dark cones, back to front.
        int trees=3+(int)(r/40);
        float[][] at=new float[trees][];
        for(int k=0;k<trees;k++){float a=rand(i,k+200)*6.283f,d=r*.65f*(float)Math.sqrt(rand(i,k+300));at[k]=new float[]{cx+(float)Math.cos(a)*d,cy+(float)Math.sin(a)*d,18+rand(i,k+400)*16};}
        for(int k=1;k<trees;k++){float[] v=at[k];int j=k-1;while(j>=0 && c.screenY(at[j][0],at[j][1])>c.screenY(v[0],v[1])){at[j+1]=at[j];j--;}at[j+1]=v;}
        for(float[] t:at)pine(p,c,t[0],t[1],t[2]);
    }
    static void pine(Painter p,RiverCamera c,float x,float y,float h){
        float bx=sx(c,x,y),by=sy(c,x,y,TOP),s=c.scale;
        p.line(bx,by,bx,by-h*.35f*s,1.6f*s,TRUNK);
        for(int layer=0;layer<3;layer++){
            float base=by-h*(.2f+layer*.22f)*s,w=(7-layer*1.8f)*s*(h/26),top=base-h*.42f*s;
            p.poly(new float[]{bx-w,base,bx+w,base,bx,top},layer%2==0?PINE:PINE_LIGHT);
        }
    }
    private static void cottage(Painter p,RiverCamera c,float x,float y,int i){
        float w=16,d=11,wall=9,roof=7;int body=i%2==0?0xffd9d2c3:0xffb04a3a,roofColor=i%2==0?0xff4a3a32:0xff2f3a44;
        float z0=TOP,z1=TOP+wall;
        // Front (south) and west walls, then the roof ridge.
        p.convexQuad(sx(c,x-w,y+d),sy(c,x-w,y+d,z0),sx(c,x+w,y+d),sy(c,x+w,y+d,z0),sx(c,x+w,y+d),sy(c,x+w,y+d,z1),sx(c,x-w,y+d),sy(c,x-w,y+d,z1),body);
        p.convexQuad(sx(c,x-w,y-d),sy(c,x-w,y-d,z0),sx(c,x-w,y+d),sy(c,x-w,y+d,z0),sx(c,x-w,y+d),sy(c,x-w,y+d,z1),sx(c,x-w,y-d),sy(c,x-w,y-d,z1),0xff9c958a);
        p.convexQuad(sx(c,x-w,y+d),sy(c,x-w,y+d,z1),sx(c,x+w,y+d),sy(c,x+w,y+d,z1),sx(c,x+w,y),sy(c,x+w,y,z1+roof),sx(c,x-w,y),sy(c,x-w,y,z1+roof),roofColor);
        p.convexQuad(sx(c,x-w,y-d),sy(c,x-w,y-d,z1),sx(c,x-w,y),sy(c,x-w,y,z1+roof),sx(c,x+w,y),sy(c,x+w,y,z1+roof),sx(c,x+w,y-d),sy(c,x+w,y-d,z1),roofColor);
        p.material(sx(c,x,y+d),sy(c,x,y+d,z0+4),6*c.scale,0,0,5*c.scale,EffectMaterial.GLOW,0,1,0x99ffd38a);
    }
    /** Wooded riverbanks around the arena: granite edge, mossy bank, and pines along the visible edges. */
    private static void shores(Painter p,RiverCamera c,ArenaCourse.Islands course,float[] view){
        float w=course.w,h=course.h,e=ArenaCourse.Islands.SHORE,far=3000;
        float[][] banks={{-far,-far,w+far,e},{-far,h-e,w+far,h+far},{-far,e,e,h-e},{w-e,e,w+far,h-e}};
        for(float[] r:banks){
            if(r[2]<view[0]-60 || r[0]>view[2]+60 || r[3]<view[1]-60 || r[1]>view[3]+60)continue;
            float l=Math.max(r[0],view[0]-100),t=Math.max(r[1],view[1]-100),rr=Math.min(r[2],view[2]+100),b=Math.min(r[3],view[3]+100);
            p.convexQuad(sx(c,l,t),sy(c,l,t,TOP),sx(c,rr,t),sy(c,rr,t,TOP),sx(c,rr,b),sy(c,rr,b,TOP),sx(c,l,b),sy(c,l,b,TOP),BANK);
        }
        // Granite lip along the water edge, and a row of pines just inland.
        float[][] edges={{e,e,w-e,e,0,-1},{e,h-e,w-e,h-e,0,1},{e,e,e,h-e,-1,0},{w-e,e,w-e,h-e,1,0}};
        for(float[] ed:edges){
            p.line(sx(c,ed[0],ed[1]),sy(c,ed[0],ed[1],RIM),sx(c,ed[2],ed[3]),sy(c,ed[2],ed[3],RIM),4*c.scale,GRANITE);
            float len=Physics.length(ed[2]-ed[0],ed[3]-ed[1]);
            for(float t=40;t<len;t+=70){float x=ed[0]+(ed[2]-ed[0])*t/len+ed[4]*30,y=ed[1]+(ed[3]-ed[1])*t/len+ed[5]*30;
                if(x<view[0]-40 || x>view[2]+40 || y<view[1]-40 || y>view[3]+40)continue;pine(p,c,x,y,20+rand((int)x,(int)y)*14);}
        }
    }
}
