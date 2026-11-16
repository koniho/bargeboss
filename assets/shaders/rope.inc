// Continuous along-rope coordinates keep braiding registered across depth-sorted spans.
float4 ropeShade(float2 uv) {
    float across=uv.y*2.0-1.0;
    float twist=uv.x*6.2831853+across*3.3;
    float strand=0.5+0.5*cos(twist);
    float fiber=0.5+0.5*sin(uv.x*62.831853+across*16.0);
    float roundness=sqrt(max(0.0,1.0-across*across));
    float light=0.47+0.45*roundness+0.16*strand+0.035*fiber-across*0.10;
    float edge=1.0-smoothstep(0.80,1.0,abs(across));
    return float4(float3(light),1.0)*edge;
}
