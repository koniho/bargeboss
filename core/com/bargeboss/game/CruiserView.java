package com.bargeboss.game;

/** Active rescue/haulout cruisers share the marina rigid models, with distant sprite LOD. */
final class CruiserView {
    static void draw(Game g,Painter world,Body b){
        RiverPainter river=world instanceof RiverPainter?(RiverPainter)world:null;
        if(river==null)return;
        RiverCamera c=river.camera;Painter p=river.canvas();float heave=WaterSurface.lift(g,b);
        float[] water=ShoreOcclusion.vessel(g,c,b,true);p.pushClipOutside(water);
        try{VesselNavigationLights.reflections(g,world,b,false);VesselRipples.draw(g,world,b.id,b.x,b.y,b.angle,38,16,b.speed(),false);}finally{p.popClip();}
        float heading=b.angle*32/(2*(float)Math.PI),whole=(float)StrictMath.floor(heading),mix=heading-whole;
        int base=(b.berth%2)*32,a=base+Math.floorMod((int)whole,32),next=base+Math.floorMod((int)whole+1,32);
        float x=c.screenX(b.x,b.y),y=c.screenY(b.x,b.y)-heave*c.scale,size=MooredBoats.CELL_WORLD*c.scale;
        int kind=b.berth%2;float live=MooredBoats.liveWeight(g,MooredBoats.length(kind),b.x,b.y);
        float alpha=1,second=alpha*mix,first=alpha*(1-mix)/(1-second);
        VesselNavigationLights.draw(g,world,b,false,heave,false);
        if(live<.999f){p.sprite(SpriteAtlas.MOORED_BOATS,a,x,y,size,size,0,first);if(second>.001f)p.sprite(SpriteAtlas.MOORED_BOATS,next,x,y,size,size,0,second);}
        MooredBoatLight.Lights lights=MooredBoatLight.lights(g,b.x,b.y,heave);
        if(live>0)MooredBoats.drawHull(g,p,kind,kind==0?1:3,b.x,b.y,heave,b.angle,0,0,live,lights);
        for(int n=0;live<.999f && n<lights.count;n++){
            float e=lights.energy[n]*(1-live);p.normalSprite(a,x,y,size,size,lights.x[n],lights.y[n],lights.z[n],lights.radius[n],lights.color[n],e*(1-mix));
            if(mix>.001f)p.normalSprite(next,x,y,size,size,lights.x[n],lights.y[n],lights.z[n],lights.radius[n],lights.color[n],e*mix);
        }
        VesselNavigationLights.draw(g,world,b,false,heave,true);
    }
    private CruiserView(){}
}
