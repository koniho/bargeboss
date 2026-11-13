package com.bargeboss.game;

/** Shared atlas layout, per-view registration and continuous heading interpolation. */
public final class SpriteAtlas {
    public static final int TUG=0, BARGE=1, STEEL=2, CONTAINER=3, BULK=4;
    private static final String[] FILES={"tug-16.png","barge-16.png","steel-16.png","container-16.png","bulk-16.png"};
    private static final float[] BARGE_X={.498408f,.511182f,.488854f,.500000f,.500000f,.490415f,.495223f,.496805f,.498408f,.514377f,.498408f,.477636f,.495223f,.482428f,.500000f,.500000f};
    private static final float[] BARGE_Y={.628726f,.625541f,.622357f,.606433f,.541182f,.544377f,.547572f,.553962f,.588917f,.587325f,.568217f,.549108f,.542780f,.547572f,.545974f,.563546f};
    private static final float[] CONTAINER_X={.498408f,.501597f,.498408f,.514377f,.503185f,.485623f,.485669f,.490415f,.500000f,.503195f,.506369f,.531949f,.503185f,.487220f,.482484f,.493610f};
    private static final float[] CONTAINER_Y={.651656f,.630955f,.618217f,.602293f,.594377f,.583195f,.586390f,.581597f,.589554f,.594331f,.587962f,.573631f,.572013f,.573610f,.580000f,.573610f};
    private static final float[] TUG_X={.504777f,.536741f,.507962f,.485623f,.496815f,.492013f,.490446f,.487220f,.517516f,.512780f,.504777f,.506390f,.500000f,.482428f,.496815f,.479233f};
    private static final float[] TUG_Y={.747134f,.752809f,.755339f,.747418f,.710128f,.708916f,.742346f,.691771f,.747134f,.736886f,.710753f,.693278f,.706933f,.696137f,.691228f,.701355f};
    private static final float[] STEEL_X={.512658f,.534700f,.520570f,.511076f,.511076f,.488959f,.496835f,.500000f,.512658f,.518927f,.496835f,.482595f,.514241f,.490536f,.493671f,.479430f};
    private static final float[] STEEL_Y={.755241f,.726302f,.684502f,.649132f,.597685f,.620193f,.652347f,.681286f,.657419f,.631613f,.589677f,.559032f,.531768f,.559100f,.589646f,.600900f};
    private static final float[] BULK_X={.507962f,.500000f,.487261f,.484026f,.507962f,.501597f,.485669f,.485623f,.504777f,.500000f,.480892f,.485623f,.514331f,.498403f,.484076f,.490415f};
    private static final float[] BULK_Y={.798153f,.798153f,.790191f,.772675f,.717380f,.717380f,.723770f,.726965f,.678726f,.680318f,.673949f,.643694f,.607157f,.610351f,.627923f,.634313f};
    private static final float STEP=(float)(Math.PI/8);
    private SpriteAtlas() {}
    public static String file(int atlas) { return FILES[atlas]; }
    public static int count(int atlas) { return 16; }
    public static int columns(int atlas) { return 4; }
    public static int rows(int atlas) { return 4; }
    public static float pivotX(int atlas,int frame) { return atlas==TUG?TUG_X[frame]:atlas==BARGE?BARGE_X[frame]:atlas==STEEL?STEEL_X[frame]:atlas==CONTAINER?CONTAINER_X[frame]:BULK_X[frame]; }
    public static float pivotY(int atlas,int frame) { return atlas==TUG?TUG_Y[frame]:atlas==BARGE?BARGE_Y[frame]:atlas==STEEL?STEEL_Y[frame]:atlas==CONTAINER?CONTAINER_Y[frame]:BULK_Y[frame]; }
    public static int direction(float angle) { return (Math.round(angle/STEP)%16+16)%16; }
    public static float turn(float angle,int frame) { return Physics.angle(angle-frame*STEP); }
    public static void draw(Painter p,int atlas,float angle,float cx,float cy,float width,float height,float alpha) {
        float heading=angle/STEP;
        int whole=(int)StrictMath.floor(heading),a=(whole%16+16)%16,b=(a+1)%16;
        // No residual bitmap rotation: every view shares one stationary world anchor.
        // Hold crisp source views for most of each sector; soften only its central 20%.
        float blend=Physics.clamp((heading-whole-.4f)*5,0,1);
        blend=blend*blend*(3-2*blend);
        p.spriteBlend(atlas,a,b,cx,cy,width,height,0,blend,alpha);
    }
}
