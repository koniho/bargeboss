// Shared Metal/AGSL local materials. Geometry, color and animation come from Java.
float effectCoverage(float2 uv,float kind,float detail,float amount) {
    float2 q=uv-float2(0.5); float r=length(q);
    if(kind<0.5) {
        float falloff=max(0.0,1.0-r*2.0);
        return falloff*falloff*amount;
    }
    if(kind<1.5) return (1.0-smoothstep(0.0,0.5,r))*0.65*amount;
    float edge=max(detail*0.2,0.001);
    float band=1.0-smoothstep(detail*0.5,detail*0.5+edge,abs(r-(0.49-detail*0.5)));
    float angle=(q.y<0.0?-1.0:1.0)*acos(clamp(q.x/max(r,0.00001),-1.0,1.0))+1.570796327;
    if(angle<0.0)angle+=6.283185307;
    return band*(amount>=0.9999?1.0:1.0-smoothstep(amount*6.283185307-edge,amount*6.283185307+edge,angle));
}
