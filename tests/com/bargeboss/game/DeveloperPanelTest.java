package com.bargeboss.game;
public final class DeveloperPanelTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void run(){
        Game root=new Game();root.configureCanvas(960,600);
        root.startDeveloperMission(Challenges.SWIMMER_RESCUE);
        check(root.activeGame()==root,"release cannot jump missions");
        ReplayInput.tap(root,40,DeveloperPanel.BUTTON_Y+16);check(!root.developerPanel.open,"release has no dev button");
        root.settings.developerTools=true;ReplayInput.tap(root,40,DeveloperPanel.BUTTON_Y+16);check(!root.developerPanel.open,"no title developer panel");root.reset();String save=root.saveProgress();
        ReplayInput.tap(root,40,DeveloperPanel.BUTTON_Y+16);check(root.developerPanel.open,"open from title");
        int tick=root.ticks;root.advance(1);check(root.ticks==tick,"picker freezes world");
        float left=130;
        for(int area=0;area<3;area++){
            root=new Game();root.configureCanvas(960,600);root.settings.developerTools=true;
            root.challengeId=Challenges.menuId(area);root.reset();save=root.saveProgress();
            root.startDeveloperMission(area==0?Challenges.MONTROSE:Challenges.TWO_BANKS);
            check(root.activeGame()==root,"other locations cannot be launched");
            int[] jobs=Challenges.locationJobs(Challenges.menuId(area));
            for(int row=0;row<jobs.length;row++){
                if(!root.developerPanel.open)ReplayInput.tap(root,40,DeveloperPanel.BUTTON_Y+16);
                ReplayInput.tap(root,left+40,240+row*52);
                Game g=root.activeGame();check(g!=root && g.challengeId==jobs[row],"exact job starts");
                check(!root.developerPanel.open && g.tutorial==null,"picker closed, playable mission");
                if(Challenges.calumet(jobs[row])){
                    check(g.voyage.job==jobs[row],"correct contiguous Calumet objective");
                    check(g.cargo[0]!=null,"Calumet cargo initialized");
                    if(jobs[row]==2)check(g.cargo[0].deliveryRecorded,"convoy has empty previous-job hulls");
                }
                if(jobs[row]==Challenges.SWIMMER_RESCUE)check(g.rescue.x.length>=20,"swimmer targets initialized");
                root.pointerDown(9,900,570);root.pointerCancel(9);
                check(root.saveProgress().equals(save),"debug launch leaves saved career alone");
            }
        }
        ReplayInput.tap(root,40,DeveloperPanel.BUTTON_Y+16);root.back();check(!root.developerPanel.open,"back closes panel");
        root.startDeveloperMission(-1);check(root.activeGame().challengeId==Challenges.INDIANA,"invalid jump ignored");
        ReplayInput.tap(root,40,DeveloperPanel.BUTTON_Y+16);ReplayInput.tap(root,600,180);
        Game active=root.activeGame();int ticks=active.ticks;Body tug=active.tug;
        ReplayInput.tap(root,790,410);check(active.lighting()==LightingProfile.NIGHT,"embedded time control");
        ReplayInput.tap(root,790,302);check(active.settings.rainLevel()==1,"embedded rain control");
        root.advance(1);check(active.ticks==ticks && active.tug==tug,"environment changes preserve frozen mission");
        root.back();root.startDeveloperMission(Challenges.INDIANA);
        check(root.activeGame().lighting()==LightingProfile.NIGHT,"mission jump preserves selected live time");
        Game training=new Game();training.configureCanvas(960,600);
        ReplayInput.tap(training,Layout.CHALLENGES);ReplayInput.tap(training,Layout.TRAINING_HARBOR);
        check(training.trainingSelected,"training is a selectable harbor");
        ReplayInput.tap(training,Layout.CHALLENGE_START);check(training.activeGame().tutorial!=null,"training harbor sets sail into tutorial");
        System.out.println("Developer picker: current-location jobs, cross-location rejection, release gating, modal routing and career isolation passed");
    }
    public static void main(String[] args){run();}
}
