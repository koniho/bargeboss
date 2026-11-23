package com.bargeboss.game;

/** Registered marina materials and upright park scenery. No lamps/exhaust are baked in.
 * Flat materials 0..3 do not cast shadows; tree/rock roots share independent mask pivots.
 * Artwork/provenance: docs/art-studies/montrose/park-v2/runtime-integration.md. */
public final class MarinaArt {
    private static final float[] PX={.4761f,.5207f,.5f,.5f*443/444,.541f,.517f*443/444,.484f,.49f*443/444};
    private static final float[] PY={.5327f,.5327f,.643f,.643f,.877f,.865f,.702f*443/444,.66f*443/444};
    // Pixel-estimated upright silhouette height above the registered footprint, in cell units.
    private static final float[] HEIGHT={0,0,0,0,.731f,.502f,.322f,.388f};
    private MarinaArt(){}
    public static float pivotX(int frame){return PX[frame];}
    public static float pivotY(int frame){return PY[frame];}
    public static boolean tree(int frame){return frame==4 || frame==5;}
    /** Vertical scenery extent in the same units as RiverCamera.riseX/riseY, not collision height. */
    public static float height(int frame,float size){return HEIGHT[frame]*size;}
    public static void draw(Painter canvas,RiverCamera c,int frame,float x,float y,float size){
        canvas.sprite(SpriteAtlas.MARINA,frame,c.screenX(x,y),c.screenY(x,y),size*c.scale,size*c.scale,0,1);
    }
    /** A gentle trunk/canopy shear, not a change of the painted isometric camera orientation.
     * The affine origin is the unchanged ground pivot. Shadow masks remain unsheared.
     * Uses shared visual ticks only; drawing neither advances time nor allocates images. */
    public static void drawAnimated(Game g,Painter canvas,RiverCamera c,int frame,float x,float y,float size,int stableSeed){
        if(!tree(frame)){draw(canvas,c,frame,x,y,size);SpotlightArt.draw(g,canvas,SpriteAtlas.MARINA,frame,x,y,c.screenX(x,y),c.screenY(x,y),size*c.scale,size*c.scale,1);return;}
        float s=size*c.scale;
        canvas.spriteAffine(SpriteAtlas.MARINA,frame,c.screenX(x,y),c.screenY(x,y),s,0,sway(g.visualTicks,frame,stableSeed)*s,s,1);
        float power=SpotlightArt.power(g,x,y);if(power>.005f)canvas.spriteAffine(SpotlightArt.receiver(SpriteAtlas.MARINA),frame,c.screenX(x,y),c.screenY(x,y),s,0,sway(g.visualTicks,frame,stableSeed)*s,s,power);
    }
    static float sway(int visualTicks,int frame,int stableSeed){
        if(!tree(frame))return 0;
        double phase=Math.floorMod(stableSeed,997)*.073,t=visualTicks/60.0;
        double wind=StrictMath.sin(t*1.13+phase)*.72+StrictMath.sin(t*.47+phase*.63)*.28;
        return (float)(wind*(frame==4?.027:.019));
    }
}
