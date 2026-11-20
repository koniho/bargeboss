#version 450
layout(location=0) in vec2 position;
layout(location=1) in vec2 uv;
layout(location=2) in vec4 color;
layout(location=3) in vec4 params;
layout(location=4) in vec4 light;
layout(location=0) out vec2 texcoord;
layout(location=1) out vec4 tint;
layout(location=2) flat out vec4 effect;
layout(location=3) out vec4 lighting;
layout(push_constant) uniform Transform {vec4 transform;} push;
void main(){gl_Position=vec4(position*push.transform.xy+push.transform.zw,0,1);texcoord=uv;tint=color;effect=params;lighting=light;}
