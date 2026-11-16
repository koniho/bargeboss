package com.bargeboss.game;
import android.graphics.*;
/** Normal textures are data, never color-managed. Frame bitmaps remain owned by CanvasPainter. */
@android.annotation.TargetApi(33)
final class NormalSpriteShader {
    private final RuntimeShader shader=new RuntimeShader("uniform shader normals; uniform float3 direction; uniform float3 tint; uniform float opacity; half4 main(float2 p){ half4 t=normals.eval(p); float3 n=normalize(t.rgb/max(float(t.a),0.001)*2.0-1.0); float a=float(t.a)*opacity*max(dot(n,direction),0.0); return half4(tint*a,a); }");
    private final java.util.LinkedHashMap<Bitmap,BitmapShader> inputs=new java.util.LinkedHashMap<Bitmap,BitmapShader>(64,.75f,true){
        protected boolean removeEldestEntry(java.util.Map.Entry<Bitmap,BitmapShader> e){return size()>64;}
    };
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    NormalSpriteShader(){paint.setShader(shader);}
    void draw(Canvas c,Bitmap bitmap,float left,float top,float w,float h,float nx,float ny,float nz,int color,float alpha){
        BitmapShader input=inputs.get(bitmap);if(input==null){input=new BitmapShader(bitmap,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);inputs.put(bitmap,input);}
        shader.setInputBuffer("normals",input);shader.setFloatUniform("direction",nx,ny,nz);
        shader.setFloatUniform("tint",((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f);shader.setFloatUniform("opacity",alpha);
        int saved=c.save();try{c.translate(left,top);c.scale(w/bitmap.getWidth(),h/bitmap.getHeight());c.drawRect(0,0,bitmap.getWidth(),bitmap.getHeight(),paint);}finally{c.restoreToCount(saved);}
    }
}
