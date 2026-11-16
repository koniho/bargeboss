package com.bargeboss.game;

/** Steady marine running lights shared by player, NPC, title and cargo vessels.
 * New deck fixtures use body-local XYZ; the mast uses the reviewed source anchor.
 * This is a readable game rig, not a complete vessel-class/length rules model. */
public final class VesselNavigationLights {
    public static final int PORT=0,STARBOARD=1,STERN=2,MAST=3;
    public static final int RED=0xffff4545,GREEN=0xff39f49a,WHITE=0xffe8f4ff;
    private static final float VIEW_BEARING=(float)(StrictMath.PI*.75),SIDE_ARC=(float)(StrictMath.PI*.625);
    // Elevated-camera readability: two authored heading steps of soft spill past
    // each nominal sector, rather than a single lamp switching at the boundary.
    private static final float CAMERA_OVERLAP=(float)(StrictMath.PI/8);
    private VesselNavigationLights(){}
    public static int count(boolean powered){return powered?4:3;}
    public static String id(int fixture){return fixture==PORT?"nav-port":fixture==STARBOARD?"nav-starboard":fixture==STERN?"nav-stern":"nav-masthead";}
    public static int color(int fixture){return fixture==PORT?RED:fixture==STARBOARD?GREEN:WHITE;}
    public static float localX(Body b,int fixture){if(b.yacht)return fixture==STERN?-52.8f:26.4f;return fixture==STERN?-(b.half+b.radius*.65f):fixture==MAST?0:b.half+b.radius*.12f;}
    public static float localY(Body b,int fixture){if(b.yacht)return fixture==PORT?-9.9f:fixture==STARBOARD?9.9f:0;return fixture==PORT?-b.radius*.65f:fixture==STARBOARD?b.radius*.65f:0;}
    public static float height(int fixture){return fixture==MAST?28:6;}
    public static float worldX(Body b,int fixture){float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);return b.x+localX(b,fixture)*c-localY(b,fixture)*s;}
    public static float worldY(Body b,int fixture){float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);return b.y+localX(b,fixture)*s+localY(b,fixture)*c;}
    /** Independent, overlapping lamp visibility; never choose a single winning fixture. */
    public static float visibility(float heading,int fixture){
        float bearing=Physics.angle(VIEW_BEARING-heading),value;
        if(fixture==PORT)value=Math.min(bearing+SIDE_ARC,-bearing);
        else if(fixture==STARBOARD)value=Math.min(SIDE_ARC-bearing,bearing);
        else if(fixture==STERN)value=Math.abs(bearing)-SIDE_ARC;
        else value=SIDE_ARC-Math.abs(bearing);
        float visible=Physics.clamp(1+value/CAMERA_OVERLAP,0,1);
        return visible*visible*(3-2*visible);
    }
    public static float screenX(Body b,int fixture,boolean powered,RiverCamera camera){
        if(powered && fixture==MAST){float size=2*(b.half+b.radius)/TugArt.HULL_FRACTION;return camera.screenX(b.x,b.y)+TugLightArt.offset(0,b.angle,true,false)*size*camera.scale;}
        return camera.screenX(worldX(b,fixture),worldY(b,fixture));
    }
    public static float screenY(Body b,int fixture,boolean powered,RiverCamera camera,float bob){
        if(powered && fixture==MAST){float size=2*(b.half+b.radius)/TugArt.HULL_FRACTION;return camera.screenY(b.x,b.y)+(TugLightArt.offset(0,b.angle,true,true)*size-bob)*camera.scale;}
        return camera.screenY(worldX(b,fixture),worldY(b,fixture))-((b.yacht?(fixture==STERN?12.1f:15.4f):height(fixture))*.81649658f+bob)*camera.scale;
    }
    private static int alpha(int color,float energy,int strength){return ((int)(Physics.clamp(energy,0,1)*strength)<<24)|(color&0xffffff);}
    private static void glow(Painter p,float x,float y,float scale,float visible,int color,int tick,int fixture){
        // The same cached feathered kernel as factory lamps avoids hard halo
        // disks. Running lamps stay lit; only their scattered glow breathes.
        SceneryEffects.glow(p,x,y,1.8f*scale,color,visible,tick,fixture*1.7f);
        p.circle(x,y,1.35f*scale,alpha(color,visible,235));
        p.circle(x,y,.45f*scale,alpha(WHITE,visible,245));
    }
    /** Barge far fixtures go behind cargo; near coaming fixtures finish in front. */
    public static void draw(Game g,Painter p,Body b,boolean powered,float bob,boolean nearCargo){
        boolean river=p instanceof RiverPainter;RiverCamera c=river?((RiverPainter)p).camera:null;
        Painter canvas=river?((RiverPainter)p).canvas():p;float scale=river?c.scale:1;
        for(int fixture=0;fixture<count(powered);fixture++){
            float visible=visibility(b.angle,fixture);if(visible<=0)continue;
            float wx=worldX(b,fixture),wy=worldY(b,fixture);
            boolean near=river?c.screenY(wx,wy)>=c.screenY(b.x,b.y):wy>=b.y;
            if(!powered && !b.yacht && near!=nearCargo)continue;
            float x=river?screenX(b,fixture,powered,c):wx,y=river?screenY(b,fixture,powered,c,bob):wy-height(fixture)-bob;
            // Newly modeled housings are distinct from painted warm work lamps.
            if(fixture!=MAST)canvas.rect(x-1.8f*scale,y-1.6f*scale,3.6f*scale,3.8f*scale,alpha(0xff101e28,visible,220));
            glow(canvas,x,y,scale,visible,color(fixture),g.visualTicks,fixture);
        }
    }
    /** Colored ripples stay on the water plane and never paint across a quay. */
    public static void reflections(Game g,Painter p,Body b,boolean powered){
        if(!(p instanceof RiverPainter))return;
        RiverCamera c=((RiverPainter)p).camera;Painter water=new WaterEffectsPainter(g,p);
        for(int fixture=0;fixture<count(powered);fixture++){
            float visible=visibility(b.angle,fixture);if(visible<=0)continue;
            float x=c.screenX(worldX(b,fixture),worldY(b,fixture)),y=c.screenY(worldX(b,fixture),worldY(b,fixture));
            for(int i=0;i<5;i++){
                float phase=g.visualTicks*.04f+fixture*1.7f+i*.9f;
                float drift=(float)StrictMath.sin(phase)*1.5f*c.scale;
                float width=(3+i*.7f+(float)StrictMath.sin(phase*.8f))*c.scale;
                float yy=y+(9+i*3)*c.scale;
                water.line(c.worldX(x+drift-width,yy),c.worldY(x+drift-width,yy),c.worldX(x+drift+width,yy),c.worldY(x+drift+width,yy),1.1f,alpha(color(fixture),visible,70-i*10));
            }
        }
    }
}
