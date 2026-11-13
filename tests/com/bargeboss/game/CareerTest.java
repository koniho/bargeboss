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
        check(Challenges.COUNT==3 && Challenges.quota(0)==Game.QUOTA && Challenges.shiftTicks(0)==Game.SHIFT_TICKS,"first watch preserves original rules");
        check(Challenges.quota(1)==6 && Challenges.shiftTicks(1)==150*60,"quick turn requires six loads in150seconds");
        check(Challenges.quota(2)==8 && Challenges.shiftTicks(2)==180*60,"heavy shift requires eight loads in180seconds");
        check(Challenges.stars(0,5,0,100)==0 && Challenges.stars(0,6,0,0)==0,"quota failure or wreck earns no stars");
        check(Challenges.stars(0,6,1,79)==1 && Challenges.stars(0,6,0,79)==2
                && Challenges.stars(0,6,1,80)==2 && Challenges.stars(0,6,0,80)==3,"stars separately reward quota, no expiry and80percent hull");
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
        check(!career.unlocked(-1) && !career.unlocked(3) && !career.select(99),"challenge indices are bounded");
    }
    private static void persistence() {
        Career source=new Career(); source.record(0,3,1700); source.record(1,2,2000); source.select(2);
        Career restored=new Career(); check(restored.restore(source.save()),"valid career restores");
        check(restored.save().equals(source.save()) && restored.selectedChallenge()==2 && restored.version()==0,"save round trip retains selection, stars and scores without dirtying host");
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
        game.ticks=game.shiftTicks()-1; game.delivered=delivered; game.lost=lost; game.hull=hull; game.score=score;
        for(int i=0;i<game.cargo.length;i++) game.cargo[i]=null;
        game.step();
    }
    private static void completedShifts() {
        Game game=quickTurn(); int version=game.progressVersion();
        game.delivered=6; game.score=500; game.ticks=game.shiftTicks()-2;
        for(int i=0;i<game.cargo.length;i++) game.cargo[i]=null;
        game.step(); check(game.mode==Game.PLAY && game.progressVersion()==version,"meeting quota before timer cannot award unfinished work");
        game.step(); check(game.mode==Game.FINISH && game.earnedStars==3 && game.career.unlocked(2),"quick turn finishes at its own timer and unlocks heavy shift");
        check(game.rankPromoted && game.career.bestScore(1)==500,"successful finish records score and promotion exactly once");
        version=game.progressVersion(); String saved=game.saveProgress();
        game.step(); game.advance(.25);
        check(game.progressVersion()==version && game.saveProgress().equals(saved),"results animation cannot award again");
        game.reset(); check(game.challengeId==1 && game.shiftTicks()==9000 && game.earnedStars==0 && !game.rankPromoted,"replay keeps challenge and clears only current result");
        finishFixture(game,6,1,70,400);
        check(game.earnedStars==1 && game.career.bestStars(1)==3 && game.progressVersion()==version,"weaker replay displays earned star without farming or reducing best result");
        Game failed=new Game(); failed.reset(); finishFixture(failed,5,0,100,5000);
        check(failed.mode==Game.FINISH && failed.earnedStars==0 && failed.progressVersion()==0,"under-quota timer expiry has no rewards");
        failed.reset(); failed.delivered=8; failed.score=9000; failed.hull=0; failed.step();
        check(failed.mode==Game.FINISH && failed.progressVersion()==0 && failed.career.totalStars()==0,"wreck before shift end awards nothing even after quota");
        failed.reset(); failed.delivered=10; failed.score=10000; failed.reset();
        check(failed.progressVersion()==0,"abandoning a shift cannot award progress");
    }
    private static void practiceIsolation() {
        Game game=quickTurn(); String saved=game.saveProgress(); int version=game.progressVersion();
        game.startTutorial(); Game practice=game.activeGame();
        check(practice!=game && practice.career!=game.career && practice.saveProgress().equals(saved),"practice receives an isolated career snapshot");
        practice.tutorial.lesson=Tutorial.TIE; practice.delivered=10; practice.score=10000;
        practice.ticks=practice.shiftTicks()-1; practice.step();
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
        finishFixture(game,6,0,100,1000); ReplayInput.tap(game,Layout.NEXT_CHALLENGE);
        check(game.mode==Game.PLAY && game.challengeId==1 && game.quota()==6 && game.shiftTicks()==9000,"results next starts the unlocked challenge");
        finishFixture(game,6,0,100,1100); ReplayInput.tap(game,Layout.FINISH_CHALLENGES);
        ReplayInput.tap(game,Layout.CHALLENGE_CARDS[2]); ReplayInput.tap(game,Layout.CHALLENGE_START);
        check(game.challengeId==2 && game.quota()==8 && game.mode==Game.PLAY,"board card selection starts heavy shift");
        finishFixture(game,8,0,100,1200); ReplayInput.tap(game,Layout.NEXT_CHALLENGE);
        check(game.mode==Game.FINISH && game.challengeId==2,"final challenge has no out-of-range next shift");
        ReplayInput.tap(game,Layout.FINISH_CHALLENGES); ReplayInput.tap(game,Layout.BACK);
        check(game.mode==Game.TITLE,"challenge board back returns to title");
    }
    private static void boundedChallenges() {
        Career unlocks=new Career(); unlocks.record(0,1,1); unlocks.record(1,1,1);
        for(int id=1;id<Challenges.COUNT;id++) {
            Game game=new Game(); game.restoreProgress(unlocks.save()); game.selectChallenge(id); game.reset();
            for(int i=0;i<game.shiftTicks() && game.mode==Game.PLAY;i++) { ReplayInput.work(game); game.step(); }
            check(game.mode==Game.FINISH && game.ticks==game.shiftTicks(),"bounded player survives full challenge "+id);
            check(game.delivered>=game.quota() && game.earnedStars>0,"bounded player can complete challenge "+id+": "+game.delivered+" loads, quota "+game.quota()+", hull "+game.hull);
            check(game.career.bestStars(id)>=game.earnedStars && game.career.bestScore(id)==game.score,"actual challenge completion records earned career rewards");
            System.out.println(game.challengeTitle()+": "+game.delivered+" delivered, score "+game.score+", hull "+game.hull+", "+game.earnedStars+" stars");
        }
    }
}
