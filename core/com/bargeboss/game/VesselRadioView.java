package com.bargeboss.game;

/** Soft expanding radio arcs above the registered mast, inside the vessel's depth/shore scope. */
final class VesselRadioView {
    static void draw(Game g,Painter p,Body boat){
        if(!g.radio.transmitting(g,boat))return;
        boolean river=p instanceof RiverPainter;RiverCamera camera=river?((RiverPainter)p).camera:null;
        float scale=river?camera.scale:1,size=2*(boat.half+boat.radius)/TugArt.HULL_FRACTION;
        float sx=(river?camera.screenX(boat.x,boat.y):boat.x)+TugLightArt.offset(0,boat.angle,river,false)*size*scale;
        float sy=(river?camera.screenY(boat.x,boat.y):boat.y)+(TugLightArt.offset(0,boat.angle,river,true)*size-WaterSurface.lift(g,boat))*scale;
        Painter canvas=river?((RiverPainter)p).canvas():p;
        float progress=g.radio.progress(),fade=(float)StrictMath.sin(Math.PI*progress);
        int color=boat==g.tug?PilotGlass.AMBER:0xffa8e9be;
        for(int wave=0;wave<3;wave++){
            float t=progress*1.4f-wave*.24f;if(t<0 || t>1)continue;
            float radius=(5+25*t)*Math.min(scale,2.4f),alpha=fade*(1-t)*.65f;
            for(int side=-1;side<=1;side+=2){
                float px=0,py=0;
                for(int segment=0;segment<=6;segment++){
                    double a=-.65+segment*(1.3/6);
                    float xx=sx+side*radius*(float)StrictMath.cos(a),yy=sy+radius*(float)StrictMath.sin(a);
                    if(segment>0)SoftCue.line(canvas,px,py,xx,yy,1.2f,SoftCue.alpha(color,alpha));
                    px=xx;py=yy;
                }
            }
        }
    }
}
