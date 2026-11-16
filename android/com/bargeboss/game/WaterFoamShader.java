package com.bargeboss.game;

import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RuntimeShader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

/** API 33+ adapter. Constructed before active drawing; one program, no per-patch allocations. */
@android.annotation.TargetApi(33)
final class WaterFoamShader {
    private final RuntimeShader shader;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix=new Matrix();
    private final float[] values=new float[9];
    WaterFoamShader(AssetManager assets) {
        try(InputStream in=assets.open("shaders/water-foam.inc")) {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[2048];int n;
            while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n);
            String source=new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
            try(InputStream rope=assets.open("shaders/rope.inc")) {
                bytes.reset();while((n=rope.read(buffer))!=-1)bytes.write(buffer,0,n);
                source+="\n"+new String(bytes.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
            }
            shader=new RuntimeShader(source+"\nuniform float phase; uniform float kind; uniform float opacity; uniform float ropeFrom; uniform float ropeTo; layout(color) uniform float4 tint;\n"
                +"half4 main(float2 uv) { if(kind>7.5) { float4 r=ropeShade(float2(mix(ropeFrom,ropeTo,uv.x),uv.y)); return half4(tint.rgb*r.rgb*opacity,r.a*opacity); } float a=foamCoverage(uv,phase,kind)*opacity; return half4(tint.rgb*a,a); }");
            paint.setShader(shader);
        } catch(IOException e) {throw new IllegalStateException("Missing shared water shader",e);}
    }
    void rope(Canvas canvas,float x,float y,float xx,float yy,float width,float from,float to,int color) {
        float dx=xx-x,dy=yy-y,len=(float)Math.hypot(dx,dy);if(len<.001f || width<=0)return;
        shader.setFloatUniform("ropeFrom",from);shader.setFloatUniform("ropeTo",to);
        draw(canvas,(x+xx)*.5f,(y+yy)*.5f,dx,dy,-dy/len*width,dx/len*width,0,8,(color>>>24)/255f,color);
    }
    void draw(Canvas canvas,float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha,int color) {
        shader.setColorUniform("tint",color|0xff000000);
        shader.setFloatUniform("phase",phase);shader.setFloatUniform("kind",(float)kind);shader.setFloatUniform("opacity",alpha);
        values[0]=ax;values[1]=bx;values[2]=x-(ax+bx)*.5f;
        values[3]=ay;values[4]=by;values[5]=y-(ay+by)*.5f;
        values[6]=0;values[7]=0;values[8]=1;matrix.setValues(values);
        int save=canvas.save();
        try {canvas.concat(matrix);canvas.drawRect(0,0,1,1,paint);} finally {canvas.restoreToCount(save);}
    }
}
