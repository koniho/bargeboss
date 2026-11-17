package com.bargeboss.game;

/** Progression must reward completed work once and survive opaque host persistence. */
public final class CareerTest {
    private static int assertions;
    private static void check(boolean condition,String message) { assertions++; if(!condition) throw new AssertionError(message); }
    public static void run() {
        challengeRules(); uniqueRewards(); persistence(); completedShifts(); practiceIsolation(); menuRouting(); boundedChallenges();
        System.out.println(assertions+" challenge, rank and progress persistence assertions passed");
    }
    private static void challengeRules() {
        check(Challenges.COUNT==8 && Challenges.quota(0)==2,"first level has two opposite-bank deliveries");
        check(Challenges.quota(1)==3 && Challenges.quota(2)==2 && Challenges.quota(3)==3,"shared slip and deep channel use three loads; lock passage uses two");
        check(Challenges.stars(0,1,0,100)==0 && Challenges.stars(0,2,0,0)==0,"incomplete goal or wreck earns no stars");
        check(Challenges.stars(0,2,1,79)==1 && Challenges.stars(0,2,0,79)==2
                && Challenges.stars(0,2,1,80)==2 && Challenges.stars(0,2,0,80)==3,"stars reward completion, safe cargo and hull condition");
        Game g=new Game();g.reset();Body waiting=new Body(90,2,260,730,0);waiting.age=g.contractSeconds()+400;g.cargo[0]=waiting;g.step();
        check(waiting.active && g.lost==0 && g.mode==Game.PLAY,"cargo never expires while a goal remains unfinished");
        g=quickTurn();Body early=new Body(1,1,g.dockX(1),g.dockY(1),-(float)Math.PI/2),first=new Body(2,2,g.dockX(2),g.dockY(2),-(float)Math.PI/2);
        check(g.sharedBerthStage() && g.canTie(early) && g.canTie(first),"an empty shared slip accepts any load rather than a scripted letter order");
    }
    private static void uniqueRewards() {
        Career career=new Career();
        check(career.unlocked(0) && !career.unlocked(1) && career.stripes()==0 && career.totalStars()==0,"new captain starts at Deck Duck with first watch unlocked");
        check(!career.select(1) && !career.record(2,3,1000) && career.version()==0,"locked challenges cannot be selected or rewarded");
        check(career.record(0,1,100) && career.unlocked(1) && !career.unlocked(2),"one successful star unlocks only the next challenge");
        check(career.rank()==1 && career.stripes()==1 && career.rankTitle().equals("LINE TAMER"),"first star awards first stripe");
        int version=career.version();
        check(!career.record(0,1,100) && career.version()==version && career.totalStars()==1,"replaying same result cannot farm stars or writes");
        career.record(0,3,90);
        check(career.bestStars(0)==3 && career.bestScore(0)==100 && career.rank()==2,"better stars retain independent best score");
        career.record(0,1,150);
        check(career.bestStars(0)==3 && career.bestScore(0)==150 && career.totalStars()==3,"better score cannot lower stars");
        career.record(1,3,200); check(career.rank()==3 && career.totalStars()==6,"six unique stars earn Harbor Hero");
        career.record(2,3,300); check(career.rank()==4 && career.stripes()==4 && career.rankTitle().equals("CAPTAIN QUACK"),"nine unique stars earn Captain Quack");
        check(!career.unlocked(-1) && !career.unlocked(Challenges.COUNT) && !career.select(99),"challenge indices are bounded");
    }
    private static void persistence() {
        Career source=new Career(); source.record(0,3,1700); source.record(1,2,2000); source.select(2);
        Career restored=new Career(); check(restored.restore(source.save()),"valid career restores");
        check(restored.save().equals(source.save()) && restored.selectedChallenge()==2 && restored.version()==0,"save round trip retains selection, stars and scores without dirtying host");
        Career legacy=new Career();
        check(legacy.restore("BB1|2|3,1700|2,2000|1,900") && legacy.bestStars(3)==0 && legacy.unlocked(3),"three-level careers retain progress and append an empty fourth level");
        check(legacy.select(3) && new Career().restore(legacy.save()),"newly unlocked page-two level persists in expanded saves");
        String before=restored.save();
        String[] invalid={null,"","BB2|0|0,0|0,0|0,0","BB1|3|3,100|3,100|3,100","BB1|-1|0,0|0,0|0,0",
                "BB1|0|4,0|0,0|0,0","BB1|0|3,-1|0,0|0,0","BB1|0|3,1000000000|0,0|0,0",
                "BB1|0|0,0|3,100|0,0","BB1|2|3,100|0,0|0,0","BB1|0|0,100|0,0|0,0",
                "BB1|0|1,12x|0,0|0,0","BB1|0|1,100|0,0","BB1|0|1,100|0,0|0,0|", "BB1|0|,|0,0|0,0"};
        for(String value:invalid) check(!restored.restore(value) && restored.save().equals(before),"malformed progress is ignored atomically");
        StringBuilder longValue=new StringBuilder(); for(int i=0;i<200;i++) longValue.append('9');
        check(!restored.restore(longValue.toString()),"oversized save is bounded before parsing");
        check(restored.select(1) && restored.version()==1,"selected challenge marks progress dirty");
        int version=restored.version(); restored.select(1); check(restored.version()==version,"same selection does not generate repeated writes");
    }
    private static Game quickTurn() {
        Career career=new Career(); career.record(0,1,100);
        Game game=new Game(); game.restoreProgress(career.save()); game.selectChallenge(1); game.reset(); return game;
    }
    private static void finishFixture(Game game,int delivered,int lost,int hull,int score) {
        game.ticks=120; game.delivered=delivered; game.lost=lost; game.hull=hull; game.score=score;
        for(int i=0;i<game.cargo.length;i++) game.cargo[i]=null;
        if(game.lockStage()){
            game.lock.reset();game.tug.x=300;game.tug.y=Lock.ENTRY_Y+120;
            game.cargo[0]=new Body(1,2,260,580,0);game.cargo[1]=new Body(2,1,340,580,0);game.lock.step(game);
            game.cargo[0].y=game.cargo[1].y=-100;game.lock.phase=Lock.COMPLETE;
        }
        game.step();
    }
    private static void completedShifts() {
        Game game=quickTurn();int version=game.progressVersion();
        game.ticks=game.shiftTicks()*3;game.step();
        check(game.mode==Game.PLAY && game.progressVersion()==version,"elapsed global watch time never ends an unfinished goal");
        finishFixture(game,game.quota(),0,100,500);
        check(game.mode==Game.FINISH && game.earnedStars==3 && game.career.unlocked(2),"last ordered delivery immediately completes level and unlocks lock");
        check(game.rankPromoted && game.career.bestScore(1)==500,"successful goal records score and promotion exactly once");
        version=game.progressVersion();String saved=game.saveProgress();
        game.step();game.advance(.25);
        check(game.progressVersion()==version && game.saveProgress().equals(saved),"results animation cannot award again");
        game.reset();check(game.challengeId==1 && game.earnedStars==0 && !game.rankPromoted,"replay keeps challenge and clears only current result");
        finishFixture(game,game.quota(),1,70,400);
        check(game.earnedStars==1 && game.career.bestStars(1)==3 && game.progressVersion()==version,"weaker replay cannot farm or reduce best result");
        Game failed=new Game();failed.reset();failed.ticks=failed.shiftTicks()*4;failed.step();
        check(failed.mode==Game.PLAY && failed.earnedStars==0 && failed.progressVersion()==0,"uncompleted goal stays playable without a global deadline");
        failed.hull=0;failed.step();
        check(failed.mode==Game.FINISH && failed.progressVersion()==0 && failed.career.totalStars()==0,"wreck before goal earns nothing");
        failed.reset();failed.delivered=1;failed.score=10000;failed.reset();
        check(failed.progressVersion()==0,"abandoning a goal cannot award progress");
    }
    private static void practiceIsolation() {
        Game game=quickTurn(); String saved=game.saveProgress(); int version=game.progressVersion();
        game.startTutorial(); Game practice=game.activeGame();
        check(practice!=game && practice.career!=game.career && practice.saveProgress().equals(saved),"practice receives an isolated career snapshot");
        practice.tutorial.lesson=Tutorial.TIE;practice.tutorial.setup(practice);
        practice.delivered=10; practice.score=10000;
        practice.ticks=practice.shiftTicks()-1; practice.step();
        for(int i=0;i<7000 && practice.tutorial.lesson!=Tutorial.DONE;i++) { ReplayInput.tutorial(game);game.step(); }
        check(practice.tutorial.lesson==Tutorial.DONE && practice.earnedStars==0,"tutorial completion never records challenge rewards");
        check(game.progressVersion()==version && game.saveProgress().equals(saved),"practice leaves parent career and dirty version unchanged");
        ReplayInput.tap(game,Layout.START);
        check(game.activeGame()==game && game.challengeId==1 && game.saveProgress().equals(saved),"practice return preserves challenge and career");
    }
    private static void menuRouting() {
        Game game=new Game(); game.configureCanvas(1400,600);
        ReplayInput.tap(game,Layout.CHALLENGES); check(game.mode==Game.CHALLENGES,"responsive title challenge target opens board");
        int ticks=game.ticks,visual=game.visualTicks; game.advance(.1);
        check(game.ticks==ticks && game.visualTicks>visual,"challenge menu animates without advancing shift");
        ReplayInput.tap(game,Layout.CHALLENGE_CARDS[1]); check(game.challengeId==0,"locked card cannot replace active selection");
        ReplayInput.tap(game,Layout.CHALLENGE_START); check(game.mode==Game.PLAY && game.challengeId==0,"challenge start launches selected first watch");
        finishFixture(game,game.quota(),0,100,1000); ReplayInput.tap(game,Layout.NEXT_CHALLENGE);
        check(game.mode==Game.PLAY && game.challengeId==1 && game.quota()==3 && game.orderStage(),"results next starts the unlocked challenge");
        finishFixture(game,game.quota(),0,100,1100); ReplayInput.tap(game,Layout.FINISH_CHALLENGES);
        ReplayInput.tap(game,Layout.CHALLENGE_CARDS[2]); ReplayInput.tap(game,Layout.CHALLENGE_START);
        check(game.challengeId==2 && game.quota()==2 && game.lockStage() && game.mode==Game.PLAY,"board card selection starts the lock passage");
        finishFixture(game,game.quota(),0,100,1200); ReplayInput.tap(game,Layout.NEXT_CHALLENGE);
        check(game.mode==Game.PLAY && game.challengeId==3 && game.depthStage(),"lock completion unlocks the appended deep-channel level");
        finishFixture(game,game.quota(),0,100,1300);ReplayInput.tap(game,Layout.NEXT_CHALLENGE);
        check(game.mode==Game.PLAY && game.challengeId==Challenges.MONTROSE,"next starts appended marina mission");
        finishFixture(game,1,0,100,1200);ReplayInput.tap(game,Layout.NEXT_CHALLENGE);
        check(game.mode==Game.PLAY && game.indianaStage(),"marina completion unlocks Indiana Harbor");
        finishFixture(game,1,0,100,1400);ReplayInput.tap(game,Layout.NEXT_CHALLENGE);
        check(game.mode==Game.PLAY && game.challengeId==Challenges.RAFT_RESCUE,"Indiana leads to raft rescue");
        finishFixture(game,2,0,100,2400);ReplayInput.tap(game,Layout.NEXT_CHALLENGE);
        check(game.challengeId==Challenges.SWIMMER_RESCUE,"raft rescue leads to swimmer rescue");
        finishFixture(game,20,0,100,2000);ReplayInput.tap(game,Layout.NEXT_CHALLENGE);
        check(game.mode==Game.FINISH,"last rescue has no out-of-range next level");
        ReplayInput.tap(game,Layout.FINISH_CHALLENGES);
        check(game.challengePage==2,"challenge board opens on the selected rescue page");
        ReplayInput.tap(game,Layout.CHALLENGE_PREV_PAGE);
        ReplayInput.tap(game,Layout.CHALLENGE_PREV_PAGE);check(game.challengePage==0,"previous page reveals the original three levels");
        ReplayInput.tap(game,Layout.CHALLENGE_NEXT_PAGE);check(game.challengePage==1,"next page reveals appended levels");
        ReplayInput.tap(game,Layout.CHALLENGE_CARDS[0]);check(game.challengeId==3,"page-two card maps to its stable catalog ID");
        ReplayInput.tap(game,Layout.CHALLENGE_CARDS[1]);check(game.challengeId==Challenges.MONTROSE,"Montrose is selectable on page two");
        ReplayInput.tap(game,Layout.CHALLENGE_CARDS[2]);check(game.challengeId==Challenges.INDIANA,"Indiana occupies the appended page-two slot");
        ReplayInput.tap(game,Layout.BACK);
        check(game.mode==Game.TITLE,"challenge board back returns to title");
    }
    private static void boundedChallenges() {
        Career unlocks=new Career(); unlocks.record(0,1,1); unlocks.record(1,1,1); unlocks.record(2,1,1);
        for(int id=0;id<Challenges.COUNT;id++) {
            if(Challenges.kind(id)==Challenges.MONTROSE)continue; // MontroseTest drives the actual towing mission.
            Game game=new Game(); game.restoreProgress(unlocks.save()); game.selectChallenge(id); game.reset();
            for(int i=0;i<game.shiftTicks() && game.mode==Game.PLAY;i++) { ReplayInput.work(game); game.step(); }
            check(game.mode==Game.FINISH && game.ticks<game.shiftTicks(),"bounded player completes the goal before its replay bound "+id);
            check(game.goalComplete() && game.goalProgress()>=game.quota() && game.earnedStars>0,"bounded player can complete challenge "+id+": "+game.goalProgress()+" goals, quota "+game.quota()+", hull "+game.hull);
            if(!game.lockStage()){
                int empties=0;for(Body b:game.cargo)if(b!=null && b.active && b.deliveryRecorded)empties++;
                check(empties==game.quota(),"all delivered hulls survive successful level completion "+id);
            }
            if(game.sharedBerthStage())check(game.towReleases>=2,"actual tow catches and release gestures clear the shared slip for both following loads");
            check(game.career.bestStars(id)>=game.earnedStars && game.career.bestScore(id)==game.score,"actual challenge completion records earned career rewards");
            System.out.println(game.challengeTitle()+": "+game.goalProgress()+" goals, score "+game.score+", hull "+game.hull+", "+game.earnedStars+" stars");
        }
    }
}
