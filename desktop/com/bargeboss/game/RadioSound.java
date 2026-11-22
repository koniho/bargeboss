package com.bargeboss.game;

import javax.sound.sampled.*;

/** Host-only playback of shared radio events. Missing audio hardware is nonfatal. */
final class RadioSound {
    private final Clip[] clips=new Clip[SoundEffects.COUNT];
    private final Clip[] loops=new Clip[EngineAudio.LAYERS];
    private Game active;
    private int sequence;
    RadioSound(){
        for(int i=0;i<clips.length;i++)clips[i]=load(SoundEffects.asset(i));
        for(int i=0;i<loops.length;i++)loops[i]=load(EngineAudio.asset(i));
    }
    private static Clip load(String asset){
        try{
            java.net.URL url=RadioSound.class.getResource("/assets/"+asset);
            if(url==null)url=new java.io.File("assets/"+asset).toURI().toURL();
            try(AudioInputStream input=AudioSystem.getAudioInputStream(url)){
                Clip clip=AudioSystem.getClip();clip.open(input);return clip;
            }
        }catch(Exception e){System.err.println("Audio unavailable: "+e.getMessage());return null;}
    }
    private void loops(Game root){
        for(int i=0;i<loops.length;i++){
            float gain=EngineAudio.gain(root,i);Clip c=loops[i];
            if(gain<=0){if(c!=null)c.stop();continue;}
            if(c==null)continue;
            if(c.isControlSupported(FloatControl.Type.MASTER_GAIN)){
                FloatControl volume=(FloatControl)c.getControl(FloatControl.Type.MASTER_GAIN);
                volume.setValue(Math.max(volume.getMinimum(),Math.min(volume.getMaximum(),20*(float)Math.log10(gain))));
            }
            if(c.isControlSupported(FloatControl.Type.SAMPLE_RATE)){
                FloatControl rate=(FloatControl)c.getControl(FloatControl.Type.SAMPLE_RATE);
                rate.setValue(Math.max(rate.getMinimum(),Math.min(rate.getMaximum(),c.getFormat().getSampleRate()*EngineAudio.rate(root,i))));
            }
            if(!c.isRunning())c.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }
    void update(Game root){
        if(active!=root.activeGame()){stop();active=root.activeGame();sequence=0;}
        int next=SoundEffects.sequence(root);
        if(active.mode!=Game.PLAY){sequence=next;stop();return;}
        loops(root);
        if(next==sequence)return;sequence=next;
        if(SoundEffects.audible(root)){int cue=SoundEffects.cue(root);for(int i=0;i<clips.length;i++)if(clips[i]!=null && SoundEffects.interrupts(cue,i))clips[i].stop();Clip clip=clips[cue];if(clip!=null){clip.setFramePosition(0);clip.start();}}
    }
    void stop(){for(Clip c:clips)if(c!=null)c.stop();for(Clip c:loops)if(c!=null)c.stop();}
    void close(){for(Clip c:clips)if(c!=null)c.close();for(Clip c:loops)if(c!=null)c.close();}
}
