package com.bargeboss.game;

import android.content.res.AssetManager;
import android.graphics.*;
import java.io.*;
import java.nio.*;

/** Ordered material triangles. Static chunks retain the mesh; live models use bounded staging. */
@android.annotation.TargetApi(34)
final class SurfaceMesh {
    private static final int VERTICES=16380,FLOATS=10;
    private final FloatBuffer vertices=ByteBuffer.allocateDirect(VERTICES*FLOATS*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private final float[] staging=new float[VERTICES*FLOATS];
    private final MeshSpecification spec;
    private final Paint paint=new Paint();
    private int count;private float left,top,right,bottom;private long recordedBytes;
    SurfaceMesh(AssetManager assets){
        String source;
        try(InputStream in=assets.open("shaders/surface.inc")){
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;
            while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);
            source=bytes.toString("UTF-8");
        }catch(IOException e){throw new IllegalStateException(e);}
        spec=MeshSpecification.make(new MeshSpecification.Attribute[]{
            new MeshSpecification.Attribute(MeshSpecification.TYPE_FLOAT2,0,"pos"),
            new MeshSpecification.Attribute(MeshSpecification.TYPE_FLOAT4,8,"surface"),
            new MeshSpecification.Attribute(MeshSpecification.TYPE_FLOAT4,24,"tint")},40,
            new MeshSpecification.Varying[]{new MeshSpecification.Varying(MeshSpecification.TYPE_FLOAT4,"surface"),new MeshSpecification.Varying(MeshSpecification.TYPE_FLOAT4,"tint")},
            "Varyings main(const Attributes a){Varyings v;v.position=a.pos;v.surface=a.surface;v.tint=a.tint;return v;}",
            source+"\nfloat2 main(const Varyings v,out float4 color){float3 shade=surfaceShade(v.surface.xy,v.surface.z,v.surface.w);color=float4(min(v.tint.rgb*shade,float3(1.0))*v.tint.a,v.tint.a);return v.position;}",
            ColorSpace.get(ColorSpace.Named.SRGB),MeshSpecification.ALPHA_TYPE_PREMULTIPLIED);
        paint.setColor(0xffffffff);reset();
    }
    private void reset(){count=0;vertices.clear();left=top=Float.MAX_VALUE;right=bottom=-Float.MAX_VALUE;}
    void begin(){reset();recordedBytes=0;}
    long recordedBytes(){return recordedBytes;}
    void add(Canvas canvas,float[] xy,float[] uv,int material,int color,float wetness){
        for(int i=2;i<xy.length-2;i+=2){
            if(count+3>VERTICES)flush(canvas);
            vertex(xy,uv,0,material,color,wetness);vertex(xy,uv,i,material,color,wetness);vertex(xy,uv,i+2,material,color,wetness);
        }
    }
    private void vertex(float[] xy,float[] uv,int i,int material,int color,float wetness){
        int at=count*FLOATS;
        staging[at++]=xy[i];staging[at++]=xy[i+1];staging[at++]=uv[i];staging[at++]=uv[i+1];staging[at++]=material;staging[at++]=wetness;
        staging[at++]=((color>>16)&255)/255f;staging[at++]=((color>>8)&255)/255f;staging[at++]=(color&255)/255f;staging[at]=(color>>>24)/255f;
        count++;left=Math.min(left,xy[i]);right=Math.max(right,xy[i]);top=Math.min(top,xy[i+1]);bottom=Math.max(bottom,xy[i+1]);
    }
    void flush(Canvas canvas){
        if(count==0)return;
        // One bulk copy avoids a JNI/direct-buffer store for every vertex component.
        vertices.put(staging,0,count*FLOATS);vertices.flip();
        Mesh mesh=new Mesh(spec,Mesh.TRIANGLES,vertices,count,new RectF(left,top,right,bottom));
        canvas.drawMesh(mesh,BlendMode.MODULATE,paint);recordedBytes+=(long)count*FLOATS*4;reset();
    }
}
