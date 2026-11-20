package com.bargeboss.game;

/** Keep the complete isometric viewport inside a finite rectangular scenery footprint. */
final class CameraCoverage {
    private CameraCoverage() {}
    static Layout follow(Layout wanted,float minX,float minY,float maxX,float maxY,float rise,float x,float y){
        Layout bounded=constrain(wanted,minX,minY,maxX,maxY,rise);
        RiverCamera c=bounded.camera;float sx=c.screenX(x,y),sy=c.screenY(x,y);
        if(sx>=64 && sx<=wanted.width-64 && sy>=112 && sy<=wanted.height-120)return bounded;
        Layout result=position(bounded,minX,minY,maxX,maxY,rise,x,y);
        if(result!=null)return result;
        float low=bounded.camera.zoom,high=Math.max(low,PinchZoom.MAX);
        Layout best=position(atZoom(wanted,high),minX,minY,maxX,maxY,rise,x,y);
        // Outside-world debug fixtures must not feed an invalid camera into gameplay.
        if(best==null)return bounded;
        for(int i=0;i<16;i++){
            float zoom=(low+high)*.5f;
            result=position(atZoom(wanted,zoom),minX,minY,maxX,maxY,rise,x,y);
            if(result==null)low=zoom;else{high=zoom;best=result;}
        }
        return best;
    }
    private static Layout atZoom(Layout l,float zoom){
        float sx=l.width*.5f,sy=l.height*.5f;
        return Layout.focused(l.width,l.height,l.camera.worldX(sx,sy),l.camera.worldY(sx,sy),zoom,sx,sy);
    }
    /** Intersect legal world translations with the diamond that keeps the hull in the safe view. */
    private static Layout position(Layout l,float minX,float minY,float maxX,float maxY,float rise,float x,float y){
        RiverCamera c=l.camera;
        float left=minX+2-c.worldX(0,l.height+rise*c.scale),right=maxX-2-c.worldX(l.width,0);
        float top=minY+2-c.worldY(0,0),bottom=maxY-2-c.worldY(l.width,l.height+rise*c.scale);
        if(left>right || top>bottom)return null;
        float[] screen={64,112,l.width-64,112,l.width-64,l.height-120,64,l.height-120};
        float[] safe=new float[8];
        for(int i=0;i<8;i+=2){safe[i]=x-c.worldX(screen[i],screen[i+1]);safe[i+1]=y-c.worldY(screen[i],screen[i+1]);}
        float[] legal=PolygonWater.clip(new float[]{left,top,right,top,right,bottom,left,bottom},safe);
        if(legal.length<6)return null;
        if(PolygonWater.contains(legal,0,0))return l;
        float best=Float.MAX_VALUE,dx=0,dy=0;
        for(int i=0;i<legal.length;i+=2){
            int j=(i+2)%legal.length;float ax=legal[i],ay=legal[i+1],vx=legal[j]-ax,vy=legal[j+1]-ay;
            float t=Physics.clamp(-(ax*vx+ay*vy)/Math.max(.000001f,vx*vx+vy*vy),0,1);
            float px=ax+vx*t,py=ay+vy*t,d=px*px+py*py;
            if(d<best){best=d;dx=px;dy=py;}
        }
        return new Layout(l.width,l.height,c.worldTop+dy,c.worldRight+dx,c.zoom);
    }
    static Layout constrain(Layout wanted,float minX,float minY,float maxX,float maxY,float rise){
        // A small world-space inset also keeps raster edge antialiasing offscreen.
        minX+=2;minY+=2;maxX-=2;maxY-=2;
        RiverCamera c=wanted.camera;
        float base=c.scale/c.zoom;
        float lift=rise/(2*RiverCamera.Q*RiverCamera.DEPTH);
        float span=(wanted.width+wanted.height/RiverCamera.DEPTH)/(2*RiverCamera.Q*base);
        float minimum=Math.max(span/(maxX-minX-lift),span/(maxY-minY-lift));
        float zoom=Math.max(c.zoom,minimum);
        Layout next=wanted;
        if(zoom>c.zoom){
            float sx=wanted.width*.5f,sy=wanted.height*.5f;
            next=Layout.focused(wanted.width,wanted.height,c.worldX(sx,sy),c.worldY(sx,sy),zoom,sx,sy);
            c=next.camera;
        }
        // Inverse corners include both water level and the raised foreground land.
        float loX=c.worldX(0,wanted.height+rise*c.scale),hiX=c.worldX(wanted.width,0);
        float loY=c.worldY(0,0),hiY=c.worldY(wanted.width,wanted.height+rise*c.scale);
        float dx=Physics.clamp(0,minX-loX,Math.max(minX-loX,maxX-hiX));
        float dy=Physics.clamp(0,minY-loY,Math.max(minY-loY,maxY-hiY));
        if(dx==0 && dy==0)return next;
        return new Layout(next.width,next.height,c.worldTop+dy,c.worldRight+dx,c.zoom);
    }
}
