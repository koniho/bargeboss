package com.bargeboss.game;

/** Shared tow target, sagging line, tension highlights and release recoil. */
public final class TowView {
    private TowView() {}
    public static void draw(Game g,Painter p) {
        Towline line=g.tow;
        Body held=g.heldCargo();
        if(g.canTow(held)) {
            float pulse=2*(float)StrictMath.sin(g.visualTicks*.12);
            ControlView.ring(p,g.tug.x,g.tug.y,39+pulse,1,g.towAiming?ControlView.GREEN:ControlView.BLUE,2.5f);
            RiverPainter.center(p,g.towAiming?"RELEASE TO TOW":"DRAG HERE TO TOW",g.tug.x+50,g.tug.y,9,VisualStyle.CREAM);
        }
        if(line.cargo!=null) {
            float x=line.cargoX(),y=line.cargoY(),xx=line.tugX(g.tug),yy=line.tugY(g.tug);
            float distance=Physics.length(xx-x,yy-y);
            float slack=Physics.clamp((line.length-distance)*.5f,0,20);
            int color=line.tension>180?VisualStyle.GOLD:0xffffe3a1;
            MooringView.rope(p,x,y,xx,yy,slack,color);
            if(line.catchTicks>0) {
                float t=1-line.catchTicks/24f;
                int glow=((int)(220*(1-t))<<24)|0x92ecc2;
                ControlView.ring(p,x,y,8+30*t,1,glow,3);
                ControlView.ring(p,xx,yy,8+30*t,1,glow,3);
            }
            ControlView.ring(p,line.cargo.x,line.cargo.y,44,1,0x9983c8d5,2);
            if(g.tutorial==null || g.tutorial.lesson!=Tutorial.RELEASE) RiverPainter.center(p,"TAP TO LET GO",line.cargo.x-47,line.cargo.y,8,ControlView.BLUE);
            RiverPainter.center(p,line.tension>12?"PULLING":"LINE SLACK",(x+xx)/2+15,(y+yy)/2,8,color);
        } else if(line.releaseTicks>0) {
            float t=line.releaseTicks/18f;
            MooringView.rope(p,line.releaseToX,line.releaseToY,
                    line.releaseToX+(line.releaseX-line.releaseToX)*t,
                    line.releaseToY+(line.releaseY-line.releaseToY)*t,18*t,((int)(230*t)<<24)|0xffe3a1);
        }
    }
}
