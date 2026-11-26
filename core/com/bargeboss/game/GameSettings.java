package com.bargeboss.game;

/** Shared player preferences; practice shares these without sharing career rewards. */
public final class GameSettings {
    public static final int OFF=0,LEFT=1,RIGHT=2;
    private int side=LEFT,version;
    private int musicLevel=100,soundLevel=100;
    private boolean musicMuted,soundMuted;
    public int musicLevel(){return musicLevel;}
    public int soundLevel(){return soundLevel;}
    public boolean musicMuted(){return musicMuted;}
    public boolean soundMuted(){return soundMuted;}
    public float musicGain(){return musicMuted?0:musicLevel*.01f;}
    public float soundGain(){return soundMuted?0:soundLevel*.01f;}
    public void setMusicLevel(int value){value=Math.max(0,Math.min(100,value));if(value!=musicLevel){musicLevel=value;version++;}}
    public void setSoundLevel(int value){value=Math.max(0,Math.min(100,value));if(value!=soundLevel){soundLevel=value;version++;}}
    public void toggleMusicMute(){musicMuted=!musicMuted;version++;}
    public void toggleSoundMute(){soundMuted=!soundMuted;version++;}
    String audioSave(){return musicLevel+","+(musicMuted?1:0)+","+soundLevel+","+(soundMuted?1:0);}
    void restoreAudio(int[] values){musicLevel=values[0];musicMuted=values[1]==1;soundLevel=values[2];soundMuted=values[3]==1;}
    public boolean developerWaves,developerTools;
    /** Render-only diagnostic: 0 solid water, 1 vessel effects, 2 all detail. Never persisted. */
    public int waterDetail=2;
    public boolean directWaterQuads=true;
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
    private int captain=-1;
    private boolean tutorialDone;
    /** Chosen player captain (PlayerPortrait.BRITTNEY..DOT), or -1 before the intro choice. */
    public int captain(){return captain;}
    public boolean captainChosen(){return captain>=0;}
    public void setCaptain(int next){if(next>=0 && next<PlayerPortrait.COUNT && next!=captain){captain=next;version++;}}
    /** First-start routing is spent once the first tutorial is completed or deliberately left. */
    public boolean tutorialDone(){return tutorialDone;}
    void completeTutorial(){if(!tutorialDone){tutorialDone=true;version++;}}
    String captainSave(){return (tutorialDone?1:0)+","+captain;}
    void restoreCaptain(boolean done,int chosen){tutorialDone=done;captain=chosen>=0 && chosen<PlayerPortrait.COUNT?chosen:-1;}
    public int joystickSide(){return side;}
    public int version(){return version;}
    public void setJoystickSide(int next){if(next==OFF)next=LEFT;if(next>=OFF && next<=RIGHT && next!=side){side=next;version++;}}
    void restore(int next){side=next==OFF?LEFT:next;version=0;}
    /** Best towing-challenge count. A practice record, not a career reward: it travels with the
     * shared settings object so the practice child can save it without touching stars or unlocks. */
    private int towingBest;
    public int towingChallengeBest(){return towingBest;}
    public void recordTowingChallenge(int tied){if(tied>towingBest && tied<=TowingChallenge.BARGES){towingBest=tied;version++;}}
    void restoreTowingBest(int value){towingBest=value;}
}
