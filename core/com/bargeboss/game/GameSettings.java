package com.bargeboss.game;

/** Shared player preferences; practice shares these without sharing career rewards. */
public final class GameSettings {
    public static final int OFF=0,LEFT=1,RIGHT=2;
    private int side=LEFT,version;
    public boolean developerWaves;
    private int waveLevel=4,waveDirection;
    double waveCos=1,waveSin;
    public float waveIntensity(){return waveLevel*.25f;}
    public int waveBearing(){return waveDirection*15;}
    public void adjustWaves(int change){waveLevel=Math.max(0,Math.min(12,waveLevel+change));}
    public void turnWaves(int steps){waveDirection=Math.floorMod(waveDirection+steps,24);double a=waveBearing()*StrictMath.PI/180;waveCos=StrictMath.cos(a);waveSin=StrictMath.sin(a);}
    public void resetWaves(){waveLevel=4;waveDirection=0;waveCos=1;waveSin=0;}
    private int rainLevel,lightningLevel,fogLevel;
    int weatherFlashTick=-1000000;
    public int fogLevel(){return fogLevel;}
    public void adjustFog(int change){fogLevel=Math.max(0,Math.min(3,fogLevel+change));}
    public int rainLevel(){return rainLevel;}
    public int lightningLevel(){return lightningLevel;}
    public void adjustRain(int change){rainLevel=Math.max(0,Math.min(3,rainLevel+change));}
    public void adjustLightning(int change){lightningLevel=Math.max(0,Math.min(2,lightningLevel+change));}
    public void resetWeather(){rainLevel=lightningLevel=fogLevel=0;weatherFlashTick=-1000000;}
    public int joystickSide(){return side;}
    public int version(){return version;}
    public void setJoystickSide(int next){if(next>=OFF && next<=RIGHT && next!=side){side=next;version++;}}
    void restore(int next){side=next;version=0;}
}
