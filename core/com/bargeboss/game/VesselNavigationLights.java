package com.bargeboss.game;

/** Marine running lights shared by player, NPC, title and cargo vessels.
 * New deck fixtures use body-local XYZ; the mast uses the reviewed source anchor.
 * This is a readable game rig, not a complete vessel-class/length rules model. */
public final class VesselNavigationLights {
    public static final int PORT=0,STARBOARD=1,STERN=2,MAST=3,SPECIAL=4,ANCHOR=5;
    public static final int RED=0xffff4545,GREEN=0xff39f49a,WHITE=0xffe8f4ff,YELLOW=0xffffcd4b;
    private static final float VIEW_BEARING=(float)(StrictMath.PI*.75),SIDE_ARC=(float)(StrictMath.PI*.625);
    // Elevated-camera readability: two authored heading steps of soft spill past
    // each nominal sector, rather than a single lamp switching at the boundary.
    private static final float CAMERA_OVERLAP=(float)(StrictMath.PI/8);
    private VesselNavigationLights(){}
    public static int count(boolean powered){return powered?4:3;}
    private static int count(Body b,boolean powered){return b.cruiser?6:!powered && !b.yacht?5:count(powered);}
    /** Waiting rescue cruisers show an all-round anchor lamp; an active tow enables running lamps. */
    public static boolean emitting(Game g,Body b,int fixture){
        if(!b.cruiser)return true;
        if(!b.active || b.deliveryRecorded)return false;
        boolean underway=g.tow.connected(b,g.tug);
        return underway?fixture>=PORT && fixture<=STERN:fixture==ANCHOR;
    }
    static int sourceCount(Body b){return b.cruiser?6:count(b.berth<0);}
    static float height(Body b,int fixture){return b.racing?(fixture==ANCHOR?9.6f:fixture==STERN?5.4f:5.8f):b.cruiser?(fixture==ANCHOR?12:fixture==STERN?4.5f:9.8f):b.yacht?(fixture==STERN?12.1f:15.4f):height(fixture);}
    /** One eased pulse per second (60/min), with a dark interval; fixed-step clock only. */
    public static float specialPulse(int ticks){
        int t=Math.floorMod(ticks,60);
        float x=t<15?t/15f:t<24?1:t<45?(45-t)/21f:0;
        return x*x*(3-2*x);
    }
    /** Leading end of a pushed barge, or zero. Contact is required, not just pickup intent. */
    public static int pushedEnd(Game g,Body b){
        if(b.yacht || b.lineAttached || g.tow.incoming(b)!=null)return 0;
        for(Feeder f:g.feeders)if(f.phase==Feeder.INBOUND && f.load==b)return 1;
        if(g.tug==null || !(g.pushing || g.navigating && !g.reversing) || !Physics.touching(g.tug,b,3))return 0;
        float dx=b.x-g.tug.x,dy=b.y-g.tug.y;
        if(dx*(float)StrictMath.cos(g.tug.angle)+dy*(float)StrictMath.sin(g.tug.angle)<=0)return 0;
        return dx*(float)StrictMath.cos(b.angle)+dy*(float)StrictMath.sin(b.angle)>=0?1:-1;
    }
    private static int forwardEnd(Game g,Body b,int pushed){
        if(b.yacht)return 1;
        Towline line=g.tow.incoming(b);
        return line!=null?(line.cargoEnd>=0?1:-1):pushed==0?1:pushed;
    }
    private static float energy(Body b,boolean powered,int fixture,int pushed,int end,int ticks){
        if(!powered && fixture==MAST)return 0;
        if(fixture==SPECIAL && (powered || b.yacht || pushed==0))return 0;
        if(fixture==STERN && pushed!=0)return 0;
        float heading=b.angle+(end<0?(float)StrictMath.PI:0);
        float visible=powered && fixture==STERN?tugSternVisibility(heading):visibility(heading,fixture);
        return visible*(fixture==SPECIAL?specialPulse(ticks):1);
    }
    public static float energy(Game g,Body b,boolean powered,int fixture){
        if(!emitting(g,b,fixture))return 0;
        if(b.cruiser && fixture==ANCHOR)return 1;
        int pushed=powered?0:pushedEnd(g,b);
        return energy(b,powered,fixture,pushed,powered?1:forwardEnd(g,b,pushed),g.visualTicks);
    }
    public static String id(int fixture){return fixture==ANCHOR?"anchor-all-round":fixture==PORT?"nav-port":fixture==STARBOARD?"nav-starboard":fixture==STERN?"nav-stern":fixture==SPECIAL?"nav-special-flasher":"nav-masthead";}
    public static int color(int fixture){return fixture==PORT?RED:fixture==STARBOARD?GREEN:fixture==SPECIAL?YELLOW:WHITE;}
    public static float localX(Body b,int fixture){if(b.racing)return fixture==STERN?-25:fixture==ANCHOR?-20:4;if(b.cruiser){float length=b.berth%2==0?39:44;return length*(fixture==STERN?-.46f:fixture==ANCHOR?.08f:.14f);}if(b.yacht)return fixture==STERN?-52.8f:26.4f;return fixture==STERN?-(b.half+b.radius*.65f):fixture==MAST?0:b.half+b.radius*.12f;}
    public static float localY(Body b,int fixture){if(b.racing)return fixture==PORT?-5.2f:fixture==STARBOARD?5.2f:0;if(b.cruiser)return fixture==PORT?-5.8f:fixture==STARBOARD?5.8f:0;if(b.yacht)return fixture==PORT?-9.9f:fixture==STARBOARD?9.9f:0;return fixture==PORT?-b.radius*.65f:fixture==STARBOARD?b.radius*.65f:0;}
    public static float height(int fixture){return fixture==MAST?28:6;}
    public static float worldX(Body b,int fixture){return worldX(b,fixture,1);}
    private static float worldX(Body b,int fixture,int end){float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);return b.x+end*(localX(b,fixture)*c-localY(b,fixture)*s);}
    public static float worldY(Body b,int fixture){return worldY(b,fixture,1);}
    private static float worldY(Body b,int fixture,int end){float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);return b.y+end*(localX(b,fixture)*s+localY(b,fixture)*c);}
    static int emittingEnd(Game g,Body b){return b.berth<0?1:forwardEnd(g,b,pushedEnd(g,b));}
    static float sourceX(Game g,Body b,int fixture){return worldX(b,fixture,emittingEnd(g,b));}
    static float sourceY(Game g,Body b,int fixture){return worldY(b,fixture,emittingEnd(g,b));}
    static float sourceHeading(Game g,Body b){return b.angle+(emittingEnd(g,b)<0?(float)StrictMath.PI:0);}
    /** Independent, overlapping lamp visibility; never choose a single winning fixture. */
    public static float visibility(float heading,int fixture){
        float bearing=Physics.angle(VIEW_BEARING-heading),value;
        if(fixture==PORT)value=Math.min(bearing+SIDE_ARC,-bearing);
        else if(fixture==STARBOARD)value=Math.min(SIDE_ARC-bearing,bearing);
        else if(fixture==STERN)value=Math.abs(bearing)-SIDE_ARC;
        else value=SIDE_ARC-Math.abs(bearing);
        return sectorVisibility(value);
    }
    /** Elevated-view tug stern lens stays fully readable at either screen broadside. */
    public static float tugSternVisibility(float heading){
        return sectorVisibility(Math.abs(Physics.angle(VIEW_BEARING-heading))-(float)(StrictMath.PI/2));
    }
    private static float sectorVisibility(float value){
        float visible=Physics.clamp(1+value/CAMERA_OVERLAP,0,1);
        return visible*visible*(3-2*visible);
    }
    public static float screenX(Body b,int fixture,boolean powered,RiverCamera camera){
        if(powered && fixture==MAST){float size=2*(b.half+b.radius)/TugArt.HULL_FRACTION;return camera.screenX(b.x,b.y)+TugLightArt.offset(0,b.angle,true,false)*size*camera.scale;}
        return camera.screenX(worldX(b,fixture),worldY(b,fixture));
    }
    public static float screenY(Body b,int fixture,boolean powered,RiverCamera camera,float bob){
        if(powered && fixture==MAST){float size=2*(b.half+b.radius)/TugArt.HULL_FRACTION;return camera.screenY(b.x,b.y)+(TugLightArt.offset(0,b.angle,true,true)*size-bob)*camera.scale;}
        return camera.screenY(worldX(b,fixture),worldY(b,fixture))-(height(b,fixture)*.81649658f+bob)*camera.scale;
    }
    private static int alpha(int color,float energy,int strength){return ((int)(Physics.clamp(energy,0,1)*strength)<<24)|(color&0xffffff);}
    private static void glow(Painter p,float x,float y,float scale,float visible,int color,int tick,int fixture){
        // The same cached feathered kernel as factory lamps avoids hard halo
        // disks. Running-lamp energy stays steady; signal flashes are explicit.
        SceneryEffects.glow(p,x,y,1.8f*scale,color,visible,tick,fixture*1.7f,.91f);
        p.circle(x,y,1.35f*scale,alpha(color,visible,235));
        p.circle(x,y,.45f*scale,alpha(WHITE,visible,245));
    }
    /** Barge far fixtures go behind cargo; near coaming fixtures finish in front. */
    public static void draw(Game g,Painter p,Body b,boolean powered,float bob,boolean nearCargo){
        boolean river=p instanceof RiverPainter;RiverCamera c=river?((RiverPainter)p).camera:null;
        Painter canvas=river?((RiverPainter)p).canvas():p;float scale=river?c.scale:1;
        int pushed=powered?0:pushedEnd(g,b),end=powered?1:forwardEnd(g,b,pushed);
        for(int fixture=0;fixture<count(b,powered);fixture++){
            float visible=b.cruiser?energy(g,b,powered,fixture):energy(b,powered,fixture,pushed,end,g.visualTicks);if(visible<=0)continue;
            float wx=worldX(b,fixture,end),wy=worldY(b,fixture,end);
            boolean near=river?c.screenY(wx,wy)>=c.screenY(b.x,b.y):wy>=b.y;
            if(b.cruiser && fixture==ANCHOR){if(!nearCargo)continue;}
            else if(!powered && (!b.yacht || b.cruiser) && near!=nearCargo)continue;
            float x=river?c.screenX(wx,wy):wx,y=river?c.screenY(wx,wy)-(height(b,fixture)*.81649658f+bob)*scale:wy-height(fixture)-bob;
            if(powered && fixture==MAST && river){x=screenX(b,fixture,true,c);y=screenY(b,fixture,true,c,bob);}
            if(b.cruiser && fixture==ANCHOR)canvas.line(x,y,x,y+2.3f*.81649658f*scale,.65f*scale,0xff647476);
            glow(canvas,x,y,scale,visible,color(fixture),g.visualTicks,fixture);
        }
    }
    /** Colored ripples stay on the water plane and never paint across a quay. */
    public static void reflections(Game g,Painter p,Body b,boolean powered){
        if(!(p instanceof RiverPainter))return;
        RiverCamera c=((RiverPainter)p).camera;
        int pushed=powered?0:pushedEnd(g,b),end=powered?1:forwardEnd(g,b,pushed);
        for(int fixture=0;fixture<count(b,powered);fixture++){
            float visible=b.cruiser?energy(g,b,powered,fixture):energy(b,powered,fixture,pushed,end,g.visualTicks);if(visible<=0)continue;
            float x=c.screenX(worldX(b,fixture,end),worldY(b,fixture,end)),y=c.screenY(worldX(b,fixture,end),worldY(b,fixture,end));
            WaterReflection.draw(g,p,c.worldX(x,y+6*c.scale),c.worldY(x,y+6*c.scale),
                    10,25,alpha(color(fixture),visible,150),b.id*7+fixture);
        }
    }
}
