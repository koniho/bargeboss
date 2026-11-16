// Shared Metal/AGSL shoreline motion in stable world units and simulation seconds.
float shorelineCrest(float u,float phase,float energy) {
    return 1.9+0.65*sin(u*0.105-phase*1.17)+0.35*sin(u*0.237+phase*0.79)+clamp(energy,0.0,1.0)*1.1;
}
float shorelineCoverage(float2 uv,float phase,float energy,float kind) {
    float e=clamp(energy,0.0,1.0),crest=shorelineCrest(uv.x,phase,e);
    if(kind>0.5) {
        float recent=max(crest,max(shorelineCrest(uv.x,phase-0.8,e)-0.18,shorelineCrest(uv.x,phase-1.6,e)-0.42));
        return (1.0-smoothstep(recent-0.5,recent+0.7,uv.y))*(0.48+0.16*e);
    }
    float broken=smoothstep(-0.55,0.55,sin(uv.x*0.43+phase*0.51)+0.45*sin(uv.x*0.91-phase*0.63));
    float edge=1.0-smoothstep(0.25,0.85+0.2*e,abs(uv.y-crest));
    float lace=(1.0-smoothstep(0.25,0.62,abs(uv.y-crest-1.7)))*0.28;
    float wash=(1.0-smoothstep(0.0,7.0,uv.y))*0.11;
    return clamp((edge*(0.32+0.55*broken)+lace*broken+wash)*(0.65+0.35*e),0.0,1.0);
}
