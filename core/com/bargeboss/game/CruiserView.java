package com.bargeboss.game;

/** Active rescue hulls reuse the authored 32-pose cabin-cruiser models and normal receivers. */
final class CruiserView {
    static void draw(Game g,Painter world,Body b){
        RiverPainter river=world instanceof RiverPainter?(RiverPainter)world:null;
        if(river==null)return;
        RiverCamera c=river.camera;Painter p=river.canvas();float rise=g.haulout.vesselRise(b),heave=rise>0?rise:WaterSurface.lift(g,b)-HullDamage.sinkDepth(b);
        float[] water=ShoreOcclusion.vessel(g,c,b,true);p.pushClipOutside(water);
        try{if(rise<=0){if(b.racing || b.hullKind==PoliceBoatModel.KIND)RacingBoatArt.shadow(g,p,b,c);if(b.neon!=0)underglow(g,p,b,c,heave);VesselNavigationLights.reflections(g,world,b,false);VesselRipples.draw(g,world,b.id,b.x,b.y,b.angle,b.hullKind>=0?MooredBoats.length(b.hullKind):b.racing?56:38,b.hullKind>=0?MooredBoats.width(b.hullKind):b.racing?14:16,b.speed(),false);}}finally{p.popClip();}
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
        if(b.neon!=0){
            // Custom paint: flood the hull with its colour from overhead and a low side light (native lit sprites).
            int frameA=a,frameB=next;float wobble=.85f+.15f*(float)StrictMath.sin(g.visualTicks*.21f+b.id);
            p.normalSprite(frameA,x,y,size,size,0,0,46,140,b.neon,alpha*(1-mix)*wobble);
            p.normalSprite(frameA,x,y,size,size,-30,30,14,90,b.neon,alpha*(1-mix)*.8f);
            if(mix>.001f){p.normalSprite(frameB,x,y,size,size,0,0,46,140,b.neon,alpha*mix*wobble);p.normalSprite(frameB,x,y,size,size,-30,30,14,90,b.neon,alpha*mix*.8f);}
        }
        VesselNavigationLights.draw(g,world,b,false,heave,true);
        if(b.hullKind==PoliceBoatModel.KIND)PoliceBoatArt.strobes(g,p,b,c,heave);
    }
    /** Neon underglow on the water, in the hull's own ripple layer (same shore clip and depth order):
     * an ellipse along the keel in world space, projected, brightening with speed. */
    static void underglow(Game g,Painter p,Body b,RiverCamera c,float heave){
        float len=(b.hullKind>=0?MooredBoats.length(b.hullKind):38)*.85f,wid=(b.hullKind>=0?MooredBoats.width(b.hullKind):16)*1.6f;
        float cs=(float)StrictMath.cos(b.angle),sn=(float)StrictMath.sin(b.angle),qx=RiverCamera.Q*c.scale,qy=RiverCamera.Q*RiverCamera.DEPTH*c.scale;
        // World axis (dx,dy) -> screen (qx*(dx+dy), qy*(dy-dx)).
        float ax=qx*(cs+sn)*len,ay=qy*(sn-cs)*len,bx=qx*(-sn+cs)*wid,by=qy*(cs+sn)*wid;
        float sx=c.screenX(b.x,b.y),sy=c.screenY(b.x,b.y);
        float pulse=.8f+.2f*(float)StrictMath.sin(g.visualTicks*.25f+b.id),speed=Math.min(1,b.speed()/40f),strength=(.3f+.7f*speed)*pulse;
        p.material(sx,sy,ax*2.1f,ay*2.1f,bx*2.1f,by*2.1f,EffectMaterial.GLOW,0,1,SoftCue.alpha(b.neon,.45f*strength));
        p.material(sx,sy,ax*1.15f,ay*1.15f,bx*1.15f,by*1.15f,EffectMaterial.GLOW,0,1,SoftCue.alpha(b.neon,.7f*strength));
    }
    private CruiserView(){}
}
