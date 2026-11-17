package com.bargeboss.game;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

/** Two predecoded loops; no decoding, allocation or gameplay decisions in update. */
final class EngineSound implements AudioManager.OnAudioFocusChangeListener {
    private final SoundPool pool;
    private final AudioManager manager;
    private final int[] sounds=new int[EngineAudio.LAYERS],streams=new int[EngineAudio.LAYERS];
    private int loaded;
    private boolean requested,focused,closed;
    private AudioFocusRequest request;
    EngineSound(Context context){
        manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        pool=new SoundPool.Builder().setMaxStreams(EngineAudio.LAYERS).setAudioAttributes(attributes).build();
        if(Build.VERSION.SDK_INT>=26)request=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes).setOnAudioFocusChangeListener(this).build();
        pool.setOnLoadCompleteListener((p,id,status)->{if(status==0)loaded++;else android.util.Log.w("BargeBoss","Engine loop failed to load: "+id);});
        for(int i=0;i<sounds.length;i++)try(AssetFileDescriptor file=context.getAssets().openFd(EngineAudio.asset(i))){sounds[i]=pool.load(file,1);}
        catch(java.io.IOException e){android.util.Log.w("BargeBoss","Engine audio unavailable",e);}
    }
    void update(Game game){
        if(closed)return;
        if(!EngineAudio.audible(game)){stop();return;}
        if(loaded!=sounds.length)return;
        if(!requested){requested=true;focused=(Build.VERSION.SDK_INT>=26?manager.requestAudioFocus(request):manager.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN))==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;}
        if(!focused)return;
        for(int i=0;i<sounds.length;i++){
            float gain=EngineAudio.gain(game,i),rate=EngineAudio.rate(game,i);
            if(streams[i]==0)streams[i]=pool.play(sounds[i],gain,gain,1,-1,rate);
            else{pool.setVolume(streams[i],gain,gain);pool.setRate(streams[i],rate);}
        }
    }
    private void silence(){for(int i=0;i<streams.length;i++){if(streams[i]!=0)pool.stop(streams[i]);streams[i]=0;}}
    public void onAudioFocusChange(int change){focused=change==AudioManager.AUDIOFOCUS_GAIN;if(!focused)silence();}
    void stop(){if(closed)return;silence();if(requested){if(Build.VERSION.SDK_INT>=26)manager.abandonAudioFocusRequest(request);else manager.abandonAudioFocus(this);}requested=focused=false;}
    void close(){stop();closed=true;pool.release();}
}
