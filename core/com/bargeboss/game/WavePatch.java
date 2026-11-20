package com.bargeboss.game;

/** Local affine water surface. Only water decals tilt; production hull artwork stays upright. */
final class WavePatch {
    private WavePatch(){}
    static void sprite(Game g,Painter p,int atlas,int frame,float x,float y,float ax,float ay,float bx,float by,float alpha){
        if(g.montroseStage() && g.mode!=Game.TITLE && p instanceof RiverPainter){
            float h=g.water.height(g,x,y,g.visualTicks,null);
            float ha=g.water.height(g,x+ax*.5f,y+ay*.5f,g.visualTicks,null)-g.water.height(g,x-ax*.5f,y-ay*.5f,g.visualTicks,null);
            float hb=g.water.height(g,x+bx*.5f,y+by*.5f,g.visualTicks,null)-g.water.height(g,x-bx*.5f,y-by*.5f,g.visualTicks,null);
            x+=RiverCamera.riseX(h);y+=RiverCamera.riseY(h);
            ax+=RiverCamera.riseX(ha);ay+=RiverCamera.riseY(ha);bx+=RiverCamera.riseX(hb);by+=RiverCamera.riseY(hb);
        }
        p.spriteAffine(atlas,frame,x,y,ax,ay,bx,by,alpha);
    }
    static void foam(Game g,Painter p,float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha){
        if(g.montroseStage() && p instanceof RiverPainter){
            float h=SeaWaves.value(g,x,y,g.visualTicks,0);
            float ha=SeaWaves.value(g,x+ax*.5f,y+ay*.5f,g.visualTicks,0)-SeaWaves.value(g,x-ax*.5f,y-ay*.5f,g.visualTicks,0);
            float hb=SeaWaves.value(g,x+bx*.5f,y+by*.5f,g.visualTicks,0)-SeaWaves.value(g,x-bx*.5f,y-by*.5f,g.visualTicks,0);
            x+=RiverCamera.riseX(h);y+=RiverCamera.riseY(h);
            ax+=RiverCamera.riseX(ha);ay+=RiverCamera.riseY(ha);bx+=RiverCamera.riseX(hb);by+=RiverCamera.riseY(hb);
        }
        p.waterFoam(x,y,ax,ay,bx,by,phase,kind,alpha);
    }
}
