// RG: octahedral world normal; B: physical height / 64. UV uses the shared 96-unit cell.
float boatSurfaceLight(float3 data, float2 uv, float3 lamp, float radius) {
    float2 f=data.rg*2.0-1.0;
    float3 n=float3(f,1.0-abs(f.x)-abs(f.y));
    if(n.z<0.0) n.xy=(1.0-abs(n.yx))*float2(n.x>=0.0?1.0:-1.0,n.y>=0.0?1.0:-1.0);
    n=normalize(n);
    float z=data.b*64.0;
    float sum=(uv.x-.5)*96.0/0.70710678;
    float diff=((uv.y-.7)*96.0+0.81649658*z)/(0.70710678*0.57735026);
    float3 delta=lamp-float3((sum-diff)*.5,(sum+diff)*.5,z);
    float d2=dot(delta,delta),falloff=max(0.0,1.0-d2/(radius*radius));
    return max(0.0,dot(n,delta)/sqrt(max(.0001,d2)))*falloff*falloff;
}
