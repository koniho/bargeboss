package com.bargeboss.game;

/** Progression must reward completed work once and survive opaque host persistence. */
public final class CareerTest {
    private static int assertions;
    private static void check(boolean condition,String message) { assertions++; if(!condition) throw new AssertionError(message); }
    public static void run() {
        challengeRules(); uniqueRewards(); persistence(); practiceIsolation(); menuRouting();
        System.out.println(assertions+" challenge, rank and progress persistence assertions passed");
    }
    private static void challengeRules() {
        check(Challenges.COUNT==17 && Challenges.quota(0)==2 && Challenges.quota(Challenges.CHICAGO)==1,"first level has two opposite-bank deliveries; the fire call is one job");
        check(Challenges.quota(1)==3 && Challenges.quota(2)==2 && Challenges.quota(3)==3,"shared slip and deep channel use three loads; lock passage uses two");
        check(Challenges.stars(0,1,0,100)==0 && Challenges.stars(0,2,0,0)==0,"incomplete goal or wreck earns no stars");
        check(Challenges.stars(0,2,1,79)==1 && Challenges.stars(0,2,0,79)==2
                && Challenges.stars(0,2,1,80)==2 && Challenges.stars(0,2,0,80)==3,"stars reward completion, safe cargo and hull condition");
        Game g=new Game();g.reset();Body waiting=new Body(90,2,260,730,0);waiting.age=g.contractSeconds()+400;g.cargo[0]=waiting;g.step();
        check(waiting.active && g.lost==0 && g.mode==Game.PLAY,"cargo never expires while a goal remains unfinished");
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
        Game game=new Game(); game.restoreProgress(career.save()); game.settings.completeTutorial(); game.selectChallenge(1); game.reset(); return game;
    }
    private static void practiceIsolation() {
        Game game=quickTurn(); String saved=game.saveProgress(); int version=game.progressVersion();
        game.startTutorial(); Game practice=game.activeGame();
        check(practice!=game && practice.career!=game.career && practice.saveProgress().equals(saved),"practice receives an isolated career snapshot");
        practice.tutorial.lesson=Tutorial.TIE;practice.tutorial.setup(practice);
        practice.delivered=10; practice.score=10000;
        practice.ticks=practice.shiftTicks()-1; practice.step();
        for(int i=0;i<14500 && practice.tutorial.lesson!=Tutorial.DONE;i++) { ReplayInput.tutorial(game);game.step(); }
        check(practice.tutorial.lesson==Tutorial.DONE && practice.earnedStars==0,"tutorial completion never records challenge rewards");
        // The towing-challenge best is a shared practice record (like preferences); career data must not move.
        String expected=saved.substring(0,saved.lastIndexOf('\n')+1)+game.settings.towingChallengeBest();
        check(game.settings.towingChallengeBest()>0 && game.saveProgress().equals(expected),"practice leaves parent career unchanged; only the shared towing-challenge best updates");
        version=game.progressVersion();
        ReplayInput.tap(game,Layout.START);
        check(game.activeGame()==game && game.challengeId==0 && game.saveProgress().equals(expected) && game.progressVersion()==version,"practice return preserves challenge and career");
    }
    private static void menuRouting() {
        Game game=new Game();game.configureCanvas(1400,600);
        ReplayInput.tap(game,Layout.CHALLENGES);check(game.mode==Game.CHALLENGES,"title opens location selector");
        int ticks=game.ticks,visual=game.visualTicks;game.advance(.1);
        check(game.ticks==ticks && game.visualTicks>visual,"location menu animates without simulation");
        for(int card=0;card<Challenges.LOCATION_COUNT;card++){
            if(card/Challenges.PAGE_SIZE>game.challengePage)ReplayInput.tap(game,Layout.CHALLENGE_NEXT_PAGE);
            check(game.challengePage==card/Challenges.PAGE_SIZE,"later locations page into view");
            ReplayInput.tap(game,Layout.CHALLENGE_CARDS[card%Challenges.PAGE_SIZE]);
            check(game.challengeId==Challenges.menuId(card),"cards select locations, not jobs");
        }
        ReplayInput.tap(game,Layout.CHALLENGE_START);
        check(game.mode==Game.PLAY && game.chicagoStage(),"selected location launches");
        game.background();ReplayInput.tap(game,Layout.RESTART);
        check(game.chicagoStage() && game.ticks==0,"restart preserves location");
        Game failed=new Game();failed.reset();failed.hull=0;failed.step();
        check(failed.mode==Game.FINISH && failed.career.totalStars()==0,"wreck earns no reward");
    }
}
