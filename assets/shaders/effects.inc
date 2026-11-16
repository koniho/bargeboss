// Shared Metal/AGSL local materials. Geometry, color and animation come from Java.
float effectCoverage(float2 uv,float kind,float detail,float amount) {
    float segments=kind>4.5?8.0:16.0; if(kind>4.5)kind=2.0;
    float2 q=uv-float2(0.5); float r=length(q);
    if(kind<0.5) {
        float falloff=max(0.0,1.0-r*2.0);
        return falloff*falloff*amount;
    }
    if(kind<1.5) return (1.0-smoothstep(0.0,0.5,r))*0.65*amount;
    if(kind>3.5) {
        float cells=max(1.0,amount),cell=fract(uv.x*cells);
        float dash=cells<1.5?1.0:smoothstep(.02,.13,cell)*(1.0-smoothstep(.78,.9,cell));
        float distance=abs(q.y),core=detail*.5;
        float coverage=.18*(1.0-smoothstep(core,.49,distance))+.82*(1.0-smoothstep(core*.7,core*1.3,distance));
        return coverage*dash*smoothstep(0.0,.035,uv.x)*(1.0-smoothstep(.965,1.0,uv.x));
    }
    if(kind>2.5) {
        float shaft=max(abs(uv.y-.5)-.12,max(.08-uv.x,uv.x-.62));
        float head=max(.55-uv.x,(abs(uv.y-.5)-(0.92-uv.x)*1.08)*.68);
        float d=min(shaft,head);
        return (.22*(1.0-smoothstep(-.01,.09,d))+.78*(1.0-smoothstep(-.025,.012,d)))*amount;
    }
    float segmented=detail<0.0?1.0:0.0; detail=abs(detail);
    float edge=max(detail*0.2,0.001);
    float band=1.0-smoothstep(detail*0.5,detail*0.5+edge,abs(r-(0.49-detail*0.5)));
    float angle=(q.y<0.0?-1.0:1.0)*acos(clamp(q.x/max(r,0.00001),-1.0,1.0))+1.570796327;
    if(angle<0.0)angle+=6.283185307;
    float cell=fract(angle/6.283185307*segments);
    float dash=segmented<.5?1.0:smoothstep(.02,.10,cell)*(1.0-smoothstep(.67,.77,cell));
    return band*dash*(amount>=0.9999?1.0:1.0-smoothstep(amount*6.283185307-edge,amount*6.283185307+edge,angle));
}
