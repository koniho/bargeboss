package com.bargeboss.game;

/** Read-only shared engine mix. Hosts own playback, never infer throttle or gear. */
public final class EngineAudio {
    public static final int LAYERS=2;
    private EngineAudio(){}
    public static String asset(int layer){return layer==0?"audio/diesel-idle-v1.wav":"audio/diesel-working-v1.wav";}
    public static boolean audible(Game root){return root.activeGame().mode==Game.PLAY;}
    private static float load(Game root){Game g=root.activeGame();return g.tug==null?0:Physics.clamp(g.tug.engineLoad,0,1);}
    public static float gain(Game root,int layer){
        if(!audible(root))return 0;
        float load=load(root),work=Physics.clamp((load-.12f)/.88f,0,1);
        work=work*work*(3-2*work);
        return layer==0?.42f-.14f*work:.48f*work;
    }
    public static float rate(Game root,int layer){float load=load(root);return layer==0?.86f+.24f*load:.78f+.34f*load;}
}
