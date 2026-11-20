package com.bargeboss.game;

import javax.sound.sampled.*;

/** Host-only playback of shared radio events. Missing audio hardware is nonfatal. */
final class RadioSound {
    private final Clip[] clips=new Clip[RadioAudio.COUNT];
    private Game active;
    private int sequence;
    RadioSound(){
        for(int i=0;i<clips.length;i++)try{
            java.net.URL url=RadioSound.class.getResource("/assets/"+RadioAudio.asset(i));
            if(url==null)url=new java.io.File("assets/"+RadioAudio.asset(i)).toURI().toURL();
            try(AudioInputStream input=AudioSystem.getAudioInputStream(url)){
                clips[i]=AudioSystem.getClip();clips[i].open(input);
            }
        }catch(Exception e){System.err.println("Radio audio unavailable: "+e.getMessage());}
    }
    void update(Game root){
        if(active!=root.activeGame()){stop();active=root.activeGame();sequence=0;}
        int next=RadioAudio.sequence(root);
        if(active.mode!=Game.PLAY){sequence=next;stop();return;}
        if(next==sequence)return;sequence=next;
        if(RadioAudio.audible(root)){stop();Clip clip=clips[RadioAudio.cue(root)];if(clip!=null){clip.setFramePosition(0);clip.start();}}
    }
    void stop(){for(Clip c:clips)if(c!=null)c.stop();}
    void close(){for(Clip c:clips)if(c!=null)c.close();}
}
