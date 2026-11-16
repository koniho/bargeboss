package com.bargeboss.game;

import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RuntimeShader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

/** API 33+ adapter. Constructed before active drawing; one program, no per-patch allocations. */
@android.annotation.TargetApi(33)
final class WaterFoamShader {
    private final RuntimeShader shader;
    // RuntimeShader copies uniforms through JNI. Preserve values across draws so
    // consecutive patches only update uniforms whose exact float value changed.
    private float lastPhase=Float.NaN,lastKind=Float.NaN,lastOpacity=Float.NaN,lastDetail=Float.NaN;
    private int lastTint;private boolean hasTint;
    private void detail(float value){if(value!=lastDetail){shader.setFloatUniform("detail",value);lastDetail=value;}}
    private void uniforms(float phase,float kind,float opacity,int color){
        int tint=color|0xff000000;
        if(!hasTint || tint!=lastTint){shader.setColorUniform("tint",tint);lastTint=tint;hasTint=true;}
        if(phase!=lastPhase){shader.setFloatUniform("phase",phase);lastPhase=phase;}
        if(kind!=lastKind){shader.setFloatUniform("kind",kind);lastKind=kind;}
        if(opacity!=lastOpacity){shader.setFloatUniform("opacity",opacity);lastOpacity=opacity;}
    }
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix=new Matrix();
    private final float[] values=new float[9],surfaceSource=new float[6],surfaceTarget=new float[6];
    private final Path surfacePath=new Path();
    WaterFoamShader(AssetManager assets) {
        try(InputStream in=assets.open("shaders/water-foam.inc")) {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[2048];int n;
            while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);
            String source=new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
            try(InputStream rope=assets.open("shaders/rope.inc")) {
                bytes.reset();while((n=rope.read(buffer))!=-1)bytes.write(buffer,0,n);
                source+="\n"+new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
            }
            try(InputStream effects=assets.open("shaders/effects.inc")) {
                bytes.reset();while((n=effects.read(buffer))!=-1)bytes.write(buffer,0,n);
                source+="\n"+new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
            }
            try(InputStream surfaces=assets.open("shaders/surface.inc")) {
                bytes.reset();while((n=surfaces.read(buffer))!=-1)bytes.write(buffer,0,n);
                source+="\n"+new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
            }
            try(InputStream shoreline=assets.open("shaders/shoreline.inc")) {
                bytes.reset();while((n=shoreline.read(buffer))!=-1)bytes.write(buffer,0,n);
                source+="\n"+new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
            }
            shader=new RuntimeShader(source+"\nuniform float phase; uniform float kind; uniform float opacity; uniform float detail; uniform float ropeFrom; uniform float ropeTo; layout(color) uniform float4 tint;\n"
                +"half4 main(float2 uv) { if(kind>17.5) { float a=shorelineCoverage(uv,phase,detail,kind-18.0)*opacity; return half4(tint.rgb*a,a); } if(kind>11.5) { float3 rgb=min(tint.rgb*surfaceShade(uv,kind-12.0,phase),float3(1.0)); return half4(rgb*opacity,opacity); } if(kind>8.5) { float a=effectCoverage(uv,kind-9.0,detail,phase)*opacity; return half4(tint.rgb*a,a); } if(kind>7.5) { float4 r=ropeShade(float2(mix(ropeFrom,ropeTo,uv.x),uv.y)); return half4(tint.rgb*r.rgb*opacity,r.a*opacity); } float a=foamCoverage(uv,phase,kind)*opacity; return half4(tint.rgb*a,a); }");
            paint.setShader(shader);
        } catch(IOException e) {throw new IllegalStateException("Missing shared water shader",e);}
    }
    boolean shoreline(Canvas canvas,float[] xy,float[] uv,float phase,float energy,int kind,int color) {
        detail(energy);
        return polygon(canvas,xy,uv,18+kind,color,phase);
    }
    boolean surface(Canvas canvas,float[] xy,float[] uv,int material,int color,float wetness) {
        return polygon(canvas,xy,uv,12+material,color,wetness);
    }
    private boolean polygon(Canvas canvas,float[] xy,float[] uv,int mode,int color,float wetness) {
        if(xy.length<6 || xy.length!=uv.length)return false;
        // Find a nondegenerate affine basis even when a BSP split leaves collinear vertices.
        int third=-1;
        for(int i=4;i<uv.length;i+=2) {
            float determinant=(uv[2]-uv[0])*(uv[i+1]-uv[1])-(uv[3]-uv[1])*(uv[i]-uv[0]);
            if(Math.abs(determinant)>.0001f){third=i;break;}
        }
        if(third<0)return false;
        for(int i=0;i<6;i++){int k=i<4?i:third+i-4;surfaceSource[i]=uv[k];surfaceTarget[i]=xy[k];}
        matrix.reset();if(!matrix.setPolyToPoly(surfaceSource,0,surfaceTarget,0,3))return false;
        uniforms(wetness,mode,(color>>>24)/255f,color);
        surfacePath.rewind();surfacePath.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)surfacePath.lineTo(xy[i],xy[i+1]);surfacePath.close();
        shader.setLocalMatrix(matrix);
        try {canvas.drawPath(surfacePath,paint);} finally {shader.setLocalMatrix(null);}
        return true;
    }
    void material(Canvas canvas,float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color) {
        detail(detail);
        draw(canvas,x,y,ax,ay,bx,by,amount,kind+9,(color>>>24)/255f,color);
    }
    void rope(Canvas canvas,float x,float y,float xx,float yy,float width,float from,float to,int color) {
        float dx=xx-x,dy=yy-y,len=(float)Math.hypot(dx,dy);if(len<.001f || width<=0)return;
        shader.setFloatUniform("ropeFrom",from);shader.setFloatUniform("ropeTo",to);
        draw(canvas,(x+xx)*.5f,(y+yy)*.5f,dx,dy,-dy/len*width,dx/len*width,0,8,(color>>>24)/255f,color);
    }
    void draw(Canvas canvas,float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha,int color) {
        uniforms(phase,kind,alpha,color);
        values[0]=ax;values[1]=bx;values[2]=x-(ax+bx)*.5f;
        values[3]=ay;values[4]=by;values[5]=y-(ay+by)*.5f;
        values[6]=0;values[7]=0;values[8]=1;matrix.setValues(values);
        int save=canvas.save();
        try {canvas.concat(matrix);canvas.drawRect(0,0,1,1,paint);} finally {canvas.restoreToCount(save);}
    }
}
