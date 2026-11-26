package com.bargeboss.game;

/** Active rescue hulls reuse the authored 32-pose cabin-cruiser models and normal receivers. */
final class CruiserView {
    static void draw(Game g,Painter world,Body b){
        RiverPainter river=world instanceof RiverPainter?(RiverPainter)world:null;
        if(river==null)return;
        RiverCamera c=river.camera;Painter p=river.canvas();float rise=g.haulout.vesselRise(b),heave=rise>0?rise:WaterSurface.lift(g,b)-HullDamage.sinkDepth(b);
        float[] water=ShoreOcclusion.vessel(g,c,b,true);p.pushClipOutside(water);
        try{if(rise<=0){if(b.racing)RacingBoatArt.shadow(g,p,b,c);VesselNavigationLights.reflections(g,world,b,false);VesselRipples.draw(g,world,b.id,b.x,b.y,b.angle,b.hullKind>=0?MooredBoats.length(b.hullKind):b.racing?56:38,b.hullKind>=0?MooredBoats.width(b.hullKind):b.racing?14:16,b.speed(),false);}}finally{p.popClip();}
        float heading=b.angle*32/(2*(float)Math.PI),whole=(float)StrictMath.floor(heading),mix=heading-whole;
        int base=(b.hullKind>=0?b.hullKind:b.racing?6:b.berth%2)*32,a=base+Math.floorMod((int)whole,32),next=base+Math.floorMod((int)whole+1,32);
        float x=c.screenX(b.x,b.y),y=c.screenY(b.x,b.y)-heave*c.scale,size=MooredBoats.CELL_WORLD*c.scale;
        float sink=HullDamage.progress(b),alpha=.92f*(1-sink*sink),second=alpha*mix,first=alpha*(1-mix)/(1-second);
        if(alpha<.01f)return;
        VesselNavigationLights.draw(g,world,b,false,heave,false);
        p.sprite(SpriteAtlas.MOORED_BOATS,a,x,y,size,size,0,first);if(second>.001f)p.sprite(SpriteAtlas.MOORED_BOATS,next,x,y,size,size,0,second);
        MooredBoatLight.Lights lights=MooredBoatLight.lights(g,b.x,b.y,heave);
        for(int n=0;n<lights.count;n++){
            float e=lights.energy[n];p.normalSprite(a,x,y,size,size,lights.x[n],lights.y[n],lights.z[n],lights.radius[n],lights.color[n],e*(1-mix));
            if(mix>.001f)p.normalSprite(next,x,y,size,size,lights.x[n],lights.y[n],lights.z[n],lights.radius[n],lights.color[n],e*mix);
        }
        VesselNavigationLights.draw(g,world,b,false,heave,true);
    }
    private CruiserView(){}
}
