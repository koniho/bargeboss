package com.bargeboss.game;

/** Navigation orientation, overlay layout, vector-only chart and control cue regressions. */
final class OverviewMapTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static final class Ink implements Painter {
        boolean chartOnly;int lines,fills;float x,y,xx,yy;
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
        public void rect(float x,float y,float w,float h,int c){if(chartOnly){check(w>0 && h>0 && (c>>>24)<255,"scenery footprints are finite translucent vectors");fills++;}}
        public void circle(float x,float y,float r,int c){}
        public void poly(float[] xy,int c){if(chartOnly){check((c>>>24)>0 && (c>>>24)<255,"non-traversable fill is more translucent than chart contours");fills++;}}
        public void material(float x,float y,float ax,float ay,float bx,float by,int k,float d,float v,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){if(chartOnly)check((c>>>24)==255,"chart strokes are opaque before group translucency");if(lines++==0){this.x=x;this.y=y;this.xx=xx;this.yy=yy;}check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(xx)&&Float.isFinite(yy),"finite lines");}
    }
    static int arrows(Game g,Ink p){int count=0;WorldArrows.begin(g);try{while(WorldArrows.pending()){WorldArrows.draw(p);count++;}}finally{WorldArrows.end();}return count;}
    static void run(){
        Game g=new Game();ReplayInput.controlFixture(g);
        for(int stage=0;stage<4;stage++){
            OverviewMap.Chart c=new OverviewMap.Chart(stage);float x=(c.minX+c.maxX)*.5f,y=(c.minY+c.maxY)*.5f;
            check(c.stage==1?c.y(x+10)>c.y(x):c.y(x+10)<c.y(x),"isometric upper-right becomes map up");
            check(c.x(y+10)>c.x(y),"isometric lower-right becomes map right");
            check(Math.abs((c.x(y+10)-c.x(y))-Math.abs(c.y(x)-c.y(x+10)))<.001f,"overhead projection has equal axis scale");
            float[] actual=stage==1?PolygonWater.bounds(MontroseHarbor.PLAY_BOUNDS):stage==2?new float[]{IndianaHarbor.MIN_X,IndianaHarbor.MIN_Y,IndianaHarbor.MAX_X,IndianaHarbor.MAX_Y}:stage==3?new float[]{CalumetRiver.MIN_X,CalumetRiver.MIN_Y,CalumetRiver.MAX_X,CalumetRiver.MAX_Y}:TrainingScenery.bounds();
            check(c.minX==actual[0] && c.minY==actual[1] && c.maxX==actual[2] && c.maxY==actual[3],"overview uses authoritative scenery coverage for the tutorial");
            check(c.boundary[0]==c.x(c.stage==1?c.minX:c.minY) && c.boundary[1]==c.y(c.stage==1?c.minY:c.maxX) && c.boundary[4]==c.x(c.stage==1?c.maxX:c.maxY) && c.boundary[5]==c.y(c.stage==1?c.maxY:c.minX),"border and world corners share one projection");
            check(c.boundary[0]==OverviewMap.EDGE && c.boundary[1]==OverviewMap.EDGE,"top-left play corner is aligned to viewport stroke");
            check(Math.abs(c.x(c.stage==1?c.maxX:c.maxY)-(OverviewMap.WIDTH-OverviewMap.EDGE))<.001f || Math.abs(c.y(c.stage==1?c.maxY:c.minX)-(OverviewMap.HEIGHT-OverviewMap.EDGE))<.001f,"full-area fit reaches at least one far viewport edge without stretching");
            Ink ink=new Ink();ink.chartOnly=true;c.paint(ink);check(ink.lines>8 && ink.fills>0 && c.navigableHoles.length>0,"chart has contours and a traversable cutout in the land fill");
        }
        for(float width:new float[]{960,1243,1440})for(int side:new int[]{GameSettings.LEFT,GameSettings.RIGHT,GameSettings.OFF}){
            g.configureCanvas(width,600);g.settings.setJoystickSide(side);Layout.Box b=OverviewMap.bounds(g);
            check(side==GameSettings.RIGHT?b.centerX()<width/2:b.centerX()>width/2,"overview opposite throttle");
            check(b.y>64 && b.y+b.h<552 && b.w==b.h,"square overview clears top and bottom HUD");
            Layout.Box slider=OverviewMap.sliderBounds(g),help=g.layout.box(Layout.HELP),practice=g.layout.box(Layout.PRACTICE);
            check(slider.centerX()==b.centerX() && slider.y-(b.y+b.h)==8 && slider.y+slider.h<=g.layout.height,"slider centered eight pixels below map");
            check(slider.x+slider.w<help.x || slider.x>practice.x+practice.w,"slider clears centered action buttons");
        }
        zoomControls();
        simultaneousControls();
        g=new Game();ReplayInput.controlFixture(g);g.tug.vx=30;g.control=Game.REVERSING;g.reversing=true;
        int reverse=arrows(g,new Ink());g.control=Game.IDLE;g.reversing=false;check(reverse==arrows(g,new Ink()),"reverse adds no velocity heading arrow");
        g=new Game();ReplayInput.controlFixture(g);Body b=g.cargo[0];b.x=300;b.y=600;g.down(b.x,b.y);for(int i=0;i<12;i++)g.step();
        check(ControlView.assistPush(g),"fixture engages push assist");g.pushX=1;g.pushY=0;g.control=Game.PUSHING;
        Ink assist=new Ink();check(arrows(g,assist)==1 && assist.lines==9,"only one feathered command arrow during assist");
        check(Math.abs(assist.x-(b.x+46))<.001f && Math.abs(assist.xx-(b.x+108))<.001f && assist.y==assist.yy,"arrow follows commanded vector without world-bound clipping");
        Ink other=new Ink();ControlView.arrow(g,other,0,0,30,30,ControlView.BLUE);check(other.lines==0,"other guidance arrows suppressed during assist");
        g=new Game();ReplayInput.controlFixture(g);int tick=g.ticks;float tx=g.tug.x,ty=g.tug.y;
        OverviewMap.draw(g,new Ink());check(g.ticks==tick && g.tug.x==tx && g.tug.y==ty,"map rendering leaves gameplay unchanged");
        g.startTutorial();Game practice=g.activeGame();practice.tutorial.lesson=Tutorial.DONE;
        check(!OverviewMap.visible(practice),"map stays below tutorial completion UI");
        check(Math.abs(OverviewMap.pulse(0)-OverviewMap.pulse(120))<.001f && OverviewMap.pulse(30)>OverviewMap.pulse(90),"player pulse loops on shared visual clock");
        System.out.println("Overview orientation, vector outlines, HUD bounds and command arrow checks passed");
    }
    private static void zoomControls(){
        for(int stage=0;stage<4;stage++) {
            Game g=new Game();if(stage==0)ReplayInput.controlFixture(g);
            else {g.challengeId=stage==1?Challenges.MONTROSE:stage==2?Challenges.INDIANA:0;g.reset();}
            OverviewMap.Chart c=OverviewMap.chart(g);
            Layout.Box slider=OverviewMap.sliderBounds(g);
            RiverCamera camera=g.layout.camera;
            float tugX=g.tug.x,tugY=g.tug.y;
            g.pointerDown(7,slider.x+24,slider.centerY());
            check(g.overview.active && g.overview.zoom()==OverviewMap.MIN_ZOOM,"left end is maximum zoom-out");
            g.pointerDown(8,slider.x+slider.w-24,slider.centerY());
            g.pointerMove(8,slider.x+slider.w-24,0);
            check(g.overview.zoom()==OverviewMap.MIN_ZOOM && !g.pinchZoom.active(),"second pointer cannot steal slider or pinch world");
            g.pointerMove(7,slider.x+slider.w+80,slider.centerY()-200);
            check(g.overview.zoom()==OverviewMap.maxZoom(g),"drag outside slider remains captured and clamps to max");
            g.pointerUp(7,slider.x+slider.w+80,slider.centerY()-200);
            check(!g.pointer && !g.overview.active && !g.navigating && !g.joystick.active && g.ropePhase==0,"release cannot drive or throw lasso");
            check(g.overview.level()==1 && g.overview.zoom()==OverviewMap.maxZoom(g),"relative indicator retains current zoom after release");
            check(g.layout.camera==camera && g.tug.x==tugX && g.tug.y==tugY,"overview zoom leaves main camera and vessel unchanged");
            for(float fx:new float[]{0,.1f,.5f,.9f,1})for(float fy:new float[]{0,.1f,.5f,.9f,1}) {
                float x=c.minX+fx*(c.maxX-c.minX),y=c.minY+fy*(c.maxY-c.minY);
                g.tug.x=x;g.tug.y=y;
                for(float fraction:new float[]{0,.5f,1}) {
                    ReplayInput.tap(g,slider.x+24+(slider.w-48)*fraction,slider.centerY());
                    check(g.overview.zoom()>=1,"zoom cannot shrink below full play-area fit");
                    checkAxis(OverviewMap.mapX(g,c.stage==1?c.minX:c.minY),OverviewMap.mapX(g,c.stage==1?c.maxX:c.maxY),OverviewMap.mapX(g,x,y),OverviewMap.WIDTH);
                    checkAxis(OverviewMap.mapY(g,c.stage==1?c.minY:c.maxX),OverviewMap.mapY(g,c.stage==1?c.maxY:c.minX),OverviewMap.mapY(g,x,y),OverviewMap.HEIGHT);
                    if(fraction==0)check(OverviewMap.mapX(g,c.stage==1?c.maxX:c.maxY)<=OverviewMap.WIDTH && OverviewMap.mapY(g,c.stage==1?c.maxY:c.minX)<=OverviewMap.HEIGHT,"max zoom-out fits entire play area independent of tug location");
                }
            }
            ReplayInput.tap(g,slider.x,slider.centerY());check(g.overview.zoom()==1 && g.overview.level()==0,"left overshoot cannot zoom beyond play-area size");
            g.pointerDown(9,slider.centerX(),slider.centerY());g.background();
            check(!g.overview.active && !g.pointer,"background cancels slider capture");
            g.pause();g.pointerDown(10,slider.centerX(),slider.centerY());g.pointerCancel(10);
            check(!g.overview.active && !g.pointer,"touch cancel releases slider");
            g.reset();check(Math.abs(g.overview.zoom()-1)<.001f,"new voyage resets map zoom");
        }
        Game root=new Game();root.startTutorial();Game practice=root.activeGame();practice.tutorial.setup(practice);
        for(int lesson=Tutorial.MOVE;lesson<Tutorial.MAP;lesson++){practice.tutorial.lesson=lesson;check(!OverviewMap.visible(practice),"map hidden before map lesson");Layout.Box hidden=OverviewMap.sliderBounds(practice);check(!OverviewMap.sliderHit(practice,hidden.centerX(),hidden.centerY()),"hidden slider cannot capture touch");}
        practice.tutorial.lesson=Tutorial.MAP;check(OverviewMap.visible(practice),"map appears for its lesson");Layout.Box slider=OverviewMap.sliderBounds(practice);
        ReplayInput.tap(root,slider.x+slider.w-24,slider.centerY());
        check(practice.overview.zoom()==OverviewMap.maxZoom(practice) && root.overview.zoom()==1,"practice routes slider to its active game");
    }
    private static void checkAxis(float low,float high,float player,float size){
        float edge=OverviewMap.EDGE;
        check(player>=edge-.002f && player<=size-edge+.002f,"tug stays within overview");
        if(high-low<=size-2*edge+.002f) {
            check(Math.abs(low-edge)<.002f,"axis smaller than viewport keeps top/left alignment");
        } else {
            check(low<=edge+.002f && high>=size-edge-.002f,"following never pulls playable edge inside viewport");
            if(player<size*.5f-.002f)check(Math.abs(low-edge)<.002f,"player may leave center near top/left boundary only");
            else if(player>size*.5f+.002f)check(Math.abs(high-(size-edge))<.002f,"player may leave center near bottom/right boundary only");
        }
    }
    private static void simultaneousControls(){
        for(boolean joystick:new boolean[]{false,true})for(boolean zoomFirst:new boolean[]{false,true})for(boolean releaseZoomFirst:new boolean[]{false,true}) {
            Game g=new Game();ReplayInput.controlFixture(g);g.settings.setJoystickSide(GameSettings.RIGHT);
            Layout.Box slider=OverviewMap.sliderBounds(g);
            float dx=joystick?Joystick.centerX(g):g.layout.camera.screenX(g.tug.x,g.tug.y);
            float dy=joystick?Joystick.centerY(g):g.layout.camera.screenY(g.tug.x,g.tug.y);
            if(zoomFirst)g.pointerDown(7,slider.x+24,slider.centerY());
            g.pointerDown(2,dx,dy);g.pointerMove(2,dx+65,dy);
            if(!zoomFirst)g.pointerDown(7,slider.x+24,slider.centerY());
            check(g.pointer && g.overview.active && (joystick?g.joystick.active:g.holdingTug()),"zoom and drive both own their touch in either start order");
            float startX=g.tug.x,startY=g.tug.y;
            for(int i=0;i<24;i++) {g.pointerMove(7,slider.x+24+(slider.w-48)*i/23,slider.centerY());g.step();}
            check(g.overview.zoom()==OverviewMap.maxZoom(g) && !g.pinchZoom.active(),"zoom moves while drive remains held without main camera pinch");
            check(Physics.length(g.tug.x-startX,g.tug.y-startY)>1 && (joystick?g.joystick.active:g.holdingTug()),"tug keeps moving during map zoom");
            if(releaseZoomFirst) {
                if(zoomFirst)g.pointerCancel(7);else g.pointerUp(7,slider.x+slider.w-24,slider.centerY());
                check(!g.overview.active && g.pointer && (joystick?g.joystick.active:g.holdingTug()),"zoom release/cancel preserves drive");
                g.pointerCancel(2);
            } else {
                g.pointerCancel(2);
                check(g.overview.active && !g.pointer,"drive cancel preserves zoom touch");
                g.pointerMove(7,slider.x+24,slider.centerY());
                check(g.overview.zoom()==OverviewMap.MIN_ZOOM,"zoom still responds after driving ends");
                g.pointerUp(7,slider.x+24,slider.centerY());
            }
            check(!g.pointer && !g.overview.active && !g.joystick.active,"both inputs release independently");
            g.pointerDown(2,Joystick.centerX(g),Joystick.centerY(g));
            g.pointerDown(7,slider.centerX(),slider.centerY());g.background();
            check(!g.pointer && !g.overview.active && !g.joystick.active,"background cancels both touch owners");
        }
    }
    public static void main(String[] args){run();}
}
