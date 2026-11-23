package com.bargeboss.game;

/** Registered canvas-only livery. Palette and frame selection are shared on every host. */
public final class BiminiArt {
    public static final int COLORS=8,LEGACY_HULL_FRAMES=192,FRAMES=COLORS*LEGACY_HULL_FRAMES;
    public static final int WIDTH=52,HEIGHT=32,LEFT=38,TOP=64;
    private static final int[] FABRIC={0xffddd4bc,0xff344860,0xff3d789b,0xff823f50,
            0xff386253,0xffb59a75,0xff46494e,0xff417d7a};
    public static int color(int palette){return FABRIC[palette];}
    public static int frame(int palette,int boatFrame){return palette*LEGACY_HULL_FRAMES+boatFrame;}
    /** Coprime station stride prevents adjacent slips from repeating; independent of hull type. */
    public static int palette(int row,int station,int side){return Math.floorMod(row*3+station*5+(side>0?3:0),COLORS);}
    private BiminiArt(){}
}
