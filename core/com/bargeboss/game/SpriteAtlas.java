package com.bargeboss.game;

/** Shared atlas layout, per-view registration and continuous heading interpolation. */
public final class SpriteAtlas {
    public static final int TUG=0, BARGE=1, STEEL=2, CONTAINER=3, BULK=4, WATER=5, NORTH_QUAY=6, SOUTH_QUAY=7, SCENERY=8, PIERS=9, BRIDGE=10,
            TITLE_BACKDROP=11, TITLE_HARBOR=12, TITLE_VESSELS=13, TITLE_WATER=14, TITLE_LAND=15;
    public static final int TITLE_SKY_FADE_SLICES=64;
    private static final String[] FILES={"tug-16.png","barge-arcade-v3-16.png","steel-16.png","container-16.png","bulk-16.png"};
    // Alpha-bound registration preserves the previous hull footprint and fixed world anchor.
    private static final float[] BARGE_V3_X={0.500000f,0.488854f,0.498454f,0.488851f,0.480892f,0.469745f,0.507987f,0.496827f,0.499906f,0.458599f,0.477670f,0.472824f,0.498381f,0.488826f,0.503195f,0.487221f,0.500000f,0.488853f,0.504792f,0.491974f,0.487262f,0.476101f,0.487221f,0.480830f,0.496815f,0.482414f,0.477636f,0.464913f,0.493631f,0.468101f,0.506390f,0.487249f};
    private static final float[] BARGE_V3_Y={0.632488f,0.642208f,0.635785f,0.641465f,0.630815f,0.635095f,0.613950f,0.613974f,0.541459f,0.552364f,0.545208f,0.561648f,0.549624f,0.560352f,0.553962f,0.572752f,0.580506f,0.584951f,0.577399f,0.590001f,0.569135f,0.579053f,0.543935f,0.544965f,0.533748f,0.545299f,0.538553f,0.557157f,0.544036f,0.564184f,0.563636f,0.579887f};
    private static final float[] BARGE_V3_SX={1.033088f,0.984868f,1.033195f,1.017251f,1.030435f,0.842702f,1.007143f,0.974104f,1.062992f,1.317996f,1.022222f,1.216122f,1.017094f,1.130887f,1.000000f,1.047716f,1.025547f,0.970543f,1.016394f,0.996499f,1.013393f,0.859555f,1.013986f,1.005545f,1.071429f,1.296793f,1.025641f,1.191821f,1.032787f,1.113343f,1.022472f,1.035571f};
    private static final float[] BARGE_V3_SY={0.981132f,1.368932f,0.977528f,1.033708f,0.984536f,1.025253f,1.014286f,0.987591f,0.990338f,0.985646f,0.971564f,0.971336f,0.985075f,0.930000f,1.000000f,0.833391f,0.963303f,1.304074f,0.960894f,1.057021f,0.970297f,1.019754f,1.014151f,1.027593f,0.981900f,0.988788f,0.981481f,0.959906f,0.959799f,0.914748f,0.946746f,0.846963f};
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
    private static final String[] HALF_FILES={"tug-half.png","barge-arcade-v3-half.png","steel-half.png","container-half.png","bulk-half.png"};
    private static final float[][] HALF_PX={
        {0.507962f,0.543131f,0.523885f,0.525559f,0.520701f,0.503195f,0.507962f,0.504792f,0.515924f,0.522364f,0.527070f,0.520767f,0.498408f,0.479233f,0.487261f,0.490415f},
        {0.485669f,0.496805f,0.484076f,0.493610f,0.466561f,0.472843f,0.485669f,0.477636f,0.479299f,0.498403f,0.476115f,0.461661f,0.476115f,0.460064f,0.468153f,0.476038f},
        {0.515823f,0.526814f,0.512658f,0.498418f,0.514241f,0.488959f,0.488924f,0.484177f,0.509494f,0.501577f,0.484177f,0.471519f,0.514241f,0.485804f,0.485759f,0.482595f},
        {0.504777f,0.501597f,0.509554f,0.514377f,0.490446f,0.482428f,0.485669f,0.495208f,0.490446f,0.500000f,0.509554f,0.535144f,0.493631f,0.472843f,0.482484f,0.509585f},
        {0.506369f,0.498403f,0.484076f,0.477636f,0.509554f,0.500000f,0.477707f,0.484026f,0.504777f,0.498403f,0.480892f,0.480831f,0.509554f,0.492013f,0.476115f,0.485623f},
    };
    private static final float[][] HALF_PY={
        {0.732043f,0.746531f,0.748029f,0.741158f,0.718631f,0.728666f,0.749430f,0.728380f,0.725674f,0.724238f,0.706628f,0.690203f,0.712241f,0.712692f,0.714287f,0.721991f},
        {0.638280f,0.638280f,0.625541f,0.603248f,0.549169f,0.549169f,0.547572f,0.563546f,0.587325f,0.582548f,0.569809f,0.550701f,0.541182f,0.547572f,0.560351f,0.569936f},
        {0.758457f,0.724695f,0.678071f,0.645916f,0.596077f,0.620193f,0.655563f,0.679678f,0.644516f,0.630000f,0.586452f,0.560645f,0.531768f,0.563923f,0.589646f,0.602508f},
        {0.658025f,0.626178f,0.611847f,0.607070f,0.594377f,0.580000f,0.583195f,0.580000f,0.589554f,0.597516f,0.584777f,0.576815f,0.576805f,0.573610f,0.580000f,0.573610f},
        {0.796561f,0.799745f,0.790191f,0.769490f,0.715783f,0.709393f,0.720575f,0.720575f,0.673949f,0.669172f,0.667580f,0.637325f,0.608754f,0.602364f,0.621534f,0.626326f},
    };
    private static final float[][] HALF_SX={
        {0.891546f,0.971371f,0.913802f,0.964222f,1.049133f,1.059071f,1.024578f,1.013268f,0.902006f,0.958563f,0.910444f,0.967923f,1.179334f,1.070867f,1.056841f,1.007542f},
        {0.984868f,0.996406f,0.810290f,0.960575f,1.286364f,1.230346f,1.111047f,1.047716f,0.970543f,0.972095f,0.851741f,1.005545f,1.239582f,1.149472f,1.076989f,1.016604f},
        {1.000416f,0.984170f,0.972222f,0.961240f,1.368752f,1.096930f,1.040816f,1.026718f,1.015604f,0.971670f,0.997596f,0.902439f,1.430627f,1.059125f,1.055102f,1.032567f},
        {0.982563f,0.991540f,0.900637f,0.946359f,1.161366f,1.026619f,1.006237f,1.004902f,0.986592f,0.933621f,0.908198f,0.919416f,1.217277f,1.012996f,0.988396f,0.977324f},
        {1.035332f,0.995579f,1.018859f,1.062331f,1.189550f,1.102157f,1.075315f,1.055663f,1.028335f,1.060135f,0.994982f,1.080934f,1.164651f,1.124713f,1.115875f,1.019693f},
    };
    private static final float[][] HALF_SY={
        {1.044444f,0.986000f,1.047325f,1.020996f,1.000000f,1.011905f,0.973140f,1.019086f,1.075342f,1.016327f,0.998016f,1.015537f,0.971591f,0.968750f,0.949393f,0.978720f},
        {1.330189f,1.033708f,1.025253f,0.973746f,0.985646f,0.962085f,0.930000f,0.823410f,1.268182f,1.039106f,1.009756f,0.994748f,0.966518f,0.959906f,0.934211f,0.857892f},
        {1.362745f,1.096045f,1.066667f,1.021186f,1.000000f,0.986486f,0.923469f,0.936482f,1.147059f,1.251852f,1.064286f,1.045270f,0.989796f,1.015982f,0.935233f,0.932331f},
        {1.124161f,0.976303f,0.954955f,0.978304f,0.970183f,0.946667f,0.946512f,0.882418f,1.080128f,1.000000f,0.997758f,0.957222f,0.983721f,0.954751f,0.951691f,0.874496f},
        {1.114583f,1.084821f,1.057692f,1.015583f,0.985915f,0.941176f,1.009009f,0.993536f,1.059140f,1.028302f,1.053719f,1.008707f,0.993007f,0.988550f,0.957265f,1.018800f},
    };
    // Pixel-registered chimney openings, in the same frame order as the hull.
    private static final float[] STACK_X={0.379585f,0.373206f,0.409888f,0.400319f,0.344498f,0.389155f,0.339713f,0.397129f,0.497500f,0.525000f,0.607656f,0.623604f,0.660287f,0.688995f,0.690590f,0.703349f,0.666667f,0.688995f,0.626794f,0.629984f,0.577352f,0.596491f,0.448166f,0.454545f,0.500800f,0.503987f,0.352472f,0.362041f,0.360447f,0.370016f,0.288676f,0.298246f};
    private static final float[] STACK_Y={0.354067f,0.347687f,0.267943f,0.271132f,0.216906f,0.236045f,0.213716f,0.229665f,0.200000f,0.200000f,0.314195f,0.336523f,0.189793f,0.212121f,0.247209f,0.269537f,0.354067f,0.363636f,0.331738f,0.341308f,0.331738f,0.354067f,0.370016f,0.389155f,0.390000f,0.386000f,0.339713f,0.355662f,0.330144f,0.346093f,0.314195f,0.320574f};
    private static final float STEP=(float)(Math.PI/16);
    private SpriteAtlas() {}
    public static String file(int atlas) {
        if(atlas==TITLE_LAND) return "title-land-v1.png";
        if(atlas>=TITLE_BACKDROP) return atlas==TITLE_HARBOR?"title-harbor-v2.png":atlas==TITLE_VESSELS?"title-vessels-v1.png":"title-backdrop-v2.png";
        if(atlas==WATER) return "water-arcade-v2.png";
        if(atlas==SCENERY || atlas==PIERS || atlas==BRIDGE) return "harbor-arcade-v2.png";
        return atlas==BRIDGE?"bridge-v1.png":atlas==SCENERY?"harbor-props-v1.png":atlas==PIERS?"harbor-piers-v1.png":atlas>=WATER?"harbor-base-v1.png":FILES[atlas];
    }
    public static String file(int atlas,int frame) { return atlas>=WATER?file(atlas):frame%2==0?FILES[atlas]:HALF_FILES[atlas]; }
    public static float widthScale(int atlas,int frame) { if(atlas==BARGE) return BARGE_V3_SX[frame]; return atlas>=WATER || frame%2==0?1:HALF_SX[atlas][frame/2]; }
    public static float heightScale(int atlas,int frame) { if(atlas==BARGE) return BARGE_V3_SY[frame]; return atlas>=WATER || frame%2==0?1:HALF_SY[atlas][frame/2]; }
    public static int sourceFrame(int atlas,int frame) { return atlas>=WATER?frame:frame/2; }
    public static int count(int atlas) { if(atlas==TITLE_LAND) return 4; return atlas==TITLE_BACKDROP?TITLE_SKY_FADE_SLICES+1:atlas==SCENERY?8:atlas==PIERS || atlas==BRIDGE || atlas==TITLE_VESSELS?2:atlas>=WATER?1:32; }
    public static int columns(int atlas) { if(atlas==TITLE_LAND) return 4; return atlas>=WATER?1:4; }
    public static int rows(int atlas) { return atlas>=WATER?1:4; }
    public static float pivotX(int atlas,int frame) { if(atlas==BARGE) return BARGE_V3_X[frame]; if(atlas>=WATER) return .5f; if(frame%2!=0) return HALF_PX[atlas][frame/2]; frame/=2; return atlas==TUG?TUG_X[frame]:atlas==BARGE?BARGE_X[frame]:atlas==STEEL?STEEL_X[frame]:atlas==CONTAINER?CONTAINER_X[frame]:BULK_X[frame]; }
    public static float pivotY(int atlas,int frame) { if(atlas==BARGE) return BARGE_V3_Y[frame]; if(atlas>=WATER) return .5f; if(frame%2!=0) return HALF_PY[atlas][frame/2]; frame/=2; return atlas==TUG?TUG_Y[frame]:atlas==BARGE?BARGE_Y[frame]:atlas==STEEL?STEEL_Y[frame]:atlas==CONTAINER?CONTAINER_Y[frame]:BULK_Y[frame]; }
    // Irregular cells from the painted source sheets. Hosts use these same normalized crop bounds.
    private static final int[][] TERRAIN={{130,340,1640,570},{120,0,1000,223},{630,680,1150,908}};
    private static int[] region(int atlas,int frame) { return TERRAIN[atlas-WATER]; }
    private static boolean arcade(int atlas) { return atlas==SCENERY || atlas==PIERS || atlas==BRIDGE; }
    private static int arcadeCell(int atlas,int frame) { return frame+(atlas==PIERS?8:atlas==BRIDGE?10:0); }
    public static float sourceLeft(int atlas,int frame) { if(atlas==TITLE_LAND) return frame/4f; if(atlas==TITLE_BACKDROP) return frame==0?.12f:(frame-1)*.12f/TITLE_SKY_FADE_SLICES; if(atlas==WATER) return 0; if(arcade(atlas)) return (arcadeCell(atlas,frame)%4)/4f; if(atlas>=TITLE_BACKDROP) return atlas==TITLE_VESSELS && frame==0?200/1536f:0; return atlas>=WATER?region(atlas,frame)[0]/1732f:(sourceFrame(atlas,frame)%4)/4f; }
    public static float sourceTop(int atlas,int frame) { if(atlas==WATER) return 0; if(arcade(atlas)) return (arcadeCell(atlas,frame)/4)/3f; if(atlas>=TITLE_BACKDROP) return atlas==TITLE_WATER?.88f:atlas==TITLE_VESSELS && frame==1?600/1024f:0; return atlas>=WATER?region(atlas,frame)[1]/908f:(sourceFrame(atlas,frame)/4)/4f; }
    public static float sourceRight(int atlas,int frame) { if(atlas==TITLE_LAND) return (frame+1)/4f; if(atlas==TITLE_BACKDROP) return frame==0?1:frame*.12f/TITLE_SKY_FADE_SLICES; if(atlas==WATER) return 1; if(arcade(atlas)) return (arcadeCell(atlas,frame)%4+1)/4f; if(atlas>=TITLE_BACKDROP) return atlas==TITLE_VESSELS && frame==0?1300/1536f:1; return atlas>=WATER?region(atlas,frame)[2]/1732f:(sourceFrame(atlas,frame)%4+1)/4f; }
    public static float sourceBottom(int atlas,int frame) { if(atlas==WATER) return 1; if(arcade(atlas)) return (arcadeCell(atlas,frame)/4+1)/3f; if(atlas>=TITLE_BACKDROP) return atlas==TITLE_VESSELS && frame==0?540/1024f:1; return atlas>=WATER?region(atlas,frame)[3]/908f:(sourceFrame(atlas,frame)/4+1)/4f; }
    public static float artWidth(int atlas,int frame) { if(atlas==WATER) return 300; if(arcade(atlas)) return 100; int[] r=region(atlas,frame); return (r[2]-r[0])*688f/1732; }
    public static float artHeight(int atlas,int frame) { if(atlas==WATER) return 300; if(arcade(atlas)) return 100; int[] r=region(atlas,frame); return (r[3]-r[1])*688f/1732; }
    public static float stackOffset(float angle,boolean river,boolean vertical) {
        float heading=angle/STEP;int whole=(int)StrictMath.floor(heading);
        int a=((whole+(river?24:0))%32+32)%32,b=(a+1)%32;
        float blend=Physics.clamp((heading-whole-.4f)*5,0,1);blend=blend*blend*(3-2*blend);
        float first=vertical?(STACK_Y[a]-pivotY(TUG,a))*heightScale(TUG,a):(STACK_X[a]-pivotX(TUG,a))*widthScale(TUG,a);
        float next=vertical?(STACK_Y[b]-pivotY(TUG,b))*heightScale(TUG,b):(STACK_X[b]-pivotX(TUG,b))*widthScale(TUG,b);
        return first+(next-first)*blend;
    }
    public static int direction(float angle) { return (Math.round(angle/STEP)%32+32)%32; }
    public static float turn(float angle,int frame) { return Physics.angle(angle-frame*STEP); }
    public static void draw(Painter p,int atlas,float angle,float cx,float cy,float width,float height,float alpha) {
        float heading=angle/STEP;
        int whole=(int)StrictMath.floor(heading),a=(whole%32+32)%32,b=(a+1)%32;
        // No residual bitmap rotation: every view shares one stationary world anchor.
        // Hold crisp source views for most of each sector; soften only its central 20%.
        float blend=Physics.clamp((heading-whole-.4f)*5,0,1);
        blend=blend*blend*(3-2*blend);
        p.spriteBlend(atlas,a,b,cx,cy,width,height,0,blend,alpha);
    }
}
