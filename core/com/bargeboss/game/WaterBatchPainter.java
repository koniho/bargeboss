package com.bargeboss.game;

/** Screen-space water polygon batching. Never reorder intersecting translucent coverage. */
public final class WaterBatchPainter implements Painter {
    public LightingProfile lighting(){return target.lighting();}
    public void lighting(LightingProfile profile){target.lighting(profile);}
    private static final int CAPACITY=256,LOOKBACK=48;
    private final Painter target;
    private final float width,height;
    private final float[][] points=new float[CAPACITY][],send=new float[CAPACITY][];
    private final float[] bounds=new float[CAPACITY*4];
    private final int[] next=new int[CAPACITY],first=new int[CAPACITY],last=new int[CAPACITY],color=new int[CAPACITY];
    private int used,groups;
    public WaterBatchPainter(Painter target,float width,float height){this.target=target;this.width=width;this.height=height;}
    public void poly(float[] xy,int argb) {
        if((argb>>>24)==0 || xy.length<6)return;
        float l=Float.MAX_VALUE,t=l,r=-l,b=-l;
        for(int i=0;i<xy.length;i+=2){l=Math.min(l,xy[i]);r=Math.max(r,xy[i]);t=Math.min(t,xy[i+1]);b=Math.max(b,xy[i+1]);}
        if(r< -1 || b< -1 || l>width+1 || t>height+1)return;
        if(used==CAPACITY)flush();
        // Two logical pixels per shape conservatively protect native AA fringes.
        l-=2;t-=2;r+=2;b+=2;
        int group=-1;
        search:for(int g=groups-1;g>=Math.max(0,groups-LOOKBACK);g--){
            for(int n=first[g];n>=0;n=next[n]){
                int o=n*4;
                if(l<=bounds[o+2] && r>=bounds[o] && t<=bounds[o+3] && b>=bounds[o+1])break search;
            }
            if(color[g]==argb){group=g;break;}
        }
        int n=used++,o=n*4;points[n]=xy;next[n]=-1;
        bounds[o]=l;bounds[o+1]=t;bounds[o+2]=r;bounds[o+3]=b;
        if(group<0){group=groups++;first[group]=last[group]=n;color[group]=argb;}
        else {next[last[group]]=n;last[group]=n;}
    }
    public void flush(){
        for(int g=0;g<groups;g++){
            int count=0;for(int n=first[g];n>=0;n=next[n])send[count++]=points[n];
            if(count==1)target.poly(send[0],color[g]);else target.polygons(send,count,color[g]);
            for(int i=0;i<count;i++)send[i]=null;
        }
        for(int i=0;i<used;i++)points[i]=null;
        used=groups=0;
    }
    public void rect(float x,float y,float w,float h,int c){flush();target.rect(x,y,w,h,c);}
    public void circle(float x,float y,float r,int c){flush();target.circle(x,y,r,c);}
    public void line(float x,float y,float xx,float yy,float w,int c){flush();target.line(x,y,xx,yy,w,c);}
    public void normalSprite(int frame,float x,float y,float w,float h,float nx,float ny,float nz,float radius,int color,float alpha){flush();target.normalSprite(frame,x,y,w,h,nx,ny,nz,radius,color,alpha);}
    public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){flush();target.sprite(a,f,x,y,w,h,r,alpha);}
    public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){flush();target.spriteBlend(a,f,ff,x,y,w,h,r,mix,alpha);}
    public void beginSurfaceBatch(){flush();target.beginSurfaceBatch();}
    public void endSurfaceBatch(){flush();target.endSurfaceBatch();}
    public void beginWaterFoamBatch(){flush();target.beginWaterFoamBatch();}
    public void endWaterFoamBatch(){flush();target.endWaterFoamBatch();}
    public void waterFoam(float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha) {
        flush();target.waterFoam(x,y,ax,ay,bx,by,phase,kind,alpha);
    }
    public void shoreline(float[] xy,float[] uv,float phase,float energy,int kind,int color){flush();target.shoreline(xy,uv,phase,energy,kind,color);}
    public void surfacePolygon(float[] xy,float[] uv,int material,int color,float wetness){flush();target.surfacePolygon(xy,uv,material,color,wetness);}
    public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){flush();target.material(x,y,ax,ay,bx,by,kind,detail,amount,color);}
    public void waterReflection(float x,float y,float ax,float ay,float bx,float by,float phase,int color){flush();target.waterReflection(x,y,ax,ay,bx,by,phase,color);}
    public void ropeSegment(float x,float y,float xx,float yy,float width,float from,float to,int color){flush();target.ropeSegment(x,y,xx,yy,width,from,to,color);}
    public void smokePuff(float x,float y,float radius,float phase,int color){flush();target.smokePuff(x,y,radius,phase,color);}
    public void spriteAffine(int a,int f,float x,float y,float ax,float ay,float bx,float by,float alpha){flush();target.spriteAffine(a,f,x,y,ax,ay,bx,by,alpha);}
    public boolean supportsUnionClips(){return target.supportsUnionClips();}
    public boolean prefersPolygonBatches(){return target.prefersPolygonBatches();}
    public boolean preparesLayers(){return target.preparesLayers();}
    public void prepareLayer(StaticLayer layer,float scale){target.prepareLayer(layer,scale);}
    public void layer(StaticLayer layer,float x,float y,float scale,float alpha){flush();target.layer(layer,x,y,scale,alpha);}
    public void pushClipOutside(float[] triangles){flush();target.pushClipOutside(triangles);}
    public void popClip(){flush();target.popClip();}
}
