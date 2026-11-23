package com.bargeboss.game;

/** Preferences survive relaunch, share across practice, and control every existing audio bus. */
public final class AudioSettingsTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void run(){
        Game g=new Game();int version=g.progressVersion();
        check(g.settings.soundGain()==1 && g.settings.musicGain()==1,"existing full-volume default");
        g.settings.setSoundLevel(40);g.settings.setMusicLevel(65);g.settings.toggleMusicMute();
        check(g.progressVersion()==version+3,"host persistence notices all audio changes");
        String save=g.saveProgress();Game restored=new Game();restored.restoreProgress(save);
        check(restored.saveProgress().equals(save),"levels and independent mute states survive relaunch");
        restored.settings.toggleMusicMute();check(Math.abs(restored.settings.musicGain()-.65f)<.00001f,"unmute restores remembered level");
        String legacy="BBG1\n"+g.career.save()+"\n2";
        restored.restoreProgress(legacy);check(restored.settings.joystickSide()==GameSettings.RIGHT && restored.settings.soundGain()==1 && restored.settings.musicGain()==1,"old saves retain controls and get full-volume defaults");
        restored.restoreProgress(g.career.save());check(restored.settings.joystickSide()==GameSettings.LEFT && restored.settings.soundLevel()==100,"bare career save migration");
        for(String invalid:new String[]{"101,0,40,0","65,2,40,0","65,0,-1,0","65,0,40,2","65,0,no,0","65,0,40","65,0,40,0,1"}){
            restored.restoreProgress(save);restored.restoreProgress("BBG2\n"+g.career.save()+"\n2\n"+invalid);
            check(restored.saveProgress().equals(save),"malformed audio leaves the entire save unchanged");
        }
        g.reset();g.mode=Game.PLAY;g.tug.engineLoad=1;
        g.settings.setSoundLevel(100);float engine=EngineAudio.gain(g,1),effect=SoundEffects.gain(g);
        g.settings.setSoundLevel(40);
        check(Math.abs(EngineAudio.gain(g,1)-engine*.4f)<.00001f && Math.abs(SoundEffects.gain(g)-effect*.4f)<.00001f,"volume scales loops and one-shots together");
        g.settings.toggleSoundMute();g.sounds.emit(SoundEffects.THUD);
        check(!EngineAudio.audible(g) && EngineAudio.gain(g,1)==0 && !SoundEffects.audible(g) && SoundEffects.gain(g)==0,"mute silences loops and events");
        g.settings.toggleSoundMute();check(g.settings.soundLevel()==40 && EngineAudio.audible(g),"unmute preserves level");
        g.settings.setSoundLevel(0);check(!EngineAudio.audible(g) && !SoundEffects.audible(g),"zero level is silent without mute");
        g.settings.setSoundLevel(1000);g.settings.setMusicLevel(-10);
        check(g.settings.soundLevel()==100 && g.settings.musicLevel()==0,"levels clamp to valid range");
        g.startTutorial();check(g.activeGame().settings==g.settings,"practice shares audio preferences");
        g.activeGame().settings.toggleSoundMute();check(!EngineAudio.audible(g),"practice audio consumes shared settings");
        for(int[] size:new int[][]{{960,600},{1333,600},{960,720}})controls(size[0],size[1]);
        System.out.println("Audio settings persistence, mix, touch and practice checks passed");
    }
    private static void controls(int width,int height){
        Game g=new Game();g.configureCanvas(width,height);g.openSettings();
        Layout.Box slider=g.layout.box(Layout.SOUND_LEVEL),music=g.layout.box(Layout.MUSIC_LEVEL);
        g.pointerDown(12,slider.centerX(),slider.centerY());check(g.settings.soundLevel()==50,"tap sets level using centered canvas coordinates");
        g.pointerMove(99,slider.x,slider.centerY());check(g.settings.soundLevel()==50,"unowned finger cannot move fader");
        g.pointerMove(12,slider.x-100,slider.centerY());check(g.settings.soundLevel()==0,"drag clamps left endpoint");
        g.pointerUp(12,slider.x+slider.w+100,slider.centerY());check(g.settings.soundLevel()==100 && !g.pointer,"drag clamps right endpoint and releases capture");
        ReplayInput.tap(g,Layout.SOUND_MUTE);check(g.settings.soundMuted() && !g.settings.musicMuted(),"mute controls are independent");
        g.pointerDown(13,slider.centerX(),slider.centerY());g.pointerCancel(13);
        g.pointerMove(13,slider.x,slider.centerY());check(g.settings.soundLevel()==50 && g.settings.soundMuted(),"cancel releases slider; adjusting level preserves mute");
        ReplayInput.tap(g,Layout.MUSIC_MUTE);check(g.settings.musicMuted(),"music mute is interactive");
        g.pointerDown(14,music.x+12,music.centerY());g.pointerUp(14,music.x+12,music.centerY());
        check(g.settings.musicLevel()==0 && g.settings.soundLevel()==50,"music fader is independent");
        ReplayInput.tap(g,Layout.SETTINGS_BACK);check(g.mode==Game.TITLE,"done remains accessible");
    }
}
