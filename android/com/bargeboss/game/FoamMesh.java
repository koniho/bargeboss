package com.bargeboss.game;

import android.content.res.AssetManager;
import android.graphics.*;
import java.io.*;
import java.nio.*;

/** Bounded ordered crest patches; API 34+ only. Coverage uses the shared kernel unchanged. */
@android.annotation.TargetApi(34)
final class FoamMesh {
    private static final int PATCHES=512,FLOATS=6;
    private final FloatBuffer vertices=ByteBuffer.allocateDirect(PATCHES*6*FLOATS*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private final float[] staging=new float[PATCHES*6*FLOATS];
    boolean bulk=true;private int staged;
    private final MeshSpecification spec;
    private final Paint paint=new Paint();
    private int count;
    private float left,top,right,bottom;
    FoamMesh(AssetManager assets){
        String source;
        try(InputStream in=assets.open("shaders/water-foam.inc")){
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] data=new byte[4096];int n;
            while((n=in.read(data))!=-1)bytes.write(data,0,n);
            source=new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
        }catch(IOException e){throw new IllegalStateException(e);}
        spec=MeshSpecification.make(new MeshSpecification.Attribute[]{
            new MeshSpecification.Attribute(MeshSpecification.TYPE_FLOAT2,0,"pos"),
            new MeshSpecification.Attribute(MeshSpecification.TYPE_FLOAT4,8,"params")},24,
            new MeshSpecification.Varying[]{new MeshSpecification.Varying(MeshSpecification.TYPE_FLOAT4,"params")},
            "Varyings main(const Attributes a){Varyings v;v.position=a.pos;v.params=a.params;return v;}",
            source+"\nlayout(color) uniform float4 tint;\nfloat2 main(const Varyings v,out float4 color){float a=foamCoverage(v.params.xy,v.params.z,5.0)*v.params.w;color=float4(tint.rgb*a,a);return v.position;}",
            ColorSpace.get(ColorSpace.Named.SRGB),MeshSpecification.ALPHA_TYPE_PREMULTIPLIED);
        paint.setColor(0xffffffff);
        reset();
    }
    private void reset(){count=0;staged=0;vertices.clear();left=top=Float.MAX_VALUE;right=bottom=-Float.MAX_VALUE;}
    void add(Canvas canvas,float x,float y,float ax,float ay,float bx,float by,float phase,float alpha){
        if(count==PATCHES)flush(canvas);
        vertex(x,y,ax,ay,bx,by,0,0,phase,alpha);vertex(x,y,ax,ay,bx,by,1,0,phase,alpha);vertex(x,y,ax,ay,bx,by,1,1,phase,alpha);
        vertex(x,y,ax,ay,bx,by,0,0,phase,alpha);vertex(x,y,ax,ay,bx,by,1,1,phase,alpha);vertex(x,y,ax,ay,bx,by,0,1,phase,alpha);
        count++;
    }
    private void vertex(float x,float y,float ax,float ay,float bx,float by,float u,float v,float phase,float alpha){
        float xx=x+ax*(u-.5f)+bx*(v-.5f),yy=y+ay*(u-.5f)+by*(v-.5f);
        if(bulk){staging[staged++]=xx;staging[staged++]=yy;staging[staged++]=u;staging[staged++]=v;staging[staged++]=phase;staging[staged++]=alpha;}
        else vertices.put(xx).put(yy).put(u).put(v).put(phase).put(alpha);
        left=Math.min(left,xx);right=Math.max(right,xx);top=Math.min(top,yy);bottom=Math.max(bottom,yy);
    }
    void flush(Canvas canvas){
        if(count==0)return;
        if(bulk)vertices.put(staging,0,staged);
        vertices.flip();
        Mesh mesh=new Mesh(spec,Mesh.TRIANGLES,vertices,count*6,new RectF(left,top,right,bottom));
        mesh.setColorUniform("tint",0xffe5fcf7);
        canvas.drawMesh(mesh,BlendMode.MODULATE,paint);
        reset();
    }
}
