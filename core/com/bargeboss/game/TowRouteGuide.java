package com.bargeboss.game;

/** Stateless, allocation-free guidance along an authored waterway centerline. */
final class TowRouteGuide {
    private TowRouteGuide(){}
    static float target(float boatX,float boatY,float[] px,float[] py,boolean returning,boolean x){
        float best=Float.MAX_VALUE,along=0,total=0;
        for(int i=0;i<px.length-1;i++){
            float dx=px[i+1]-px[i],dy=py[i+1]-py[i],len=Physics.length(dx,dy);
            float t=Physics.clamp(((boatX-px[i])*dx+(boatY-py[i])*dy)/(len*len),0,1);
            float ex=boatX-px[i]-t*dx,ey=boatY-py[i]-t*dy,d=ex*ex+ey*ey;
            if(d<best){best=d;along=total+t*len;} total+=len;
        }
        float distance=Physics.clamp(along+(returning?-45:45),0,total);
        for(int i=0;i<px.length-1;i++){
            float dx=px[i+1]-px[i],dy=py[i+1]-py[i],len=Physics.length(dx,dy);
            if(distance<=len || i==px.length-2){float t=distance/len;return x?px[i]+t*dx:py[i]+t*dy;}
            distance-=len;
        }
        return x?px[0]:py[0];
    }
}
