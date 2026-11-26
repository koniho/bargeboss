package com.bargeboss.game;

/** Bounded broad atmospheric scattering, above scenery/craft and below HUD.
 * Existing soft materials only: no texture, blur target or particle allocation. */
final class FogLighting {
    static final int MAX_LIGHTS=24;
    private FogLighting(){}
    private static final class Lights {
        int count;final float[] x=new float[MAX_LIGHTS],y=new float[MAX_LIGHTS],size=new float[MAX_LIGHTS],power=new float[MAX_LIGHTS];
        final int[] color=new int[MAX_LIGHTS];
        void add(Game g,float sx,float sy,float diameter,float energy,int rgb){
            float r=diameter*.5f;
            if(energy<=.005f || sx+r<0 || sx-r>g.layout.width || sy+r<0 || sy-r>g.layout.height)return;
            // Fade lights entering the view before ranking; retain a strict submission cap.
            float edge=Math.min(Math.min(sx+r,g.layout.width-sx+r),Math.min(sy+r,g.layout.height-sy+r));
            energy*=SeaWaves.smooth(0,r,edge);
            int at=count;if(at==MAX_LIGHTS){at--;if(energy<=power[at])return;}else count++;
            while(at>0 && energy>power[at-1]){x[at]=x[at-1];y[at]=y[at-1];size[at]=size[at-1];power[at]=power[at-1];color[at]=color[at-1];at--;}
            x[at]=sx;y[at]=sy;size[at]=diameter;power[at]=energy;color[at]=rgb;
        }
    }
    private static final ThreadLocal<Lights> SCRATCH=new ThreadLocal<Lights>(){protected Lights initialValue(){return new Lights();}};
    static void draw(Game g,Painter p){
        float fog=g.weather.fog(g);if(fog==0)return;
        Lights out=SCRATCH.get();out.count=0;RiverCamera c=g.layout.camera;
        for(int id=0;id<3;id++)out.add(g,c.screenX(MontroseBeaconArt.x(id),MontroseBeaconArt.y(id)),
            c.screenY(MontroseBeaconArt.x(id),MontroseBeaconArt.y(id))-MontroseBeaconArt.lampZ(id)*ArticulatedModel.Z_SCALE*c.scale,
            (130+fog*55)*c.scale,MontroseBeaconArt.power(g.visualTicks),MontroseBeaconArt.color(id));
        vessel(g,out,g.tug);if(g.teacherBoat()!=null)vessel(g,out,g.teacherBoat());
        for(Body b:g.cargo)if(b!=null && b.active)vessel(g,out,b);
        for(Feeder f:g.feeders)if(f.phase!=Feeder.IDLE)vessel(g,out,f.boat);
        if(g.trafficActive())vessel(g,out,g.trafficBoat);
        for(int i=0;i<MontroseHarbor.PEDESTALS.length;i++){
            float[] a=MontroseHarbor.PEDESTALS[i];
            out.add(g,c.screenX(a[0],a[1]),c.screenY(a[0],a[1])-MarinaPedestals.LAMP_Z*ArticulatedModel.Z_SCALE*c.scale,
                (70+fog*24)*c.scale,.24f*MarinaPedestals.brightness(g,i),0xffffd68d);
        }
        for(int i=0;i<out.count;i++){
            int alpha=Math.round(fog*23*out.power[i]);if(alpha==0)continue;
            // A single wide continuous falloff, never a sharp lens ring or cone.
            p.material(out.x[i],out.y[i],out.size[i],0,0,out.size[i]*.85f,EffectMaterial.GLOW,0,1,
                (alpha<<24)|(out.color[i]&0xffffff));
        }
    }
    private static void vessel(Game g,Lights out,Body b){
        if(!b.active)return;RiverCamera c=g.layout.camera;float fog=g.weather.fog(g);
        float bob=WaterSurface.lift(g,b);
        if(b.berth<0){
            float size=2*(b.half+b.radius)/TugArt.HULL_FRACTION;
            for(int i=0;i<TugLightArt.COUNT;i++)out.add(g,c.screenX(b.x,b.y)+TugLightArt.offset(i,b.angle,true,false)*size*c.scale,
                c.screenY(b.x,b.y)+(TugLightArt.offset(i,b.angle,true,true)*size-bob)*c.scale,
                (90+fog*32)*c.scale,.65f*TugLightArt.opacity(i,b.angle,true)*SceneryEffects.workIntensity(g.visualTicks,b.id*.71f),0xffffbc6b);
        }
        for(int f=0;f<VesselNavigationLights.sourceCount(b);f++){
            float power=VesselNavigationLights.energy(g,b,b.berth<0,f);if(power<=0)continue;
            float sx=c.screenX(VesselNavigationLights.sourceX(g,b,f),VesselNavigationLights.sourceY(g,b,f));
            float sy=c.screenY(VesselNavigationLights.sourceX(g,b,f),VesselNavigationLights.sourceY(g,b,f))-(VesselNavigationLights.height(b,f)*ArticulatedModel.Z_SCALE+bob)*c.scale;
            if(b.berth<0 && f==VesselNavigationLights.MAST){sx=VesselNavigationLights.screenX(b,f,true,c);sy=VesselNavigationLights.screenY(b,f,true,c,bob);}
            out.add(g,sx,sy,
                (70+fog*27)*c.scale,.5f*power,VesselNavigationLights.color(f));
        }
    }
}
