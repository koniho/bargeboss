package com.bargeboss.game;

/** Painted floating boost canisters and sparse foam spirals. All motion uses shared ticks. */
final class BoostPickupArt {
    private static final float TAU=(float)(StrictMath.PI*2);
    private BoostPickupArt(){}
    private static float sin(float a){return (float)StrictMath.sin(a);}
    private static float cos(float a){return (float)StrictMath.cos(a);}
    private static float flash(Game g,int i){return g.transit.flashBuoy==i && g.transit.flash>0?1-g.transit.flash/40f:-1;}
    /** Caller owns the all-shore water cutout. Foam remains below every hull. */
    static void water(Game g,Painter p,int i){
        RiverCamera c=g.layout.camera;float x=MontroseTransit.BUOYS[i*2],y=MontroseTransit.BUOYS[i*2+1],s=c.scale;
        float sx=c.screenX(x,y),sy=c.screenY(x,y),hit=flash(g,i);
        if(g.transit.spent[i]==0){
            Painter water=new RiverPainter(p,c);float t=g.visualTicks*.009f+i*1.73f;
            // Broken, slowly turning foam strands rather than a luminous targeting circle.
            for(int arm=0;arm<2;arm++)for(int j=0;j<5;j++){
                float u=j/4f,a=t+arm*TAU*.5f+u*1.6f,r=16+u*17;
                float xx=x+cos(a)*r,yy=y+sin(a)*r,length=6+u*7,width=2+u*2;
                water.spriteAffine(SpriteAtlas.FOAM,(i+j)%4,xx,yy,-sin(a)*length,cos(a)*length,cos(a)*width,sin(a)*width,(.1f+.08f*u)*( .85f+.15f*sin(t+j)));
            }
            // A small contact wake anchors the heavy flotation collar in the water.
            p.spriteAffine(SpriteAtlas.RIPPLES,i%4,sx,sy,25*s,0,0,25*s*RiverCamera.DEPTH,.16f);
        }
        if(hit>=0){
            float r=(12+hit*45)*s,fade=(1-hit)*(1-hit);
            p.spriteAffine(SpriteAtlas.RIPPLES,(i+1)%4,sx,sy,r*2,0,0,r*2*RiverCamera.DEPTH,.4f*fade);
            for(int k=0;k<5;k++){
                float a=k*TAU/5+.2f,rr=(8+hit*25)*s;
                star(p,sx+cos(a)*rr,sy+sin(a)*rr*RiverCamera.DEPTH-hit*10*s,(.65f+fade*.6f)*s,SoftCue.alpha(0xffffdea0,fade*.7f));
            }
        }
    }
    /** Fixed painted perspective; only physical wave heave/roll tilts the canister. */
    static void draw(Game g,Painter p,int i){
        boolean ready=g.transit.spent[i]==0;float hit=flash(g,i);
        if(!ready && (hit<0 || hit>.3f))return;
        RiverCamera c=g.layout.camera;float wx=MontroseTransit.BUOYS[i*2],wy=MontroseTransit.BUOYS[i*2+1],s=c.scale;
        float fade=ready?1:Math.max(0,1-hit/.3f);
        float height=g.water.height(g,wx,wy,g.visualTicks,null);
        float slope=g.water.height(g,wx+6,wy+6,g.visualTicks,null)-g.water.height(g,wx-6,wy-6,g.visualTicks,null);
        float roll=Physics.clamp(slope*.06f,-.07f,.07f),ca=cos(roll),sa=sin(roll);
        float x=c.screenX(wx,wy),y=c.screenY(wx,wy)-height*s;
        p.pushClipOutside(ShoreOcclusion.region(g,c,wx-32,wy-32,wx+32,wy+32,c.screenY(wx,wy),p.supportsUnionClips()));
        try{
            p.spriteAffine(SpriteAtlas.BOOST_CANISTER,0,x,y-(ready?0:hit*12*s),ca*16*s,sa*16*s,-sa*24*s,ca*24*s,fade);
            // Soft wet collar seam, registered to the artwork's base rather than a halo.
            p.spriteAffine(SpriteAtlas.FOAM,i%4,x,y,13*s,0,0,3*s,.2f*fade);
        }finally{p.popClip();}
    }
    private static void star(Painter p,float x,float y,float r,int color){
        p.poly(new float[]{x,y-r*1.5f,x+r*.25f,y-r*.25f,x+r,y,x+r*.25f,y+r*.25f,x,y+r*1.5f,x-r*.25f,y+r*.25f,x-r,y,x-r*.25f,y-r*.25f},color);
    }
}
