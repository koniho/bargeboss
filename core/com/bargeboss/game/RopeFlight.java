package com.bargeboss.game;

/** One ground/height trajectory for the flying tether, shared by depth sorting and drawing. */
public final class RopeFlight {
    private RopeFlight(){}
    public static void sample(Game g,float fraction,float[] out) {
        boolean returning=g.ropePhase==2;
        float t=Physics.clamp(g.ropeTicks/(float)(returning?Game.ROPE_RETURN_TICKS:Game.ROPE_FLIGHT_TICKS),0,1);
        float eased=1-(1-t)*(1-t),distance=returning?1-eased:eased,u=distance*fraction;
        float currentX=g.ropeOriginX(),currentY=g.ropeOriginY(),currentHeight=RopeAttachment.height(g.ropeBody)+WaterSurface.lift(g,g.ropeBody);
        float sx=returning?currentX:g.ropeStartX,sy=returning?currentY:g.ropeStartY,h=returning?currentHeight:g.ropeStartHeight;
        float endRise=!g.ropeToTug && g.tutorial!=null?GroundArt.LAND_RISE:0;
        out[0]=sx+(g.ropeEndX-RiverCamera.riseX(endRise)-sx)*u+(currentX-sx)*(1-fraction);
        out[1]=sy+(g.ropeEndY-RiverCamera.riseY(endRise)-sy)*u+(currentY-sy)*(1-fraction);
        out[2]=h*(1-u)+LassoPath.ARC_HEIGHT*(float)StrictMath.sin(u*Math.PI)+(currentHeight-h)*(1-fraction)+endRise*u;
    }
}
