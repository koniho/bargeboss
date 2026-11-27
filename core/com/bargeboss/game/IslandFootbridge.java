package com.bargeboss.game;

/** Timber/steel lift footbridge. Prepared span translates continuously; towers and lamps stay fixed. */
final class IslandFootbridge {
    private final ArticulatedModel fixed,span;
    private final ArticulatedModel.Translation lift;
    private ArticulatedModel.Pose pose;
    private final float ax,ay,bx,by;
    private final float[][] lamps;
    private static final float LOW=8,HIGH=46;
    IslandFootbridge(ArenaCourse course,int k){
        float dx=course.bx[k]-course.ax[k],dy=course.by[k]-course.ay[k],len=Physics.length(dx,dy),yaw=(float)StrictMath.atan2(dy,dx);
        ax=course.ax[k]-dx/len*10;ay=course.ay[k]-dy/len*10;bx=course.bx[k]+dx/len*10;by=course.by[k]+dy/len*10;len+=20;
        ArticulatedModel.Builder b=new ArticulatedModel.Builder();int root=b.node("island-lift-span-"+k,-1,ax,ay,0,yaw,0);
        int steel=0xff7c9192,wood=0xffa39a80;
        b.box(root,len*.5f,0,0,len,18,2.5f,wood,SurfaceMaterial.WOOD);
        // Low open rails and diagonal bracing preserve a clear view through the structure.
        for(int side=-1;side<=1;side+=2){
            b.box(root,len*.5f,side*8.5f,11,len,1.2f,1.4f,steel);
            b.box(root,len*.5f,side*8.5f,2,len,1.2f,1.4f,steel);
            for(int j=0;j<=8;j++)b.box(root,j*len/8,side*8.5f,6,1.3f,1.3f,12,steel);
            for(int j=0;j<8;j++)b.beam(root,j*len/8,side*8.5f,2,(j+1)*len/8,side*8.5f,11,.7f,steel);
        }
        span=b.build();lift=span.translation(span.pose(0));
        b=new ArticulatedModel.Builder();root=b.node("island-lift-towers-"+k,-1,ax,ay,0,yaw,0);
        for(int end=0;end<2;end++){
            float x=end==0?-3:len+3;
            for(int side=-1;side<=1;side+=2){
                b.box(root,x,side*12,3,8,8,6,0xff8a9390,SurfaceMaterial.CONCRETE);
                b.box(root,x,side*12,33,3,3,64,steel);
                b.box(root,x,side*12,62,6,6,3,0xff445a63);
            }
            b.box(root,x,0,64,4,28,4,steel);
            b.box(root,x,0,66,7,8,4,0xff3c505a);
        }
        fixed=b.build();fixed.prepare(fixed.pose(0));lamps=new float[][]{fixed.pose(0).point(0,-3,0,69),fixed.pose(0).point(0,len+3,0,69)};update(0);
    }
    void update(float open){float e=open*open*(3-2*open);pose=lift.pose(0,0,(LOW+(HIGH-LOW)*e)/ArticulatedModel.Z_SCALE);}
    float[] spanPoint(float x){return pose.point(0,x,0,0);}
    int faces(){return span.sourceFaceCount()+fixed.sourceFaceCount();}
    void draw(Game g,Painter p,float open){
        RiverCamera c=g.layout.camera;float x=c.screenX((ax+bx)*.5f,(ay+by)*.5f),y=c.screenY((ax+bx)*.5f,(ay+by)*.5f),pad=(Physics.length(bx-ax,by-ay)*.6f+75)*c.scale;
        if(x+pad<0 || x-pad>g.layout.width || y+pad<0 || y-pad>g.layout.height)return;
        float split=(ay+by-ax-bx)*.5f;
        fixed.drawRange(g,p,fixed.pose(0),Float.NEGATIVE_INFINITY,split);
        span.draw(g,p,pose);
        fixed.drawRange(g,p,fixed.pose(0),split,Float.POSITIVE_INFINITY);
        int color=open>=.8f?0xff80c9ac:open>0?0xffd9ac67:0xffca7468;
        for(int end=0;end<2;end++){float px=lamps[end][0],py=lamps[end][1],z=lamps[end][2]*ArticulatedModel.Z_SCALE;
            p.material(c.screenX(px,py),c.screenY(px,py)-z*c.scale,9*c.scale,0,0,9*c.scale,EffectMaterial.GLOW,0,1,SoftCue.alpha(color,.6f));
            p.circle(c.screenX(px,py),c.screenY(px,py)-z*c.scale,1.5f*c.scale,color);
        }
    }
}
