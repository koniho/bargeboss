// Shared Metal / AGSL opaque surface RGB modulation, anchored in model units.
// No clocks or scene reads. Coarse features remain readable at distant camera scales.
float surfaceHash(float2 p) {
    float3 q=fract(float3(p.x,p.y,p.x)*0.1031);
    q+=dot(q,q.yzx+33.33);
    return fract((q.x+q.y)*q.z);
}
float surfaceNoise(float2 p) {
    float2 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
    return mix(mix(surfaceHash(i),surfaceHash(i+float2(1,0)),f.x),
        mix(surfaceHash(i+float2(0,1)),surfaceHash(i+float2(1,1)),f.x),f.y);
}
float3 surfaceShade(float2 uv,float material,float wetness) {
    // Concrete aggregate is fine model-space grain, separate from slab dimensions.
    float grainScale=(material>0.5 && material<1.5)?2.6:0.65;
    float broad=surfaceNoise(uv*0.17),grain=surfaceNoise(uv*grainScale);
    float seam=1.0-smoothstep(0.0,0.28,min(abs(fract(uv.x/24.0)-0.5)*24.0,abs(fract(uv.y/18.0)-0.5)*18.0));
    float3 shade=float3(0.82+0.24*broad+0.05*grain);
    if(material<0.5) {
        float rust=smoothstep(0.32,0.70,broad*0.8+grain*0.2);
        shade=mix(float3(0.70,0.68,0.65),float3(1.28,0.91,0.70),rust);
        float2 rivet=float2(fract(uv.x/9.0)*9.0-4.5,fract(uv.y/9.0)*9.0-4.5);
        float bolt=1.0-smoothstep(0.45,0.80,length(rivet));
        shade*=1.0-seam*0.20;shade+=bolt*0.17;
    } else if(material<1.5) {
        shade=float3(0.88,0.90,0.86)*(0.955+broad*0.045+grain*0.045);
        shade*=1.0-seam*0.06;
    } else if(material<2.5) {
        float chip=smoothstep(0.68,0.82,broad*0.6+grain*0.4);
        shade=mix(float3(1.12,1.06,0.89),float3(0.34,0.31,0.28),chip);
        shade*=0.90+grain*0.14;
    } else if(material>3.5 && material<4.5) {
        float timber=surfaceNoise(uv*float2(0.08,0.75));
        float join=1.0-smoothstep(0.08,0.30,abs(fract(uv.y/6.0)-0.5)*6.0);
        shade=float3(1.04,0.95,0.81)*(0.78+timber*0.30)*(1.0-join*0.30);
    } else {
        shade=mix(float3(0.83,0.91,0.95),float3(1.23,0.82,0.63),smoothstep(0.62,0.78,broad));
        shade*=1.0-seam*0.18;
        if(material>4.5)shade*=0.83+0.17*cos(uv.x*1.570796327);
    }
    float wet=clamp(wetness,0.0,1.0);
    return shade*mix(float3(1.0),float3(0.66,0.76,0.72),wet);
}
