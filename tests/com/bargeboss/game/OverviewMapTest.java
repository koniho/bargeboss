package com.bargeboss.game;

/** Navigation orientation, overlay layout, vector-only chart and control cue regressions. */
final class OverviewMapTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static final class Ink implements Painter {
        boolean chartOnly;int lines,fills;float x,y,xx,yy;
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
        public void rect(float x,float y,float w,float h,int c){if(chartOnly)throw new AssertionError("chart has no filled panel or terrain");}
        public void circle(float x,float y,float r,int c){}
        public void poly(float[] xy,int c){if(chartOnly){check((c>>>24)>0 && (c>>>24)<255,"water fill is more translucent than chart contours");fills++;}}
        public void material(float x,float y,float ax,float ay,float bx,float by,int k,float d,float v,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){if(chartOnly)check((c>>>24)==255,"chart strokes are opaque before group translucency");if(lines++==0){this.x=x;this.y=y;this.xx=xx;this.yy=yy;}check(Float.isFinite(x)&&Float.isFinite(y)&&Float.isFinite(xx)&&Float.isFinite(yy),"finite lines");}
    }
    static int arrows(Game g,Ink p){int count=0;WorldArrows.begin(g);try{while(WorldArrows.pending()){WorldArrows.draw(p);count++;}}finally{WorldArrows.end();}return count;}
    static void run(){
        Game g=new Game();ReplayInput.controlFixture(g);
        for(int stage=0;stage<4;stage++){
            OverviewMap.Chart c=new OverviewMap.Chart(stage);float x=(c.minX+c.maxX)*.5f,y=(c.minY+c.maxY)*.5f;
            check(c.y(x+10)<c.y(x),"isometric upper-right becomes map up");
            check(c.x(y+10)>c.x(y),"isometric lower-right becomes map right");
            check(Math.abs((c.x(y+10)-c.x(y))-(c.y(x)-c.y(x+10)))<.001f,"overhead projection has equal axis scale");
            float[] actual=stage==1?PolygonWater.bounds(MontroseHarbor.PLAY_BOUNDS):stage==2?new float[]{IndianaHarbor.MIN_X,IndianaHarbor.MIN_Y,IndianaHarbor.MAX_X,IndianaHarbor.MAX_Y}:stage==3?new float[]{CalumetRiver.MIN_X,CalumetRiver.MIN_Y,CalumetRiver.MAX_X,CalumetRiver.MAX_Y}:PolygonWater.bounds(TrainingChannel.WATER_BOUNDARY);
            check(c.minX==actual[0]-c.margin && c.minY==actual[1]-c.margin && c.maxX==actual[2]+c.margin && c.maxY==actual[3]+c.margin,"overview uses authoritative play extents");
            check(c.boundary[0]==c.x(c.minY) && c.boundary[1]==c.y(c.maxX) && c.boundary[4]==c.x(c.maxY) && c.boundary[5]==c.y(c.minX),"border and world corners share one projection");
            check(stage==0?c.enclosed && c.margin>0 && c.x(actual[1])>c.boundary[0] && c.y(actual[2])>c.boundary[1]:!c.enclosed && c.margin==0,"only land-enclosed maps receive extra margin");
            Ink ink=new Ink();ink.chartOnly=true;c.paint(ink);check(ink.lines>8 && ink.fills>0,"chart has shoreline contours and water fill");
        }
        for(float width:new float[]{960,1243,1440})for(int side:new int[]{GameSettings.LEFT,GameSettings.RIGHT,GameSettings.OFF}){
            g.configureCanvas(width,600);g.settings.setJoystickSide(side);Layout.Box b=OverviewMap.bounds(g);
            check(side==GameSettings.RIGHT?b.centerX()<width/2:b.centerX()>width/2,"overview opposite throttle");
            check(b.y>64 && b.y+b.h<552 && b.w==b.h,"square overview clears top and bottom HUD");

        }
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
    public static void main(String[] args){run();}
}
