package com.bargeboss.game;

/** Read-only shared engine and environmental loop mix. Hosts own playback, never infer throttle or gear. */
public final class EngineAudio {
    public static final int LAYERS=4,FIRE=2,SIZZLE=3;
    private EngineAudio(){}
    public static String asset(int layer){return new String[]{"audio/diesel-idle-v1.wav","audio/diesel-working-v1.wav","audio/timber-fire-roar-v1.wav","audio/fire-water-sizzle-v1.wav"}[layer];}
    public static boolean audible(Game root){return root.activeGame().mode==Game.PLAY;}
    private static float load(Game root){Game g=root.activeGame();return g.tug==null?0:Physics.clamp(g.tug.engineLoad,0,1);}
    public static float gain(Game root,int layer){
        if(!audible(root))return 0;
        if(layer>=FIRE){
            Game g=root.activeGame();if(g.tutorial==null || g.tug==null)return 0;
            FireworksLesson f=g.tutorial.fireworks;
            float mix=0;
            for(int pile=0;pile<FireworksLesson.PILES;pile++){
                float distance=Physics.length(g.tug.x-FireworksLesson.x(pile),g.tug.y-FireworksLesson.y(pile));
                float near=Physics.clamp(1-distance/800,0,1);near*=near;
                float power=layer==FIRE?(f.heat(pile)>0?.55f*(.3f+.7f*f.heat(pile)/FireworksLesson.PILE_HEAT):0):.65f*Physics.clamp(f.sizzle(pile)/12f,0,1);
                mix=Math.max(mix,near*power);
            }
            return mix*SoundEffects.ambienceGain(root);
        }
        float load=load(root),work=Physics.clamp((load-.12f)/.88f,0,1);
        work=work*work*(3-2*work);
        return (layer==0?.42f-.14f*work:.48f*work)*SoundEffects.engineGain(root);
    }
    public static float rate(Game root,int layer){if(layer>=FIRE)return 1;float load=load(root);return layer==0?.72f+.20f*load:.68f+.28f*load;}
}
