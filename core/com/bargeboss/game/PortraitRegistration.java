package com.bargeboss.game;

/** Painted shoulder/base registration in 192px cells; frame zero keeps its accepted layout. */
final class PortraitRegistration {
    static final String[] FILES={"instructor-portrait-v2.png","portrait-bridge-operator-steel-v1.png","portrait-bridge-operator-night-v1.png","portrait-harbor-dispatcher-v1.png","portrait-lock-keeper-v1.png","portrait-dock-foreman-v1.png","portrait-fire-rescue-captain-v1.png","portrait-gavin-dockmaster-v1.png","portrait-chas-v1.png","portrait-alex-v1.png","portrait-captain-whitney-v1.png","portrait-captain-wesley-v1.png","portrait-captain-dot-v2.png"};
    // Midpoint of the occupied bottom nine rows, alpha >128; bottom-most occupied row.
    private static final float[][] X={{98.5f,99,103.5f,99,98,101},{95,95,95,95,95,95},{102,90,78,102,90,78},{97.5f,97.5f,97.5f,97.5f,97.5f,97.5f},{95.5f,95.5f,95.5f,95.5f,95.5f,95.5f},{98.5f,90.5f,88.5f,98.5f,90.5f,90},{95,95,95,95,95,95},{96,95.5f,95.5f,95.5f,95.5f,95.5f},{96,96,96,96,96,96},{96,96,96,96,96,96},{95.5f,96,96,96,96,96},{95,95.5f,95.5f,95.5f,95.5f,95.5f},{96,95.5f,95.5f,95.5f,95.5f,95.5f}};
    private static final float[][] Y={{171,171,171,171,171,171},{179,179,179,179,179,179},{180,180,180,180,180,180},{181,181,181,181,181,181},{181,180,180,181,181,181},{182,182,181,179,179,179},{183,183,183,183,183,183},{182,182,182,182,182,182},{182,182,182,182,182,182},{182,182,182,182,182,182},{182,182,182,182,182,182},{182,182,182,182,182,182},{182,182,182,182,182,182}};
    static boolean atlas(int atlas){return atlas>=SpriteAtlas.WHITNEY_PORTRAIT && atlas<=SpriteAtlas.DOT_PORTRAIT || atlas==SpriteAtlas.ALEX_PORTRAIT || atlas==SpriteAtlas.CHAS_PORTRAIT || atlas==SpriteAtlas.GAVIN_PORTRAIT || atlas>=SpriteAtlas.INSTRUCTOR_PORTRAIT && atlas<=SpriteAtlas.FIRE_RESCUE_PORTRAIT;}
    static float pivot(int atlas,int frame,boolean vertical){float[][] a=vertical?Y:X;int i=index(atlas);return .5f+(a[i][frame]-a[i][0])/192f;}
    static String file(int atlas){return FILES[index(atlas)];}
    /** Row in FILES/X/Y: radio NPCs first, then the three selectable player captains. */
    static int index(int atlas){return atlas>=SpriteAtlas.WHITNEY_PORTRAIT?10+atlas-SpriteAtlas.WHITNEY_PORTRAIT:atlas==SpriteAtlas.ALEX_PORTRAIT?9:atlas==SpriteAtlas.CHAS_PORTRAIT?8:atlas==SpriteAtlas.GAVIN_PORTRAIT?7:atlas-SpriteAtlas.INSTRUCTOR_PORTRAIT;}
    private PortraitRegistration(){}
}
