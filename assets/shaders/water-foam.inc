// Shared Metal / AGSL kernel. Local UVs only; all placement and time come from Java.
// No scene texture reads, iterative fluid simulation or full-screen pass.
float foamCoverage(float2 uv, float phase, float kind) {
    float2 p = uv * 2.0 - 1.0;
    float edge = (1.0-smoothstep(0.78,1.0,abs(p.x))) * (1.0-smoothstep(0.78,1.0,abs(p.y)));
    float warp = sin(p.x*7.0+phase)*0.09 + sin(p.y*9.0-phase*2.0)*0.06;
    float grain = 0.5 + 0.25*sin(p.x*23.0+p.y*17.0+phase) + 0.25*sin(p.x*37.0-p.y*29.0-phase*2.0);
    if (kind >= 4.0) {
        // Broken crest: broad translucent swell beneath a narrow, uneven foam ridge.
        float center = kind < 5.0 ? 0.3*p.x*p.x-0.12 : 0.0;
        float d = abs(p.y-center-warp);
        float swell = (1.0-smoothstep(0.12,0.52,d))*0.22;
        float ridge = (1.0-smoothstep(0.035,0.17,d))*smoothstep(0.20,0.65,grain);
        return edge*(swell+ridge*0.78);
    }
    // Shoulder wakes curl into long wisps; prop wash breaks into cellular islands.
    float curve = kind < 2.0 ? 0.28*sin(p.x*2.4+kind*3.141593)+warp : warp;
    float d = length(float2(p.x*0.82,(p.y-curve)*1.12));
    float envelope = 1.0-smoothstep(0.34,1.0,d);
    float filaments = 0.5+0.5*sin((p.y-curve)*20.0+sin(p.x*8.0+phase)*1.8-phase);
    float broken = smoothstep(kind > 2.5 ? 0.40 : 0.28,0.78,grain*0.65+filaments*0.35);
    return edge*envelope*(0.12+0.88*broken);
}
