package com.bargeboss.game;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import java.lang.reflect.Method;

/** Native transport of shared rumble bands. API36 frequency envelopes are prepared once. */
final class EngineRumble {
    private final Context context;
    private final Vibrator vibrator;
    private final AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build();
    private final VibrationEffect[] patterns;
    private final boolean trace;
    private boolean frequencyControl;
    private float minimum,maximum;
    private int level;
    EngineRumble(Context context){
        patterns=Build.VERSION.SDK_INT>=26?new VibrationEffect[6]:null;
        this.context=context;vibrator=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        trace=(context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0 && context instanceof android.app.Activity && ((android.app.Activity)context).getIntent().getBooleanExtra("bb_audio_trace",false);
        if(Build.VERSION.SDK_INT>=26 && vibrator!=null && vibrator.hasAmplitudeControl()){
            frequencyControl=prepareFrequencyEnvelopes();
            if(!frequencyControl)for(int i=1;i<patterns.length;i++){
                // Older hardware has no frequency choice. Keep its fallback quiet and steady.
                int a=Math.round(EngineHaptics.amplitude(i)*160);
                patterns[i]=VibrationEffect.createWaveform(new long[]{1000},new int[]{a},0);
            }
        }
        if(trace)android.util.Log.i("BargeBossRumble","frequencyControl="+frequencyControl+" minimumHz="+minimum+" maximumHz="+maximum);
    }
    /** Public API reflection keeps the existing pre-36 compile SDK and older Android support. */
    private boolean prepareFrequencyEnvelopes(){
        if(Build.VERSION.SDK_INT<36)return false;
        try{
            if(!(Boolean)Vibrator.class.getMethod("areEnvelopeEffectsSupported").invoke(vibrator))return false;
            Object profile=Vibrator.class.getMethod("getFrequencyProfile").invoke(vibrator);
            Object info=Vibrator.class.getMethod("getEnvelopeEffectInfo").invoke(vibrator);
            if(profile==null || info==null)return false;
            minimum=((Number)profile.getClass().getMethod("getMinFrequencyHz").invoke(profile)).floatValue();
            maximum=((Number)profile.getClass().getMethod("getMaxFrequencyHz").invoke(profile)).floatValue();
            if(!Float.isFinite(minimum) || !Float.isFinite(maximum) || minimum<=0 || maximum<minimum)return false;
            long minTime=((Number)info.getClass().getMethod("getMinControlPointDurationMillis").invoke(info)).longValue();
            long maxTime=((Number)info.getClass().getMethod("getMaxControlPointDurationMillis").invoke(info)).longValue();
            int points=Math.min(8,((Number)info.getClass().getMethod("getMaxSize").invoke(info)).intValue());
            if(minTime<=0 || maxTime<minTime || points<2)return false;
            Class<?> type=Class.forName("android.os.VibrationEffect$WaveformEnvelopeBuilder");
            Method initial=type.getMethod("setInitialFrequencyHz",float.class);
            Method point=type.getMethod("addControlPoint",float.class,float.class,long.class);
            Method build=type.getMethod("build");
            Method repeat=VibrationEffect.class.getMethod("createRepeatingEffect",VibrationEffect.class);
            for(int i=1;i<patterns.length;i++){
                Object builder=type.getConstructor().newInstance();
                float frequency=EngineHaptics.frequency(i,minimum,maximum),amplitude=EngineHaptics.amplitude(i);
                initial.invoke(builder,frequency);
                point.invoke(builder,amplitude,frequency,Math.max(minTime,Math.min(maxTime,120)));
                for(int p=1;p<points;p++)point.invoke(builder,amplitude,frequency,Math.max(minTime,Math.min(maxTime,1000)));
                patterns[i]=(VibrationEffect)repeat.invoke(null,build.invoke(builder));
            }
            return true;
        }catch(ReflectiveOperationException|RuntimeException e){
            android.util.Log.w("BargeBossRumble","Frequency envelopes unavailable; using quiet amplitude fallback",e);return false;
        }
    }
    void update(Game game,boolean feedback){
        if(Build.VERSION.SDK_INT<26 || patterns[1]==null)return;
        boolean allowed=Settings.System.getInt(context.getContentResolver(),Settings.System.HAPTIC_FEEDBACK_ENABLED,1)!=0;
        int next=feedback || !allowed?0:EngineHaptics.band(game.engineRumble());
        if(next==level)return;
        if(next==0){stop();return;}
        level=next;vibrator.vibrate(patterns[next],attributes);
        if(trace)android.util.Log.i("BargeBossRumble","band="+next+" frequencyHz="+(frequencyControl?EngineHaptics.frequency(next,minimum,maximum):0));
    }
    void stop(){if(level!=0 && vibrator!=null)vibrator.cancel();level=0;}
}
