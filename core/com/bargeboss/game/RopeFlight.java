package com.bargeboss.game;

/** One ground/height trajectory for the flying tether, shared by depth sorting and drawing.
 *
 * The throw is a whip, not a lob: a short wind-up coils the loop back past the hull, then the tip
 * snaps out on an overshooting ease while an S-wave travels down the line from the hand to the tip
 * and dies out as the loop lands (the crack). A miss springs back slack with a decaying wobble.
 * Timing is unchanged (ROPE_FLIGHT_TICKS / ROPE_RETURN_TICKS); only the drawn shape differs. */
public final class RopeFlight {
    private RopeFlight(){}
    /** Wind-up share of the throw, tip overshoot strength, and wave size as a share of reach. */
    static final float WIND_UP=.16f,OVERSHOOT=1.25f,WAVE=.16f;

    /** Tip progress along the throw at time t: a small back-coil, then an overshooting snap to 1. */
    static float tip(float t){
        if(t<WIND_UP)return -.07f*(float)StrictMath.sin(Math.PI*t/WIND_UP);
        float x=(t-WIND_UP)/(1-WIND_UP)-1;
        return 1+(OVERSHOOT+1)*x*x*x+OVERSHOOT*x*x;
    }
    /** 0..1 strength of the crack flash at the tip (peaks as the wave arrives). */
    static float crack(Game g){
        if(g.ropePhase!=1)return 0;
        float t=g.ropeTicks/(float)Game.ROPE_FLIGHT_TICKS;
        return Physics.clamp(1-Math.abs(t-.86f)/.14f,0,1);
    }
    public static void sample(Game g,float fraction,float[] out) {
        boolean returning=g.ropePhase==2;
        float t=Physics.clamp(g.ropeTicks/(float)(returning?Game.ROPE_RETURN_TICKS:Game.ROPE_FLIGHT_TICKS),0,1);
        float distance=returning?1-(1-(1-t)*(1-t)):tip(t),u=distance*fraction;
        float currentX=g.ropeOriginX(),currentY=g.ropeOriginY(),currentHeight=RopeAttachment.height(g.ropeBody)+WaterSurface.lift(g,g.ropeBody);
        float sx=returning?currentX:g.ropeStartX,sy=returning?currentY:g.ropeStartY,h=returning?currentHeight:g.ropeStartHeight;
        float endRise=!g.ropeToTug && !g.montroseStage()?GroundArt.LAND_RISE:0;
        float ex=g.ropeEndX-RiverCamera.riseX(endRise),ey=g.ropeEndY-RiverCamera.riseY(endRise);
        float dx=ex-sx,dy=ey-sy,reach=Math.max(1,Physics.length(dx,dy)),nx=-dy/reach,ny=dx/reach;
        // Travelling S-wave: zero at the hand and the tip, strongest mid-line, gone when the loop lands.
        float envelope=(float)StrictMath.sin(Math.PI*Physics.clamp(fraction,0,1));
        float wave,lift;
        if(returning){
            float decay=(1-t)*(1-t);
            wave=WAVE*.7f*reach*decay*envelope*(float)StrictMath.sin(2*Math.PI*(fraction*2.1f+t*3.2f));
            lift=0;
        }else{
            float decay=(float)StrictMath.pow(1-t,1.4);
            float phase=2*(float)Math.PI*(fraction*1.3f-t*2.6f);
            wave=WAVE*reach*decay*envelope*(float)StrictMath.sin(phase);
            lift=.45f*WAVE*reach*decay*envelope*(float)StrictMath.cos(phase);
        }
        wave=Math.min(Math.abs(wave),38)*Math.signum(wave);
        out[0]=sx+dx*u+(currentX-sx)*(1-fraction)+nx*wave;
        out[1]=sy+dy*u+(currentY-sy)*(1-fraction)+ny*wave;
        // A flat, fast line: a low arc that peaks early, plus the wave's vertical component.
        float arc=LassoPath.ARC_HEIGHT*.55f*(float)StrictMath.sin(Math.PI*Physics.clamp(u,0,1));
        out[2]=Math.max(.5f,h*(1-u)+arc+Math.max(-6,Math.min(18,lift))+(currentHeight-h)*(1-fraction)+endRise*Physics.clamp(u,0,1));
    }
}
