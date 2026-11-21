package com.bargeboss.game;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

/** Continuous low-amplitude motor texture. No tap fallback on devices without amplitude control. */
final class EngineRumble {
    private final Context context;
    private final Vibrator vibrator;
    private final AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build();
    private final VibrationEffect[] patterns;
    private int level;
    EngineRumble(Context context){
        patterns=Build.VERSION.SDK_INT>=26?new VibrationEffect[6]:null;
        this.context=context;vibrator=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        if(Build.VERSION.SDK_INT>=26 && vibrator!=null && vibrator.hasAmplitudeControl()){
            for(int i=1;i<patterns.length;i++){
                int a=20+i*9;
                patterns[i]=VibrationEffect.createWaveform(new long[]{24,24,24,24,24,24,24,24},
                        new int[]{a,a+3,a+5,a+3,a,a-3,a-5,a-3},0);
            }
        }
    }
    void update(Game game,boolean feedback){
        if(Build.VERSION.SDK_INT<26 || patterns[1]==null)return;
        float strength=game.engineRumble();
        boolean allowed=Settings.System.getInt(context.getContentResolver(),Settings.System.HAPTIC_FEEDBACK_ENABLED,1)!=0;
        int next=feedback || !allowed || strength<.025f?0:Math.max(1,Math.min(5,Math.round(strength/.28f*5)));
        if(next==level)return;
        if(next==0){stop();return;}
        level=next;vibrator.vibrate(patterns[next],attributes);
    }
    void stop(){if(level!=0 && vibrator!=null)vibrator.cancel();level=0;}
}
