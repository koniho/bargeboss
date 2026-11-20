#version 450
#extension GL_GOOGLE_include_directive : require
#define float2 vec2
#define float3 vec3
#define float4 vec4
#include "water-foam.inc"
#include "rope.inc"
#include "effects.inc"
#include "surface.inc"
#include "shoreline.inc"
#include "boat-surface-light.inc"
layout(set=0,binding=0) uniform sampler2D imageA;
layout(set=0,binding=1) uniform sampler2D imageB;
layout(location=0) in vec2 texcoord;
layout(location=1) in vec4 tint;
layout(location=2) flat in vec4 effect;
layout(location=3) in vec4 lighting;
layout(early_fragment_tests) in;
layout(location=0) out vec4 result;
layout(constant_id=0) const int family=0;
vec4 cell(sampler2D image,vec2 uv){if(any(lessThan(uv,vec2(0)))||any(greaterThan(uv,vec2(1))))return vec4(0);return texture(image,uv);}
void main(){
 float kind=effect.x,phase=effect.y,detail=effect.z;vec2 uv=texcoord;
 if(family==40){result=mix(cell(imageA,uv),cell(imageB,lighting.xy),phase)*tint;return;}
 if(family>=32){result=tint*foamCoverage(uv,phase,kind-24);return;}
 if(family==30){vec4 t=texture(imageA,uv);result=tint*t.a*boatSurfaceLight(t.rgb/max(t.a,.001),uv,lighting.xyz,lighting.w);return;}
 if(family>=20){result=tint*shorelineCoverage(uv,phase,detail,kind-20);return;}
 if(family>=14){result=vec4(min(tint.rgb*surfaceShade(uv,kind-14,phase),vec3(tint.a)),tint.a);return;}
 if(family>=11){result=tint*effectCoverage(uv,kind-11,detail,phase);return;}
 if(family==10){result=tint*ropeShade(uv);return;}
 if(family>=2){result=tint*foamCoverage(uv,phase,kind-2);return;}
 if(kind==1){float d=length(uv*2-1);result=tint*(1-smoothstep(1-fwidth(d),1,d));return;}
 result=texture(imageA,uv)*tint;
}
