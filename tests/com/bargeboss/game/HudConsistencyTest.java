package com.bargeboss.game;
/** Every playable stage shows the same HUD: overview map and chart zoom, developer panel button (when
 * enabled), objective ribbon, settings and joystick. Guards against new stages drifting. */
public final class HudConsistencyTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    /** Counts painter calls inside a screen rectangle, so a HUD element can be proven drawn. */
    private static final class Probe implements Painter {
        final float x0,y0,x1,y1;int hits;
        Probe(Layout.Box b){x0=b.x;y0=b.y;x1=b.x+b.w;y1=b.y+b.h;}
        private void at(float x,float y){if(x>=x0 && x<=x1 && y>=y0 && y<=y1)hits++;}
        public void rect(float x,float y,float w,float h,int c){at(x+w*.5f,y+h*.5f);}
        public void circle(float x,float y,float r,int c){at(x,y);}
        public void poly(float[] xy,int c){if(xy.length>=2)at(xy[0],xy[1]);}
        public void line(float x,float y,float xx,float yy,float w,int c){at((x+xx)*.5f,(y+yy)*.5f);}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){at(x,y);}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float m,float alpha){at(x,y);}
    }
    private static int drawnIn(Game root,Layout.Box box){Probe p=new Probe(box);root.render(p);return p.hits;}
    public static void run(){
        int stages=0;
        for(int location=0;location<Challenges.LOCATION_COUNT;location++){
            int entry=Challenges.menuId(location);
            for(int job:Challenges.locationJobs(entry)){
                Game root=new Game();ReplayInput.veteran(root);root.configureCanvas(1333,750);root.settings.developerTools=true;
                root.challengeId=entry;root.reset();root.mode=Game.PLAY;root.dialogue.close(root);
                if(job!=entry){root.startDeveloperMission(job);}
                Game g=root.activeGame();g.mode=Game.PLAY;g.dialogue.close(g);if(g.montroseStage())g.montroseArrival.clear();
                for(int t=0;t<5;t++)g.step();g.dialogue.close(g);
                String name=Challenges.title(job);
                check(OverviewMap.visible(g),"overview map is shown on "+name);
                check(drawnIn(root,OverviewMap.bounds(g))>4,"overview map draws on "+name);
                check(drawnIn(root,OverviewMap.sliderBounds(g))>2,"chart zoom draws on "+name);
                check(drawnIn(root,new Layout.Box(12,DeveloperPanel.BUTTON_Y,160,36))>0,"developer panel button draws on "+name);
                check(drawnIn(root,new Layout.Box(g.layout.width*.5f-200,8,400,48))>2,"objective ribbon draws on "+name);
                check(drawnIn(root,new Layout.Box(Joystick.centerX(g)-60,Joystick.centerY(g)-60,120,120))>2,"joystick draws on "+name);
                stages++;
            }
        }
        System.out.println(stages+" stages share the same HUD: map, chart zoom, dev panel, objective ribbon, joystick");
    }
    public static void main(String[] a){run();}
}
