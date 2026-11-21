package com.bargeboss.game;

/** Native pointer ownership, projected steering, persistence and real tutorial driving. */
public final class JoystickTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static void steps(Game g,int n){for(int i=0;i<n;i++)g.step();}
    private static Game playing(){Game g=new Game();ReplayInput.startPractice(g);g=g.activeGame();g.tutorial.lesson=Tutorial.FIGURE_EIGHT;g.tutorial.setup(g);return g;}
    private static void hold(Game g,float x,float y){float cx=Joystick.centerX(g),cy=Joystick.centerY(g);g.pointerDown(3,cx,cy);g.pointerMove(3,cx+x,cy+y);}
    private static void release(Game g){g.pointerUp(3,Joystick.centerX(g)+g.joystick.x*42,Joystick.centerY(g)+g.joystick.y*42);}
    private static float loadedSpeed(int count) {
        Game g=new Game();g.challengeId=1;g.resetPracticeYard();g.bridge.phase=Bridge.OPEN;
        for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}
        g.tug=new Body(0,-1,300,100,-(float)Math.PI/2);
        for(int i=0;i<count;i++) {g.addCargo(2,300,100+110*(i+1));g.cargo[i].angle=-(float)Math.PI/2;g.tow.attach(g,g.cargo[i],i==0?g.tug:g.cargo[i-1]);}
        hold(g,-36,-21);float total=0;
        for(int i=0;i<180;i++){g.step();if(i>=150)total+=g.tug.speed();}
        check(g.tow.loadCount(g.tug)==count,"joystick towing retains all attached cargo");return total/30;
    }
    public static void run(){
        Game g=new Game();check(g.settings.joystickSide()==GameSettings.LEFT,"default left joystick");
        ReplayInput.tap(g,Layout.SETTINGS);check(g.mode==Game.SETTINGS,"settings opens from title");
        int v=g.progressVersion();ReplayInput.tap(g,Layout.JOYSTICK_RIGHT);
        check(g.settings.joystickSide()==GameSettings.RIGHT && g.progressVersion()>v,"setting dirties native save");
        v=g.progressVersion();ReplayInput.tap(g,Layout.JOYSTICK_RIGHT);check(g.progressVersion()==v,"same choice is not a write");
        Game restored=new Game();restored.restoreProgress(g.saveProgress());check(restored.saveProgress().equals(g.saveProgress()),"settings and career round trip");
        restored.settings.restore(GameSettings.OFF);check(restored.settings.joystickSide()==GameSettings.LEFT,"legacy OFF preference migrates to left throttle");
        String valid=restored.saveProgress();
        for(String invalid:new String[]{"BBG1\nbroken\n1","BBG1\n"+g.career.save()+"\n9",g.saveProgress()+"\n","BBG2\n"+g.career.save()+"\n1"}){
            restored.restoreProgress(invalid);check(restored.saveProgress().equals(valid),"invalid wrapper cannot partially restore");
        }
        restored.restoreProgress(g.career.save());check(restored.settings.joystickSide()==GameSettings.LEFT && restored.career.save().equals(g.career.save()),"legacy career loads with default joystick");
        ReplayInput.tap(g,Layout.SETTINGS_BACK);check(g.mode==Game.TITLE,"settings returns to title");
        ReplayInput.startPractice(g);Game practice=g.activeGame();String career=g.career.save();v=g.progressVersion();
        practice.openSettings();ReplayInput.tap(g,Layout.JOYSTICK_LEFT);
        check(g.settings.joystickSide()==GameSettings.LEFT && g.progressVersion()>v && g.career.save().equals(career),"practice preferences persist without changing career");
        ReplayInput.tap(g,Layout.SETTINGS_BACK);check(practice.mode==Game.PLAY && Joystick.visible(practice),"helm lesson temporarily demonstrates throttle without changing saved preference");
        ReplayInput.down(g,practice.tug.x,practice.tug.y);ReplayInput.move(g,practice.downX+90,practice.downY);steps(g,15);
        check(!practice.navigating && !practice.joystick.active,"tug dragging never drives the boat");g.cancelInputs();
        for(float[] size:new float[][]{{960,600},{1333,600},{960,720}})for(int side:new int[]{GameSettings.LEFT,GameSettings.RIGHT}){
            g=playing();g.configureCanvas(size[0],size[1]);g.settings.setJoystickSide(side);
            float cx=Joystick.centerX(g),cy=Joystick.centerY(g);
            check(cx>=Joystick.RADIUS+8 && cx+Joystick.RADIUS+8<=g.layout.width && cy+Joystick.RADIUS+8<=g.layout.height-44,"pad fits safe logical canvas above footer");
            hold(g,0,0);steps(g,1);check(g.joystick.active && !g.navigating,"center dead zone is neutral");
            g.pointerMove(3,cx+3,cy);steps(g,1);check(!g.navigating,"small thumb jitter stays neutral");
            g.pointerMove(3,cx+100,cy);steps(g,1);
            check(g.navigating && g.joystick.throttle()==1 && !g.pushing && !g.towAiming,"joystick drives immediately without hold or cargo gesture");
            float jx=g.joystick.x,jy=g.joystick.y;
            g.pointerDown(7,cx-30,cy);g.pointerMove(7,cx-30,cy);g.pointerUp(7,cx-30,cy);
            check(g.joystick.active && jx==g.joystick.x && jy==g.joystick.y,"another pointer cannot steal or release pad");
            steps(g,20);check(g.joystick.x==jx && g.joystick.y==jy,"camera easing preserves screen thumb direction");
            float speed=g.tug.speed();release(g);check(!g.joystick.active && !g.pointer && !g.navigating && g.tug.speed()==speed,"release ends thrust without deleting momentum");
            hold(g,25,0);check(g.joystick.throttle()>0 && g.joystick.throttle()<1,"part travel has proportional thrust");
            g.pointerCancel(3);check(!g.joystick.active,"native cancel releases joystick");
            hold(g,42,0);g.background();check(!g.joystick.active && g.mode==Game.PAUSE,"background stops thrust and pauses");
            g.pause();hold(g,42,0);g.configureCanvas(size[0]+10,size[1]);check(!g.joystick.active,"resize cancels pad");
            hold(g,42,0);g.openSettings();int ticks=g.ticks;g.advance(1);check(g.ticks==ticks && !g.joystick.active,"settings freezes shift and cancels thumb");
            ReplayInput.tap(g,Layout.SETTINGS_BACK);check(g.mode==Game.PLAY,"settings returns to exact prior play mode");
        }
        for(float[] direction:new float[][]{{42,0},{-42,0},{0,42},{0,-42}}){
            g=playing();float dx=(direction[0]-direction[1]/RiverCamera.DEPTH)/(2*RiverCamera.Q),dy=(direction[0]+direction[1]/RiverCamera.DEPTH)/(2*RiverCamera.Q);
            g.tug.angle=(float)StrictMath.atan2(dy,dx);float x=g.tug.x,y=g.tug.y;hold(g,direction[0],direction[1]);steps(g,25);
            RiverCamera c=g.layout.camera;float sx=c.screenX(g.tug.x,g.tug.y)-c.screenX(x,y),sy=c.screenY(g.tug.x,g.tug.y)-c.screenY(x,y);
            check(sx*direction[0]+sy*direction[1]>10,"boat follows screen direction under isometric projection");
            check(Math.abs(sx*direction[1]-sy*direction[0])<Math.abs(sx*direction[0]+sy*direction[1])*.12f,"projected direction follows thumb despite water drift: "+sx+","+sy);
        }
        g=playing();int moves=0;
        for(;moves<3000 && g.tutorial.lesson==Tutorial.FIGURE_EIGHT;moves++){
            RiverCamera c=g.layout.camera;float dx=c.screenX(g.tutorial.driveTargetX(),g.tutorial.driveTargetY())-c.screenX(g.tug.x,g.tug.y),dy=c.screenY(g.tutorial.driveTargetX(),g.tutorial.driveTargetY())-c.screenY(g.tug.x,g.tug.y),len=Math.max(1,Physics.length(dx,dy));
            if(!g.pointer)hold(g,0,0);g.pointerMove(3,Joystick.centerX(g)+42*dx/len,Joystick.centerY(g)+42*dy/len);g.step();
        }
        check(g.tutorial.lesson==Tutorial.REVERSE && g.tutorial.driveMarker==0,"joystick alone completes both real driving goals");
        release(g);steps(g,60);check(g.tutorial.lesson==Tutorial.REVERSE,"lifting keeps the reverse exercise pending");
        float empty=loadedSpeed(0),one=loadedSpeed(1),two=loadedSpeed(2);
        check(empty>one*1.08f && one>two*1.08f,"joystick speed falls with actual linked load: "+empty+","+one+","+two);
        Game low=playing(),high=playing();hold(low,42,0);hold(high,42,0);
        for(int i=0;i<60;i++)low.advance(1.0/30);for(int i=0;i<288;i++)high.advance(1.0/144);
        check(low.ticks==high.ticks && low.tug.x==high.tug.x && low.tug.y==high.tug.y,"joystick physics is identical at 30 and 144 Hz");
        System.out.println(checks+" joystick input, settings persistence and tutorial assertions passed");
    }
    public static void main(String[] args){run();}
}
