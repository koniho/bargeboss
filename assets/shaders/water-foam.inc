// Shared Metal / AGSL kernel. Local UVs only; all placement and time come from Java.
// No scene texture reads, iterative fluid simulation or full-screen pass.
float foamHash(float2 p) {
    float3 q=fract(float3(p.x,p.y,p.x)*0.1031);
    q+=dot(q,q.yzx+33.33);
    return fract((q.x+q.y)*q.z);
}
float foamNoise(float2 p) {
    float2 cell=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);
    return mix(mix(foamHash(cell),foamHash(cell+float2(1,0)),f.x),
               mix(foamHash(cell+float2(0,1)),foamHash(cell+float2(1,1)),f.x),f.y);
}
float foamCoverage(float2 uv, float phase, float kind) {
    float2 p = uv * 2.0 - 1.0;
    float2 flow=float2(cos(phase),sin(phase));
    float edge = (1.0-smoothstep(0.78,1.0,abs(p.x))) * (1.0-smoothstep(0.78,1.0,abs(p.y)));
    if (kind >= 7.0) {
        float billow=foamNoise(p*2.4+flow*0.5);
        float detail=foamNoise(p*6.0-flow*0.8);
        float density=billow*0.65+detail*0.35;
        float radius=length(p+float2((billow-0.5)*0.24,(detail-0.5)*0.18));
        float cloud=1.0-smoothstep(0.30,0.96,radius+(0.5-density)*0.18);
        return edge*cloud*(0.48+0.52*density);
    }
    if (kind >= 6.0) {
        float sway = sin(uv.y*17.0-phase)*0.12 + sin(uv.y*31.0+phase*2.0)*0.035;
        float taper = 0.24+uv.y*0.35;
        float spread = 1.0-smoothstep(taper*0.35,taper,abs(p.x-sway));
        float ripple = 0.5+0.5*sin(uv.y*67.0+phase*2.0+foamNoise(uv*float2(5,13)+flow)*5.0);
        float glints = smoothstep(0.35,0.90,ripple);
        return edge*spread*(1.0-uv.y*.8)*(0.10+0.90*glints);
    }
    if (kind >= 2.0 && kind < 4.0) {
        float radius=length(p),angle=phase+radius*6.0;
        float2 swirl=float2(p.x*cos(angle)-p.y*sin(angle),p.x*sin(angle)+p.y*cos(angle));
        float cells=foamNoise(swirl*8.0+flow);
        float curl=foamNoise(swirl*15.0-flow*0.7);
        float envelope=1.0-smoothstep(0.2,1.0,radius);
        return edge*envelope*(0.25+0.75*smoothstep(kind>2.5?0.35:0.20,0.75,cells*.6+curl*.4));
    }
    float warp = sin(p.x*7.0+phase)*0.09 + sin(p.y*9.0-phase*2.0)*0.06;
    float grain = 0.65*foamNoise(p*7.0+flow)+0.35*foamNoise(p*17.0-flow);
    if (kind >= 4.0) {
        // Broken crest: broad translucent swell beneath a narrow, uneven foam ridge.
        float center = kind < 5.0 ? 0.3*p.x*p.x-0.12 : 0.0;
        float d = abs(p.y-center-warp);
        float swell = (1.0-smoothstep(0.18,0.68,d))*0.35;
        float ridge = (1.0-smoothstep(0.06,0.26,d))*smoothstep(0.20,0.65,grain);
        return edge*(swell+ridge*0.65);
    }
    // Shoulder wakes curl into long wisps; prop wash breaks into cellular islands.
    float curve = kind < 2.0 ? 0.28*sin(p.x*2.4+kind*3.141593)+warp : warp;
    float d = length(float2(p.x*0.82,(p.y-curve)*1.12));
    float envelope = 1.0-smoothstep(0.34,1.0,d);
    float filaments = foamNoise(float2(p.x*4.0,(p.y-curve)*13.0)+flow);
    float broken = smoothstep(kind > 2.5 ? 0.30 : 0.18,0.78,grain*0.65+filaments*0.35);
    return edge*envelope*(0.30+0.70*broken);
}
