package com.bargeboss.game;

/** Shared soundtrack selection and mix; hosts stream it and own focus/background pause. */
public final class MusicAudio {
    private MusicAudio(){}
    public static String asset(){return "audio/music-silence-v1.wav";}
    public static float gain(Game root){
        Game active=root.activeGame();
        float duck=active.mode==Game.PLAY && active.settings.soundGain()>0?SoundEffects.ambienceGain(root):1;
        return .22f*active.settings.musicGain()*duck;
    }
}
