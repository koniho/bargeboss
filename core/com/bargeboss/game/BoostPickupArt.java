package com.bargeboss.game;

/** Arcade energy pickups: screen-facing prismatic glyph above an open, projected water orbit.
 * Fixed geometry/counts and shared ticks only; no particle store or generated frame textures. */
final class BoostPickupArt {
    private static final int CYAN=0xff42e6f5,ICE=0xffdcfff4,GOLD=0xffffc52f;
    private static final float TAU=(float)(StrictMath.PI*2);
    private static final float[] BOLT={3,-17,-11,3,-2,3,-6,18,13,-5,3,-5,8,-17};
    private BoostPickupArt(){}
    private static float sin(float a){return (float)StrictMath.sin(a);}
    private static float cos(float a){return (float)StrictMath.cos(a);}
    private static float phase(Game g,int i){return g.visualTicks*.025f+i*1.73f;}
    private static int alpha(int color,float amount){return SoftCue.alpha(color,Physics.clamp(amount,0,1));}
    private static float pulse(Game g,int i){return .78f+.22f*sin(phase(g,i)*2);}
    private static float flash(Game g,int i){return g.transit.flashBuoy==i && g.transit.flash>0?1-g.transit.flash/40f:-1;}

    /** Caller owns the all-shore water cutout; every orbit stays underneath vessels. */
    static void water(Game g,Painter p,int i){
        RiverCamera c=g.layout.camera;float x=MontroseTransit.BUOYS[i*2],y=MontroseTransit.BUOYS[i*2+1];
        float sx=c.screenX(x,y),sy=c.screenY(x,y),s=c.scale,t=phase(g,i),power=pulse(g,i);
        boolean ready=g.transit.spent[i]==0;float hit=flash(g,i);
        if(ready){
            // Painted surface texture ties the luminous circuit to moving water.
            p.spriteAffine(SpriteAtlas.RIPPLES,i%4,sx,sy,112*s,0,0,112*s*RiverCamera.DEPTH,.13f*power);
            p.material(sx,sy,70*s,0,0,38*s,EffectMaterial.GLOW,0,1,alpha(CYAN,.18f*power));
            // Three tapered comets, with room between them: no flat target hoop.
            for(int arm=0;arm<3;arm++){
                float start=t+arm*TAU/3;
                for(int j=0;j<9;j++){
                    float u=j/9f,a=start+u*1.32f,b=start+(j+1)/9f*1.32f;
                    float bright=(.1f+.7f*u)*power;
                    ribbon(p,sx,sy,MontroseTransit.REACH*s,a,b,(2+u*2)*s,alpha(CYAN,bright*.19f));
                    ribbon(p,sx,sy,MontroseTransit.REACH*s,a,b,(.35f+u*.85f)*s,alpha(CYAN,bright));
                }
                float tip=start+1.32f;
                star(p,sx+cos(tip)*48*s,sy+sin(tip)*48*s*RiverCamera.DEPTH,2.2f*s,alpha(ICE,.85f*power));
            }
            // Inward chevrons breathe together; their center remains open for the vessel.
            for(int k=0;k<4;k++){
                float a=k*TAU/4+.7854f,r=37+2*sin(t*2),cx=sx+cos(a)*r*s,cy=sy+sin(a)*r*s*RiverCamera.DEPTH;
                float dx=cos(a)*s,dy=sin(a)*s*RiverCamera.DEPTH;
                float nx=-sin(a)*s,ny=cos(a)*s*RiverCamera.DEPTH;
                p.poly(new float[]{cx+dx*6+nx*5,cy+dy*6+ny*5,cx,cy,cx+dx*6-nx*5,cy+dy*6-ny*5,cx+dx*3,cy+dy*3},alpha(GOLD,.65f*power));
            }
        }else if(hit<0){
            // Actual re-arm progress, quiet enough to distinguish an empty pickup.
            float charge=1-g.transit.spent[i]/(float)MontroseTransit.REARM_TICKS;
            int steps=24;for(int k=0;k<steps;k++){
                float a=-TAU*.25f+k*TAU/steps,b=a+TAU/steps*.68f;
                int color=k/(float)steps<charge?0x7042e6f5:0x183b7285;
                p.line(sx+cos(a)*13*s,sy+sin(a)*13*s*RiverCamera.DEPTH,sx+cos(b)*13*s,sy+sin(b)*13*s*RiverCamera.DEPTH,.9f*s,color);
            }
        }
        if(hit>=0){
            float r=(22+hit*58)*s,fade=(1-hit)*(1-hit);
            p.spriteAffine(SpriteAtlas.RIPPLES,(i+1)%4,sx,sy,r*2,0,0,r*2*RiverCamera.DEPTH,.55f*fade);
            for(int k=0;k<8;k++){
                float a=k*TAU/8+.2f,rr=(16+hit*60)*s;
                float xx=sx+cos(a)*rr,yy=sy+sin(a)*rr*RiverCamera.DEPTH;
                p.line(xx,yy,xx-cos(a)*(3+hit*7)*s,yy-sin(a)*(3+hit*7)*s*RiverCamera.DEPTH,1.4f*s,alpha(GOLD,fade*.8f));
                star(p,xx,yy,(1.2f+fade*1.4f)*s,alpha(ICE,fade));
            }
        }
    }

    /** A holographic collectible, not a physical buoy. Ground depth and shore clipping still apply. */
    static void draw(Game g,Painter p,int i){
        boolean ready=g.transit.spent[i]==0;float hit=flash(g,i);
        if(!ready && (hit<0 || hit>.3f))return;
        RiverCamera c=g.layout.camera;float wx=MontroseTransit.BUOYS[i*2],wy=MontroseTransit.BUOYS[i*2+1],s=c.scale;
        float t=phase(g,i),scale=ready?1:Math.max(0,1-hit/.3f),power=pulse(g,i);
        float x=c.screenX(wx,wy),y=c.screenY(wx,wy)-(31+sin(t*2)*3+(ready?0:hit*40))*s;
        p.pushClipOutside(ShoreOcclusion.region(g,c,wx-80,wy-80,wx+80,wy+80,c.screenY(wx,wy),p.supportsUnionClips()));
        try{
            float unit=s*scale;
            p.material(x,y,61*unit,0,0,72*unit,EffectMaterial.GLOW,0,1,alpha(CYAN,.26f*power*scale));
            // Four open beveled brackets frame the lightning without a filled badge/disc.
            for(int side=-1;side<=1;side+=2){
                float xx=x+side*23*unit;
                p.poly(new float[]{x+side*5*unit,y-27*unit,xx,y-8*unit,xx,y+8*unit,x+side*5*unit,y+27*unit,x+side*9*unit,y+18*unit,x+side*18*unit,y+6*unit,x+side*18*unit,y-6*unit,x+side*9*unit,y-18*unit},alpha(0xff126277,scale));
                p.line(x+side*5*unit,y-27*unit,xx,y-8*unit,1.3f*unit,alpha(ICE,.9f*scale));
                p.line(xx,y-8*unit,xx,y+8*unit,1.8f*unit,alpha(CYAN,scale));
                p.line(xx,y+8*unit,x+side*5*unit,y+27*unit,.8f*unit,alpha(CYAN,.65f*scale));
            }
            // Extruded gold core and cream bevel; fixed authored glyph, not a platform font.
            shape(p,BOLT,x+1.8f*unit,y+2.7f*unit,unit,alpha(0xff8e4b14,scale));
            shape(p,BOLT,x,y,unit,alpha(GOLD,scale));
            shape(p,new float[]{3,-17,-11,3,-7,1,5,-14},x,y,unit,alpha(0xfffff3c1,scale));
            shape(p,new float[]{3,-5,13,-5,-6,18,-1,11,9,-3},x,y,unit,alpha(0xfff28b17,scale));
            p.line(x+3*unit,y-17*unit,x+8*unit,y-17*unit,1.3f*unit,alpha(ICE,scale));
            star(p,x+5*unit,y-16*unit,3.2f*unit,alpha(0xfffff6cb,power*scale));
            if(ready)for(int j=0;j<4;j++){
                float u=(g.visualTicks+j*23+i*11)%92/92f;
                float xx=x+sin(j*2.4f+u*2)*17*s,yy=c.screenY(wx,wy)-(3+u*30)*s;
                star(p,xx,yy,(.7f+u*.7f)*s,alpha(CYAN,(float)StrictMath.sin(u*StrictMath.PI)*.7f));
            }
        }finally{p.popClip();}
    }
    /** Adjacent quads share endpoints so translucent orbit joints never form bright beads. */
    private static void ribbon(Painter p,float x,float y,float r,float a,float b,float width,int color){
        float inner=r-width*.5f,outer=r+width*.5f,d=RiverCamera.DEPTH;
        p.poly(new float[]{x+cos(a)*inner,y+sin(a)*inner*d,x+cos(a)*outer,y+sin(a)*outer*d,
                x+cos(b)*outer,y+sin(b)*outer*d,x+cos(b)*inner,y+sin(b)*inner*d},color);
    }
    private static void shape(Painter p,float[] points,float x,float y,float scale,int color){
        float[] q=new float[points.length];for(int k=0;k<q.length;k+=2){q[k]=x+points[k]*scale;q[k+1]=y+points[k+1]*scale;}p.poly(q,color);
    }
    private static void star(Painter p,float x,float y,float r,int color){
        p.poly(new float[]{x,y-r*1.5f,x+r*.3f,y-r*.3f,x+r,y,x+r*.3f,y+r*.3f,x,y+r*1.5f,x-r*.3f,y+r*.3f,x-r,y,x-r*.3f,y-r*.3f},color);
    }
}
