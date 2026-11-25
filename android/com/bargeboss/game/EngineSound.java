package com.bargeboss.game;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.MediaPlayer;
import android.os.Build;

/** Predecoded engine loops and shared radio events; no decoding during playback. */
final class EngineSound implements AudioManager.OnAudioFocusChangeListener {
    private final SoundPool pool;
    private final boolean trace;
    private final AudioManager manager;
    private final int[] sounds=new int[EngineAudio.LAYERS+SoundEffects.COUNT],streams=new int[EngineAudio.LAYERS];
    private int loaded,radioSequence;
    private final int[] effects=new int[SoundEffects.COUNT];
    private Game radioGame;
    private boolean requested,focused,closed;
    private AudioFocusRequest request;
    private MediaPlayer music;
    private boolean musicReady;
    EngineSound(Context context){
        trace=(context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0 && context instanceof android.app.Activity && ((android.app.Activity)context).getIntent().getBooleanExtra("bb_audio_trace",false);
        manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        pool=new SoundPool.Builder().setMaxStreams(EngineAudio.LAYERS+SoundEffects.VOICES).setAudioAttributes(attributes).build();
        if(Build.VERSION.SDK_INT>=26)request=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes).setOnAudioFocusChangeListener(this).build();
        if(MusicAudio.available())try(AssetFileDescriptor file=context.getAssets().openFd(MusicAudio.asset())){
            music=new MediaPlayer();music.setAudioAttributes(attributes);
            music.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength());
            music.setLooping(true);
            music.setOnPreparedListener(player->{if(!closed)musicReady=true;});
            music.setOnErrorListener((player,what,extra)->{musicReady=false;android.util.Log.w("BargeBoss","Music unavailable: "+what+"/"+extra);return true;});
            music.prepareAsync();
        }catch(java.io.IOException | RuntimeException e){if(music!=null)music.release();music=null;android.util.Log.w("BargeBoss","Music unavailable",e);}
        pool.setOnLoadCompleteListener((p,id,status)->{if(status==0)loaded++;else android.util.Log.w("BargeBoss","Engine loop failed to load: "+id);});
        for(int i=0;i<sounds.length;i++)try(AssetFileDescriptor file=context.getAssets().openFd(i<EngineAudio.LAYERS?EngineAudio.asset(i):SoundEffects.asset(i-EngineAudio.LAYERS))){sounds[i]=pool.load(file,1);}
        catch(java.io.IOException e){android.util.Log.w("BargeBoss","Engine audio unavailable",e);}
    }
    void update(Game game){
        if(closed)return;
        Game active=game.activeGame();
        if(radioGame!=active){silence();radioGame=active;radioSequence=0;}
        boolean effectsAudible=EngineAudio.audible(game);
        float musicGain=MusicAudio.gain(game);
        if(!effectsAudible && (musicGain<=0 || !musicReady)){radioSequence=SoundEffects.sequence(game);stop();return;}
        if(!requested){requested=true;focused=(Build.VERSION.SDK_INT>=26?manager.requestAudioFocus(request):manager.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN))==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;}
        if(!focused){radioSequence=SoundEffects.sequence(game);return;}
        if(musicReady){
            music.setVolume(musicGain,musicGain);
            if(musicGain>0 && !music.isPlaying()){music.start();if(trace)android.util.Log.i("BargeBossAudio","music started gain="+musicGain);}
            else if(musicGain<=0 && music.isPlaying())music.pause();
        }
        if(!effectsAudible){radioSequence=SoundEffects.sequence(game);silence();return;}
        if(loaded!=sounds.length)return;
        for(int i=0;i<streams.length;i++){
            float gain=EngineAudio.gain(game,i),rate=EngineAudio.rate(game,i);
            if(streams[i]==0)streams[i]=pool.play(sounds[i],gain,gain,1,-1,rate);
            else{pool.setVolume(streams[i],gain,gain);pool.setRate(streams[i],rate);}
        }
        float effectGain=SoundEffects.gain(game);
        for(int stream:effects)if(stream!=0)pool.setVolume(stream,effectGain,effectGain);
        int sequence=SoundEffects.sequence(game);
        if(sequence!=radioSequence){radioSequence=sequence;if(SoundEffects.audible(game)){
            int cue=SoundEffects.cue(game);
            for(int i=0;i<effects.length;i++)if(SoundEffects.interrupts(cue,i) && effects[i]!=0){pool.stop(effects[i]);effects[i]=0;}
            int radioStream=effects[cue]=pool.play(sounds[EngineAudio.LAYERS+cue],effectGain,effectGain,2,0,1);
            if(trace)android.util.Log.i("BargeBossAudio","cue="+SoundEffects.cue(game)+" stream="+radioStream+" tick="+active.ticks+" asset="+SoundEffects.asset(SoundEffects.cue(game)));
        }}
    }
    private void silence(){for(int i=0;i<effects.length;i++){if(effects[i]!=0)pool.stop(effects[i]);effects[i]=0;}for(int i=0;i<streams.length;i++){if(streams[i]!=0)pool.stop(streams[i]);streams[i]=0;}}
    public void onAudioFocusChange(int change){focused=change==AudioManager.AUDIOFOCUS_GAIN;if(!focused){silence();pauseMusic();}}
    private void pauseMusic(){if(musicReady && music.isPlaying())music.pause();}
    void stop(){if(closed)return;silence();pauseMusic();if(requested){if(Build.VERSION.SDK_INT>=26)manager.abandonAudioFocusRequest(request);else manager.abandonAudioFocus(this);}requested=focused=false;}
    void close(){stop();closed=true;pool.release();if(music!=null){music.release();music=null;}musicReady=false;}
}
