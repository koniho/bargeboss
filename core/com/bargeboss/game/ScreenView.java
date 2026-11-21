package com.bargeboss.game;

/** Landscape harbor cabinet, with the same shared scene and controls on every host. */
public final class ScreenView {
    private static final int INK=VisualStyle.OUTLINE, NAVY=VisualStyle.NAVY, PAPER=PilotGlass.TEXT,
            GOLD=PilotGlass.AMBER, MUTED=PilotGlass.MUTED;
    private ScreenView() {}
    private static void text(Painter p,String s,float x,float y,float size,int c) { Lettering.draw(p,s,x,y,size,c); }
    private static void center(Painter p,String s,float x,float y,float size,int c) { Lettering.center(p,s,x,y,size,c); }
    public static void hud(Game g,Painter p) {
        float width=Math.min(720,g.layout.width-32),x=(g.layout.width-width)*.5f;
        String title=g.tutorial!=null?"PRACTICE - INDUSTRIAL HARBOR":g.challengeTitle();
        String subtitle;
        if(g.tutorial!=null) {
            subtitle=g.tutorial.entering?"DOCKSIDE RADIO / CHANNEL 08":"LESSON "+Math.min(Tutorial.DONE,g.tutorial.lesson+1)+" / "+Tutorial.DONE;
            if(g.trainingChannel())subtitle+=" / GATE "+g.tutorial.channelGate+" / "+(TrainingChannel.X.length-1);
        } else if(g.lockStage()) {
            boolean window=g.lock.phase==Lock.ENTRY || g.lock.phase==Lock.EXIT;
            subtitle=g.goalProgress()+" / "+g.quota()+" THROUGH LOCK / "+(window?
                    (g.lock.phase==Lock.ENTRY?"ENTRY CLOSES IN ":"EXIT CLOSES IN ")+g.lock.secondsRemaining()+"S":g.lock.signal());
            subtitle+=" / WATER "+Math.round(g.lock.waterLevel()*100)+"%"+(g.lock.phase==Lock.FILLING?" RISING":"");
        } else subtitle=g.goalProgress()+" / "+g.quota()+" / "+g.goalStatus();
        if(g.tutorial==null && g.hull<100)subtitle+=" / HULL "+g.hull+"%";
        PilotGlass.ribbon(p,x,8,width,48);
        center(p,title,g.layout.width*.5f,14,Math.min(16,(width-24)/Math.max(1,Lettering.width(title,1))),PAPER);
        center(p,subtitle,g.layout.width*.5f,38,Math.min(9,(width-24)/Math.max(1,Lettering.width(subtitle,1))),GOLD);
    }
    private static void titleSettings(Game g,Painter p) {
        Layout.Box setup=g.controlBox(Layout.SETTINGS);
        PilotGlass.button(p,setup.x,setup.y,setup.w,setup.h,false);
        center(p,"SETTINGS",setup.centerX(),setup.y+14,11,PAPER);
    }
    private static void titleAction(Painter p,Layout.Box b,String label,String detail,boolean primary){
        PilotGlass.button(p,b.x,b.y,b.w,b.h,primary);
        int ink=PilotGlass.buttonText(primary);
        text(p,label,b.x+20,b.y+17,22,ink);
        text(p,detail,b.x+20,b.y+48,10,primary?NAVY:MUTED);
        PilotGlass.chevron(p,b.x+b.w-22,b.centerY(),1,ink);
    }
    public static void title(Game g,Painter p) {
        TitleView.background(g,p);TitleView.voyage(g,p);titleSettings(g,p);
        p.sprite(SpriteAtlas.TITLE_LOGO,0,230,116,400,200,0,1);
        float x=g.layout.width-356,y=130+g.layout.offsetY;
        PilotGlass.panel(p,x,y,328,350,0xe8101c30);
        text(p,"YOUR VOYAGE",x+24,y+21,10,GOLD);
        String harbor=Challenges.locationTitle(g.challengeId);
        text(p,harbor,x+24,y+49,Math.min(24,284/Math.max(1,Lettering.width(harbor,1))),PAPER);
        text(p,goalSummary(g.challengeId),x+24,y+80,Math.min(10,284/Math.max(1,Lettering.width(goalSummary(g.challengeId),1))),MUTED);
        Layout.Box choose=g.controlBox(Layout.CHALLENGES);
        PilotGlass.button(p,choose.x,choose.y,choose.w,choose.h,false);
        text(p,"CHOOSE HARBOR",choose.x+16,choose.y+14,14,PAPER);
        PilotGlass.chevron(p,choose.x+choose.w-18,choose.centerY(),1,GOLD);
        text(p,"TIME OF DAY",x+24,y+167,9,MUTED);LightingChoices.draw(g,p);
        titleAction(p,g.controlBox(Layout.START),"SET SAIL","BEGIN JOBS AT THIS HARBOR",true);
        Layout.Box learn=g.controlBox(Layout.LEARN);
        text(p,"FIRST TIME AT THE HELM?",learn.x+2,learn.y-20,10,GOLD);
        titleAction(p,learn,"LEARN TO DRIVE","GUIDED LESSONS WITH YOUR INSTRUCTOR",false);
        PilotGlass.ribbon(p,28,g.layout.height-58,350,42);
        captainBadge(p,54,g.layout.height-37,.38f,g.career.stripes());
        text(p,g.career.rankTitle(),80,g.layout.height-49,14,PAPER);
        text(p,g.career.totalStars()+" STARS / CAPTAIN'S PROGRESS",80,g.layout.height-29,9,MUTED);
    }

    private static void harbor(Game g,Painter p) {
        int[] sky={0xff7467a8,0xff8c6ea7,0xffae7aa5,0xffd98d9c,0xfff5aa8f,0xffffc487};
        float horizon=312+g.layout.offsetY;
        for(int i=0;i<sky.length;i++) p.rect(0,i*horizon/6,g.layout.width,horizon/6+1,sky[i]);
        p.circle(550+g.layout.offsetX,258+g.layout.offsetY,65,0xffffd8a0);
        for(int i=0;i<(int)(g.layout.width/72)+2;i++) {
            float x=(i*83+g.visualTicks/35f)%(g.layout.width+140)-100,y=46+(i%4)*49;
            p.rect(x,y,42+i%3*17,9,0x66f4a8a0); p.rect(x+15,y-7,45,8,0x55ffc2a0);
        }
        for(int i=0;i<(int)(g.layout.width/38)+2;i++) {
            int x=i*38-10,h=35+(i*31)%111;
            p.rect(x,horizon+1-h,31,h,0xff555877); p.rect(x+5,horizon-8-h,20,9,0xff555877);
            if(i%4==0) p.line(x+15,horizon-8-h,x+15,horizon-32-h,2,0xff555877);
            for(int row=0;row<h/14;row++) for(int col=0;col<3;col++)
                if((row+col+i)%3==0) p.rect(x+5+col*8,horizon+10-h+row*13,3,4,0xffbd9b9c);
        }
        p.rect(0,horizon,g.layout.width,g.layout.height-horizon,VisualStyle.TEAL); p.rect(0,horizon,g.layout.width,6,0xfff1b682);
        for(int side=0;side<2;side++) {
            float x=side==0?5:g.layout.width-5,dir=side==0?1:-1;
            p.poly(new float[]{x,horizon+1,x+dir*146,horizon-34,x+dir*151,horizon-15,x+dir*4,horizon+20},0xff27364e);
            p.line(x,horizon-2,x+dir*146,horizon-37,6,0xffb08782);
            for(int i=1;i<7;i++) {
                float xx=x+dir*i*21,yy=horizon-2-i*5;
                p.line(xx,yy,xx+dir*14,yy+12,3,0xff727281);
                p.rect(xx-2,yy-6,4,6,GOLD); p.rect(xx-4,yy+24,8,2,0x88eebc70);
            }
        }
        for(int row=0;row<(g.layout.height-horizon)/17;row++) for(int col=0;col<g.layout.width/72+1;col++) {
            float x=col*72+(row%2)*23,y=horizon+14+row*17;
            float drift=(float)StrictMath.sin(g.visualTicks*.025+row*.73+col)*8;
            int color=col>5 && col<10 && row<10?0x99ffc276:row%3==0?0x88578aab:0x6657b8be;
            p.rect(x+drift,y,15+(row+col)%4*7,2+(row%2),color);
        }
        p.poly(new float[]{0,g.layout.height-70,73,g.layout.height-95,73,g.layout.height,0,g.layout.height},0xff1b2b40);
        p.poly(new float[]{g.layout.width,g.layout.height-73,g.layout.width-67,g.layout.height-96,g.layout.width-67,g.layout.height,g.layout.width,g.layout.height},0xff1b2b40);
        for(int i=0;i<3;i++) { p.rect(0,g.layout.height-59+i*22,45,10,0xff293b50); p.rect(g.layout.width-45,g.layout.height-63+i*22,45,10,0xff293b50); }
    }
    public static void help(Game g,Painter p) {
        p=menu(g,p);
        overlay(p,105,411);
        center(p,"CAPTAIN'S FIELD GUIDE",480,133,24,GOLD);
        instruction(p,1,185,"USE THE THROTTLE","Pull farther for thrust. Release to coast.");
        instruction(p,2,251,"DRAG THE CARGO","Press, drag and hold. Center for assist.");
        instruction(p,3,317,"AIM, THEN RELEASE","Drag cargo toward a bollard. Release.");
        instruction(p,4,383,"LINK A BARGE TRAIN","Drag cargo to tug or barge. Release to link.");
        text(p,"TAP THE CONNECTED BARGE TO LET GO.",159,447,10,GOLD);
        button(g,p,Layout.LEARN,"PRACTICE CONTROLS",false);
        button(g,p,Layout.START,"RESUME VOYAGE",true);
        center(p,"YOUR VOYAGE WAITS HERE.",770,231,12,MUTED);
        center(p,"COMPLETE THE GOAL TO ADVANCE.",770,257,11,MUTED);
    }
    private static void instruction(Painter p,int index,float y,String title,String detail) {
        p.circle(126,y+15,19,0xff26334a);center(p,""+index,126,y+6,20,GOLD);
        text(p,title,159,y,16,PAPER); text(p,detail,159,y+28,10,MUTED);
    }
    public static void settings(Game g,Painter p) {
        p.rect(0,0,g.layout.width,g.layout.height,NAVY);p=menu(g,p);
        PilotGlass.panel(p,180,96,600,436,0xf0101c30);
        text(p,"YOUR CONTROLS",212,124,28,PAPER);
        text(p,"MAKE THE HELM COMFORTABLE",212,162,10,MUTED);
        text(p,"THROTTLE SIDE",212,202,17,PAPER);
        text(p,"CHOOSE YOUR DRIVING HAND",212,228,10,MUTED);
        boolean left=g.settings.joystickSide()==GameSettings.LEFT;
        Layout.Box[] sides={Layout.JOYSTICK_LEFT,Layout.JOYSTICK_RIGHT};
        for(int i=0;i<2;i++){
            Layout.Box b=sides[i];boolean selected=i==0?left:!left;
            PilotGlass.segment(p,b.x,b.y,b.w,b.h,selected);
            center(p,i==0?"LEFT":"RIGHT",b.centerX()+5,b.y+20,14,selected?GOLD:PAPER);
        }
        p.line(212,264,736,264,1,PilotGlass.TRACK);
        text(p,"HAPTIC FEEDBACK",212,292,17,PAPER);
        text(p,"FEEL CONTACTS AND CONTROL CUES",212,319,10,MUTED);
        PilotGlass.toggle(p,672,292,g.hapticsEnabled);
        text(p,g.hapticsEnabled?"ON":"OFF",631,301,11,g.hapticsEnabled?GOLD:MUTED);
        text(p,"THROTTLE TO DRIVE. HOLD CARGO TO ASSIST.",212,375,11,MUTED);
        button(g,p,Layout.SETTINGS_BACK,"DONE",true);
    }
    public static void pause(Game g,Painter p) {
        p=menu(g,p);
        PilotGlass.panel(p,216,126,528,300,0xf0101c30);
        p.rect(244,155,6,25,GOLD);p.rect(256,155,6,25,GOLD);
        text(p,"VOYAGE PAUSED",280,153,26,PAPER);
        text(p,g.challengeTitle(),244,204,12,MUTED);
        button(g,p,Layout.RESUME,"RESUME VOYAGE",true);
        button(g,p,Layout.RESTART,"RESTART",false);
        button(g,p,Layout.TITLE_SCREEN,"BACK TO TITLE",false);
    }
    public static void finish(Game g,Painter p) {
        p=menu(g,p);overlay(p,82,454);
        boolean complete=g.goalComplete() && g.hull>0;
        text(p,g.hull<=0?"TUG OUT OF SERVICE":complete?"HARBOR COMPLETE":"TRY AGAIN, CAPTAIN",60,110,28,PAPER);
        text(p,g.challengeTitle(),60,151,12,MUTED);
        captainBadge(p,190,250,1.2f,g.career.stripes());
        center(p,g.career.rankTitle(),190,328,17,GOLD);
        center(p,g.rankPromoted?"NEW CAPTAIN STRIPES!":"CAPTAIN'S RANK",190,355,10,MUTED);
        for(int i=0;i<3;i++)star(p,410+i*72,213,24,g.earnedStars>i?GOLD:0xff465366);
        text(p,g.earnedStars==3?"GOLD STRIPES":g.earnedStars==2?"SILVER STRIPES":g.earnedStars==1?"BRONZE STRIPES":"KEEP PRACTICING",340,250,11,GOLD);
        metric(p,340,280,"GOAL",g.goalProgress()+" / "+g.quota());
        metric(p,505,280,"HULL",g.hull+"%");
        metric(p,670,280,"SCORE",""+g.score);
        text(p,"PERSONAL BEST  "+g.career.locationScore(g.challengeId),340,366,10,MUTED);
        boolean next=Challenges.nextStage(g.challengeId)>=0 && g.career.unlocked(Challenges.nextStage(g.challengeId));
        String note=g.lockStage() && g.lock.failed()?g.lock.failure():next?"NEXT HARBOR: "+Challenges.locationTitle(Challenges.nextStage(g.challengeId)):"REPLAY TO IMPROVE YOUR RATING";
        center(p,note,480,409,11,PAPER);
        button(g,p,Layout.FINISH_CHALLENGES,"CHOOSE HARBOR",false);
        button(g,p,Layout.AGAIN,"REPLAY",!next);
        if(next)button(g,p,Layout.NEXT_CHALLENGE,"NEXT HARBOR",true);
    }
    private static void metric(Painter p,float x,float y,String label,String value){
        p.rect(x,y,145,68,0x70070f1e);
        text(p,label,x+12,y+10,9,MUTED);text(p,value,x+12,y+31,24,PAPER);
    }
    private static String goalSummary(int id) {
        return Challenges.calumet(id)?"FOUR JOBS / INLET TO BASIN FIVE":Challenges.stageEntry(id)==Challenges.MONTROSE?"THREE JOBS / TOW AND RESCUE":"STEEL TOW / HARBOR AND CANAL";
    }
    /** Persistent career board; levels finish when their displayed goal is complete. */
    public static void challenges(Game g,Painter p) {
        harbor(g,p);p.rect(0,0,g.layout.width,g.layout.height,0xdd0b1728);
        Painter screen=p;p=centered(g,p);
        text(p,"CHOOSE YOUR HARBOR",60,40,30,PAPER);
        text(p,"ONE PLACE. CONNECTED JOBS. SAIL AT YOUR OWN PACE.",60,84,11,MUTED);
        for(int slot=0;slot<Layout.CHALLENGE_CARDS.length;slot++){
            int id=Challenges.menuId(g.challengePage*Challenges.PAGE_SIZE+slot);if(!Challenges.valid(id))continue;
            Layout.Box b=Layout.CHALLENGE_CARDS[slot];boolean open=g.career.unlocked(id),selected=Challenges.stageEntry(g.challengeId)==id;
            PilotGlass.panel(p,b.x,b.y,b.w,b.h,selected?0xf0202e43:0xe0101c30);
            if(selected)p.rect(b.x+18,b.y,70,3,GOLD);
            text(p,slot==0?"RIVER WORK":slot==1?"MARINA RESCUE":"INDUSTRIAL CANAL",b.x+20,b.y+22,10,GOLD);
            String name=Challenges.locationTitle(id);
            text(p,name,b.x+20,b.y+49,Math.min(23,220/Math.max(1,Lettering.width(name,1))),PAPER);
            int[] jobs=Challenges.locationJobs(id);int completed=0;for(int job:jobs)if(g.career.bestStars(job)>0)completed++;
            text(p,""+jobs.length,b.x+20,b.y+93,40,open?GOLD:MUTED);
            text(p,jobs.length==1?"CONNECTED JOB":"CONNECTED JOBS",b.x+62,b.y+105,12,PAPER);
            text(p,"ONE CONTINUOUS WATERWAY",b.x+62,b.y+126,8,MUTED);
            text(p,slot==0?"LAKE ENTRANCE TO BASIN FIVE":slot==1?"TOW, UNRAFT AND RESCUE":"HARBOR AND STEELWORKS",b.x+20,b.y+168,10,PAPER);
            text(p,completed+" / "+jobs.length+" JOBS COMPLETED",b.x+20,b.y+194,10,MUTED);
            p.rect(b.x+20,b.y+218,220,3,PilotGlass.TRACK);
            if(completed>0)p.rect(b.x+20,b.y+218,220f*completed/jobs.length,3,GOLD);
            if(open){
                if(selected){PilotGlass.tick(p,b.x+25,b.y+248,4,GOLD);text(p,"SELECTED",b.x+42,b.y+241,13,GOLD);}
                else{text(p,"CHOOSE HARBOR",b.x+20,b.y+241,13,PAPER);PilotGlass.chevron(p,b.x+b.w-24,b.y+249,1,PAPER);}
            }else{text(p,"EARN BRONZE TO UNLOCK",b.x+20,b.y+241,11,MUTED);}
        }
        center(p,"DEPARTING FROM "+Challenges.locationTitle(g.challengeId),480,439,12,PAPER);
        text(p,"TIME OF DAY",60,477,10,MUTED);LightingChoices.draw(g,screen);
        button(g,p,Layout.BACK,"BACK",false);button(g,p,Layout.CHALLENGE_START,"SET SAIL",true);
        if(Challenges.pageCount()>1){
            if(g.challengePage>0)button(g,p,Layout.CHALLENGE_PREV_PAGE,"PREV",false);
            if(g.challengePage+1<Challenges.pageCount())button(g,p,Layout.CHALLENGE_NEXT_PAGE,"NEXT",false);
        }
    }
    private static void lock(Painter p,float x,float y) {
        ControlView.ring(p,x,y-6,13,1,0xff718092,5);
        PilotGlass.panel(p,x-19,y-5,38,27,0xff43566a);
        p.circle(x,y+7,3,GOLD); p.rect(x-1.5f,y+7,3,7,GOLD);
    }
    /** A tiny uniform badge: a cheerful duck above braided gold captain bands. */
    private static void captainBadge(Painter p,float x,float y,float scale,int stripes) {
        p=new Transform(p,x,y,scale,0);
        p.poly(new float[]{-38,-45,38,-45,35,29,0,47,-35,29},INK);
        p.poly(new float[]{-34,-41,34,-41,31,26,0,42,-31,26},GOLD);
        p.poly(new float[]{-30,-37,30,-37,27,24,0,37,-27,24},NAVY);
        p.circle(-5,-18,12,PAPER); p.circle(7,-25,9,PAPER);
        p.poly(new float[]{14,-28,25,-23,14,-19},0xfff39b38);
        p.circle(10,-27,1.8f,INK);
        p.poly(new float[]{-17,-20,-24,-26,-21,-14,-9,-7,7,-9,13,-17},PAPER);
        p.rect(-2,-37,18,5,PAPER); p.rect(-3,-32,20,3,INK); p.rect(4,-37,5,3,GOLD);
        if(stripes==0) { p.circle(0,15,8,0xff617287); p.circle(0,15,4,NAVY); }
        for(int i=0;i<stripes;i++) {
            float yy=1+i*7;
            p.rect(-23,yy,46,4,GOLD); p.rect(-23,yy,46,1,0xffffec96);
            for(int j=0;j<8;j++) p.line(-22+j*6,yy+4,-18+j*6,yy,1,0xffb17a27);
        }
    }
    public static void star(Painter p,float x,float y,float r,int color) {
        float[] points=new float[20];
        for(int i=0;i<10;i++) {
            double a=-Math.PI/2+i*Math.PI/5; float rr=i%2==0?r:r*.45f;
            points[i*2]=x+(float)StrictMath.cos(a)*rr; points[i*2+1]=y+(float)StrictMath.sin(a)*rr;
        }
        float[] shadow=points.clone(); for(int i=1;i<shadow.length;i+=2) shadow[i]+=4;
        p.poly(shadow,INK); p.poly(points,color);
        p.line(x,y-r+5,x-r*.34f,y-r*.05f,2,color==GOLD?0xffffef96:0xff647489);
    }
    public static void overlay(Painter p,float top,float height) {
        p.rect(0,Layout.HEADER_HEIGHT,Layout.WIDTH,Layout.HEIGHT-Layout.HEADER_HEIGHT,0xcc07101f);
        PilotGlass.panel(p,28,top,904,height,0xf0101c30);
        p.line(56,top+7,904,top+7,1,0x80ffc52f);
    }
    public static void button(Game g,Painter p,Layout.Box box,String label,boolean primary) { button(g,p,box.x,box.y,box.w,box.h,label,primary); }
    public static void button(Game g,Painter p,float x,float y,float w,float h,String label,boolean primary) {
        PilotGlass.button(p,x,y,w,h,primary);
        float size=Math.min(label.length()>18?13:16,(w-58)/Math.max(1,Lettering.width(label,1)));
        center(p,label,x+w/2-(primary && w>=220?8:0),y+(h-size)/2,size,PilotGlass.buttonText(primary));
        if(primary && w>=220)PilotGlass.chevron(p,x+w-24,y+h*.5f,1,NAVY);
    }

    public static Painter scaled(Painter p,float x,float y,float scale) { return new Transform(p,x,y,scale,0); }
    public static Painter offset(Painter p,float x,float y) { return x==0 && y==0?p:new Transform(p,x,y,1,0); }
    public static Painter centered(Game g,Painter p) { return offset(p,g.layout.offsetX,g.layout.offsetY); }
    public static Painter menu(Game g,Painter p) {
        p.rect(0,0,g.layout.width,g.layout.height,0x8807101f);
        return centered(g,p);
    }
    /** Scale/shear for vector lettering; atlas sprites retain an unskewed projection. */
    private static final class Transform implements Painter {
        public LightingProfile lighting(){return p.lighting();}
        private final Painter p; private final float x,y,scale,shear;
        Transform(Painter p,float x,float y,float scale,float shear) { this.p=p; this.x=x; this.y=y; this.scale=scale; this.shear=shear; }
        private float tx(float xx,float yy) { return x+(xx+shear*yy)*scale; }
        private float ty(float yy) { return y+yy*scale; }
        public void pushClipOutside(float[] a) {
            float[] b=new float[a.length];for(int i=0;i<a.length;i+=2){b[i]=tx(a[i],a[i+1]);b[i+1]=ty(a[i+1]);}p.pushClipOutside(b);
        }
        public void popClip(){p.popClip();}
        public void rect(float xx,float yy,float w,float h,int c) {
            if(shear!=0) { xx-=.3f; yy-=.3f; w+=.6f; h+=.6f; }
            poly(new float[]{xx,yy,xx+w,yy,xx+w,yy+h,xx,yy+h},c);
        }
        public void circle(float xx,float yy,float r,int c) { p.circle(tx(xx,yy),ty(yy),r*scale,c); }
        public void poly(float[] a,int c) { float[] b=new float[a.length]; for(int i=0;i<a.length;i+=2) { b[i]=tx(a[i],a[i+1]); b[i+1]=ty(a[i+1]); } p.poly(b,c); }
        public void line(float xx,float yy,float bx,float by,float w,int c) { p.line(tx(xx,yy),ty(yy),tx(bx,by),ty(by),w*scale,c); }
        public boolean supportsUnionClips(){return p.supportsUnionClips();}
        public boolean prefersPolygonBatches(){return p.prefersPolygonBatches();}
        public boolean retainedSurfaces(StaticLayer layer,float xx,float yy,float size){return shear==0 && p.retainedSurfaces(layer,x+xx*scale,y+yy*scale,size*scale);}
        public boolean preparesLayers(){return shear==0 && p.preparesLayers();}
        public void prepareLayer(StaticLayer layer,float size){if(shear==0)p.prepareLayer(layer,size*scale);}
        public void layer(StaticLayer layer,float xx,float yy,float size,float alpha) {
            if(shear==0)p.layer(layer,x+xx*scale,y+yy*scale,size*scale,alpha);
            else layer.draw(this,xx,yy,size,alpha);
        }
        public void beginSurfaceBatch(){p.beginSurfaceBatch();}
        public void endSurfaceBatch(){p.endSurfaceBatch();}
        public void beginWaterFoamBatch(){p.beginWaterFoamBatch();}
        public void endWaterFoamBatch(){p.endWaterFoamBatch();}
        public void waterFoam(float xx,float yy,float ax,float ay,float bx,float by,float phase,int kind,float alpha){p.waterFoam(tx(xx,yy),ty(yy),(ax+shear*ay)*scale,ay*scale,(bx+shear*by)*scale,by*scale,phase,kind,alpha);}
        public void shoreline(float[] xy,float[] uv,float phase,float energy,int kind,int color){
            float[] projected=new float[xy.length];for(int i=0;i<xy.length;i+=2){projected[i]=tx(xy[i],xy[i+1]);projected[i+1]=ty(xy[i+1]);}
            p.shoreline(projected,uv,phase,energy,kind,color);
        }
    public void surfacePolygon(float[] xy,float[] uv,int material,int color,float wetness){
            float[] projected=new float[xy.length];for(int i=0;i<xy.length;i+=2){projected[i]=tx(xy[i],xy[i+1]);projected[i+1]=ty(xy[i+1]);}
            p.surfacePolygon(projected,uv,material,color,wetness);
        }
    public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){p.material(tx(x,y),ty(y),(ax+shear*ay)*scale,ay*scale,(bx+shear*by)*scale,by*scale,kind,detail,amount,color);}
    public void waterReflection(float x,float y,float ax,float ay,float bx,float by,float phase,int color){p.waterReflection(tx(x,y),ty(y),(ax+shear*ay)*scale,ay*scale,(bx+shear*by)*scale,by*scale,phase,color);}
    public void ropeSegment(float x,float y,float xx,float yy,float width,float from,float to,int color){p.ropeSegment(tx(x,y),ty(y),tx(xx,yy),ty(yy),width*scale,from,to,color);}
    public void smokePuff(float x,float y,float radius,float phase,int color){p.smokePuff(tx(x,y),ty(y),radius*scale,phase,color);}
    public void spriteAffine(int a,int f,float xx,float yy,float ax,float ay,float bx,float by,float alpha){p.spriteAffine(a,f,tx(xx,yy),ty(yy),(ax+shear*ay)*scale,ay*scale,(bx+shear*by)*scale,by*scale,alpha);}
        public void sprite(int atlas,int frame,float cx,float cy,float width,float height,float rotation,float alpha) {
            p.sprite(atlas,frame,x+cx*scale,y+cy*scale,width*scale,height*scale,rotation,alpha);
        }
        public void spriteBlend(int atlas,int frameA,int frameB,float cx,float cy,float width,float height,float rotation,float blend,float alpha) {
            p.spriteBlend(atlas,frameA,frameB,x+cx*scale,y+cy*scale,width*scale,height*scale,rotation,blend,alpha);
        }
    }
}
