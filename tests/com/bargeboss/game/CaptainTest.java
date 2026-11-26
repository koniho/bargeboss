package com.bargeboss.game;

/** First-start routing, captain choice persistence and HUD portrait state priority/timing. */
final class CaptainTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void steps(Game g,int n){for(int i=0;i<n;i++)g.step();}
    private static void card(Game g,int i){ReplayInput.tap(g,Layout.CAPTAIN_CARDS[i]);}

    static void run(){
        firstStart();
        persistence();
        titlePicker();
        portraitStates();
        triggers();
        System.out.println(checks+" first-start, captain persistence and portrait state assertions passed");
    }

    private static void firstStart(){
        Game root=new Game();
        check(!root.settings.captainChosen() && !root.settings.tutorialDone() && root.firstVoyage(),"fresh install has no captain and owes the first tutorial");
        ReplayInput.tap(root,Layout.START);
        check(root.mode==Game.CAPTAINS && root.captainContinuesStart && root.activeGame()==root,"first Start opens the captain intro before anything else");
        ReplayInput.tap(root,Layout.CAPTAIN_CONFIRM);
        check(root.mode==Game.CAPTAINS && !root.settings.captainChosen(),"confirm needs a chosen card");
        int version=root.progressVersion();
        card(root,PlayerPortrait.DOT);
        check(root.captainChoice==PlayerPortrait.DOT && !root.settings.captainChosen(),"tapping a card only highlights it");
        ReplayInput.tap(root,Layout.CAPTAIN_CONFIRM);
        Game practice=root.activeGame();
        check(root.settings.captain()==PlayerPortrait.DOT && root.progressVersion()>version,"confirm stores the captain and marks progress dirty");
        check(practice!=root && practice.tutorial!=null && practice.tutorial.lesson==Tutorial.MOVE && !practice.tutorial.resumeShift,"first Start continues straight into the first tutorial");
        check(practice.settings==root.settings && practice.settings.captain()==PlayerPortrait.DOT,"practice shares the chosen captain");
        // Completing it spends the routing, then the voyage continues at the selected harbor.
        practice.tutorial.lesson=Tutorial.DONE;practice.tutorial.exitRequested=true;
        root.advance(1.0/60);
        check(root.settings.tutorialDone() && !root.firstVoyage(),"completed tutorial is remembered");
        check(root.activeGame()==root && root.mode==Game.PLAY && root.tutorial==null,"first tutorial hands over to the harbor jobs");
        root.pause();ReplayInput.tap(root,Layout.TITLE_SCREEN);
        check(root.mode==Game.TITLE,"pause returns to title");
        ReplayInput.tap(root,Layout.START);
        check(root.activeGame()==root && root.mode==Game.PLAY && root.tutorial==null,"later Starts keep the direct harbor behaviour");

        // Deliberately leaving the first tutorial also spends first-start routing.
        Game quitter=new Game();quitter.settings.setCaptain(PlayerPortrait.WESLEY);
        ReplayInput.tap(quitter,Layout.START);
        check(quitter.activeGame()!=quitter && quitter.activeGame().tutorial!=null,"captain already chosen: first Start goes straight to the tutorial");
        quitter.activeGame().pause();ReplayInput.tap(quitter,Layout.TITLE_SCREEN);
        check(quitter.activeGame()==quitter && quitter.mode==Game.TITLE && quitter.settings.tutorialDone(),"leaving the first tutorial for the title spends the routing");
        ReplayInput.tap(quitter,Layout.START);
        check(quitter.mode==Game.PLAY && quitter.activeGame()==quitter,"no forced second tutorial");

        // Existing careers with earned stars never get routed into the tutorial.
        Career career=new Career();career.record(0,1,100);
        Game veteran=new Game();veteran.restoreProgress(career.save());veteran.settings.setCaptain(PlayerPortrait.WHITNEY);
        check(!veteran.firstVoyage(),"earned stars count as an experienced captain");
        ReplayInput.tap(veteran,Layout.START);
        check(veteran.activeGame()==veteran && veteran.mode==Game.PLAY,"veteran Start enters the harbor directly");
        // A veteran without a captain still picks one once, then sails as before.
        Game upgrade=new Game();upgrade.restoreProgress(career.save());
        ReplayInput.tap(upgrade,Layout.START);check(upgrade.mode==Game.CAPTAINS,"upgrading players pick a captain once");
        card(upgrade,PlayerPortrait.WESLEY);ReplayInput.tap(upgrade,Layout.CAPTAIN_CONFIRM);
        check(upgrade.activeGame()==upgrade && upgrade.mode==Game.PLAY && upgrade.settings.captain()==PlayerPortrait.WESLEY,"after picking, a veteran sails straight to the harbor");
        // The training-harbor menu route is unchanged.
        Game training=ReplayInput.veteran(new Game());ReplayInput.tap(training,Layout.LEARN);
        check(training.activeGame()!=training && training.activeGame().tutorial!=null,"Learn still enters practice directly");
    }

    private static void persistence(){
        Game g=new Game();g.settings.setCaptain(PlayerPortrait.WESLEY);
        String save=g.saveProgress();
        check(save.startsWith("BBG4\n") && save.endsWith("\n0,1\n0"),"captain and tutorial state are saved: "+save);
        Game restored=new Game();restored.restoreProgress(save);
        check(restored.settings.captain()==PlayerPortrait.WESLEY && !restored.settings.tutorialDone(),"captain restores without the tutorial flag");
        g.settings.completeTutorial();restored=new Game();restored.restoreProgress(g.saveProgress());
        check(restored.settings.captain()==PlayerPortrait.WESLEY && restored.settings.tutorialDone() && restored.saveProgress().equals(g.saveProgress()),"both flags round-trip exactly");
        int version=g.progressVersion();g.settings.setCaptain(PlayerPortrait.WESLEY);g.settings.completeTutorial();
        check(g.progressVersion()==version,"unchanged values do not dirty progress");
        Career career=new Career();
        Game legacy=new Game();legacy.restoreProgress("BBG2\n"+career.save()+"\n2\n80,0,70,1");
        check(legacy.settings.joystickSide()==GameSettings.RIGHT && legacy.settings.musicLevel()==80 && !legacy.settings.captainChosen() && !legacy.settings.tutorialDone(),"BBG2 saves restore with no captain");
        for(String bad:new String[]{"1,3","2,0","1","1,0,0","x,1","1,-2","1,01"}){
            Game corrupt=new Game();corrupt.settings.setJoystickSide(GameSettings.RIGHT);
            corrupt.restoreProgress("BBG3\n"+career.save()+"\n1\n100,0,100,0\n"+bad);
            check(corrupt.settings.joystickSide()==GameSettings.RIGHT && !corrupt.settings.captainChosen(),"corrupt captain line is rejected atomically: "+bad);
        }
        Game unset=new Game();unset.restoreProgress("BBG3\n"+career.save()+"\n1\n100,0,100,0\n1,-1");
        check(unset.settings.tutorialDone() && !unset.settings.captainChosen(),"no-captain value restores");
        Game practice=ReplayInput.veteran(new Game());practice.settings.setCaptain(PlayerPortrait.DOT);practice.startTutorial();
        check(practice.activeGame().settings.captain()==PlayerPortrait.DOT,"practice snapshot keeps the captain");
    }

    private static void titlePicker(){
        Game g=ReplayInput.veteran(new Game());
        for(float[] size:new float[][]{{960,600},{1243,600},{960,720}}){
            g.configureCanvas(size[0],size[1]);
            Layout.Box captain=g.controlBox(Layout.CAPTAIN);
            check(captain.x>=0 && captain.x+captain.w<=size[0] && captain.y+captain.h<=size[1],"title captain button stays on canvas");
            for(Layout.Box other:new Layout.Box[]{g.controlBox(Layout.START),g.controlBox(Layout.CHALLENGES),g.controlBox(Layout.SETTINGS)})
                check(captain.x>=other.x+other.w || other.x>=captain.x+captain.w || captain.y>=other.y+other.h || other.y>=captain.y+captain.h,"title captain button has its own touch region");
        }
        ReplayInput.tap(g,Layout.CAPTAIN);
        check(g.mode==Game.CAPTAINS && !g.captainContinuesStart && g.captainChoice==PlayerPortrait.WHITNEY,"title opens the picker on the current captain");
        card(g,PlayerPortrait.DOT);ReplayInput.tap(g,Layout.BACK);
        check(g.mode==Game.TITLE && g.settings.captain()==PlayerPortrait.WHITNEY,"back leaves the captain unchanged");
        ReplayInput.tap(g,Layout.CAPTAIN);card(g,PlayerPortrait.WESLEY);ReplayInput.tap(g,Layout.CAPTAIN_CONFIRM);
        check(g.mode==Game.TITLE && g.activeGame()==g && g.settings.captain()==PlayerPortrait.WESLEY,"changing captain returns to the title without starting");
        ReplayInput.tap(g,Layout.CAPTAIN);g.back();
        check(g.mode==Game.TITLE,"host Back leaves the picker");
        for(int i=0;i<PlayerPortrait.COUNT;i++){
            check(ScreenView.story(i).length>=4 && ScreenView.story(i).length<=10,"backstory wraps into the card");
            for(String line:ScreenView.story(i))check(Lettering.width(line,10)<=Layout.CAPTAIN_CARDS[i].w-34,"wrapped lines fit the card");
            check(SpriteAtlas.file(PlayerPortrait.atlas(i)).startsWith("portrait-captain-") && SpriteAtlas.count(PlayerPortrait.atlas(i))==6,"captain portraits use six-frame portrait sheets");
        }
    }

    private static void portraitStates(){
        Game g=ReplayInput.veteran(new Game());g.reset();PlayerPortrait face=g.portrait;
        check(face.state(g)==PlayerPortrait.IDLE,"starts idle");
        g.visualTicks=0;check(face.frame(g)==PlayerPortrait.BLINK,"idle blinks at the start of its period");
        g.visualTicks=PlayerPortrait.BLINK_TICKS;check(face.frame(g)==PlayerPortrait.NEUTRAL,"blink lasts BLINK_TICKS");
        g.visualTicks=PlayerPortrait.BLINK_PERIOD+3;check(face.frame(g)==PlayerPortrait.BLINK,"blinks repeat");
        g.visualTicks=PlayerPortrait.BLINK_PERIOD+14;check(face.frame(g)==PlayerPortrait.BLINK,"every third period double-blinks");
        g.visualTicks=40;check(face.frame(g)==PlayerPortrait.NEUTRAL,"idle face between blinks");
        // Talking: a radio reply in flight.
        g.radio.transmit(RadioAudio.REPLY);
        check(face.state(g)==PlayerPortrait.TALK,"radio reply animates the mouth");
        boolean open=false,small=false;
        for(int t=0;t<40;t++){g.visualTicks=t;int frame=face.frame(g);open|=frame==PlayerPortrait.OPEN;small|=frame==PlayerPortrait.SMALL;
            check(frame==PlayerPortrait.OPEN || frame==PlayerPortrait.SMALL || frame==PlayerPortrait.NEUTRAL,"talking only uses mouth frames");}
        check(open && small,"talking alternates open and small mouths");
        // Wince outranks talking; mad outranks wince.
        PlayerPortrait.wince(g);
        check(face.state(g)==PlayerPortrait.WINCE && face.frame(g)==PlayerPortrait.FLINCH,"wince overrides speech");
        face.mad();
        check(face.state(g)==PlayerPortrait.MAD && face.frame(g)==PlayerPortrait.ANGRY,"mad overrides wince");
        steps(g,PlayerPortrait.MAD_TICKS-1);check(face.state(g)==PlayerPortrait.MAD,"mad holds for MAD_TICKS");
        steps(g,1);check(face.state(g)!=PlayerPortrait.MAD,"mad expires");
        check(face.winceTicks==0 && face.state(g)!=PlayerPortrait.WINCE,"shorter wince expired underneath mad");
        PlayerPortrait.wince(g);steps(g,PlayerPortrait.WINCE_TICKS-1);check(face.state(g)==PlayerPortrait.WINCE,"wince holds for WINCE_TICKS");
        steps(g,1);check(face.state(g)!=PlayerPortrait.WINCE,"wince expires");
        // Acknowledgements answer finished incoming calls.
        g=ReplayInput.veteran(new Game());g.reset();face=g.portrait;steps(g,2);
        g.radio.receiveBridge();
        steps(g,53);check(face.line(g)==null && face.state(g)!=PlayerPortrait.TALK,"no reply while the call is still incoming");
        steps(g,2);
        check(face.line(g)!=null && face.state(g)==PlayerPortrait.TALK,"captain acknowledges a finished call");
        check(face.line(g).equals(PlayerPortrait.ACKS[PlayerPortrait.WHITNEY][0]),"lines come from the chosen captain's voice");
        steps(g,PlayerPortrait.ACK_TICKS);check(face.line(g)==null && face.state(g)==PlayerPortrait.IDLE,"acknowledgement is short");
        g.radio.receiveBridge();steps(g,60);
        check(face.line(g)==null,"acknowledgements are rate limited");
        steps(g,PlayerPortrait.ACK_GAP);g.dialogue.show(g,"INSTRUCTOR","QUESTION?","DETAIL","YES.");steps(g,80);
        check(face.line(g)==null,"questions are answered by the player's own choice, not an automatic line");
        g.dialogue.close(g);steps(g,2);
        check(face.line(g)!=null,"a pending call is acknowledged once the dialogue closes");
        g.settings.setCaptain(PlayerPortrait.DOT);
        check(face.line(g).equals(PlayerPortrait.ACKS[PlayerPortrait.DOT][1]),"voice follows the current captain");
        g.reset();check(face.line(g)==null && face.state(g)==PlayerPortrait.IDLE,"reset clears reactions");
        // Drawing never mutates portrait state.
        g.portrait.mad();int mad=g.portrait.madTicks;Renderer.draw(g,new Parity());check(g.portrait.madTicks==mad,"rendering cannot advance the portrait");
    }

    private static void triggers(){
        // Mad: own tug takes hull damage.
        Game g=ReplayInput.veteran(new Game());ReplayInput.controlFixture(g);g.tug.x=415;g.tug.y=500;g.tug.vx=100;steps(g,1);
        check(g.hull<100 && g.hull==100-g.lastHit && g.portrait.state(g)==PlayerPortrait.MAD,"hard impact damages the hull and makes the captain mad");
        // Wince: the tug or a lined vessel damages another vessel's hull (HullDamage).
        for(float closing:new float[]{60,77.9f,78,110}){
            g=ReplayInput.veteran(new Game());ReplayInput.controlFixture(g);Body other=g.cargo[0];
            g.splashes.contact(g.tug,other,300,400,closing,0,0,0,closing);g.splashes.step(g);
            check((g.portrait.state(g)==PlayerPortrait.WINCE)==(closing>=HullDamage.HARD),"wince only for hard hits on other vessels: "+closing);
        }
        g=ReplayInput.veteran(new Game());ReplayInput.controlFixture(g);
        Body lined=g.cargo[0],other=g.cargo[1];lined.lineAttached=true;
        g.splashes.contact(other,lined,300,400,0,0,90,0,90);g.splashes.step(g);
        check(g.portrait.state(g)==PlayerPortrait.WINCE,"a lined barge striking another vessel counts, in either argument order");
        g=ReplayInput.veteran(new Game());ReplayInput.controlFixture(g);
        g.splashes.contact(g.cargo[0],g.cargo[1],300,400,95,0,0,0,95);g.splashes.step(g);
        check(g.portrait.state(g)==PlayerPortrait.IDLE,"collisions between other vessels are not the captain's fault");
        g.splashes.contact(g.tug,null,300,400,95,0,0,0,95);g.splashes.step(g);
        check(g.portrait.state(g)==PlayerPortrait.IDLE,"hitting the shore is not hitting another vessel");
        g.ticks+=HullDamage.COOLDOWN;g.splashes.step(g);g.splashes.contact(g.tug,g.cargo[1],300,400,95,0,0,0,95);g.splashes.step(g);g.splashes.step(g);
        check(g.portrait.winceTicks==PlayerPortrait.WINCE_TICKS,"each contact winces once, on its first tick");
    }
}
