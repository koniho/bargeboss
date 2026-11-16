#include <metal_stdlib>
using namespace metal;
#include "../../assets/shaders/water-foam.inc"
#include "../../assets/shaders/rope.inc"
#include "../../assets/shaders/effects.inc"
struct Vertex { float2 position; float2 uv; float4 color; uint kind; float phase; float detail; };
struct Raster { float4 position [[position]]; float2 uv; float4 color; uint kind [[flat]]; float phase [[flat]]; float detail [[flat]]; };
vertex Raster bbVertex(uint index [[vertex_id]], const device Vertex *vertices [[buffer(0)]], constant float4 &transform [[buffer(1)]]) {
    Vertex v=vertices[index];
    return {float4(v.position*transform.xy+transform.zw,0,1),v.uv,v.color,v.kind,v.phase,v.detail};
}
fragment float4 bbFragment(Raster v [[stage_in]], texture2d<float> image [[texture(0)]]) {
    constexpr sampler sampling(coord::normalized,address::clamp_to_edge,filter::linear);
    if(v.kind>=11) return v.color*effectCoverage(v.uv,float(v.kind-11),v.detail,v.phase);
    if(v.kind==10) return v.color*ropeShade(v.uv);
    if(v.kind>=2) return v.color*foamCoverage(v.uv,v.phase,float(v.kind-2));
    if(v.kind==1) {
        float distance=length(v.uv*2-1);
        float coverage=1-smoothstep(1-fwidth(distance),1,distance);
        return v.color*coverage;
    }
    return image.sample(sampling,v.uv)*v.color;
}
