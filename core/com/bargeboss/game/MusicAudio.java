package com.bargeboss.game;

/** Shared soundtrack selection and mix; hosts stream it and own focus/background pause. */
public final class MusicAudio {
    private MusicAudio(){}
    /** The shipped soundtrack, or null when the build has none (public builds). Hosts create
     * no player without one: looping a stand-in clip of a few milliseconds restarted Android's
     * MediaPlayer about 20 times a second and cut frame pacing to about 37 FPS. */
    public static String asset(){return null;}
    public static boolean available(){return asset()!=null;}
    public static float gain(Game root){
        Game active=root.activeGame();
        float duck=active.mode==Game.PLAY && active.settings.soundGain()>0?SoundEffects.ambienceGain(root):1;
        return .22f*active.settings.musicGain()*duck;
    }
}
